package com.mowtiie.keyheimer.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.biometric.BiometricManager;
import androidx.preference.PreferenceManager;

import java.util.Base64;

public final class AppLockManager {

    private static final int FREE_ATTEMPTS = 4;
    private static final int MAX_DELAY_SHIFT = 5;
    private static final long BASE_DELAY_MILLIS = 30_000L;
    private static final long MAX_DELAY_MILLIS = 15 * 60_000L;

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
        if (timeoutMillis >= 0 && System.currentTimeMillis() - backgroundedAtMillis >= timeoutMillis) {
            return true;
        }
        backgroundedAtMillis = 0L;
        return false;
    }

    public boolean isLockConfigured(Context context) {
        return hasPassword(context) || (isBiometricEnabled(context) && isBiometricUsable(context));
    }

    public boolean isBiometricUsable(Context context) {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public boolean hasPassword(Context context) {
        return !prefs(context).getString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_HASH, "").isEmpty();
    }

    public boolean isBiometricEnabled(Context context) {
        return prefs(context).getBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, false);
    }

    static long delayForFailures(int failures) {
        if (failures <= FREE_ATTEMPTS) {
            return 0L;
        }
        int shift = Math.min(failures - FREE_ATTEMPTS - 1, MAX_DELAY_SHIFT);
        return Math.min(BASE_DELAY_MILLIS << shift, MAX_DELAY_MILLIS);
    }

    public long blockedRemainingMillis(Context context) {
        long blockedUntil = prefs(context).getLong(PreferenceKeys.KEY_LOCK_BLOCKED_UNTIL, 0L);
        long remaining = blockedUntil - System.currentTimeMillis();
        return Math.max(0L, Math.min(remaining, MAX_DELAY_MILLIS));
    }

    public void recordFailedAttempt(Context context) {
        SharedPreferences prefs = prefs(context);
        int failures = prefs.getInt(PreferenceKeys.KEY_LOCK_FAILED_ATTEMPTS, 0) + 1;
        long delay = delayForFailures(failures);
        SharedPreferences.Editor editor = prefs.edit()
                .putInt(PreferenceKeys.KEY_LOCK_FAILED_ATTEMPTS, failures);
        if (delay > 0) {
            editor.putLong(PreferenceKeys.KEY_LOCK_BLOCKED_UNTIL, System.currentTimeMillis() + delay);
        }
        editor.apply();
    }

    public void clearFailedAttempts(Context context) {
        prefs(context).edit()
                .remove(PreferenceKeys.KEY_LOCK_FAILED_ATTEMPTS)
                .remove(PreferenceKeys.KEY_LOCK_BLOCKED_UNTIL)
                .apply();
    }

    public void setPassword(Context context, char[] password) {
        byte[] salt = HashUtil.generateSalt();
        int iterations = HashUtil.DEFAULT_ITERATIONS;
        String hash = HashUtil.hash(password, salt, iterations);
        prefs(context).edit()
                .putString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_HASH, hash)
                .putString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_SALT, Base64.getEncoder().encodeToString(salt))
                .putInt(PreferenceKeys.KEY_APP_LOCK_PASSWORD_ITERATIONS, iterations)
                .apply();
    }

    public void upgradePasswordHashIfNeeded(Context context, char[] verifiedPassword) {
        int iterations = prefs(context).getInt(
                PreferenceKeys.KEY_APP_LOCK_PASSWORD_ITERATIONS, HashUtil.DEFAULT_ITERATIONS);
        if (iterations < HashUtil.DEFAULT_ITERATIONS) {
            setPassword(context, verifiedPassword);
        }
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