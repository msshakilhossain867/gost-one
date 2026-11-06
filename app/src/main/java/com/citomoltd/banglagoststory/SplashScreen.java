package com.citomoltd.banglagoststory;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (!prefs.contains("theme_mode") && prefs.contains("dark_mode")) {
            themeMode = prefs.getBoolean("dark_mode", false)
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;
        }
        AppCompatDelegate.setDefaultNightMode(themeMode);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        SeekBar seekBar = findViewById(R.id.seekBar);

        // Disable user interaction
        seekBar.setEnabled(false);

        // Animate the progress
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(seekBar, "progress", 0, 100);
        progressAnimator.setDuration(3500); // Animation duration (3 seconds)
        progressAnimator.setRepeatCount(ObjectAnimator.INFINITE); // Repeat indefinitely
        progressAnimator.setRepeatMode(ObjectAnimator.RESTART); // Restart after reaching max
        progressAnimator.start();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;

                com.citomoltd.banglagoststory.utils.AppOpenManager appOpenManager =
                        com.citomoltd.banglagoststory.utils.MyApp.getAppOpenManager();

                if (appOpenManager != null && appOpenManager.isAdAvailable()) {
                    appOpenManager.showAdIfAvailable(SplashScreen.this, () -> navigateToMainActivity());
                } else {
                    navigateToMainActivity();
                }
            }
        }, 3000);
    }

    private void navigateToMainActivity() {
        Intent splashIntent = new Intent(SplashScreen.this, MainActivity.class);
        startActivity(splashIntent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }



}