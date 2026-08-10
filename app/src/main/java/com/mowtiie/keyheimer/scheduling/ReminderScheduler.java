package com.mowtiie.keyheimer.scheduling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.mowtiie.keyheimer.data.Secret;

public final class ReminderScheduler {

    static final String EXTRA_SECRET_ID = "secret_id";

    private ReminderScheduler() {
    }

    public static void scheduleReminder(Context context, Secret secret) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        PendingIntent pendingIntent = buildPendingIntent(context, secret.getId());
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                secret.getNextTriggerAt(),
                pendingIntent
        );
    }

    public static void cancelReminder(Context context, String secretId) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        alarmManager.cancel(buildPendingIntent(context, secretId));
    }

    private static PendingIntent buildPendingIntent(Context context, String secretId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_SECRET_ID, secretId);
        return PendingIntent.getBroadcast(
                context,
                secretId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}