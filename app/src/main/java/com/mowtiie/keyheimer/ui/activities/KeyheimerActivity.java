package com.mowtiie.keyheimer.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.util.AppLockManager;
import com.mowtiie.keyheimer.util.PreferenceKeys;
import com.mowtiie.keyheimer.util.SecurityScreenUtil;

public abstract class KeyheimerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyNightMode();
        super.onCreate(savedInstanceState);
        applyContrastTheme();
        applyDynamicColor();
        SecurityScreenUtil.apply(getWindow(), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (AppLockManager.getInstance().isLockRequired(this)) {
            startActivity(new Intent(this, LockActivity.class));
        }
    }

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void applyNightMode() {
        String mode = prefs().getString(PreferenceKeys.KEY_THEME_MODE, PreferenceKeys.THEME_MODE_SYSTEM);
        int nightMode;
        if (PreferenceKeys.THEME_MODE_LIGHT.equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (PreferenceKeys.THEME_MODE_DARK.equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }

    private void applyContrastTheme() {
        String contrast = prefs().getString(PreferenceKeys.KEY_THEME_CONTRAST, PreferenceKeys.CONTRAST_STANDARD);
        if (PreferenceKeys.CONTRAST_MEDIUM.equals(contrast)) {
            setTheme(R.style.Theme_Keyheimer_MediumContrast);
        } else if (PreferenceKeys.CONTRAST_HIGH.equals(contrast)) {
            setTheme(R.style.Theme_Keyheimer_HighContrast);
        }
    }

    private void applyDynamicColor() {
        boolean enabled = prefs().getBoolean(PreferenceKeys.KEY_DYNAMIC_COLOR, true);
        if (enabled && DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
    }
}