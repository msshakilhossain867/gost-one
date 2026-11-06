package com.citomoltd.banglagoststory;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.citomoltd.banglagoststory.adapters.TextChunkAdapter;
import com.citomoltd.banglagoststory.utils.AdHelper;
import com.citomoltd.banglagoststory.utils.CoinManager;
import com.citomoltd.banglagoststory.utils.DatabaseHelper;
import com.citomoltd.banglagoststory.utils.ReadingStatsManager;
import com.citomoltd.banglagoststory.utils.StoryQuoteCardGenerator;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StoryViewActivity extends AppCompatActivity {

    private View storyViewRoot;
    private RecyclerView recyclerView;
    private TextChunkAdapter adapter;
    private List<String> textChunks;
    private ImageView back_arrow, bookmarkIcon, btnTts, btnShare;
    private ImageView btnReadingTheme;
    private TextView stToolbar, tvReadingTime;
    private View ln_bookmark, zoomControls;
    private View zoomIn, zoomOut;
    private ProgressBar progressBarReading;
    private FrameLayout adContainer;

    // Reading Stats
    private ReadingStatsManager readingStatsManager;
    private long sessionStartTime = 0;
    private int currentProgress = 0;

    private DatabaseHelper databaseHelper;
    private CoinManager coinManager;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideButtonsRunnable = this::hideZoomButtons;
    private float textSize = 18f;
    private static final float MIN_TEXT_SIZE = 14f;
    private static final float MAX_TEXT_SIZE = 30f;

    private static final String PREFS_NAME = "ZoomPrefs";
    private static final String TEXT_SIZE_KEY = "TextSize";
    private static final String KEY_READING_THEME = "reading_canvas_theme";

    private String storyTitle = "";
    private String storyContentFile = "";
    private boolean isBookmarked = false;

    // TTS Audio feature
    private TextToSpeech textToSpeech;
    private boolean isSpeaking = false;
    private int currentTtsChunk = 0;

    // App Update
    private AppUpdateManager appUpdateManager;
    private ActivityResultLauncher<IntentSenderRequest> activityResultLauncher;
    private InstallStateUpdatedListener updateListener;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_story_view);
        storyViewRoot = findViewById(R.id.storyViewRoot);

        ViewCompat.setOnApplyWindowInsetsListener(storyViewRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerView);
        back_arrow = findViewById(R.id.back_arrow);
        stToolbar = findViewById(R.id.stToolbar);
        tvReadingTime = findViewById(R.id.tvReadingTime);
        ln_bookmark = findViewById(R.id.ln_bookmark);
        bookmarkIcon = findViewById(R.id.bookmark);
        btnTts = findViewById(R.id.btnTts);
        btnShare = findViewById(R.id.btnShare);
        btnReadingTheme = findViewById(R.id.btnReadingTheme);

        zoomIn = findViewById(R.id.zoomInButton);
        zoomOut = findViewById(R.id.zoomOutButton);
        zoomControls = findViewById(R.id.zoomControls);
        progressBarReading = findViewById(R.id.progressBarReading);
        adContainer = findViewById(R.id.adContainer);

        databaseHelper = new DatabaseHelper(this);
        coinManager = CoinManager.getInstance(this);
        readingStatsManager = new ReadingStatsManager(this);
        sessionStartTime = System.currentTimeMillis();

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        textSize = preferences.getFloat(TEXT_SIZE_KEY, 18f);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        textChunks = new ArrayList<>();
        adapter = new TextChunkAdapter(this, textChunks);
        adapter.setTextSize(textSize);
        recyclerView.setAdapter(adapter);

        storyTitle = getIntent().getStringExtra("story_title");
        storyContentFile = getIntent().getStringExtra("story_content");
        if (storyTitle == null) storyTitle = "গল্প";
        stToolbar.setText(storyTitle);

        if (storyContentFile == null || storyContentFile.trim().isEmpty()) {
            storyContentFile = databaseHelper.getStoryLinkByName(storyTitle);
            if (storyContentFile == null || storyContentFile.trim().isEmpty()) {
                java.util.HashMap<String, String> fallbackStory = databaseHelper.getRandomStory();
                if (fallbackStory != null) {
                    if ("গল্প".equals(storyTitle) || storyTitle.isEmpty()) {
                        storyTitle = fallbackStory.get("name");
                        stToolbar.setText(storyTitle);
                    }
                    storyContentFile = fallbackStory.get("link");
                }
            }
        }

        if (storyContentFile != null && !storyContentFile.trim().isEmpty()) {
            storyContentFile = storyContentFile.trim();
            loadTextFileInChunks(storyContentFile);
            readingStatsManager.saveLastRead(storyTitle, storyContentFile, 0);
            coinManager.saveLastRead(storyTitle, storyContentFile);
            com.citomoltd.banglagoststory.utils.StoryReminderManager.recordStoryRead(this, storyTitle);
        }

        int savedTheme = preferences.getInt(KEY_READING_THEME, 0);
        applyReadingTheme(savedTheme, false);

        updateBookmarkUI();
        initTextToSpeech();
        initAppUpdate();

        AdHelper.initializeAd(this);
        AdHelper.loadBannerAd(this, adContainer);

        // Scroll listener for reading progress bar and zoom button auto-hide
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rView, int dx, int dy) {
                super.onScrolled(rView, dx, dy);

                int offset = rView.computeVerticalScrollOffset();
                int extent = rView.computeVerticalScrollExtent();
                int range = rView.computeVerticalScrollRange();

                if (range - extent > 0) {
                    // Fix: Added multiplication sign (*) here
                    currentProgress = (100 * offset) / (range - extent);
                    progressBarReading.setProgress(Math.min(100, Math.max(0, currentProgress)));
                }

                if (dy != 0) {
                    showZoomButtons();
                    handler.removeCallbacks(hideButtonsRunnable);
                    handler.postDelayed(hideButtonsRunnable, 2500);
                }
            }
        });

        zoomIn.setOnClickListener(v -> {
            if (textSize < MAX_TEXT_SIZE) {
                textSize += 2f;
                adapter.setTextSize(textSize);
                saveTextSize(textSize);
                scaleButton(zoomIn);
            }
        });

        zoomOut.setOnClickListener(v -> {
            if (textSize > MIN_TEXT_SIZE) {
                textSize -= 2f;
                adapter.setTextSize(textSize);
                saveTextSize(textSize);
                scaleButton(zoomOut);
            }
        });

        back_arrow.setOnClickListener(v -> navigateBack());

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack();
            }
        });

        ln_bookmark.setOnClickListener(v -> toggleBookmark());
        btnShare.setOnClickListener(v -> showShareOptionsDialog());
        btnTts.setOnClickListener(v -> toggleTts());
        btnReadingTheme.setOnClickListener(v -> showReadingThemeDialog());
    }

    private void navigateBack() {
        try {
            if (isTaskRoot() || getIntent().getBooleanExtra("from_notification", false)) {
                Intent homeIntent = new Intent(this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(homeIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showReadingThemeDialog() {
        String[] themes = {
                "🌑 Default Theme",
                "📜 Vintage Sepia",
                "🌑 OLED Pitch Black",
                "🩸 Horror Crimson",
                "❄️ Ghostly Blue"
        };

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentTheme = preferences.getInt(KEY_READING_THEME, 0);

        new AlertDialog.Builder(this)
                .setTitle("পড়ার থিম নির্বাচন (Reading Theme)")
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    dialog.dismiss();
                    preferences.edit().putInt(KEY_READING_THEME, which).apply();
                    applyReadingTheme(which, true);
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void applyReadingTheme(int themeIndex, boolean showToast) {
        int bgColor;
        int textColor;
        String name;

        switch (themeIndex) {
            case 1:
                bgColor = Color.parseColor("#251D14");
                textColor = Color.parseColor("#F5E6D3");
                name = "প্রাচীন পাণ্ডুলিপি থিম";
                break;
            case 2:
                bgColor = Color.parseColor("#000000");
                textColor = Color.parseColor("#E2E8F0");
                name = "নিশুতি অন্ধকার থিম";
                break;
            case 3:
                bgColor = Color.parseColor("#15060A");
                textColor = Color.parseColor("#FECDD3");
                name = "রক্তবর্ণ রোমাঞ্চ থিম";
                break;
            case 4:
                bgColor = Color.parseColor("#07111D");
                textColor = Color.parseColor("#BFDBFE");
                name = "শীতল কুয়াশা থিম";
                break;
            default:
                bgColor = ContextCompat.getColor(this, R.color.window_background);
                textColor = ContextCompat.getColor(this, R.color.text_primary);
                name = "ডিফল্ট থিম";
                break;
        }

        if (storyViewRoot != null) {
            storyViewRoot.setBackgroundColor(bgColor);
        }
        if (recyclerView != null) {
            recyclerView.setBackgroundColor(bgColor);
        }
        if (adapter != null) {
            adapter.setTextColor(textColor);
        }

        if (showToast) {
            Toast.makeText(this, name + " সক্রিয় হয়েছে", Toast.LENGTH_SHORT).show();
        }
    }

    private void showShareOptionsDialog() {
        String[] shareOptions = {
                "🖼️ Photo Card",
                "📝 Text Share"
        };

        new AlertDialog.Builder(this)
                .setTitle("গল্প শেয়ার করুন")
                .setItems(shareOptions, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) {
                        String excerpt = textChunks.isEmpty() ? storyTitle : textChunks.get(0);
                        StoryQuoteCardGenerator.shareQuoteCard(this, storyTitle, excerpt);
                    } else {
                        shareStoryAsText();
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void shareStoryAsText() {
        StringBuilder shareBody = new StringBuilder();
        shareBody.append("📖 ").append(storyTitle).append("\n\n");
        if (!textChunks.isEmpty()) {
            String firstChunk = textChunks.get(0);
            if (firstChunk.length() > 250) {
                shareBody.append(firstChunk.substring(0, 250)).append("...\n\n");
            } else {
                shareBody.append(firstChunk).append("\n\n");
            }
        }
        shareBody.append("সম্পূর্ণ গল্পটি পড়তে ডাউনলোড করুন বাংলা ভৌতিক ও রহস্য গল্প অ্যাপ:\n");
        shareBody.append("https://play.google.com/store/apps/details?id=").append(getPackageName());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, storyTitle);
        intent.putExtra(Intent.EXTRA_TEXT, shareBody.toString());
        startActivity(Intent.createChooser(intent, "গল্পটি শেয়ার করুন"));
    }

    private void updateBookmarkUI() {
        if (storyTitle == null || storyContentFile == null) return;
        isBookmarked = databaseHelper.isBookmarked(storyTitle, storyContentFile);
        if (bookmarkIcon != null) {
            bookmarkIcon.setImageResource(isBookmarked
                    ? R.drawable.baseline_bookmark_24
                    : R.drawable.baseline_bookmark_border_24);
            bookmarkIcon.setColorFilter(isBookmarked
                    ? Color.parseColor("#FBBF24")
                    : Color.WHITE);
        }
    }

    private void toggleBookmark() {
        if (isBookmarked) {
            databaseHelper.removeBookmark(storyTitle, storyContentFile);
            isBookmarked = false;
            Toast.makeText(this, "বুকমার্ক থেকে সরানো হয়েছে", Toast.LENGTH_SHORT).show();
        } else {
            databaseHelper.adBookmark(storyTitle, storyContentFile);
            isBookmarked = true;
            Toast.makeText(this, "বুকমার্কে সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show();
        }
        updateBookmarkUI();
    }

    private void initTextToSpeech() {
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
                    try {
                        Locale banglaLocale = new Locale("bn", "BD");
                        int result = textToSpeech.setLanguage(banglaLocale);
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            textToSpeech.setLanguage(Locale.getDefault());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void toggleTts() {
        if (textToSpeech == null) {
            initTextToSpeech();
            Toast.makeText(this, "অডিও ইঞ্জিন প্রস্তুত হচ্ছে, একটু অপেক্ষা করুন...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSpeaking) {
            stopTtsPlayback();
            Toast.makeText(this, "অডিও পাঠ বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show();
        } else {
            if (textChunks.isEmpty()) return;

            isSpeaking = true;
            btnTts.setImageResource(R.drawable.ic_volume_off_24);
            btnTts.setColorFilter(Color.parseColor("#FBBF24"));
            currentTtsChunk = 0;
            speakNextChunk();
            Toast.makeText(this, "গল্প পাঠ শুরু হয়েছে...", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopTtsPlayback() {
        isSpeaking = false;
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (btnTts != null) {
            btnTts.setImageResource(R.drawable.ic_volume_up_24);
            btnTts.setColorFilter(Color.WHITE);
        }
    }

    private void speakNextChunk() {
        if (!isSpeaking || textToSpeech == null || currentTtsChunk >= textChunks.size()) {
            stopTtsPlayback();
            return;
        }

        String chunk = textChunks.get(currentTtsChunk);
        textToSpeech.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "story_tts_" + currentTtsChunk);

        textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    if (isSpeaking) {
                        currentTtsChunk++;
                        speakNextChunk();
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(StoryViewActivity.this::stopTtsPlayback);
            }
        });
    }

    private void saveTextSize(float size) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(TEXT_SIZE_KEY, size);
        editor.apply();
    }

    private void showZoomButtons() {
        if (zoomControls.getVisibility() != View.VISIBLE) {
            zoomControls.setVisibility(View.VISIBLE);
            zoomControls.setAlpha(0f);
            zoomControls.animate().alpha(1f).setDuration(200).start();
        }
    }

    private void hideZoomButtons() {
        if (zoomControls.getVisibility() == View.VISIBLE) {
            zoomControls.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                zoomControls.setVisibility(View.GONE);
            }).start();
        }
    }

    private void scaleButton(View button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 1.2f, 1.0f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);
        scaleX.start();
        scaleY.start();
    }

    private void loadTextFileInChunks(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        fileName = fileName.trim();

        try {
            InputStream inputStream;
            try {
                inputStream = getAssets().open(fileName);
            } catch (IOException e) {
                if (!fileName.endsWith(".txt")) {
                    inputStream = getAssets().open(fileName + ".txt");
                } else {
                    throw e;
                }
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder chunk = new StringBuilder();
            String line;
            int wordCount = 0;
            int totalWordCount = 0;

            textChunks.clear();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    if (chunk.length() > 0 && chunk.charAt(chunk.length() - 1) != '\n') {
                        chunk.append("\n");
                    }
                } else {
                    chunk.append(line).append("\n");
                    int lineWords = line.trim().split("\\s+").length;
                    wordCount += lineWords;
                    totalWordCount += lineWords;
                }

                if (wordCount >= 200) {
                    textChunks.add(chunk.toString());
                    chunk.setLength(0);
                    wordCount = 0;
                }
            }

            if (chunk.length() > 0) {
                textChunks.add(chunk.toString());
            }

            reader.close();
            inputStream.close();

            int minutes = Math.max(1, (int) Math.ceil(totalWordCount / 180.0));
            tvReadingTime.setText(String.format(Locale.getDefault(), "⏱️ %d মিনিট পড়া", minutes));
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "গল্প লোড করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
        }
    }

    private void initAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this);
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) {
                        Toast.makeText(this, "আপডেট সম্পন্ন হয়নি", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        updateListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate();
            }
        };

        appUpdateManager.registerListener(updateListener);
        checkForAppUpdate();
    }

    private void checkForAppUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activityResultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    );
                } catch (Exception ignored) {}
            }
        });
    }

    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "আপডেট ডাউনলোড সম্পন্ন হয়েছে!", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("রিস্টার্ট করুন", view -> {
            if (appUpdateManager != null) {
                appUpdateManager.completeUpdate();
            }
        });
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        snackbar.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTtsPlayback();

        if (sessionStartTime > 0 && readingStatsManager != null) {
            long elapsedSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000;
            readingStatsManager.addReadingTime((int) elapsedSeconds);
            readingStatsManager.saveLastRead(storyTitle, storyContentFile, currentProgress);
            if (currentProgress >= 90) {
                readingStatsManager.incrementStoriesCompleted();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (appUpdateManager != null && updateListener != null) {
                appUpdateManager.unregisterListener(updateListener);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionStartTime = System.currentTimeMillis();
        AdHelper.loadBannerAd(this, adContainer);
        updateBookmarkUI();
    }

    @Override
    protected void onDestroy() {
        // Fix for ANR: ব্যাকগ্রাউন্ড থ্রেড বা অন্য থ্রেডে সেফলি টেক্সট-টু-স্পিচ শাটডাউন হ্যান্ডেল করা হলো
        // যাতে মেইন থ্রেড ব্লক না হয়ে যায়।
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                textToSpeech.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            textToSpeech = null;
        }

        AdHelper.clearBannerAd();
        super.onDestroy();
    }
}