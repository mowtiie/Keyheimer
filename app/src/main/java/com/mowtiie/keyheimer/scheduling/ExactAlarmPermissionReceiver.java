package com.mowtiie.keyheimer.scheduling;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.preference.PreferenceManager;

import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.PreferenceKeys;

import java.util.List;

public class ExactAlarmPermissionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (!ReminderScheduler.canScheduleExactAlarms(appContext)) {
            PreferenceManager.getDefaultSharedPreferences(appContext).edit()
                    .remove(PreferenceKeys.KEY_EXACT_ALARM_PROMPT_DISMISSED)
                    .apply();
        }

        PendingResult result = goAsync();
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                List<Secret> secrets = new SecretDao(appContext).getAll();
                for (Secret secret : secrets) {
                    if (secret.isActive()) {
                        ReminderScheduler.scheduleReminder(appContext, secret);
                    }
                }
            } finally {
                result.finish();
            }
        });
    }
}
