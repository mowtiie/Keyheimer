package com.mowtiie.keyheimer.scheduling;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.mowtiie.keyheimer.data.Secret;

final class NotificationHelper {

    private static final String CHANNEL_ID = "keyheimer_reminders";

    private NotificationHelper() {
    }

    static void showReminder(Context context, Secret secret) {
        ensureChannel(context);

        Intent verifyIntent = new Intent(Intent.ACTION_VIEW);
        verifyIntent.setClassName(context, "com.keyheimer.ui.VerifyActivity");
        verifyIntent.putExtra("secret_id", secret.getId());
        verifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                secret.getId().hashCode(),
                verifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("Time to recall: " + secret.getName())
                .setContentText("Tap to verify you still remember it")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(secret.getId().hashCode(), builder.build());
    }

    private static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Passphrase reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Reminders to recall your stored passphrases");
            manager.createNotificationChannel(channel);
        }
    }
}