package com.mowtiie.keyheimer.ui.adapters;

import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.databinding.ItemSecretBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SecretAdapter extends RecyclerView.Adapter<SecretAdapter.SecretViewHolder> {

    public interface Listener {
        void onSecretClicked(Secret secret);

        void onVerifyNowClicked(Secret secret);
    }

    private final Listener listener;
    private final List<Secret> secrets = new ArrayList<>();

    public SecretAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Secret> newSecrets) {
        secrets.clear();
        secrets.addAll(newSecrets);
        notifyDataSetChanged();
    }

    public Secret getSecretAt(int position) {
        return secrets.get(position);
    }

    @NonNull
    @Override
    public SecretViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSecretBinding binding = ItemSecretBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SecretViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SecretViewHolder holder, int position) {
        holder.bind(secrets.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return secrets.size();
    }

    static class SecretViewHolder extends RecyclerView.ViewHolder {

        private final ItemSecretBinding binding;

        SecretViewHolder(@NonNull ItemSecretBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Secret secret, Listener listener) {
            binding.textSecretName.setText(secret.getName());
            binding.textSecretSubtitle.setText(buildSubtitle(secret));
            bindStats(secret);
            itemView.setOnClickListener(v -> listener.onSecretClicked(secret));
            binding.buttonVerifyNow.setOnClickListener(v -> listener.onVerifyNowClicked(secret));
        }

        private String buildSubtitle(Secret secret) {
            String interval = secret.getIntervalValue() + " " +
                    pluralize(secret.getIntervalUnit().name().toLowerCase(Locale.US), secret.getIntervalValue());
            if (!secret.isActive()) {
                return "Paused";
            }
            CharSequence relative = DateUtils.getRelativeTimeSpanString(
                    secret.getNextTriggerAt(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
            String time = DateFormat.getTimeFormat(itemView.getContext()).format(secret.getNextTriggerAt());
            return "Every " + interval + " • Next: " + relative + " at " + time;
        }

        private void bindStats(Secret secret) {
            int totalAttempts = secret.getSuccessCount() + secret.getFailCount();
            if (totalAttempts == 0) {
                binding.textSecretStats.setVisibility(View.GONE);
                return;
            }
            binding.textSecretStats.setVisibility(View.VISIBLE);
            binding.textSecretStats.setText(itemView.getContext().getString(
                    R.string.field_secret_stats, secret.getSuccessCount(), totalAttempts));
        }

        private String pluralize(String unit, int value) {
            return value == 1 ? unit : unit + "s";
        }
    }
}