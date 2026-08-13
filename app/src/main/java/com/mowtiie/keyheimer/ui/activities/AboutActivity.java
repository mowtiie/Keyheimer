package com.mowtiie.keyheimer.ui.activities;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.databinding.ActivityAboutBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.about_container, new AboutFragment())
                    .commit();
        }
    }

    public static class AboutFragment extends PreferenceFragmentCompat {

        private int easterEggCounter;
        private final int EASTER_EGG_COUNT = 7;

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.preferences_about, rootKey);

            Preference appVersion = findPreference("app_version");
            Preference appLicense = findPreference("app_license");

            if (appVersion != null) {
                try {
                    PackageManager packageManager = requireContext().getPackageManager();
                    PackageInfo packageInfo = packageManager.getPackageInfo(requireContext().getPackageName(), 0);
                    appVersion.setSummary(packageInfo.versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            if (appVersion != null) {
                appVersion.setOnPreferenceClickListener(preference -> {
                    easterEggCounter++;
                    if (easterEggCounter == EASTER_EGG_COUNT) {
                        String easterEggMessage = getString(R.string.app_easter_egg);
                        Toast.makeText(requireContext(), easterEggMessage, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }

            if (appLicense != null) {
                appLicense.setOnPreferenceClickListener(preference -> {
                    showLicenseDialog();
                    return true;
                });
            }
        }

        private void showLicenseDialog() {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.preference_app_license)
                    .setIcon(R.drawable.ic_license)
                    .setMessage(readLicenseFromAssets())
                    .setPositiveButton(R.string.dialog_button_close, null);

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        private String readLicenseFromAssets() {
            StringBuilder stringBuilder = new StringBuilder();
            AssetManager assetManager = requireContext().getAssets();

            try (InputStream inputStream = assetManager.open("license.txt")) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return stringBuilder.toString();
        }
    }
}