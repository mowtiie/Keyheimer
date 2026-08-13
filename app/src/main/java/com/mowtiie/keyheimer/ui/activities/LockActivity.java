package com.mowtiie.keyheimer.ui.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.util.Base64;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
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
    private boolean hasPassword;
    private boolean biometricAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppLockManager lockManager = AppLockManager.getInstance();
        hasPassword = lockManager.hasPassword(this);
        biometricAvailable = lockManager.isBiometricEnabled(this) && canUseBiometric();

        binding.tilPassword.setVisibility(hasPassword ? View.VISIBLE : View.GONE);
        binding.buttonUnlockPassword.setVisibility(hasPassword ? View.VISIBLE : View.GONE);
        binding.buttonUseBiometric.setVisibility(biometricAvailable ? View.VISIBLE : View.GONE);

        binding.buttonUnlockPassword.setOnClickListener(v -> attemptPasswordUnlock());
        binding.buttonUseBiometric.setOnClickListener(v -> showBiometricPrompt());

        setUpEdgeToEdgeInsets();
        setUpBackPressedHandling();

        if (biometricAvailable) {
            showBiometricPrompt();
        }
    }

    private void setUpBackPressedHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    private boolean canUseBiometric() {
        return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_biometric_prompt_title))
                .setNegativeButtonText(getString(hasPassword ? R.string.lock_use_password_instead : R.string.delete_confirm_negative))
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

    private void attemptPasswordUnlock() {
        char[] password = charsOf(binding.inputPassword.getText());
        boolean matches = verifyPassword(password);
        Arrays.fill(password, '\0');
        if (matches) {
            unlock();
        } else {
            binding.tilPassword.setError(getString(R.string.lock_password_error));
            binding.inputPassword.setText(null);
        }
    }

    private boolean verifyPassword(char[] password) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String storedHash = prefs.getString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_HASH, "");
        String saltEncoded = prefs.getString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_SALT, "");
        int iterations = prefs.getInt(PreferenceKeys.KEY_APP_LOCK_PASSWORD_ITERATIONS, HashUtil.DEFAULT_ITERATIONS);
        if (storedHash.isEmpty() || saltEncoded.isEmpty()) {
            return false;
        }
        byte[] salt = Base64.decode(saltEncoded, Base64.NO_WRAP);
        return HashUtil.verify(password, salt, iterations, storedHash);
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