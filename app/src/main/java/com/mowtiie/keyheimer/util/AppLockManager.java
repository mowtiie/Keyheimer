package com.mowtiie.keyheimer.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public final class AppLockManager {

    private static volatile AppLockManager instance;

    // 0 means "never unlocked this process" (i.e. locked).
    private long unlockedAtMillis = 0L;
    // 0 means "not currently backgrounded" (still continuously foreground since last unlock).
    private long backgroundedAtMillis = 0L;

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
            unlockedAtMillis = System.currentTimeMillis();
            backgroundedAtMillis = 0L;
            return false;
        }
        if (unlockedAtMillis == 0L) {
            return true;
        }
        if (backgroundedAtMillis == 0L) {
            return false;
        }
        long timeoutMillis = resolveTimeoutMillis(context);
        if (timeoutMillis < 0) {
            return false;
        }
        return System.currentTimeMillis() - backgroundedAtMillis >= timeoutMillis;
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
        unlockedAtMillis = System.currentTimeMillis();
        backgroundedAtMillis = 0L;
    }

    /** Called when the whole app goes to background — starts the cooldown clock. */
    public void markLocked() {
        backgroundedAtMillis = System.currentTimeMillis();
    }

    /** Forces a lock immediately, bypassing any cooldown — used by "Lock now". */
    public void forceLock() {
        unlockedAtMillis = 0L;
        backgroundedAtMillis = 0L;
    }

    private long resolveTimeoutMillis(Context context) {
        String value = prefs(context).getString(PreferenceKeys.KEY_LOCK_TIMEOUT, PreferenceKeys.LOCK_TIMEOUT_1M);
        switch (value) {
            case PreferenceKeys.LOCK_TIMEOUT_NEVER:
                return -1L;
            case PreferenceKeys.LOCK_TIMEOUT_30S:
                return 30_000L;
            case PreferenceKeys.LOCK_TIMEOUT_1M:
                return 60_000L;
            case PreferenceKeys.LOCK_TIMEOUT_5M:
                return 5 * 60_000L;
            case PreferenceKeys.LOCK_TIMEOUT_15M:
                return 15 * 60_000L;
        }
        // LOCK_TIMEOUT_IMMEDIATELY, and the fallback for any unrecognized value.
        return 0L;
    }

    private SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}