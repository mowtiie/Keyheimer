package com.mowtiie.keyheimer;

import android.app.Application;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.mowtiie.keyheimer.util.AppLockManager;

public class KeyheimerApp extends Application implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        AppLockManager.getInstance().markLocked();
    }
}