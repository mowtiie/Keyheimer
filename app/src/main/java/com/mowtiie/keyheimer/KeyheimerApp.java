package com.mowtiie.keyheimer;

import android.app.Application;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.mowtiie.keyheimer.data.Secret;
import com.mowtiie.keyheimer.data.SecretDao;
import com.mowtiie.keyheimer.scheduling.ReminderScheduler;
import com.mowtiie.keyheimer.util.AppExecutors;
import com.mowtiie.keyheimer.util.AppLockManager;
import com.mowtiie.keyheimer.util.CrashHandler;

public class KeyheimerApp extends Application implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.install(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        repairStoredIntervals();
    }

    private void repairStoredIntervals() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            for (Secret secret : new SecretDao(this).repairIntervals()) {
                if (secret.isActive()) {
                    ReminderScheduler.scheduleReminder(this, secret);
                }
            }
        });
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        AppLockManager.getInstance().markLocked();
    }
}