package com.citomoltd.banglagoststory.utils;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Collections;

public class MyApp extends Application {

    private static AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Configure MobileAds with test device ID on emulator/test devices
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                .setTestDeviceIds(Collections.singletonList(AdRequest.DEVICE_ID_EMULATOR))
                .build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {});

        // Initialize App Open Ad Manager
        appOpenManager = new AppOpenManager(this);

        // Theme Setup: Light, Dark, System
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (!prefs.contains("theme_mode") && prefs.contains("dark_mode")) {
            themeMode = prefs.getBoolean("dark_mode", false)
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;
        }
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // On cloud/emulator test environments, suppress FCM auto-init to prevent TOO_MANY_REGISTRATIONS
        if (isRunningOnEmulator()) {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().setAutoInitEnabled(false);
            } catch (Throwable ignored) {}
        }

        // Schedule Daily Inactivity Story Reminder (5:00 PM - 10:00 PM)
        StoryReminderManager.scheduleDailyReminder(this);
    }

    public static boolean isRunningOnEmulator() {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("google_sdk"));
    }

    public static AppOpenManager getAppOpenManager() {
        return appOpenManager;
    }
}


