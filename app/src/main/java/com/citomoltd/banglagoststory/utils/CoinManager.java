package com.citomoltd.banglagoststory.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CoinManager {

    private static final String PREF_NAME = "BanglaGhostStoryPrefs";
    private static final String KEY_COIN_BALANCE = "coin_balance";
    private static final String KEY_DAILY_BONUS_TIME = "daily_bonus_time";
    private static final String KEY_LAST_READ_TITLE = "last_read_title";
    private static final String KEY_LAST_READ_FILE = "last_read_file";
    private static final String KEY_LAST_READ_TIME = "last_read_time";

    private final SharedPreferences prefs;

    private static CoinManager instance;

    public static synchronized CoinManager getInstance(Context context) {
        if (instance == null) {
            instance = new CoinManager(context.getApplicationContext());
        }
        return instance;
    }

    private CoinManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Backwards compatibility migration
        SharedPreferences adPrefs = context.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE);
        SharedPreferences unlockPrefs = context.getSharedPreferences("UnlockPrefs", Context.MODE_PRIVATE);

        if (!prefs.contains(KEY_COIN_BALANCE)) {
            int oldAdCoins = adPrefs.getInt("coin_balance", -1);
            int oldUnlockCoins = unlockPrefs.getInt("coins", -1);
            int initialCoins = 20;
            if (oldAdCoins != -1) {
                initialCoins = oldAdCoins;
            } else if (oldUnlockCoins != -1) {
                initialCoins = oldUnlockCoins;
            }
            prefs.edit().putInt(KEY_COIN_BALANCE, initialCoins).apply();
        }
    }

    public int getCoins() {
        return prefs.getInt(KEY_COIN_BALANCE, 20);
    }

    public void addCoins(int amount) {
        int current = getCoins();
        prefs.edit().putInt(KEY_COIN_BALANCE, current + amount).apply();
    }

    public boolean deductCoins(int amount) {
        int current = getCoins();
        if (current >= amount) {
            prefs.edit().putInt(KEY_COIN_BALANCE, current - amount).apply();
            return true;
        }
        return false;
    }

    public boolean isStoryUnlocked(String storyIdOrFile) {
        if (storyIdOrFile == null || storyIdOrFile.isEmpty()) return false;
        return prefs.getBoolean("story_unlocked_" + storyIdOrFile, false);
    }

    public void unlockStory(String storyIdOrFile) {
        if (storyIdOrFile != null && !storyIdOrFile.isEmpty()) {
            prefs.edit().putBoolean("story_unlocked_" + storyIdOrFile, true).apply();
        }
    }

    public boolean canClaimDailyBonus() {
        long lastClaim = prefs.getLong(KEY_DAILY_BONUS_TIME, 0);
        long now = System.currentTimeMillis();
        return (now - lastClaim) >= (24 * 60 * 60 * 1000L);
    }

    public void claimDailyBonus(int bonusAmount) {
        addCoins(bonusAmount);
        prefs.edit().putLong(KEY_DAILY_BONUS_TIME, System.currentTimeMillis()).apply();
    }

    public void saveLastRead(String title, String file) {
        prefs.edit()
                .putString(KEY_LAST_READ_TITLE, title)
                .putString(KEY_LAST_READ_FILE, file)
                .putLong(KEY_LAST_READ_TIME, System.currentTimeMillis())
                .apply();
    }

    public String getLastReadTitle() {
        return prefs.getString(KEY_LAST_READ_TITLE, null);
    }

    public String getLastReadFile() {
        return prefs.getString(KEY_LAST_READ_FILE, null);
    }
}
