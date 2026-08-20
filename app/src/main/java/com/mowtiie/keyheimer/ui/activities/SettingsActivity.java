package com.mowtiie.keyheimer.ui.activities;

import android.content.Context;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
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
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.databinding.ActivitySettingsBinding;
import com.mowtiie.keyheimer.scheduling.NotificationHelper;
import com.mowtiie.keyheimer.scheduling.ReminderScheduler;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.AppLockManager;
import com.mowtiie.keyheimer.util.BackupManager;
import com.mowtiie.keyheimer.util.HashUtil;
import com.mowtiie.keyheimer.util.PreferenceKeys;
import com.mowtiie.keyheimer.util.ThemeUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends KeyheimerActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setTitle(R.string.toolbar_settings);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

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

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private SecretDao secretDao;

        private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                    if (uri != null) {
                        performExport(uri);
                    }
                });

        private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) {
                        performImport(uri);
                    }
                });

        // Held as a field (not a local/lambda-only reference) because
        // SharedPreferences only keeps a weak reference to registered listeners.
        private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
                (sharedPreferences, key) -> {
                    if (PreferenceKeys.KEY_THEME_MODE.equals(key)) {
                        ThemeUtil.applyNightMode(sharedPreferences);
                    } else if (PreferenceKeys.KEY_THEME_CONTRAST.equals(key)
                            || PreferenceKeys.KEY_DYNAMIC_COLOR.equals(key)) {
                        requireActivity().recreate();
                    } else if (PreferenceKeys.KEY_BIOMETRIC_ENABLED.equals(key)
                            || PreferenceKeys.KEY_APP_LOCK_PASSWORD_HASH.equals(key)) {
                        updateLockTimeoutEnabled();
                    }
                };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_settings, rootKey);
            secretDao = new SecretDao(requireContext());
            setUpDynamicColorPreference();
            setUpBiometricPreference();
            setUpAppLockPreference();
            setUpExactAlarmPreference();
            setUpBackupPreferences();
            updateLockTimeoutEnabled();
        }

        @Override
        public void onResume() {
            super.onResume();
            updateExactAlarmSummary();
            updateLockTimeoutEnabled();
            prefs().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            prefs().unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        private void setUpDynamicColorPreference() {
            SwitchPreferenceCompat preference = findPreference(PreferenceKeys.KEY_DYNAMIC_COLOR);
            if (preference == null) {
                return;
            }
            if (!DynamicColors.isDynamicColorAvailable()) {
                preference.setEnabled(false);
                preference.setChecked(false);
                preference.setSummary(R.string.preference_switch_dynamic_colors_summary_unsupported);
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
                preference.setSummary(R.string.preference_switch_biometric_summary_unsupported);
                return;
            }

            preference.setOnPreferenceChangeListener((changedPreference, newValue) -> {
                if (Boolean.TRUE.equals(newValue)) {
                    authenticateBeforeEnablingBiometric((SwitchPreferenceCompat) changedPreference);
                    return false;
                }
                return true;
            });
        }

        private void authenticateBeforeEnablingBiometric(SwitchPreferenceCompat preference) {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.dialog_title_biometric_prompt))
                    .setNegativeButtonText(getString(R.string.dialog_button_cancel))
                    .build();

            BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            preference.setChecked(true);
                        }
                    });
            prompt.authenticate(promptInfo);
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
                    ? R.string.preference_app_lock_summary_set
                    : R.string.preference_app_lock_summary_unset);
        }

        private void showAppLockDialog(Preference preference) {
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_app_lock_password, null);
            TextInputEditText passwordInput = dialogView.findViewById(R.id.input_app_lock_password);
            TextInputEditText confirmInput = dialogView.findViewById(R.id.input_app_lock_password_confirm);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setIcon(R.drawable.ic_lock)
                    .setTitle(R.string.dialog_title_app_lock)
                    .setMessage(R.string.dialog_message_app_lock)
                    .setView(dialogView)
                    .setPositiveButton(R.string.dialog_button_save, null)
                    .setNegativeButton(R.string.dialog_button_cancel, null);

            if (!currentPasswordHash().isEmpty()) {
                builder.setNeutralButton(R.string.dialog_button_remove, (dialog, which) -> {
                    clearPassword();
                    updateAppLockSummary(preference);
                });
            }

            AlertDialog dialog = builder.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                char[] password = charsOf(passwordInput.getText());
                char[] confirmPassword = charsOf(confirmInput.getText());

                if (password.length < 6) {
                    passwordInput.setError(getString(R.string.field_error_password_length));
                    Arrays.fill(confirmPassword, '\0');
                    return;
                }
                if (!Arrays.equals(password, confirmPassword)) {
                    confirmInput.setError(getString(R.string.field_error_password_mismatch));
                    Arrays.fill(password, '\0');
                    Arrays.fill(confirmPassword, '\0');
                    return;
                }

                savePassword(password);
                Arrays.fill(password, '\0');
                Arrays.fill(confirmPassword, '\0');
                dialog.dismiss();
                updateAppLockSummary(preference);
            });
        }

        private void updateLockTimeoutEnabled() {
            Preference preference = findPreference(PreferenceKeys.KEY_LOCK_TIMEOUT);
            if (preference == null) {
                return;
            }
            preference.setEnabled(AppLockManager.getInstance().isLockConfigured(requireContext()));
        }

        private void setUpExactAlarmPreference() {
            Preference preference = findPreference(PreferenceKeys.KEY_EXACT_ALARMS);
            if (preference == null) {
                return;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                preference.setEnabled(false);
                preference.setSummary(R.string.preference_exact_alarms_summary_not_needed);
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
                    ? R.string.preference_exact_alarms_summary_allowed
                    : R.string.preference_exact_alarms_summary_not_allowed);
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

        private void setUpBackupPreferences() {
            Preference exportPreference = findPreference(PreferenceKeys.KEY_BACKUP_EXPORT);
            if (exportPreference != null) {
                exportPreference.setOnPreferenceClickListener(clicked -> {
                    exportLauncher.launch(defaultBackupFilename());
                    return true;
                });
            }

            Preference importPreference = findPreference(PreferenceKeys.KEY_BACKUP_IMPORT);
            if (importPreference != null) {
                importPreference.setOnPreferenceClickListener(clicked -> {
                    importLauncher.launch(new String[]{"application/json"});
                    return true;
                });
            }
        }

        private String defaultBackupFilename() {
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            return "keyheimer-backup-" + timestamp + ".json";
        }

        private void performExport(Uri uri) {
            Context appContext = requireContext().getApplicationContext();
            AppExecutors.getInstance().diskIO().execute(() -> {
                List<Secret> secrets = secretDao.getAll();
                if (secrets.isEmpty()) {
                    AppExecutors.getInstance().mainThread(() -> showToast(R.string.toast_export_no_secrets));
                    return;
                }
                try (OutputStream out = appContext.getContentResolver().openOutputStream(uri)) {
                    if (out == null) {
                        throw new IOException("Unable to open output stream");
                    }

                    BackupManager.exportToJson(secrets, out);
                    AppExecutors.getInstance().mainThread(() -> showToast(R.string.toast_export_successful));
                } catch (Exception e) {
                    AppExecutors.getInstance().mainThread(() -> showToast(R.string.toast_export_failed));
                }
            });
        }

        private void performImport(Uri uri) {
            Context appContext = requireContext().getApplicationContext();
            AppExecutors.getInstance().diskIO().execute(() -> {
                try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
                    if (in == null) {
                        throw new IOException("Unable to open input stream");
                    }

                    List<Secret> imported = BackupManager.importFromJson(in);
                    for (Secret secret : imported) {
                        secretDao.insert(secret);
                    }
                    AppExecutors.getInstance().mainThread(() -> {
                        for (Secret secret : imported) {
                            if (secret.isActive()) {
                                ReminderScheduler.scheduleReminder(appContext, secret);
                            }
                        }
                        showToast(getString(R.string.toast_import_successful, imported.size()));
                    });
                } catch (Exception e) {
                    AppExecutors.getInstance().mainThread(() -> showToast(R.string.toast_import_failed));
                }
            });
        }

        private void showToast(int stringRes) {
            if (isAdded()) {
                Toast.makeText(requireContext(), stringRes, Toast.LENGTH_SHORT).show();
            }
        }

        private void showToast(String message) {
            if (isAdded()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
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
}