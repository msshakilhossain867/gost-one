package com.citomoltd.banglagoststory.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.citomoltd.banglagoststory.MainActivity;
import com.citomoltd.banglagoststory.R;
import com.citomoltd.banglagoststory.StoryViewActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class StoryReminderManager {

    private static final String TAG = "StoryReminderManager";
    public static final String PREFS_REMINDER = "story_reminder_prefs";
    public static final String KEY_LAST_APP_OPEN = "last_app_open_time";
    public static final String KEY_LAST_STORY_READ = "last_story_read_time";
    public static final String KEY_LAST_REMINDER_DATE = "last_reminder_date";

    public static final String CHANNEL_ID = "daily_story_reminder_channel";
    public static final int NOTIFICATION_ID = 2005;
    public static final String ACTION_DAILY_REMINDER = "com.citomoltd.banglagoststory.ACTION_DAILY_REMINDER";

    public static void scheduleDailyReminder(Context context) {
        if (context == null) return;

        SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean isNotifyEnabled = settings.getBoolean("notifications_enabled", true);
        if (!isNotifyEnabled) {
            cancelReminder(context);
            return;
        }

        Calendar now = Calendar.getInstance();

        // Reminder scheduled between 5:00 PM and 10:00 PM (Default at 6:30 PM / 18:30)
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, 18);
        target.set(Calendar.MINUTE, 30);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        // If current time is already past 6:30 PM today, schedule for tomorrow 6:30 PM
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, StoryReminderReceiver.class);
        intent.setAction(ACTION_DAILY_REMINDER);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                1005,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
            }
            Log.d(TAG, "Daily reminder scheduled for: " + target.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule daily reminder: " + e.getMessage());
        }
    }

    public static void cancelReminder(Context context) {
        if (context == null) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, StoryReminderReceiver.class);
        intent.setAction(ACTION_DAILY_REMINDER);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                1005,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
            Log.d(TAG, "Daily reminder cancelled.");
        }
    }

    public static void recordAppOpened(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_REMINDER, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_APP_OPEN, System.currentTimeMillis())
                .apply();
    }

    public static void recordStoryRead(Context context, String storyTitle) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_REMINDER, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_STORY_READ, System.currentTimeMillis())
                .apply();
    }

    public static void sendStoryReminderNotification(Context context) {
        if (context == null) return;

        SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean isNotifyEnabled = settings.getBoolean("notifications_enabled", true);
        if (!isNotifyEnabled) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_REMINDER, Context.MODE_PRIVATE);
        long lastAppOpen = prefs.getLong(KEY_LAST_APP_OPEN, 0);
        long lastStoryRead = prefs.getLong(KEY_LAST_STORY_READ, 0);
        long lastActivity = Math.max(lastAppOpen, lastStoryRead);
        long nowMs = System.currentTimeMillis();

        // 1. Inactivity check: if user already entered/read in the app in last 4 hours, skip notification today
        if (lastActivity > 0 && (nowMs - lastActivity) < 4L * 60 * 60 * 1000) {
            Log.d(TAG, "User was recently active in the app. Skipping notification today.");
            scheduleDailyReminder(context);
            return;
        }

        // 2. Once per day check: avoid duplicate notifications on the same date
        String todayDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String lastSentDate = prefs.getString(KEY_LAST_REMINDER_DATE, "");
        if (todayDate.equals(lastSentDate)) {
            Log.d(TAG, "Reminder already sent today.");
            scheduleDailyReminder(context);
            return;
        }

        // 3. Evening window check: between 17:00 (5 PM) and 22:00 (10 PM)
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 17 || hour >= 22) {
            Log.d(TAG, "Current hour (" + hour + ") is outside 17:00 - 22:00 window.");
            scheduleDailyReminder(context);
            return;
        }

        // 4. Fetch random story from database
        DatabaseHelper db = new DatabaseHelper(context);
        HashMap<String, String> randomStory = db.getRandomStory();

        String storyName = (randomStory != null && randomStory.get("name") != null)
                ? randomStory.get("name")
                : "অদ্ভূত রহস্যময় পরিণয়";
        String storyLink = (randomStory != null && randomStory.get("link") != null && !randomStory.get("link").isEmpty())
                ? randomStory.get("link")
                : "rs1.txt";

        TwoLineView twoLineView = new TwoLineView(context);
        String preview = "";
        if (storyLink != null && !storyLink.isEmpty()) {
            preview = twoLineView.readFirstTwoLinesFromFile(storyLink);
        }
        if (preview.isEmpty()) {
            preview = "আজ সন্ধ্যার বিশেষ ভৌতিক গল্পটি এখনও পড়েননি? গা ছমছমে রহস্যের স্বাদ নিন এখনই।";
        }

        // 5. Create Notification Channel
        createNotificationChannel(context);

        // 6. Build Intent to open StoryViewActivity with proper back stack to MainActivity
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent storyIntent = new Intent(context, StoryViewActivity.class);
        storyIntent.putExtra("story_title", storyName);
        storyIntent.putExtra("story_content", storyLink);
        storyIntent.putExtra("from_notification", true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(mainIntent);
        stackBuilder.addNextIntent(storyIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                1006,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String notifTitle = "👻 আজকের রোমাঞ্চকর গল্প: " + storyName;
        String notifBody = preview + " পড়তে ট্যাপ করুন...";

        Bitmap largeIcon = null;
        try {
            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.img_cat_horror);
        } catch (Exception ignored) {}

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_auto_stories_24)
                .setContentTitle(notifTitle)
                .setContentText(notifBody)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(preview + "\n\n📖 রোমাঞ্চকর সম্পূর্ণ গল্পটি পড়তে এখনই ট্যাপ করুন!")
                        .setBigContentTitle(notifTitle)
                        .setSummaryText("গল্পের দৈনিক রিমাইন্ডার"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.accent_horror))
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            nm.notify(NOTIFICATION_ID, builder.build());
            prefs.edit().putString(KEY_LAST_REMINDER_DATE, todayDate).apply();
            Log.d(TAG, "Daily story reminder notification displayed successfully: " + storyName);
        }

        // Reschedule for next day
        scheduleDailyReminder(context);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "দৈনিক গল্প পড়ার রিমাইন্ডার",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("প্রতিদিন বিকাল ৫টা থেকে রাত ১০টার মধ্যে নতুন গল্পের রিমাইন্ডার নোটিফিকেশন");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
