package com.citomoltd.banglagoststory.fragments;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.citomoltd.banglagoststory.MainActivity;
import com.citomoltd.banglagoststory.R;
import com.citomoltd.banglagoststory.utils.CoinManager;
import com.citomoltd.banglagoststory.utils.ReadingStatsManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    // Profile & Stats Views
    private TextView tvAppVersion, tvUserRank;
    private TextView tvStatReadingTime, tvStatStoriesDone, tvStatCoins;

    // Theme Segmented Cards
    private MaterialCardView btnThemeLight, btnThemeDark, btnThemeSystem;
    private TextView tvLabelThemeLight, tvLabelThemeDark, tvLabelThemeSystem;
    private TextView tvCurrentTheme;

    // Preferences & Control Views
    private View rowFontSize;
    private TextView tvCurrentFontSize;
    private MaterialSwitch switchNotifications;
    private View rowClearCache;
    private TextView tvCacheSize;

    // Actions
    private View rate_us, shareApp, checkUpdate, more_app, privacy_policy;

    private ReadingStatsManager readingStatsManager;
    private CoinManager coinManager;

    private static final String PREFS_NAME = "ZoomPrefs";
    private static final String TEXT_SIZE_KEY = "TextSize";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        readingStatsManager = new ReadingStatsManager(requireContext());
        coinManager = CoinManager.getInstance(requireContext());

        // Initialize UI Elements
        tvAppVersion = view.findViewById(R.id.tvAppVersion);
        tvUserRank = view.findViewById(R.id.tvUserRank);
        tvStatReadingTime = view.findViewById(R.id.tvStatReadingTime);
        tvStatStoriesDone = view.findViewById(R.id.tvStatStoriesDone);
        tvStatCoins = view.findViewById(R.id.tvStatCoins);

        btnThemeLight = view.findViewById(R.id.btnThemeLight);
        btnThemeDark = view.findViewById(R.id.btnThemeDark);
        btnThemeSystem = view.findViewById(R.id.btnThemeSystem);
        tvLabelThemeLight = view.findViewById(R.id.tvLabelThemeLight);
        tvLabelThemeDark = view.findViewById(R.id.tvLabelThemeDark);
        tvLabelThemeSystem = view.findViewById(R.id.tvLabelThemeSystem);
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme);

        rowFontSize = view.findViewById(R.id.rowFontSize);
        tvCurrentFontSize = view.findViewById(R.id.tvCurrentFontSize);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        rowClearCache = view.findViewById(R.id.rowClearCache);
        tvCacheSize = view.findViewById(R.id.tvCacheSize);

        rate_us = view.findViewById(R.id.rate_us);
        shareApp = view.findViewById(R.id.shareApp);
        checkUpdate = view.findViewById(R.id.checkUpdate);
        more_app = view.findViewById(R.id.more_app);
        privacy_policy = view.findViewById(R.id.privacy_policy);

        setupAppVersion();
        setupProfileAndStats();
        setupThemeSelector();
        setupFontSize();
        setupNotificationsSwitch();
        setupCacheCleaner();
        setupActionListeners();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupProfileAndStats();
        updateCacheSizeText();
    }

    private void setupAppVersion() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            tvAppVersion.setText("ভার্সন " + pInfo.versionName + " • অফলাইন হরর রিডার");
        } catch (Exception e) {
            tvAppVersion.setText("ভার্সন ১.৭ • অফলাইন হরর রিডার");
        }
    }

    private void setupProfileAndStats() {
        if (readingStatsManager != null) {
            int minutes = readingStatsManager.getTotalReadMinutes();
            int stories = readingStatsManager.getStoriesCompleted();
            String rank = readingStatsManager.getReaderRank();

            tvStatReadingTime.setText(String.format(Locale.getDefault(), "%d মি.", minutes));
            tvStatStoriesDone.setText(String.format(Locale.getDefault(), "%d টি", stories));
            tvUserRank.setText(rank);
        }

        if (coinManager != null) {
            int coins = coinManager.getCoins();
            tvStatCoins.setText(String.valueOf(coins));
        }
    }

    private void setupThemeSelector() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        int currentTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (!prefs.contains("theme_mode") && prefs.contains("dark_mode")) {
            currentTheme = prefs.getBoolean("dark_mode", false)
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;
        }

        updateThemeSegmentUI(currentTheme);

        btnThemeLight.setOnClickListener(v -> applyTheme(AppCompatDelegate.MODE_NIGHT_NO));
        btnThemeDark.setOnClickListener(v -> applyTheme(AppCompatDelegate.MODE_NIGHT_YES));
        btnThemeSystem.setOnClickListener(v -> applyTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    private void updateThemeSegmentUI(int themeMode) {
        int colorPrimary = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int colorStroke = ContextCompat.getColor(requireContext(), R.color.card_stroke);
        int colorSurfaceSec = ContextCompat.getColor(requireContext(), R.color.card_surface_secondary);
        int colorTextPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int colorTextSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        // Reset all 3
        resetButton(btnThemeLight, tvLabelThemeLight, colorStroke, colorSurfaceSec, colorTextSecondary);
        resetButton(btnThemeDark, tvLabelThemeDark, colorStroke, colorSurfaceSec, colorTextSecondary);
        resetButton(btnThemeSystem, tvLabelThemeSystem, colorStroke, colorSurfaceSec, colorTextSecondary);

        if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            highlightButton(btnThemeLight, tvLabelThemeLight, colorPrimary, colorTextPrimary);
            tvCurrentTheme.setText("☀️ লাইট মোড (ডে থিম) সক্রিয় রয়েছে");
        } else if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            highlightButton(btnThemeDark, tvLabelThemeDark, colorPrimary, colorTextPrimary);
            tvCurrentTheme.setText("🌙 নাইট মোড (ডার্ক থিম) সক্রিয় রয়েছে");
        } else {
            highlightButton(btnThemeSystem, tvLabelThemeSystem, colorPrimary, colorTextPrimary);
            tvCurrentTheme.setText("⚙️ সিস্টেম অটো থিম সক্রিয় রয়েছে");
        }
    }

    private void resetButton(MaterialCardView card, TextView label, int strokeColor, int bgColor, int textColor) {
        card.setStrokeColor(strokeColor);
        card.setStrokeWidth((int) (1.2f * getResources().getDisplayMetrics().density));
        card.setCardBackgroundColor(bgColor);
        label.setTextColor(textColor);
    }

    private void highlightButton(MaterialCardView card, TextView label, int highlightColor, int textColor) {
        card.setStrokeColor(highlightColor);
        card.setStrokeWidth((int) (2.5f * getResources().getDisplayMetrics().density));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_surface));
        label.setTextColor(highlightColor);
    }

    private void applyTheme(int newMode) {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("theme_mode", newMode)
                .putBoolean("dark_mode", newMode == AppCompatDelegate.MODE_NIGHT_YES)
                .apply();

        AppCompatDelegate.setDefaultNightMode(newMode);
        updateThemeSegmentUI(newMode);

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("last_selected_tab", R.id.nav_settings)
                    .apply();

            Intent intent = mainActivity.getIntent();
            intent.putExtra("from_recreate", true);
            mainActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            mainActivity.recreate();
        }
    }

    private void setupFontSize() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        float currentSize = prefs.getFloat(TEXT_SIZE_KEY, 18f);
        updateFontSizeLabel(currentSize);

        rowFontSize.setOnClickListener(v -> {
            String[] options = {
                    "ছোট (Small - ১৬ pt)",
                    "স্বাভাবিক (Medium - ১৮ pt)",
                    "বড় (Large - ২২ pt)",
                    "অতি বড় (Extra Large - ২৬ pt)"
            };
            float[] sizes = {16f, 18f, 22f, 26f};

            float curSize = prefs.getFloat(TEXT_SIZE_KEY, 18f);
            int selectedIndex = 1;
            for (int i = 0; i < sizes.length; i++) {
                if (Math.abs(sizes[i] - curSize) < 1f) {
                    selectedIndex = i;
                    break;
                }
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("গল্পের ফন্ট সাইজ নির্বাচন করুন")
                    .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                        float chosenSize = sizes[which];
                        prefs.edit().putFloat(TEXT_SIZE_KEY, chosenSize).apply();
                        updateFontSizeLabel(chosenSize);
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "ফন্ট সাইজ পরিবর্তিত হয়েছে!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        });
    }

    private void updateFontSizeLabel(float size) {
        if (size <= 16f) {
            tvCurrentFontSize.setText("১৬ pt (ছোট)");
        } else if (size <= 18f) {
            tvCurrentFontSize.setText("১৮ pt (স্বাভাবিক)");
        } else if (size <= 22f) {
            tvCurrentFontSize.setText("২২ pt (বড়)");
        } else {
            tvCurrentFontSize.setText("২৬ pt (অতি বড়)");
        }
    }

    private void setupNotificationsSwitch() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean isNotifyEnabled = prefs.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(isNotifyEnabled);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
            if (isChecked) {
                com.citomoltd.banglagoststory.utils.StoryReminderManager.scheduleDailyReminder(requireContext());
                Toast.makeText(requireContext(), "দৈনিক গল্পের রিমাইন্ডার চালু হয়েছে", Toast.LENGTH_SHORT).show();
            } else {
                com.citomoltd.banglagoststory.utils.StoryReminderManager.cancelReminder(requireContext());
                Toast.makeText(requireContext(), "দৈনিক গল্পের রিমাইন্ডার বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCacheCleaner() {
        updateCacheSizeText();
        rowClearCache.setOnClickListener(v -> clearAppCache());
    }

    private void updateCacheSizeText() {
        try {
            long cacheSize = getDirSize(requireContext().getCacheDir());
            File externalCache = requireContext().getExternalCacheDir();
            if (externalCache != null) {
                cacheSize += getDirSize(externalCache);
            }

            if (cacheSize <= 0) {
                tvCacheSize.setText("০.০ KB (মেমোরি সম্পূর্ণ পরিষ্কার)");
            } else {
                double sizeMb = cacheSize / (1024.0 * 1024.0);
                if (sizeMb >= 1.0) {
                    tvCacheSize.setText(String.format(Locale.getDefault(), "%.1f MB মেমোরি খালি করা যাবে", sizeMb));
                } else {
                    double sizeKb = cacheSize / 1024.0;
                    tvCacheSize.setText(String.format(Locale.getDefault(), "%.1f KB মেমোরি খালি করা যাবে", sizeKb));
                }
            }
        } catch (Exception e) {
            tvCacheSize.setText("০.০ KB (পরিষ্কার)");
        }
    }

    private long getDirSize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirSize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }

    private void clearAppCache() {
        try {
            deleteDir(requireContext().getCacheDir());
            File externalCache = requireContext().getExternalCacheDir();
            if (externalCache != null) {
                deleteDir(externalCache);
            }
            updateCacheSizeText();
            Toast.makeText(requireContext(), "অ্যাপ ক্যাশ মেমোরি সম্পূর্ণ পরিষ্কার হয়েছে!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "ক্যাশ পরিষ্কার করা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void setupActionListeners() {
        rate_us.setOnClickListener(v -> openAppRating());
        shareApp.setOnClickListener(v -> shareApp(requireContext()));
        checkUpdate.setOnClickListener(v -> openAppRating());
        more_app.setOnClickListener(v -> openMoreApps());
        privacy_policy.setOnClickListener(v -> openPrivacyPolicy());
    }

    private void openPrivacyPolicy() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/rohossomoy-voter-golpo-policy"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "গোপনীয়তা নীতি লোড করা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void openMoreApps() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=citomoltd")));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "প্লে স্টোর পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    public void shareApp(Context context) {
        try {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "বাংলা রহস্যময় ও ভৌতিক গল্পের সেরা অ্যাপ ডাউনলোড করুন:\nhttps://play.google.com/store/apps/details?id=" + context.getPackageName());
            sendIntent.setType("text/plain");
            context.startActivity(Intent.createChooser(sendIntent, "শেয়ার করুন"));
        } catch (Exception e) {
            Toast.makeText(context, "অ্যাপ শেয়ার করা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppRating() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + requireActivity().getPackageName())));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + requireActivity().getPackageName())));
            } catch (Exception ignored) {}
        }
    }
}
