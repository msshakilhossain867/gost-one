package com.citomoltd.banglagoststory.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.citomoltd.banglagoststory.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

public class AppOpenManager implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private static final String LOG_TAG = "AppOpenManager";
    private final MyApp myApplication;
    private AppOpenAd appOpenAd = null;
    private Activity currentActivity;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private long loadTime = 0;

    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    public AppOpenManager(MyApp myApplication) {
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        loadAd();
    }

    public void loadAd() {
        if (isLoadingAd || isAdAvailable()) {
            return;
        }

        isLoadingAd = true;
        try {
            AdRequest request = new AdRequest.Builder().build();
            AppOpenAd.load(
                    myApplication,
                    myApplication.getString(R.string.admob_app_open_id),
                    request,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            Log.d(LOG_TAG, "App Open Ad loaded successfully.");
                            appOpenAd = ad;
                            isLoadingAd = false;
                            loadTime = (new Date()).getTime();
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            Log.d(LOG_TAG, "App Open Ad failed to load: " + loadAdError.getMessage());
                            isLoadingAd = false;
                            appOpenAd = null;
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load App Open Ad", e);
            isLoadingAd = false;
        }
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    public boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    public void showAdIfAvailable() {
        if (currentActivity != null) {
            showAdIfAvailable(currentActivity, () -> {});
        }
    }

    public void showAdIfAvailable(@NonNull Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            onShowAdCompleteListener.onShowAdComplete();
            return;
        }

        if (isShowingAd) {
            Log.d(LOG_TAG, "The app open ad is already showing.");
            return;
        }

        if (!isAdAvailable()) {
            Log.d(LOG_TAG, "The app open ad is not ready yet.");
            onShowAdCompleteListener.onShowAdComplete();
            loadAd();
            return;
        }

        appOpenAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(LOG_TAG, "App Open Ad dismissed.");
                        appOpenAd = null;
                        isShowingAd = false;
                        onShowAdCompleteListener.onShowAdComplete();
                        loadAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        Log.d(LOG_TAG, "App Open Ad failed to show: " + adError.getMessage());
                        appOpenAd = null;
                        isShowingAd = false;
                        onShowAdCompleteListener.onShowAdComplete();
                        loadAd();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(LOG_TAG, "App Open Ad showed.");
                        isShowingAd = true;
                    }
                }
        );

        isShowingAd = true;
        try {
            appOpenAd.show(activity);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to show App Open Ad", e);
            isShowingAd = false;
            appOpenAd = null;
            onShowAdCompleteListener.onShowAdComplete();
            loadAd();
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        // Show App Open Ad when app enters foreground
        showAdIfAvailable();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (!isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
