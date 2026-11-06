package com.citomoltd.banglagoststory.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.citomoltd.banglagoststory.PrimeStoryActivity;
import com.citomoltd.banglagoststory.R;
import com.citomoltd.banglagoststory.RsStoryListActivity;
import com.citomoltd.banglagoststory.StoryViewActivity;
import com.citomoltd.banglagoststory.VtStoryActivity;
import com.citomoltd.banglagoststory.utils.AdHelper;
import com.citomoltd.banglagoststory.utils.CoinManager;
import com.citomoltd.banglagoststory.utils.ReadingStatsManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;

public class HomeFragment extends Fragment {

    private Context mContext;

    private static final int MAX_ADS_PER_HOUR = 3;
    private static final String PREFS_NAME = "AdPrefs";
    private static final String WATCHED_ADS_COUNT = "watched_ads_count";
    private static final String LAST_RESET_TIME = "last_reset_time";

    private SharedPreferences preferences;
    private CoinManager coinManager;

    private RewardedAd rewardedAd;
    private TextView coinBalanceTextView, watchedAdsTextView, remainingTimeTextView;
    private CountDownTimer countDownTimer;
    private RelativeLayout rl_pmStory;
    private MaterialButton watchAdButton;
    private LinearLayout ln_vtStory, ln_rsStory;
    private View cardContinueReading, btnContinueReading;
    private TextView tvLastReadTitle;
    private MaterialButton btnDailyBonus;
    private ReadingStatsManager readingStatsManager;
    private TextView tvRankTitle, tvTotalReadTime, tvStreakDays;

    @Override
    public void onAttach(@NonNull Context context) {
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        coinManager = CoinManager.getInstance(requireContext());
        readingStatsManager = new ReadingStatsManager(requireContext());

        ln_vtStory = view.findViewById(R.id.ln_vtStory);
        ln_rsStory = view.findViewById(R.id.ln_rsStory);
        coinBalanceTextView = view.findViewById(R.id.coinBalance);
        watchedAdsTextView = view.findViewById(R.id.watchedAdsText);
        remainingTimeTextView = view.findViewById(R.id.remainingTime);
        watchAdButton = view.findViewById(R.id.watchAdButton);
        rl_pmStory = view.findViewById(R.id.rl_pmStory);

        cardContinueReading = view.findViewById(R.id.cardContinueReading);
        btnContinueReading = view.findViewById(R.id.btnContinueReading);
        tvLastReadTitle = view.findViewById(R.id.tvLastReadTitle);
        btnDailyBonus = view.findViewById(R.id.btnDailyBonus);

        tvRankTitle = view.findViewById(R.id.tvRankTitle);
        tvTotalReadTime = view.findViewById(R.id.tvTotalReadTime);
        tvStreakDays = view.findViewById(R.id.tvStreakDays);

        loadRewardedAd();

        updateCoinBalanceUI();
        updateWatchedAdsCountUI();
        updateRemainingTimeUI();
        updateContinueReadingUI();
        updateDailyBonusUI();
        updateReaderStatsUI();

        View cardHeroBanner = view.findViewById(R.id.cardHeroBanner);
        View btnHeroExplore = view.findViewById(R.id.btnHeroExplore);

        View.OnClickListener openHorrorListener = v -> {
            Intent intent = new Intent(mContext, VtStoryActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        };

        if (cardHeroBanner != null) cardHeroBanner.setOnClickListener(openHorrorListener);
        if (btnHeroExplore != null) btnHeroExplore.setOnClickListener(openHorrorListener);

        ln_vtStory.setOnClickListener(openHorrorListener);
        ln_rsStory.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, RsStoryListActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        rl_pmStory.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, PrimeStoryActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        watchAdButton.setOnClickListener(v -> showRewardedAd());

        btnDailyBonus.setOnClickListener(v -> {
            if (coinManager.canClaimDailyBonus()) {
                coinManager.claimDailyBonus(10);
                updateCoinBalanceUI();
                updateDailyBonusUI();
                Toast.makeText(mContext, "🎉 অভিনন্দন! আপনি ১০টি ফ্রি কয়েন পেয়েছেন!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "আজকের বোনাস নেওয়া হয়েছে। আগামীকাল আবার আসুন!", Toast.LENGTH_SHORT).show();
            }
        });

        btnContinueReading.setOnClickListener(v -> {
            String title = coinManager.getLastReadTitle();
            String file = coinManager.getLastReadFile();
            if (title != null && file != null) {
                if (getActivity() != null) {
                    AdHelper.showAdIfNeeded(getActivity(), () -> {
                        Intent intent = new Intent(mContext, StoryViewActivity.class);
                        intent.putExtra("story_title", title);
                        intent.putExtra("story_content", file);
                        startActivity(intent);
                        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    });
                } else {
                    Intent intent = new Intent(mContext, StoryViewActivity.class);
                    intent.putExtra("story_title", title);
                    intent.putExtra("story_content", file);
                    startActivity(intent);
                }
            }
        });
    }

    private void updateContinueReadingUI() {
        String lastTitle = coinManager.getLastReadTitle();
        String lastFile = coinManager.getLastReadFile();
        if (lastTitle != null && !lastTitle.isEmpty() && lastFile != null && !lastFile.isEmpty()) {
            cardContinueReading.setVisibility(View.VISIBLE);
            tvLastReadTitle.setText(lastTitle);
        } else {
            cardContinueReading.setVisibility(View.GONE);
        }
    }

    private void updateDailyBonusUI() {
        if (btnDailyBonus == null) return;
        if (coinManager.canClaimDailyBonus()) {
            btnDailyBonus.setText("🎁 দৈনিক বোনাস");
            btnDailyBonus.setEnabled(true);
            btnDailyBonus.setAlpha(1.0f);
        } else {
            btnDailyBonus.setText("✓ ক্লেইম করা হয়েছে");
            btnDailyBonus.setEnabled(false);
            btnDailyBonus.setAlpha(0.6f);
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(requireContext(), getString(R.string.admob_rewarded_id), adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                rewardedAd = null;
            }
        });
    }

    public void showRewardedAd() {
        if (!canWatchAd()) {
            Toast.makeText(mContext, "আপনি প্রতি ঘণ্টায় সর্বোচ্চ ৩টি বিজ্ঞাপন দেখতে পারবেন।", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rewardedAd == null) {
            Toast.makeText(mContext, "বিজ্ঞাপন প্রস্তুত হচ্ছে, অনুগ্রহ করে কিছুক্ষণ পর চেষ্টা করুন।", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
            return;
        }

        new AlertDialog.Builder(mContext)
                .setTitle("কয়েন অর্জন")
                .setMessage("একটি বিজ্ঞাপন দেখে ১০টি ফ্রি কয়েন অর্জন করতে চান?")
                .setPositiveButton("বিজ্ঞাপন দেখুন", (dialog, which) -> {
                    if (rewardedAd != null && getActivity() != null) {
                        rewardedAd.show(requireActivity(), rewardItem -> {
                            incrementWatchedAdsCount();
                            coinManager.addCoins(10);
                            updateCoinBalanceUI();
                            Toast.makeText(mContext, "🎉 আপনি ১০টি কয়েন অর্জন করেছেন!", Toast.LENGTH_SHORT).show();
                            loadRewardedAd();
                        });
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private boolean canWatchAd() {
        long currentTime = System.currentTimeMillis();
        long lastResetTime = preferences.getLong(LAST_RESET_TIME, 0);
        int watchedAdsCount = preferences.getInt(WATCHED_ADS_COUNT, 0);

        if (currentTime - lastResetTime >= 3600000) { // 1 hour
            resetWatchedAdsCount();
            return true;
        }
        return watchedAdsCount < MAX_ADS_PER_HOUR;
    }

    private void incrementWatchedAdsCount() {
        int watchedAdsCount = preferences.getInt(WATCHED_ADS_COUNT, 0);
        preferences.edit().putInt(WATCHED_ADS_COUNT, watchedAdsCount + 1).apply();
        updateWatchedAdsCountUI();
    }

    private void resetWatchedAdsCount() {
        preferences.edit().putInt(WATCHED_ADS_COUNT, 0).putLong(LAST_RESET_TIME, System.currentTimeMillis()).apply();
        updateWatchedAdsCountUI();
        updateRemainingTimeUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCoinBalanceUI();
        updateContinueReadingUI();
        updateDailyBonusUI();
        updateReaderStatsUI();
    }

    private void updateReaderStatsUI() {
        if (readingStatsManager != null && tvRankTitle != null) {
            tvRankTitle.setText(readingStatsManager.getReaderRank());
            tvTotalReadTime.setText(readingStatsManager.getTotalReadMinutes() + " মিনিট");
            tvStreakDays.setText("🔥 " + readingStatsManager.getStreakDays() + " দিন");
        }
    }

    private void updateCoinBalanceUI() {
        if (coinBalanceTextView != null && coinManager != null) {
            coinBalanceTextView.setText("Coins: " + coinManager.getCoins());
        }
    }

    private void updateWatchedAdsCountUI() {
        if (watchedAdsTextView != null && preferences != null) {
            int watchedAdsCount = preferences.getInt(WATCHED_ADS_COUNT, 0);
            watchedAdsTextView.setText("বিজ্ঞাপন দেখা হয়েছে: " + watchedAdsCount + "/" + MAX_ADS_PER_HOUR);
        }
    }

    private void updateRemainingTimeUI() {
        if (remainingTimeTextView == null || preferences == null) return;
        long currentTime = System.currentTimeMillis();
        long lastResetTime = preferences.getLong(LAST_RESET_TIME, 0);
        long nextResetTime = lastResetTime + 3600000;

        if (currentTime < nextResetTime && preferences.getInt(WATCHED_ADS_COUNT, 0) >= MAX_ADS_PER_HOUR) {
            long remainingTime = nextResetTime - currentTime;

            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            countDownTimer = new CountDownTimer(remainingTime, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long minutes = (millisUntilFinished / (60 * 1000)) % 60;
                    long seconds = (millisUntilFinished / 1000) % 60;
                    remainingTimeTextView.setText(String.format("পরবর্তী বিজ্ঞাপন: %02d:%02d", minutes, seconds));
                    remainingTimeTextView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFinish() {
                    resetWatchedAdsCount();
                    remainingTimeTextView.setVisibility(View.GONE);
                }
            };
            countDownTimer.start();
        } else {
            remainingTimeTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        rewardedAd = null;
    }
}
