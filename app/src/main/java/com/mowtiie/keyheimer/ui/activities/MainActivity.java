package com.mowtiie.keyheimer.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.databinding.ActivityMainBinding;
import com.mowtiie.keyheimer.ui.adapters.SecretAdapter;
import com.mowtiie.keyheimer.ui.dialogs.VerifySecretBottomSheet;
import com.mowtiie.keyheimer.util.AppExecutors;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SecretAdapter.Listener {

    public static final String EXTRA_VERIFY_SECRET_ID = "verify_secret_id";

    private ActivityMainBinding binding;
    private SecretDao dao;
    private SecretAdapter adapter;

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
        handleVerifyIntent(getIntent());
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
        VerifySecretBottomSheet.newInstance(secretId).show(getSupportFragmentManager(), "verify_secret");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
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
    }

    private void loadSecrets() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Secret> secrets = dao.getAll();
            AppExecutors.getInstance().mainThread(() -> {
                adapter.submitList(secrets);
                binding.emptyStateContainer.setVisibility(secrets.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
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