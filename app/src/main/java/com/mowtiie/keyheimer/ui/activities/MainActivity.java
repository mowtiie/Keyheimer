package com.mowtiie.keyheimer.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.databinding.ActivityMainBinding;
import com.mowtiie.keyheimer.scheduling.ReminderScheduler;
import com.mowtiie.keyheimer.ui.adapters.SecretAdapter;
import com.mowtiie.keyheimer.ui.dialogs.VerifySecretBottomSheet;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.AppLockManager;
import com.mowtiie.keyheimer.util.CrashReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends KeyheimerActivity implements SecretAdapter.Listener {

    public static final String EXTRA_VERIFY_SECRET_ID = "verify_secret_id";

    private enum SortOption {
        NAME_ASC, NAME_DESC, UPDATED_DESC, UPDATED_ASC, CREATED_DESC, CREATED_ASC
    }

    private ActivityMainBinding binding;
    private SecretDao dao;
    private SecretAdapter adapter;
    private List<Secret> allSecrets = new ArrayList<>();
    private String currentQuery = "";
    private SortOption currentSort = SortOption.NAME_ASC;

    private final ActivityResultLauncher<Intent> saveCrashLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                        android.net.Uri uri = result.getData().getData();
                        if (uri == null) return;

                        if (CrashReporter.writeReportToUri(this, uri)) {
                            Toast.makeText(this, R.string.toast_crash_save_success, Toast.LENGTH_SHORT).show();
                            CrashReporter.deleteReport(this);
                        } else {
                            Toast.makeText(this, R.string.toast_crash_save_failure, Toast.LENGTH_SHORT).show();
                        }
                    });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        dao = new SecretDao(this);

        binding.recyclerSecrets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SecretAdapter(this);
        binding.recyclerSecrets.setAdapter(adapter);

        binding.fabAddSecret.setOnClickListener(v -> startActivity(new Intent(this, AddEditSecretActivity.class)));

        setUpEdgeToEdgeInsets();
        requestNotificationPermissionIfNeeded();
        requestExactAlarmPermissionIfNeeded();
        handleVerifyIntent(getIntent());

        if (savedInstanceState == null) {
            CrashReporter.showDialogIfPending(this, saveCrashLauncher);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleVerifyIntent(intent);
    }

    private void handleVerifyIntent(Intent intent) {
        String secretId = intent.getStringExtra(EXTRA_VERIFY_SECRET_ID);
        if (secretId != null) {
            showVerifySheet(secretId);
        }
    }

    private void showVerifySheet(String secretId) {
        VerifySecretBottomSheet.newInstance(secretId)
                .show(getSupportFragmentManager(), "verify_secret");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ReminderScheduler.canScheduleExactAlarms(this)) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_alarm)
                .setTitle(R.string.exact_alarm_permission_title)
                .setMessage(R.string.exact_alarm_permission_message)
                .setPositiveButton(R.string.exact_alarm_permission_positive, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.exact_alarm_permission_negative, null)
                .show();
    }

    private void setUpEdgeToEdgeInsets() {
        int listBasePadding = getResources().getDimensionPixelSize(R.dimen.list_bottom_padding);
        int fabBaseMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        FloatingActionButton fab = binding.fabAddSecret;

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            binding.appBarLayout.setPadding(bars.left, bars.top, bars.right, 0);

            binding.recyclerSecrets.setPadding(bars.left, 0, bars.right, listBasePadding + bars.bottom);

            ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            fabParams.leftMargin = fabBaseMargin + bars.left;
            fabParams.rightMargin = fabBaseMargin + bars.right;
            fabParams.bottomMargin = fabBaseMargin + bars.bottom;
            fab.setLayoutParams(fabParams);

            return windowInsets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSecrets();
        invalidateOptionsMenu();
    }

    private void loadSecrets() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Secret> secrets = dao.getAll();
            AppExecutors.getInstance().mainThread(() -> {
                allSecrets = secrets;
                applyFilterAndSort();
            });
        });
    }

    private void applyFilterAndSort() {
        List<Secret> visible = new ArrayList<>();
        String query = currentQuery.trim().toLowerCase(Locale.getDefault());
        for (Secret secret : allSecrets) {
            if (query.isEmpty() || secret.getName().toLowerCase(Locale.getDefault()).contains(query)) {
                visible.add(secret);
            }
        }
        sortSecrets(visible);

        adapter.submitList(visible);

        if (visible.isEmpty()) {
            binding.textEmptyState.setText(
                    allSecrets.isEmpty() ? R.string.empty_state_message : R.string.search_empty_state);
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            binding.emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private void sortSecrets(List<Secret> secrets) {
        switch (currentSort) {
            case NAME_ASC:
                secrets.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case NAME_DESC:
                secrets.sort((a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                break;
            case UPDATED_DESC:
                secrets.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
                break;
            case UPDATED_ASC:
                secrets.sort((a, b) -> Long.compare(a.getUpdatedAt(), b.getUpdatedAt()));
                break;
            case CREATED_DESC:
                secrets.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            case CREATED_ASC:
                secrets.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                applyFilterAndSort();
                return true;
            }
        });

        menu.findItem(sortMenuItemId(currentSort)).setChecked(true);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem lockNowItem = menu.findItem(R.id.action_lock_now);
        if (lockNowItem != null) {
            lockNowItem.setVisible(AppLockManager.getInstance().isLockConfigured(this));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_lock_now) {
            AppLockManager.getInstance().markLocked();
            startActivity(new Intent(this, LockActivity.class));
            return true;
        }
        SortOption selected = sortOptionForMenuItem(item.getItemId());
        if (selected != null) {
            currentSort = selected;
            item.setChecked(true);
            applyFilterAndSort();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int sortMenuItemId(SortOption option) {
        switch (option) {
            case NAME_DESC:
                return R.id.sort_name_desc;
            case UPDATED_DESC:
                return R.id.sort_updated_desc;
            case UPDATED_ASC:
                return R.id.sort_updated_asc;
            case CREATED_DESC:
                return R.id.sort_created_desc;
            case CREATED_ASC:
                return R.id.sort_created_asc;
            case NAME_ASC:
            default:
                return R.id.sort_name_asc;
        }
    }

    private SortOption sortOptionForMenuItem(int itemId) {
        if (itemId == R.id.sort_name_asc) {
            return SortOption.NAME_ASC;
        } else if (itemId == R.id.sort_name_desc) {
            return SortOption.NAME_DESC;
        } else if (itemId == R.id.sort_updated_desc) {
            return SortOption.UPDATED_DESC;
        } else if (itemId == R.id.sort_updated_asc) {
            return SortOption.UPDATED_ASC;
        } else if (itemId == R.id.sort_created_desc) {
            return SortOption.CREATED_DESC;
        } else if (itemId == R.id.sort_created_asc) {
            return SortOption.CREATED_ASC;
        }
        return null;
    }

    @Override
    public void onSecretClicked(Secret secret) {
        Intent intent = new Intent(this, AddEditSecretActivity.class);
        intent.putExtra(AddEditSecretActivity.EXTRA_SECRET_ID, secret.getId());
        startActivity(intent);
    }

    @Override
    public void onVerifyNowClicked(Secret secret) {
        showVerifySheet(secret.getId());
    }
}