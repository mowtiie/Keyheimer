package com.mowtiie.keyheimer.util;

import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class SecurityScreenUtil {

    private SecurityScreenUtil() {
    }

    public static AlertDialog show(Context context, MaterialAlertDialogBuilder builder) {
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            apply(dialog.getWindow(), context);
        }
        dialog.show();
        return dialog;
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