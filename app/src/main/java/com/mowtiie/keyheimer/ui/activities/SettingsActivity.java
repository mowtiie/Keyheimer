package com.mowtiie.keyheimer.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.databinding.ActivitySettingsBinding;
import com.mowtiie.keyheimer.scheduling.ReminderScheduler;
import com.mowtiie.keyheimer.util.HashUtil;
import com.mowtiie.keyheimer.util.PreferenceKeys;
import com.mowtiie.keyheimer.util.ThemeUtil;

import java.util.Arrays;

public class SettingsActivity extends KeyheimerActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setUpEdgeToEdgeInsets();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(binding.settingsContainer.getId(), new SettingsFragment())
                    .commit();
        }
    }

    private void setUpEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.appBarLayout.setPadding(bars.left, bars.top, bars.right, 0);
            binding.settingsContainer.setPadding(0, 0, 0, bars.bottom);
            return windowInsets;
        });
    }

    public class SettingsFragment extends PreferenceFragmentCompat {

        private final SharedPreferences.OnSharedPreferenceChangeListener themeChangeListener =
                (sharedPreferences, key) -> {
                    if (PreferenceKeys.KEY_THEME_MODE.equals(key)) {
                        ThemeUtil.applyNightMode(sharedPreferences);
                    } else if (PreferenceKeys.KEY_THEME_CONTRAST.equals(key)
                            || PreferenceKeys.KEY_DYNAMIC_COLOR.equals(key)) {
                        requireActivity().recreate();
                    }
                };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_settings, rootKey);
            setUpDynamicColorPreference();
            setUpBiometricPreference();
            setUpAppLockPreference();
            setUpExactAlarmPreference();
        }

        @Override
        public void onResume() {
            super.onResume();
            updateExactAlarmSummary();
            prefs().registerOnSharedPreferenceChangeListener(themeChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            prefs().unregisterOnSharedPreferenceChangeListener(themeChangeListener);
        }

        private void setUpDynamicColorPreference() {
            SwitchPreferenceCompat preference = findPreference(PreferenceKeys.KEY_DYNAMIC_COLOR);
            if (preference == null) {
                return;
            }
            if (!DynamicColors.isDynamicColorAvailable()) {
                preference.setEnabled(false);
                preference.setChecked(false);
                preference.setSummary(R.string.settings_dynamic_color_unsupported);
            }
        }

        private void setUpBiometricPreference() {
            SwitchPreferenceCompat preference = findPreference(PreferenceKeys.KEY_BIOMETRIC_ENABLED);
            if (preference == null) {
                return;
            }
            BiometricManager biometricManager = BiometricManager.from(requireContext());
            int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                preference.setEnabled(false);
                preference.setChecked(false);
                preference.setSummary(R.string.settings_biometric_unsupported);
            }
        }

        private void setUpAppLockPreference() {
            Preference preference = findPreference(PreferenceKeys.KEY_APP_LOCK);
            if (preference == null) {
                return;
            }
            updateAppLockSummary(preference);
            preference.setOnPreferenceClickListener(clicked -> {
                showAppLockDialog(preference);
                return true;
            });
        }

        private void updateAppLockSummary(Preference preference) {
            boolean hasPassword = !currentPasswordHash().isEmpty();
            preference.setSummary(hasPassword
                    ? R.string.settings_app_lock_summary_set
                    : R.string.settings_app_lock_summary_unset);
        }

        private void showAppLockDialog(Preference preference) {
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_app_lock_password, null);
            TextInputEditText passwordInput = dialogView.findViewById(R.id.input_app_lock_password);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setIcon(R.drawable.ic_lock)
                    .setTitle(R.string.settings_app_lock_dialog_title)
                    .setView(dialogView)
                    .setPositiveButton(R.string.settings_app_lock_dialog_save, null)
                    .setNegativeButton(R.string.delete_confirm_negative, null);

            if (!currentPasswordHash().isEmpty()) {
                builder.setNeutralButton(R.string.settings_app_lock_dialog_remove, (dialog, which) -> {
                    clearPassword();
                    updateAppLockSummary(preference);
                });
            }

            AlertDialog dialog = builder.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                char[] password = charsOf(passwordInput.getText());
                if (password.length < 6) {
                    passwordInput.setError(getString(R.string.settings_app_lock_error_length));
                    return;
                }
                savePassword(password);
                Arrays.fill(password, '\0');
                dialog.dismiss();
                updateAppLockSummary(preference);
            });
        }

        private void setUpExactAlarmPreference() {
            Preference preference = findPreference(PreferenceKeys.KEY_EXACT_ALARMS);
            if (preference == null) {
                return;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                preference.setEnabled(false);
                preference.setSummary(R.string.settings_exact_alarms_summary_not_needed);
                return;
            }

            preference.setOnPreferenceClickListener(clicked -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
                return true;
            });
            updateExactAlarmSummary();
        }

        private void updateExactAlarmSummary() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return;
            }
            Preference preference = findPreference(PreferenceKeys.KEY_EXACT_ALARMS);
            if (preference == null) {
                return;
            }
            boolean allowed = ReminderScheduler.canScheduleExactAlarms(requireContext());
            preference.setSummary(allowed
                    ? R.string.settings_exact_alarms_summary_allowed
                    : R.string.settings_exact_alarms_summary_not_allowed);
        }

        private SharedPreferences prefs() {
            return PreferenceManager.getDefaultSharedPreferences(requireContext());
        }

        private String currentPasswordHash() {
            return prefs().getString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_HASH, "");
        }

        private void savePassword(char[] password) {
            byte[] salt = HashUtil.generateSalt();
            int iterations = HashUtil.DEFAULT_ITERATIONS;
            String hash = HashUtil.hash(password, salt, iterations);
            prefs().edit()
                    .putString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_HASH, hash)
                    .putString(PreferenceKeys.KEY_APP_LOCK_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putInt(PreferenceKeys.KEY_APP_LOCK_PASSWORD_ITERATIONS, iterations)
                    .apply();
        }

        private void clearPassword() {
            prefs().edit()
                    .remove(PreferenceKeys.KEY_APP_LOCK_PASSWORD_HASH)
                    .remove(PreferenceKeys.KEY_APP_LOCK_PASSWORD_SALT)
                    .remove(PreferenceKeys.KEY_APP_LOCK_PASSWORD_ITERATIONS)
                    .apply();
        }

        private char[] charsOf(Editable editable) {
            if (editable == null || editable.length() == 0) {
                return new char[0];
            }

            char[] chars = new char[editable.length()];
            editable.getChars(0, editable.length(), chars, 0);
            return chars;
        }
    }
}