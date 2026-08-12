package com.mowtiie.keyheimer.util;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.mowtiie.keyheimer.R;

public final class ThemeUtil {

    private ThemeUtil() {
    }

    public static void applyNightMode(SharedPreferences prefs) {
        int nightMode = resolveNightMode(prefs);
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }

    public static int resolveNightMode(SharedPreferences prefs) {
        String mode = prefs.getString(PreferenceKeys.KEY_THEME_MODE, PreferenceKeys.THEME_MODE_SYSTEM);
        if (PreferenceKeys.THEME_MODE_LIGHT.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (PreferenceKeys.THEME_MODE_DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static int resolveContrastThemeOverlay(SharedPreferences prefs) {
        String contrast = prefs.getString(PreferenceKeys.KEY_THEME_CONTRAST, PreferenceKeys.CONTRAST_STANDARD);
        if (PreferenceKeys.CONTRAST_MEDIUM.equals(contrast)) {
            return R.style.Theme_Keyheimer_MediumContrast;
        }
        if (PreferenceKeys.CONTRAST_HIGH.equals(contrast)) {
            return R.style.Theme_Keyheimer_HighContrast;
        }
        return 0;
    }

    public static boolean isDynamicColorEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(PreferenceKeys.KEY_DYNAMIC_COLOR, true);
    }
}