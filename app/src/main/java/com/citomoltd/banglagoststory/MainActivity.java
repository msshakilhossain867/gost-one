package com.citomoltd.banglagoststory;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.citomoltd.banglagoststory.fragments.HomeFragment;
import com.citomoltd.banglagoststory.fragments.BookmarkFragment;
import com.citomoltd.banglagoststory.fragments.SettingsFragment;
import com.citomoltd.banglagoststory.utils.AdHelper;
import com.citomoltd.banglagoststory.utils.StoryReminderManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private long backPressedTime = 0;
    private static final long BACK_PRESS_INTERVAL = 2000;
    private Fragment currentFragment;
    private BottomNavigationView bottomNavigation;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
// ---------------------------------------------------------
        // 🔥 UI Components Init
        // ---------------------------------------------------------
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        AdHelper.initializeAd(this);
        askNotificationPermission();

        // Record app activity and maintain daily reminder schedule
       StoryReminderManager.recordAppOpened(this);
       StoryReminderManager.scheduleDailyReminder(this);




        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int lastSelectedItem = prefs.getInt("last_selected_tab", -1);

        Fragment initialFragment;
        int selectedItemId;


        // ---------------------------------------------------------
        // 🔥 CHANGED: Recreate (Dark Mode toggle) Handling
        // ---------------------------------------------------------
        if (getIntent().getBooleanExtra("from_recreate", false)
                && lastSelectedItem == R.id.nav_settings) {

            initialFragment = new SettingsFragment();
            selectedItemId = R.id.nav_settings;

        } else {


            initialFragment = new HomeFragment();
            selectedItemId = R.id.nav_home;
            // First time open → Clear last tab
            prefs.edit().remove("last_selected_tab").apply();
        }


        // ---------------------------------------------------------
        // 🔥 Set Initial Tab & Fragment
        // ---------------------------------------------------------
        bottomNavigation.setSelectedItemId(selectedItemId);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, initialFragment)
                .commitAllowingStateLoss();

        currentFragment = initialFragment;



        // ---------------------------------------------------------
        // 🔥 Bottom Navigation Listener (Smooth & Optimized)
        // ---------------------------------------------------------
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Prevent re-creating the same fragment if already visible
            if (currentFragment != null) {
                if (itemId == R.id.nav_home && currentFragment instanceof HomeFragment) return true;
                if (itemId == R.id.nav_bookmark && currentFragment instanceof BookmarkFragment) return true;
                if (itemId == R.id.nav_settings && currentFragment instanceof SettingsFragment) return true;
            }

            Fragment selectedFragment = null;
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_bookmark) {
                selectedFragment = new BookmarkFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();

                currentFragment = selectedFragment;

                // Save last selected tab
                prefs.edit()
                        .putInt("last_selected_tab", itemId)
                        .apply();
            }

            return true;
        });



        // ---------------------------------------------------------
        // 🔥 Toolbar Menu Listener
        // ---------------------------------------------------------
        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.rating) {
                openAppRating();

            } else if (itemId == R.id.privacy_policy) {
                openPrivacyPolicy();

            } else if (itemId == R.id.moreApp) {
                openMoreApps();

            } else if (itemId == R.id.shareApp) {
                ShareApp(MainActivity.this);
            }
            return true;
        });



        // ---------------------------------------------------------
        // 🔥 Back Press Handler
        // ---------------------------------------------------------
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (System.currentTimeMillis() - backPressedTime < BACK_PRESS_INTERVAL) {
                    finish();
                } else {
                    backPressedTime = System.currentTimeMillis();
                    Toast.makeText(getApplicationContext(),
                            "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
            }
        });




    }




    // ========================= FCM Notification =========================
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    //TODO: Inform user that that your app will not show notifications.
                    subscribeToFCMTopic();
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                }
            });

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
                subscribeToFCMTopic();
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                if(!isFinishing() && !isDestroyed()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Notification permission")
                            .setMessage("🔔 Don’t miss out! Enable notifications to read new stories first.")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);

                                }
                            })
                            .setNegativeButton("NO Thanks", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();
                }
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }else {
            showSubscriptionDialog();
        }
    }

    private void subscribeToFCMTopic() {
        SharedPreferences prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE);
        boolean isSubscribed = prefs.getBoolean("is_subscribed", false);

        if (isSubscribed) {
            return;
        }

        // Check if running on an emulator to avoid TOO_MANY_REGISTRATIONS logcat errors
        if (com.citomoltd.banglagoststory.utils.MyApp.isRunningOnEmulator()) {
            Log.d("FCM", "Running in emulator/test environment; skipping FCM subscription.");
            prefs.edit().putBoolean("is_subscribed", true).apply();
            return;
        }

        try {
            int playServicesStatus = com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
            if (playServicesStatus != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                Log.w("FCM", "Google Play Services not available, skipping FCM topic subscription.");
                prefs.edit().putBoolean("is_subscribed", true).apply();
                return;
            }

            FirebaseMessaging fcm = FirebaseMessaging.getInstance();
            fcm.getToken().addOnCompleteListener(tokenTask -> {
                if (tokenTask.isSuccessful()) {
                    fcm.subscribeToTopic("allUsers")
                            .addOnCompleteListener(task -> {
                                prefs.edit().putBoolean("is_subscribed", true).apply();
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "নোটিফিকেশন চালু হয়েছে", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.w("FCM", "Topic subscription skipped: " + task.getException());
                                }
                            });
                } else {
                    Log.w("FCM", "Token retrieval failed: " + tokenTask.getException());
                    prefs.edit().putBoolean("is_subscribed", true).apply();
                }
            });
        } catch (Throwable e) {
            Log.w("FCM", "Failed to subscribe to FCM topic: " + e.getMessage());
            prefs.edit().putBoolean("is_subscribed", true).apply();
        }
    }

    private void showSubscriptionDialog() {
        SharedPreferences prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE);
        boolean isSubscribed = prefs.getBoolean("is_subscribed", false);

        if (!isSubscribed) {
            new AlertDialog.Builder(this)
                    .setTitle("নোটিফিকেশন চালু করবেন?")
                    .setMessage("নতুন গল্প এলে আপনি কি Notification পেতে চান?")
                    .setPositiveButton("হ্যাঁ", (dialog, which) -> {
                        subscribeToFCMTopic();
                    })
                    .setNegativeButton("না", (dialog, which) -> {
                        prefs.edit().putBoolean("is_subscribed", false).apply();
                    })
                    .show();
        }
    }



    private void openPrivacyPolicy() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/rohossomoy-voter-golpo-policy"));
        startActivity(intent);
    }

    private void openMoreApps() {
        String developerName = "https://play.google.com/store/apps/developer?id=citomoltd";
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(developerName)));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(developerName)));
        }
    }

    public void ShareApp(Context context) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Download now: https://play.google.com/store/apps/details?id=" + context.getPackageName());
        sendIntent.setType("text/plain");
        context.startActivity(Intent.createChooser(sendIntent, "Share via"));
    }

    private void openAppRating() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }
}
