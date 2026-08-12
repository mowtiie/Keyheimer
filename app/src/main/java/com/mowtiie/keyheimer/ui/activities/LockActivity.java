package com.mowtiie.keyheimer.ui.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.util.Base64;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.databinding.ActivityLockBinding;
import com.mowtiie.keyheimer.util.AppLockManager;
import com.mowtiie.keyheimer.util.HashUtil;
import com.mowtiie.keyheimer.util.PreferenceKeys;

import java.util.Arrays;

public class LockActivity extends KeyheimerActivity {

    private ActivityLockBinding binding;
    private boolean hasPin;
    private boolean biometricAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppLockManager lockManager = AppLockManager.getInstance();
        hasPin = lockManager.hasPin(this);
        biometricAvailable = lockManager.isBiometricEnabled(this) && canUseBiometric();

        binding.tilPin.setVisibility(hasPin ? View.VISIBLE : View.GONE);
        binding.buttonUnlockPin.setVisibility(hasPin ? View.VISIBLE : View.GONE);
        binding.buttonUseBiometric.setVisibility(biometricAvailable ? View.VISIBLE : View.GONE);

        binding.buttonUnlockPin.setOnClickListener(v -> attemptPinUnlock());
        binding.buttonUseBiometric.setOnClickListener(v -> showBiometricPrompt());

        setUpEdgeToEdgeInsets();

        if (biometricAvailable) {
            showBiometricPrompt();
        }
    }

    private boolean canUseBiometric() {
        return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_biometric_prompt_title))
                .setNegativeButtonText(getString(hasPin ? R.string.lock_use_pin_instead : R.string.delete_confirm_negative))
                .build();

        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        unlock();
                    }
                });
        prompt.authenticate(promptInfo);
    }

    private void attemptPinUnlock() {
        char[] pin = charsOf(binding.inputPin.getText());
        boolean matches = verifyPin(pin);
        Arrays.fill(pin, '\0');
        if (matches) {
            unlock();
        } else {
            binding.tilPin.setError(getString(R.string.lock_pin_error));
            binding.inputPin.setText(null);
        }
    }

    private boolean verifyPin(char[] pin) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String storedHash = prefs.getString(PreferenceKeys.KEY_APP_LOCK_PIN_HASH, "");
        String saltEncoded = prefs.getString(PreferenceKeys.KEY_APP_LOCK_PIN_SALT, "");
        int iterations = prefs.getInt(PreferenceKeys.KEY_APP_LOCK_PIN_ITERATIONS, HashUtil.DEFAULT_ITERATIONS);
        if (storedHash.isEmpty() || saltEncoded.isEmpty()) {
            return false;
        }
        byte[] salt = Base64.decode(saltEncoded, Base64.NO_WRAP);
        return HashUtil.verify(pin, salt, iterations, storedHash);
    }

    private void unlock() {
        AppLockManager.getInstance().markUnlocked();
        finish();
    }

    private void setUpEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.lockRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
    }

    private static char[] charsOf(Editable editable) {
        if (editable == null || editable.length() == 0) {
            return new char[0];
        }
        char[] chars = new char[editable.length()];
        editable.getChars(0, editable.length(), chars, 0);
        return chars;
    }
}