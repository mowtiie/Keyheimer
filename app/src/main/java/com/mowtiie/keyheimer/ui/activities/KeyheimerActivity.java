package com.mowtiie.keyheimer.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.mowtiie.keyheimer.util.AppLockManager;
import com.mowtiie.keyheimer.util.SecurityScreenUtil;
import com.mowtiie.keyheimer.util.ThemeUtil;

public abstract class KeyheimerActivity extends AppCompatActivity {

    private int appliedContrastOverlay;
    private boolean appliedDynamicColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.applyNightMode(prefs());
        super.onCreate(savedInstanceState);

        appliedContrastOverlay = ThemeUtil.resolveContrastThemeOverlay(prefs());
        if (appliedContrastOverlay != 0) {
            setTheme(appliedContrastOverlay);
        }

        appliedDynamicColor = ThemeUtil.isDynamicColorEnabled(prefs());
        if (appliedDynamicColor && DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivityIfAvailable(this);
        }

        SecurityScreenUtil.apply(getWindow(), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (AppLockManager.getInstance().isLockRequired(this)) {
            startActivity(new Intent(this, LockActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appliedContrastOverlay != ThemeUtil.resolveContrastThemeOverlay(prefs())
                || appliedDynamicColor != ThemeUtil.isDynamicColorEnabled(prefs())) {
            recreate();
        }
    }

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }
}