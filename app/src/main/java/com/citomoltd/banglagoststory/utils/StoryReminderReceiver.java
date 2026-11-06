package com.citomoltd.banglagoststory.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StoryReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_DAILY_REMINDER = "com.citomoltd.banglagoststory.ACTION_DAILY_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        Log.d("StoryReminderReceiver", "Broadcast received with action: " + action);

        if (ACTION_DAILY_REMINDER.equals(action)) {
            // Trigger reminder if user has been inactive today between 5 PM and 10 PM
            StoryReminderManager.sendStoryReminderNotification(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            // Reschedule alarm on system boot or app update
            StoryReminderManager.scheduleDailyReminder(context);
        }
    }
}
