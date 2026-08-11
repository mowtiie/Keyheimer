package com.mowtiie.keyheimer.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.databinding.ActivityAddEditSecretBinding;
import com.mowtiie.keyheimer.scheduling.ReminderScheduler;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.HashUtil;
import com.mowtiie.keyheimer.util.IntervalConverter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

public class AddEditSecretActivity extends AppCompatActivity {

    public static final String EXTRA_SECRET_ID = "secret_id";

    private static final Secret.IntervalUnit[] INTERVAL_UNITS = {
            Secret.IntervalUnit.DAY, Secret.IntervalUnit.WEEK, Secret.IntervalUnit.MONTH
    };
    private static final int DEFAULT_REMINDER_HOUR = 9;
    private static final int DEFAULT_REMINDER_MINUTE = 0;

    private ActivityAddEditSecretBinding binding;
    private SecretDao dao;
    private boolean editMode;
    private Secret existingSecret;
    private int selectedHour = DEFAULT_REMINDER_HOUR;
    private int selectedMinute = DEFAULT_REMINDER_MINUTE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAddEditSecretBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String secretId = getIntent().getStringExtra(EXTRA_SECRET_ID);
        editMode = secretId != null;

        dao = new SecretDao(this);

        binding.toolbar.setTitle(editMode ? R.string.title_edit_secret : R.string.title_add_secret);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setSupportActionBar(binding.toolbar);

        setUpIntervalUnitDropdown();
        setUpReminderTimePicker();
        setUpEdgeToEdgeInsets();

        if (editMode) {
            loadExistingSecret(secretId);
        } else {
            binding.inputIntervalUnit.setText(getResources().getStringArray(R.array.interval_units)[0], false);
            updateTimeDisplay();
        }
    }

    private void setUpIntervalUnitDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.interval_units));
        binding.inputIntervalUnit.setAdapter(adapter);
    }

    private void setUpReminderTimePicker() {
        binding.inputReminderTime.setOnClickListener(v -> openTimePicker());
    }

    private void openTimePicker() {
        int clockFormat = DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText(R.string.reminder_time_picker_title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            selectedHour = picker.getHour();
            selectedMinute = picker.getMinute();
            updateTimeDisplay();
        });
        picker.show(getSupportFragmentManager(), "reminder_time_picker");
    }

    private void updateTimeDisplay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);
        binding.inputReminderTime.setText(DateFormat.getTimeFormat(this).format(calendar.getTime()));
    }

    private void setUpEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.addEditRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.appBarLayout.setPadding(bars.left, bars.top, bars.right, 0);
            binding.scrollContent.setPadding(0, 0, 0, bars.bottom);
            return windowInsets;
        });
    }

    private void loadExistingSecret(String secretId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Secret secret = dao.getById(secretId);
            AppExecutors.getInstance().mainThread(() -> populateFields(secret));
        });
    }

    private void populateFields(Secret secret) {
        if (secret == null) {
            finish();
            return;
        }
        existingSecret = secret;
        binding.inputName.setText(secret.getName());
        binding.inputHint.setText(secret.getHint());
        binding.inputIntervalValue.setText(String.valueOf(secret.getIntervalValue()));
        binding.inputIntervalUnit.setText(
                getResources().getStringArray(R.array.interval_units)[indexOfUnit(secret.getIntervalUnit())],
                false);
        selectedHour = secret.getReminderHour();
        selectedMinute = secret.getReminderMinute();
        updateTimeDisplay();
        binding.switchActive.setChecked(secret.isActive());
        binding.tilPassphrase.setHelperText(getString(R.string.helper_passphrase_edit));
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_edit_secret, menu);
        menu.findItem(R.id.action_delete).setVisible(editMode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            attemptSave();
            return true;
        }
        if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void attemptSave() {
        String name = textOf(binding.inputName);
        if (name.isEmpty()) {
            binding.tilName.setError(getString(R.string.error_name_required));
            return;
        }
        binding.tilName.setError(null);

        int intervalValue = parsePositiveInt(textOf(binding.inputIntervalValue));
        if (intervalValue <= 0) {
            binding.tilIntervalValue.setError(getString(R.string.error_interval_value_required));
            return;
        }
        binding.tilIntervalValue.setError(null);

        char[] passphrase = charsOf(binding.inputPassphrase.getText());
        char[] confirmPassphrase = charsOf(binding.inputConfirmPassphrase.getText());
        boolean changingPassphrase = passphrase.length > 0;

        if (!editMode && !changingPassphrase) {
            binding.tilPassphrase.setError(getString(R.string.error_passphrase_required));
            return;
        }
        if (changingPassphrase && !Arrays.equals(passphrase, confirmPassphrase)) {
            binding.tilConfirmPassphrase.setError(getString(R.string.error_passphrase_mismatch));
            return;
        }
        binding.tilPassphrase.setError(null);
        binding.tilConfirmPassphrase.setError(null);

        String hint = textOf(binding.inputHint);
        Secret.IntervalUnit intervalUnit = INTERVAL_UNITS[indexOfUnitLabel(textOf(binding.inputIntervalUnit))];
        boolean active = binding.switchActive.isChecked();

        Secret secret = existingSecret != null ? existingSecret : new Secret();
        secret.setId(existingSecret != null ? existingSecret.getId() : UUID.randomUUID().toString());
        secret.setName(name);
        secret.setHint(hint.isEmpty() ? null : hint);
        secret.setIntervalValue(intervalValue);
        secret.setIntervalUnit(intervalUnit);
        secret.setActive(active);
        secret.setReminderHour(selectedHour);
        secret.setReminderMinute(selectedMinute);
        secret.setNextTriggerAt(IntervalConverter.computeNextTriggerAt(
                intervalValue, intervalUnit, selectedHour, selectedMinute));

        if (changingPassphrase) {
            byte[] salt = HashUtil.generateSalt();
            int iterations = HashUtil.DEFAULT_ITERATIONS;
            secret.setSalt(salt);
            secret.setIterations(iterations);
            secret.setHash(HashUtil.hash(passphrase, salt, iterations));
        }
        Arrays.fill(passphrase, '\0');
        Arrays.fill(confirmPassphrase, '\0');

        persistAndSchedule(secret);
    }

    private void persistAndSchedule(Secret secret) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (editMode) {
                dao.update(secret);
            } else {
                dao.insert(secret);
            }
            AppExecutors.getInstance().mainThread(() -> {
                if (secret.isActive()) {
                    ReminderScheduler.scheduleReminder(this, secret);
                } else {
                    ReminderScheduler.cancelReminder(this, secret.getId());
                }
                finish();
            });
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_delete_outline)
                .setTitle(getString(R.string.delete_confirm_title, existingSecret.getName()))
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.delete_confirm_positive, (dialog, which) -> deleteSecret())
                .setNegativeButton(R.string.delete_confirm_negative, null)
                .show();
    }

    private void deleteSecret() {
        String secretId = existingSecret.getId();
        AppExecutors.getInstance().diskIO().execute(() -> {
            dao.delete(secretId);
            AppExecutors.getInstance().mainThread(() -> {
                ReminderScheduler.cancelReminder(this, secretId);
                finish();
            });
        });
    }

    private static String textOf(TextInputEditText editText) {
        Editable editable = editText.getText();
        return editable == null ? "" : editable.toString().trim();
    }

    private static String textOf(AutoCompleteTextView autoCompleteTextView) {
        Editable editable = autoCompleteTextView.getText();
        return editable == null ? "" : editable.toString().trim();
    }

    private static char[] charsOf(Editable editable) {
        if (editable == null || editable.length() == 0) {
            return new char[0];
        }
        char[] chars = new char[editable.length()];
        editable.getChars(0, editable.length(), chars, 0);
        return chars;
    }

    private static int parsePositiveInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int indexOfUnit(Secret.IntervalUnit unit) {
        for (int i = 0; i < INTERVAL_UNITS.length; i++) {
            if (INTERVAL_UNITS[i] == unit) {
                return i;
            }
        }
        return 0;
    }

    private int indexOfUnitLabel(String label) {
        String[] labels = getResources().getStringArray(R.array.interval_units);
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(label)) {
                return i;
            }
        }
        return 0;
    }
}