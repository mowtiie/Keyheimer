package com.mowtiie.keyheimer.ui.dialogs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.databinding.BottomSheetVerifySecretBinding;
import com.mowtiie.keyheimer.scheduling.ReminderScheduler;
import com.mowtiie.keyheimer.ui.activities.AddEditSecretActivity;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.HashUtil;
import com.mowtiie.keyheimer.util.IntervalConverter;
import com.mowtiie.keyheimer.util.SecurityScreenUtil;

import java.util.Arrays;

public class VerifySecretBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SECRET_ID = "secret_id";
    private static final int FAILURES_BEFORE_RESET_OPTION = 3;
    private static final long SUCCESS_AUTO_CLOSE_DELAY_MS = 1500L;

    public static VerifySecretBottomSheet newInstance(String secretId) {
        VerifySecretBottomSheet sheet = new VerifySecretBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SECRET_ID, secretId);
        sheet.setArguments(args);
        return sheet;
    }

    private BottomSheetVerifySecretBinding binding;
    private SecretDao dao;
    private Secret secret;
    private int failureCount = 0;
    private final Handler autoCloseHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoCloseRunnable = this::dismissAllowingStateLoss;

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            SecurityScreenUtil.apply(getDialog().getWindow(), requireContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVerifySecretBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dao = new SecretDao(requireContext());

        binding.buttonVerify.setOnClickListener(v -> attemptVerify());
        binding.buttonDone.setOnClickListener(v -> dismiss());
        binding.buttonResetPassphrase.setOnClickListener(v -> openResetPassphrase());

        loadSecret(requireArguments().getString(ARG_SECRET_ID));
    }

    private void loadSecret(String secretId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Secret loaded = dao.getById(secretId);
            AppExecutors.getInstance().mainThread(() -> bindSecret(loaded));
        });
    }

    private void bindSecret(Secret loaded) {
        if (loaded == null) {
            dismissAllowingStateLoss();
            return;
        }
        secret = loaded;
        binding.textSecretName.setText(secret.getName());
        boolean hasHint = secret.getHint() != null && !secret.getHint().isEmpty();
        binding.tilPassphrase.setHelperText(hasHint ? secret.getHint() : null);
    }

    private void attemptVerify() {
        if (secret == null) {
            return;
        }
        char[] attempt = charsOf(binding.inputPassphrase.getText());
        if (attempt.length == 0) {
            binding.tilPassphrase.setError(getString(R.string.error_passphrase_required));
            return;
        }
        binding.tilPassphrase.setError(null);
        binding.buttonVerify.setEnabled(false);

        AppExecutors.getInstance().diskIO().execute(() -> {
            boolean matched = HashUtil.verify(attempt, secret.getSalt(), secret.getIterations(), secret.getHash());
            Arrays.fill(attempt, '\0');
            AppExecutors.getInstance().mainThread(() -> handleResult(matched));
        });
    }

    private void handleResult(boolean matched) {
        binding.buttonVerify.setEnabled(true);
        if (matched) {
            onVerifySuccess();
        } else {
            onVerifyFailure();
        }
    }

    private void onVerifySuccess() {
        long nextTriggerAt = IntervalConverter.computeNextTriggerAt(
                secret.getIntervalValue(), secret.getIntervalUnit(),
                secret.getReminderHour(), secret.getReminderMinute());
        String secretId = secret.getId();

        AppExecutors.getInstance().diskIO().execute(() -> {
            dao.updateVerificationResult(secretId, true, nextTriggerAt);
            Secret updated = dao.getById(secretId);
            AppExecutors.getInstance().mainThread(() -> {
                if (updated != null && updated.isActive()) {
                    ReminderScheduler.scheduleReminder(requireContext(), updated);
                }
                showSuccessState();
            });
        });
    }

    private void showSuccessState() {
        binding.groupInput.setVisibility(View.GONE);
        binding.groupSuccess.setVisibility(View.VISIBLE);
        autoCloseHandler.postDelayed(autoCloseRunnable, SUCCESS_AUTO_CLOSE_DELAY_MS);
    }

    private void onVerifyFailure() {
        failureCount++;
        binding.inputPassphrase.setText(null);
        binding.tilPassphrase.setError(getString(R.string.verify_error_message));
        binding.inputPassphrase.requestFocus();

        AppExecutors.getInstance().diskIO().execute(() ->
                dao.updateVerificationResult(secret.getId(), false, secret.getNextTriggerAt()));

        if (failureCount >= FAILURES_BEFORE_RESET_OPTION) {
            binding.buttonResetPassphrase.setVisibility(View.VISIBLE);
        }
    }

    private void openResetPassphrase() {
        Intent intent = new Intent(requireContext(), AddEditSecretActivity.class);
        intent.putExtra(AddEditSecretActivity.EXTRA_SECRET_ID, secret.getId());
        startActivity(intent);
        dismiss();
    }

    private static char[] charsOf(Editable editable) {
        if (editable == null || editable.length() == 0) {
            return new char[0];
        }
        char[] chars = new char[editable.length()];
        editable.getChars(0, editable.length(), chars, 0);
        return chars;
    }

    @Override
    public void onDestroyView() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable);
        binding = null;
        super.onDestroyView();
    }
}