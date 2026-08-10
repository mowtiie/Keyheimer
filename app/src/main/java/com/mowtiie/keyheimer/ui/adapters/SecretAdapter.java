package com.mowtiie.keyheimer.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.databinding.ItemSecretBinding;

import java.text.DateFormat;
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
            itemView.setOnClickListener(v -> listener.onSecretClicked(secret));
            binding.buttonVerifyNow.setOnClickListener(v -> listener.onVerifyNowClicked(secret));
        }

        private String buildSubtitle(Secret secret) {
            String interval = secret.getIntervalValue() + " " +
                    pluralize(secret.getIntervalUnit().name().toLowerCase(Locale.US), secret.getIntervalValue());
            String nextDate = DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(secret.getNextTriggerAt());
            return secret.isActive() ? "Every " + interval + " • Next: " + nextDate : "Paused";
        }

        private String pluralize(String unit, int value) {
            return value == 1 ? unit : unit + "s";
        }
    }
}