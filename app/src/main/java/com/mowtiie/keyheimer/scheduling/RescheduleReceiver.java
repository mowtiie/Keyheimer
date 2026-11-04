package com.mowtiie.keyheimer.scheduling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.IntervalConverter;

import java.util.List;

public class RescheduleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean timeZoneChanged = Intent.ACTION_TIMEZONE_CHANGED.equals(action);
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !timeZoneChanged) {
            return;
        }

        Context appContext = context.getApplicationContext();
        PendingResult result = goAsync();
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                SecretDao dao = new SecretDao(appContext);
                List<Secret> secrets = dao.getAll();
                for (Secret secret : secrets) {
                    if (!secret.isActive()) {
                        continue;
                    }
                    if (timeZoneChanged) {
                        long aligned = IntervalConverter.alignToReminderTime(
                                secret.getNextTriggerAt(), secret.getReminderHour(), secret.getReminderMinute());
                        if (aligned != secret.getNextTriggerAt()) {
                            secret.setNextTriggerAt(aligned);
                            dao.updateNextTrigger(secret.getId(), aligned);
                        }
                    }
                    ReminderScheduler.scheduleReminder(appContext, secret);
                }
            } finally {
                result.finish();
            }
        });
    }
}
