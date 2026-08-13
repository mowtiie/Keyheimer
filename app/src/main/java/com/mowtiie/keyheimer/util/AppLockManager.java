package com.mowtiie.keyheimer.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public final class AppLockManager {

    private static volatile AppLockManager instance;

    private boolean locked = true;

    private AppLockManager() {
    }

    public static AppLockManager getInstance() {
        if (instance == null) {
            synchronized (AppLockManager.class) {
                if (instance == null) {
                    instance = new AppLockManager();
                }
            }
        }
        return instance;
    }

    public boolean isLockRequired(Context context) {
        if (!isLockConfigured(context)) {
            locked = false;
            return false;
        }
        return locked;
    }

    public boolean isLockConfigured(Context context) {
        return hasPassword(context) || isBiometricEnabled(context);
    }

    public boolean hasPassword(Context context) {
        return !prefs(context).getString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_HASH, "").isEmpty();
    }

    public boolean isBiometricEnabled(Context context) {
        return prefs(context).getBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, false);
    }

    public void markUnlocked() {
        locked = false;
    }

    public void markLocked() {
        locked = true;
    }

    private SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}