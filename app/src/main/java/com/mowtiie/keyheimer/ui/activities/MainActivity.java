package com.mowtiie.keyheimer.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.databinding.ActivityMainBinding;
import com.mowtiie.keyheimer.ui.adapters.SecretAdapter;
import com.mowtiie.keyheimer.util.AppExecutors;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SecretAdapter.Listener {

    private ActivityMainBinding binding;
    private SecretDao dao;
    private SecretAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        dao = new SecretDao(this);

        binding.recyclerSecrets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SecretAdapter(this);
        binding.recyclerSecrets.setAdapter(adapter);

        setUpEdgeToEdgeInsets();
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
    }

    @Override
    public void onVerifyNowClicked(Secret secret) {
    }
}