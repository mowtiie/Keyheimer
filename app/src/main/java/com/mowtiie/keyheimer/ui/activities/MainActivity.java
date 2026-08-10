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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.ui.adapters.SecretAdapter;
import com.mowtiie.keyheimer.util.AppExecutors;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SecretAdapter.Listener {

    private SecretDao dao;
    private SecretAdapter adapter;
    private View emptyStateContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dao = new SecretDao(this);
        emptyStateContainer = findViewById(R.id.empty_state_container);

        RecyclerView recyclerView = findViewById(R.id.recycler_secrets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SecretAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_secret);

        setUpEdgeToEdgeInsets(recyclerView, fab);
    }

    private void setUpEdgeToEdgeInsets(RecyclerView recyclerView, FloatingActionButton fab) {
        View rootView = findViewById(R.id.main_root);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);

        int listBasePadding = getResources().getDimensionPixelSize(R.dimen.list_bottom_padding);
        int fabBaseMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            appBarLayout.setPadding(bars.left, bars.top, bars.right, 0);

            recyclerView.setPadding(bars.left, 0, bars.right, listBasePadding + bars.bottom);

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
                emptyStateContainer.setVisibility(secrets.isEmpty() ? View.VISIBLE : View.GONE);
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