package com.mowtiie.keyheimer.scheduling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.util.AppExecutors;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Context appContext = context.getApplicationContext();
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
