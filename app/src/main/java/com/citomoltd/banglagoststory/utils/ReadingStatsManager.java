package com.citomoltd.banglagoststory.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReadingStatsManager {

    private static final String PREF_NAME = "reading_stats";
    private static final String KEY_TOTAL_READ_MINUTES = "total_read_minutes";
    private static final String KEY_STORIES_COMPLETED = "stories_completed";
    private static final String KEY_STREAK_DAYS = "streak_days";
    private static final String KEY_LAST_READ_DATE = "last_read_date";

    // Resume reading
    private static final String KEY_LAST_STORY_TITLE = "last_story_title";
    private static final String KEY_LAST_STORY_FILE = "last_story_file";
    private static final String KEY_LAST_STORY_PROGRESS = "last_story_progress"; // 0 to 100
    private static final String KEY_LAST_STORY_TIMESTAMP = "last_story_timestamp";

    private final SharedPreferences prefs;

    public ReadingStatsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        updateDailyStreak();
    }

    public void addReadingTime(int seconds) {
        if (seconds <= 0) return;
        int currentMinutes = prefs.getInt(KEY_TOTAL_READ_MINUTES, 0);
        int addedMinutes = Math.max(1, seconds / 60);
        prefs.edit().putInt(KEY_TOTAL_READ_MINUTES, currentMinutes + addedMinutes).apply();
    }

    public void incrementStoriesCompleted() {
        int count = prefs.getInt(KEY_STORIES_COMPLETED, 0);
        prefs.edit().putInt(KEY_STORIES_COMPLETED, count + 1).apply();
    }

    public int getTotalReadMinutes() {
        return prefs.getInt(KEY_TOTAL_READ_MINUTES, 0);
    }

    public int getStoriesCompleted() {
        return prefs.getInt(KEY_STORIES_COMPLETED, 0);
    }

    public int getStreakDays() {
        return prefs.getInt(KEY_STREAK_DAYS, 1);
    }

    public String getReaderRank() {
        int minutes = getTotalReadMinutes();
        if (minutes < 30) {
            return "👻 শিক্ষানবিস পাঠক";
        } else if (minutes < 120) {
            return "🔦 অশরীরী অনুসন্ধানী";
        } else if (minutes < 300) {
            return "📿 তান্ত্রিক শিকারী";
        } else {
            return "💀 অমর ভৌতিক মাস্টার";
        }
    }

    private void updateDailyStreak() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = prefs.getString(KEY_LAST_READ_DATE, "");

        if (!today.equals(lastDate)) {
            int currentStreak = prefs.getInt(KEY_STREAK_DAYS, 0);
            if (!lastDate.isEmpty()) {
                // Check if yesterday
                long diff = System.currentTimeMillis() - prefs.getLong("last_read_millis", 0);
                long oneDayMillis = 24 * 60 * 60 * 1000L;
                if (diff < 2 * oneDayMillis) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
            } else {
                currentStreak = 1;
            }

            prefs.edit()
                    .putString(KEY_LAST_READ_DATE, today)
                    .putLong("last_read_millis", System.currentTimeMillis())
                    .putInt(KEY_STREAK_DAYS, currentStreak)
                    .apply();
        }
    }

    // Save current reading position for "Continue Reading"
    public void saveLastRead(String title, String file, int progressPercent) {
        if (title == null || title.isEmpty() || file == null || file.isEmpty()) return;
        prefs.edit()
                .putString(KEY_LAST_STORY_TITLE, title)
                .putString(KEY_LAST_STORY_FILE, file)
                .putInt(KEY_LAST_STORY_PROGRESS, Math.max(0, Math.min(100, progressPercent)))
                .putLong(KEY_LAST_STORY_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    public boolean hasLastRead() {
        return !getLastReadTitle().isEmpty() && !getLastReadFile().isEmpty();
    }

    public String getLastReadTitle() {
        return prefs.getString(KEY_LAST_STORY_TITLE, "");
    }

    public String getLastReadFile() {
        return prefs.getString(KEY_LAST_STORY_FILE, "");
    }

    public int getLastReadProgress() {
        return prefs.getInt(KEY_LAST_STORY_PROGRESS, 0);
    }
}
