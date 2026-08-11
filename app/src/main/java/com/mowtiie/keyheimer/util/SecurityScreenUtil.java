package com.mowtiie.keyheimer.util;

import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import androidx.preference.PreferenceManager;

public final class SecurityScreenUtil {

    private SecurityScreenUtil() {
    }

    public static void apply(Window window, Context context) {
        boolean enabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceKeys.KEY_SECURITY_SCREEN, true);
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}