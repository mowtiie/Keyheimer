package com.mowtiie.keyheimer.scheduling;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.mowtiie.keyheimer.R;
import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.ui.activities.MainActivity;

public final class NotificationHelper {

    private static final String CHANNEL_ID = "keyheimer_reminders";

    private NotificationHelper() {
    }

    static void showReminder(Context context, Secret secret) {
        ensureChannel(context);

        Intent verifyIntent = new Intent(context, MainActivity.class);
        verifyIntent.putExtra(MainActivity.EXTRA_VERIFY_SECRET_ID, secret.getId());
        verifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                secret.getId().hashCode(),
                verifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_keyheimer)
                .setContentTitle("Time to recall " + secret.getName())
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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Passphrase reminders", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Reminders to recall your stored passphrases");
            manager.createNotificationChannel(channel);
        }
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }
}