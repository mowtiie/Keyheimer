package com.mowtiie.keyheimer.ui.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
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
import com.mowtiie.keyheimer.util.IntervalConverter;
import com.mowtiie.keyheimer.util.PreferenceKeys;
import com.mowtiie.keyheimer.util.SecurityScreenUtil;

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

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    private final ActivityResultLauncher<Intent> crashSaveLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Uri uri = result.getData() == null ? null : result.getData().getData();
                if (result.getResultCode() != RESULT_OK || uri == null) {
                    return;
                }
                Context appContext = getApplicationContext();
                AppExecutors.getInstance().diskIO().execute(() -> {
                    boolean saved = CrashReporter.writeReportToUri(appContext, uri);
                    if (saved) {
                        CrashReporter.deleteReport(appContext);
                    }
                    AppExecutors.getInstance().mainThread(() -> Toast.makeText(appContext,
                            saved ? R.string.toast_crash_save_success : R.string.toast_crash_save_failure,
                            Toast.LENGTH_SHORT).show());
                });
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
        new ItemTouchHelper(new SwipeCallback()).attachToRecyclerView(binding.recyclerSecrets);

        binding.fabAddSecret.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditSecretActivity.class)));

        setUpEdgeToEdgeInsets();
        requestNotificationPermissionIfNeeded();
        requestExactAlarmPermissionIfNeeded();
        handleVerifyIntent(getIntent());
        CrashReporter.showDialogIfPending(this, crashSaveLauncher);
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ReminderScheduler.canScheduleExactAlarms(this)
                || prefs.getBoolean(PreferenceKeys.KEY_EXACT_ALARM_PROMPT_DISMISSED, false)) {
            return;
        }
        prefs.edit().putBoolean(PreferenceKeys.KEY_EXACT_ALARM_PROMPT_DISMISSED, true).apply();
        SecurityScreenUtil.show(this, new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_alarm)
                .setTitle(R.string.dialog_title_request_alarm_permission)
                .setMessage(R.string.dialog_message_request_alarm_permission)
                .setPositiveButton(R.string.dialog_button_grant, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.dialog_button_dismiss, null));
    }

    private void setUpEdgeToEdgeInsets() {
        int listBasePadding = getResources().getDimensionPixelSize(R.dimen.list_bottom_padding);
        int fabBaseMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        FloatingActionButton fab = binding.fabAddSecret;

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            binding.appBarLayout.setPadding(bars.left, bars.top, bars.right, 0);

            binding.recyclerSecrets.setPadding(bars.left, 0, bars.right, listBasePadding + bars.bottom);

            ViewGroup.MarginLayoutParams fabParams =
                    (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
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
                    allSecrets.isEmpty() ? R.string.empty_indicator_main : R.string.empty_indicator_search);
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

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.menu_item_search_hint));
        if (!currentQuery.isEmpty()) {
            searchItem.expandActionView();
            searchView.setQuery(currentQuery, false);
        }
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
            AppLockManager.getInstance().forceLock();
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

    private void deleteSecretWithUndo(Secret secret) {
        String secretId = secret.getId();
        ReminderScheduler.cancelReminder(this, secretId);
        AppExecutors.getInstance().diskIO().execute(() -> {
            dao.delete(secretId);
            AppExecutors.getInstance().mainThread(() -> {
                loadSecrets();
                showUndoSnackbar(secret);
            });
        });
    }

    private void showUndoSnackbar(Secret secret) {
        Snackbar.make(binding.mainRoot,
                        getString(R.string.snackbar_secret_deleted, secret.getName()), Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_undo, v -> restoreSecret(secret))
                .show();
    }

    private void restoreSecret(Secret secret) {
        if (secret.isActive() && secret.getNextTriggerAt() < System.currentTimeMillis()) {
            secret.setNextTriggerAt(IntervalConverter.computeNextTriggerAt(
                    secret.getIntervalValue(), secret.getIntervalUnit(),
                    secret.getReminderHour(), secret.getReminderMinute()));
        }
        AppExecutors.getInstance().diskIO().execute(() -> {
            boolean restored = dao.restore(secret);
            AppExecutors.getInstance().mainThread(() -> {
                if (!restored) {
                    Toast.makeText(this, R.string.toast_secret_save_failed, Toast.LENGTH_SHORT).show();
                } else if (secret.isActive()) {
                    ReminderScheduler.scheduleReminder(this, secret);
                }
                loadSecrets();
            });
        });
    }

    private void togglePauseResume(Secret secret) {
        String secretId = secret.getId();
        boolean activate = !secret.isActive();
        AppExecutors.getInstance().diskIO().execute(() -> {
            Secret current = dao.getById(secretId);
            Secret updated = null;
            if (current != null) {
                if (activate) {
                    dao.activate(secretId, IntervalConverter.computeNextTriggerAt(
                            current.getIntervalValue(), current.getIntervalUnit(),
                            current.getReminderHour(), current.getReminderMinute()));
                } else {
                    dao.setActive(secretId, false);
                }
                updated = dao.getById(secretId);
            }
            Secret result = updated;
            AppExecutors.getInstance().mainThread(() -> {
                if (result != null && result.isActive()) {
                    ReminderScheduler.scheduleReminder(this, result);
                } else {
                    ReminderScheduler.cancelReminder(this, secretId);
                }
                loadSecrets();
            });
        });
    }

    private Drawable tintedIcon(int drawableRes, int color) {
        Drawable base = ContextCompat.getDrawable(this, drawableRes);
        Drawable icon = DrawableCompat.wrap(base.mutate());
        DrawableCompat.setTint(icon, color);
        return icon;
    }

    private final class SwipeCallback extends ItemTouchHelper.SimpleCallback {

        private final ColorDrawable deleteBackground;
        private final ColorDrawable toggleBackground;
        private final Drawable deleteIcon;
        private final Drawable pauseIcon;
        private final Drawable playIcon;
        private final int iconMargin;

        SwipeCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            deleteBackground = new ColorDrawable(MaterialColors.getColor(
                    MainActivity.this, androidx.appcompat.R.attr.colorError, Color.RED));
            toggleBackground = new ColorDrawable(MaterialColors.getColor(
                    MainActivity.this, androidx.appcompat.R.attr.colorPrimary, Color.DKGRAY));
            int onError = MaterialColors.getColor(
                    MainActivity.this, com.google.android.material.R.attr.colorOnError, Color.WHITE);
            int onPrimary = MaterialColors.getColor(
                    MainActivity.this, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);

            deleteIcon = tintedIcon(R.drawable.ic_delete, onError);
            pauseIcon = tintedIcon(R.drawable.ic_pause, onPrimary);
            playIcon = tintedIcon(R.drawable.ic_play_arrow, onPrimary);
            iconMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            Secret secret = adapter.getSecretAt(position);
            if (direction == ItemTouchHelper.LEFT) {
                deleteSecretWithUndo(secret);
            } else {
                togglePauseResume(secret);
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            int top = itemView.getTop();
            int bottom = itemView.getBottom();
            int iconSize = bottom - top - 2 * iconMargin;

            int position = viewHolder.getBindingAdapterPosition();
            if (dX > 0 && position != RecyclerView.NO_POSITION) {
                toggleBackground.setBounds(itemView.getLeft(), top, itemView.getLeft() + (int) dX, bottom);
                toggleBackground.draw(c);

                Secret secret = adapter.getSecretAt(position);
                Drawable icon = secret.isActive() ? pauseIcon : playIcon;
                int left = itemView.getLeft() + iconMargin;
                icon.setBounds(left, top + iconMargin, left + iconSize, bottom - iconMargin);
                icon.draw(c);
            } else if (dX < 0) {
                deleteBackground.setBounds(itemView.getRight() + (int) dX, top, itemView.getRight(), bottom);
                deleteBackground.draw(c);

                int right = itemView.getRight() - iconMargin;
                deleteIcon.setBounds(right - iconSize, top + iconMargin, right, bottom - iconMargin);
                deleteIcon.draw(c);
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}