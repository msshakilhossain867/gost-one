package com.citomoltd.banglagoststory.utils;

import android.app.Activity;
import android.util.Log;
import android.widget.FrameLayout;

import com.citomoltd.banglagoststory.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdHelper {

    private static final String TAG = "AdHelper";
    private static boolean isFirstClick = true;
    private static int clickCount = 0;
    private static final int CLICKS_PER_AD = 3;

    private static InterstitialAd mInterstitialAd;
    private static boolean isAdLoading = false;

    private static AdView mAdView;
    private static boolean isBannerAdLoading = false;

    public interface AdListener {
        void onAdClosed();
    }

    public static void initializeAd(Activity activity) {
        loadNewAd(activity);
    }

    public static void showAdIfNeeded(Activity activity, AdListener listener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (listener != null) listener.onAdClosed();
            return;
        }

        clickCount++;
        Log.d(TAG, "Story clicked. Count: " + clickCount + ", isFirstClick: " + isFirstClick);

        boolean shouldShow = isFirstClick || (clickCount >= CLICKS_PER_AD);

        if (shouldShow && mInterstitialAd != null) {
            InterstitialAd adToShow = mInterstitialAd;
            mInterstitialAd = null; // Prevent showing the same ad again
            isFirstClick = false;   // 1st click requirement fulfilled
            clickCount = 0;         // Reset count right here: next ad will show after 3 clicks from here

            Log.d(TAG, "Showing interstitial ad. Resetting count to 0.");

            adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed by user.");
                    if (listener != null) listener.onAdClosed();
                    loadNewAd(activity);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    Log.d(TAG, "Ad failed to show: " + adError.getMessage());
                    if (listener != null) listener.onAdClosed();
                    loadNewAd(activity);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed successfully.");
                }
            });

            try {
                adToShow.show(activity);
            } catch (Exception e) {
                Log.e(TAG, "Error displaying interstitial ad", e);
                if (listener != null) listener.onAdClosed();
                loadNewAd(activity);
            }
        } else {
            if (shouldShow && mInterstitialAd == null) {
                Log.d(TAG, "Ad should show but not ready yet. Preloading...");
                loadNewAd(activity);
            }
            if (listener != null) listener.onAdClosed();
        }
    }

    private static void loadNewAd(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (isAdLoading) {
            Log.d(TAG, "Ad is already loading.");
            return;
        }

        if (mInterstitialAd != null) {
            Log.d(TAG, "Ad already loaded.");
            return;
        }

        isAdLoading = true;
        try {
            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(activity.getApplicationContext(), activity.getString(R.string.admob_interstitial_id), adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(InterstitialAd interstitialAd) {
                            Log.d(TAG, "Interstitial ad loaded successfully.");
                            mInterstitialAd = interstitialAd;
                            isAdLoading = false;
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            Log.d(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                            mInterstitialAd = null;
                            isAdLoading = false;
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to load interstitial ad", e);
            isAdLoading = false;
        }
    }

    public static void loadBannerAd(Activity activity, FrameLayout adContainer) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || adContainer == null) {
            return;
        }

        try {
            if (mAdView != null) {
                clearBannerAd();
            }

            mAdView = new AdView(activity);
            mAdView.setAdUnitId(activity.getString(R.string.admob_banner_id));
            mAdView.setAdSize(AdSize.BANNER);

            adContainer.removeAllViews();
            adContainer.addView(mAdView);

            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load banner ad", e);
        }
    }

    public static void clearBannerAd() {
        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
    }
}
