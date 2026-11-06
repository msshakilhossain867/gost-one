package com.citomoltd.banglagoststory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.citomoltd.banglagoststory.utils.AdHelper;
import com.citomoltd.banglagoststory.utils.CoinManager;
import com.citomoltd.banglagoststory.utils.DatabaseHelper;
import com.citomoltd.banglagoststory.utils.TwoLineView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;

public class PrimeStoryActivity extends AppCompatActivity {



    private RecyclerView recyclerView;
    private final ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private MyAdapter myAdapter;
    private DatabaseHelper databaseHelper;
    private CoinManager coinManager;
    private TwoLineView twoLineView;

    private RewardedAd rewardedAd;
    private TextView coinBalanceView, tvEmptyState;
    private EditText edSearch;
    private ImageView back_arrow;
    private final int[] images = {R.drawable.img_cat_prime, R.drawable.img_cat_horror, R.drawable.img_cat_mystery, R.drawable.img1, R.drawable.img2};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prime_story);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        recyclerView = findViewById(R.id.recyclerView);
        coinBalanceView = findViewById(R.id.coinBalance);
        back_arrow = findViewById(R.id.back_arrow);
        edSearch = findViewById(R.id.edSearch);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        coinManager = CoinManager.getInstance(this);
        databaseHelper = new DatabaseHelper(this);
        twoLineView = new TwoLineView(this);

        updateCoinBalanceUI();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(25);
        myAdapter = new MyAdapter();
        recyclerView.setAdapter(myAdapter);

        loadData(databaseHelper.getAllDataPrime());
        loadRewardedAd();

        back_arrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        edSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String key = s.toString().trim();
                if (key.isEmpty()) {
                    loadData(databaseHelper.getAllDataPrime());
                } else {
                    loadData(databaseHelper.searchPrime(key));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCoinBalanceUI();
        if (myAdapter != null) {
            myAdapter.notifyDataSetChanged();
        }
    }

    private void updateCoinBalanceUI() {
        if (coinBalanceView != null && coinManager != null) {
            coinBalanceView.setText(String.valueOf(coinManager.getCoins()));
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.admob_rewarded_id), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        rewardedAd = null;
                    }
                });
    }

    public void loadData(Cursor cursor) {
        arrayList.clear();
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    HashMap<String, String> item = new HashMap<>();
                    item.put("id", cursor.getString(0));
                    item.put("name", cursor.getString(1));
                    item.put("link", cursor.getString(2));
                    item.put("isFav", cursor.getString(3));
                    arrayList.add(item);
                }
            } finally {
                cursor.close();
            }
        }

        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(arrayList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (myAdapter != null) {
            myAdapter.notifyDataSetChanged();
        }
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycler_lc, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> item = arrayList.get(position);
            String stId = item.get("id");
            String title = item.get("name");
            String content = item.get("link");

            // Always unlock by unique story file or story ID rather than volatile adapter position
            String unlockKey = (content != null && !content.isEmpty()) ? content : stId;
            boolean isUnlocked = coinManager.isStoryUnlocked(unlockKey);

            holder.itemTitle.setText(title);
            String firstTwoLines = "";
            if (twoLineView != null && content != null && !content.isEmpty()) {
                firstTwoLines = twoLineView.readFirstTwoLinesFromFile(content);
            }
            holder.itemStory.setText(firstTwoLines);

            holder.storyImage.setImageResource(images[position % images.length]);

            if (isUnlocked) {
                holder.unlockButton.setVisibility(View.GONE);
                holder.nextButton.setVisibility(View.VISIBLE);
            } else {
                holder.unlockButton.setVisibility(View.VISIBLE);
                holder.nextButton.setVisibility(View.GONE);
            }

            holder.unlockButton.setOnClickListener(v -> {
                int currentCoins = coinManager.getCoins();

                AlertDialog.Builder builder = new AlertDialog.Builder(PrimeStoryActivity.this);
                builder.setTitle("গল্প আনলক করুন")
                        .setMessage("আপনি ১০টি কয়েন দিয়ে অথবা বিজ্ঞাপন দেখে গল্পটি আনলক করতে পারবেন। কোনটি বেছে নিবেন?")
                        .setPositiveButton("১০ কয়েন ব্যবহার করুন", (dialog, which) -> {
                            if (coinManager.deductCoins(10)) {
                                coinManager.unlockStory(unlockKey);
                                updateCoinBalanceUI();
                                notifyItemChanged(position);
                                Toast.makeText(PrimeStoryActivity.this, "🎉 গল্পটি সফলভাবে আনলক হয়েছে!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(PrimeStoryActivity.this, "পর্যাপ্ত কয়েন নেই! হোম স্ক্রিনে বিজ্ঞাপন দেখে কয়েন নিন।", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("বিজ্ঞাপন দেখুন", (dialog, which) -> {
                            if (rewardedAd != null) {
                                rewardedAd.show(PrimeStoryActivity.this, rewardItem -> {
                                    coinManager.unlockStory(unlockKey);
                                    notifyItemChanged(position);
                                    loadRewardedAd();
                                    Toast.makeText(PrimeStoryActivity.this, "🎉 বিজ্ঞাপন দেখে গল্পটি আনলক করা হয়েছে!", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                Toast.makeText(PrimeStoryActivity.this, "বিজ্ঞাপন প্রস্তুত নয়, কিছুক্ষণ পর চেষ্টা করুন।", Toast.LENGTH_SHORT).show();
                                loadRewardedAd();
                            }
                        })
                        .setNeutralButton("বাতিল", (dialog, which) -> dialog.dismiss())
                        .show();
            });

            holder.nextButton.setOnClickListener(v -> {
                AdHelper.showAdIfNeeded(PrimeStoryActivity.this, () -> {
                    Intent intent = new Intent(PrimeStoryActivity.this, StoryViewActivity.class);
                    intent.putExtra("story_title", title);
                    intent.putExtra("story_content", content);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });
            });
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView itemTitle, itemStory;
            MaterialButton unlockButton, nextButton;
            ImageView storyImage;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemTitle = itemView.findViewById(R.id.itemTitle);
                itemStory = itemView.findViewById(R.id.itemStory);
                unlockButton = itemView.findViewById(R.id.unlockButton);
                nextButton = itemView.findViewById(R.id.nextButton);
                storyImage = itemView.findViewById(R.id.storyImage);
            }
        }

    }


}