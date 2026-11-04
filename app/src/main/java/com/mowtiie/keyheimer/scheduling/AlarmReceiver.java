package com.mowtiie.keyheimer.scheduling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.IntervalConverter;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String secretId = intent.getStringExtra(ReminderScheduler.EXTRA_SECRET_ID);
        if (secretId == null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        PendingResult result = goAsync();
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                SecretDao dao = new SecretDao(appContext);
                Secret secret = dao.getById(secretId);
                if (secret == null || !secret.isActive()) {
                    return;
                }

                long nextTriggerAt = IntervalConverter.computeNextTriggerAt(secret.getIntervalValue(), secret.getIntervalUnit(), secret.getReminderHour(), secret.getReminderMinute());
                secret.setNextTriggerAt(nextTriggerAt);
                dao.update(secret);

                ReminderScheduler.scheduleReminder(appContext, secret);
                NotificationHelper.showReminder(appContext, secret);
            } finally {
                result.finish();
            }
        });
    }
}
