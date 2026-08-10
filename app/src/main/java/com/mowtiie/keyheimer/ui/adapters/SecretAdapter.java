package com.mowtiie.keyheimer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class SecretAdapter extends RecyclerView.Adapter<SecretAdapter.SecretViewHolder> {

    interface Listener {
        void onSecretClicked(Secret secret);

        void onVerifyNowClicked(Secret secret);
    }

    private final Listener listener;
    private final List<Secret> secrets = new ArrayList<>();

    SecretAdapter(Listener listener) {
        this.listener = listener;
    }

    void submitList(List<Secret> newSecrets) {
        secrets.clear();
        secrets.addAll(newSecrets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SecretViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_secret, parent, false);
        return new SecretViewHolder(view);
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

        private final MaterialTextView nameText;
        private final MaterialTextView subtitleText;
        private final MaterialButton verifyButton;

        SecretViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_secret_name);
            subtitleText = itemView.findViewById(R.id.text_secret_subtitle);
            verifyButton = itemView.findViewById(R.id.button_verify_now);
        }

        void bind(Secret secret, Listener listener) {
            nameText.setText(secret.getName());
            subtitleText.setText(buildSubtitle(secret));
            itemView.setOnClickListener(v -> listener.onSecretClicked(secret));
            verifyButton.setOnClickListener(v -> listener.onVerifyNowClicked(secret));
        }

        private String buildSubtitle(Secret secret) {
            String interval = secret.getIntervalValue() + " " + pluralize(secret.getIntervalUnit().name().toLowerCase(Locale.US), secret.getIntervalValue());
            String nextDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(secret.getNextTriggerAt());
            String status = secret.isActive() ? "Every " + interval + " • Next: " + nextDate : "Paused";
            return status;
        }

        private String pluralize(String unit, int value) {
            return value == 1 ? unit : unit + "s";
        }
    }
}