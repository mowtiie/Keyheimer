package com.mowtiie.keyheimer.ui.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.scheduling.ReminderScheduler;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.HashUtil;
import com.mowtiie.keyheimer.util.IntervalConverter;

import java.util.Arrays;
import java.util.UUID;

public class AddEditSecretActivity extends AppCompatActivity {

    public static final String EXTRA_SECRET_ID = "secret_id";

    private static final Secret.IntervalUnit[] INTERVAL_UNITS = {
            Secret.IntervalUnit.DAY, Secret.IntervalUnit.WEEK, Secret.IntervalUnit.MONTH
    };

    private SecretDao dao;
    private boolean editMode;
    private Secret existingSecret;

    private TextInputLayout tilName;
    private TextInputEditText inputName;
    private TextInputLayout tilPassphrase;
    private TextInputEditText inputPassphrase;
    private TextInputLayout tilConfirmPassphrase;
    private TextInputEditText inputConfirmPassphrase;
    private TextInputEditText inputHint;
    private TextInputLayout tilIntervalValue;
    private TextInputEditText inputIntervalValue;
    private AutoCompleteTextView inputIntervalUnit;
    private MaterialSwitch switchActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_add_edit_secret);

        String secretId = getIntent().getStringExtra(EXTRA_SECRET_ID);
        editMode = secretId != null;

        dao = new SecretDao(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(editMode ? R.string.title_edit_secret : R.string.title_add_secret);
        toolbar.setNavigationOnClickListener(v -> finish());
        setSupportActionBar(toolbar);

        bindViews();
        setUpIntervalUnitDropdown();
        setUpEdgeToEdgeInsets();

        if (editMode) {
            loadExistingSecret(secretId);
        } else {
            inputIntervalUnit.setText(getResources().getStringArray(R.array.interval_units)[0], false);
        }
    }

    private void bindViews() {
        tilName = findViewById(R.id.til_name);
        inputName = findViewById(R.id.input_name);
        tilPassphrase = findViewById(R.id.til_passphrase);
        inputPassphrase = findViewById(R.id.input_passphrase);
        tilConfirmPassphrase = findViewById(R.id.til_confirm_passphrase);
        inputConfirmPassphrase = findViewById(R.id.input_confirm_passphrase);
        inputHint = findViewById(R.id.input_hint);
        tilIntervalValue = findViewById(R.id.til_interval_value);
        inputIntervalValue = findViewById(R.id.input_interval_value);
        inputIntervalUnit = findViewById(R.id.input_interval_unit);
        switchActive = findViewById(R.id.switch_active);
    }

    private void setUpIntervalUnitDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.interval_units));
        inputIntervalUnit.setAdapter(adapter);
    }

    private void setUpEdgeToEdgeInsets() {
        View rootView = findViewById(R.id.add_edit_root);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        View scrollContent = findViewById(R.id.scroll_content);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBarLayout.setPadding(bars.left, bars.top, bars.right, 0);
            scrollContent.setPadding(0, 0, 0, bars.bottom);
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
        inputName.setText(secret.getName());
        inputHint.setText(secret.getHint());
        inputIntervalValue.setText(String.valueOf(secret.getIntervalValue()));
        inputIntervalUnit.setText(
                getResources().getStringArray(R.array.interval_units)[indexOfUnit(secret.getIntervalUnit())],
                false);
        switchActive.setChecked(secret.isActive());
        tilPassphrase.setHelperText(getString(R.string.helper_passphrase_edit));
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
        String name = textOf(inputName);
        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_name_required));
            return;
        }
        tilName.setError(null);

        int intervalValue = parsePositiveInt(textOf(inputIntervalValue));
        if (intervalValue <= 0) {
            tilIntervalValue.setError(getString(R.string.error_interval_value_required));
            return;
        }
        tilIntervalValue.setError(null);

        char[] passphrase = charsOf(inputPassphrase.getText());
        char[] confirmPassphrase = charsOf(inputConfirmPassphrase.getText());
        boolean changingPassphrase = passphrase.length > 0;

        if (!editMode && !changingPassphrase) {
            tilPassphrase.setError(getString(R.string.error_passphrase_required));
            return;
        }
        if (changingPassphrase && !Arrays.equals(passphrase, confirmPassphrase)) {
            tilConfirmPassphrase.setError(getString(R.string.error_passphrase_mismatch));
            return;
        }
        tilPassphrase.setError(null);
        tilConfirmPassphrase.setError(null);

        String hint = textOf(inputHint);
        Secret.IntervalUnit intervalUnit = INTERVAL_UNITS[indexOfUnitLabel(textOf(inputIntervalUnit))];
        boolean active = switchActive.isChecked();

        Secret secret = existingSecret != null ? existingSecret : new Secret();
        secret.setId(existingSecret != null ? existingSecret.getId() : UUID.randomUUID().toString());
        secret.setName(name);
        secret.setHint(hint.isEmpty() ? null : hint);
        secret.setIntervalValue(intervalValue);
        secret.setIntervalUnit(intervalUnit);
        secret.setActive(active);
        secret.setNextTriggerAt(IntervalConverter.computeNextTriggerAt(intervalValue, intervalUnit));

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
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
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