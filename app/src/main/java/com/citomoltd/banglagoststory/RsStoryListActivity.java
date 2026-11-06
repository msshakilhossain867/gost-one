package com.citomoltd.banglagoststory;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.citomoltd.banglagoststory.utils.AdHelper;
import com.citomoltd.banglagoststory.utils.DatabaseHelper;
import com.citomoltd.banglagoststory.utils.TwoLineView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class RsStoryListActivity extends AppCompatActivity {



    private EditText edSearch;
    private RecyclerView rsRecyclerView;
    private TextView tvEmptyState;
    private DatabaseHelper databaseHelper;
    private final ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private RohosoViewAdapter myAdapter;
    private final int[] images = {R.drawable.img_cat_mystery, R.drawable.img_cat_horror, R.drawable.img_cat_prime, R.drawable.img1, R.drawable.img2};
    private TwoLineView twoLineView;
    private ImageView back_arrow;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rs_story_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        edSearch = findViewById(R.id.edSearch);
        back_arrow = findViewById(R.id.back_arrow);
        rsRecyclerView = findViewById(R.id.rsRecyclerView);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        databaseHelper = new DatabaseHelper(this);
        twoLineView = new TwoLineView(this);

        rsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        rsRecyclerView.setHasFixedSize(true);
        rsRecyclerView.setItemViewCacheSize(25);
        myAdapter = new RohosoViewAdapter();
        rsRecyclerView.setAdapter(myAdapter);

        loadData(databaseHelper.getAllData());
        AdHelper.initializeAd(this);

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
                    loadData(databaseHelper.getAllData());
                } else {
                    loadData(databaseHelper.searchRS(key));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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

    public class RohosoViewAdapter extends RecyclerView.Adapter<RohosoViewAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.re_item_file, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> item = arrayList.get(position);

            String stId = item.get("id");
            String title = item.get("name");
            String content = item.get("link");
            String isFav = item.get("isFav");

            int currentStatus = (isFav != null) ? Integer.parseInt(isFav) : 0;
            int id = (stId != null) ? Integer.parseInt(stId) : 0;

            holder.storySerialNumber.setText(String.valueOf(position + 1));
            holder.tv_rs_tittle.setText(title);

            String firstTwoLines = "";
            if (twoLineView != null && content != null && !content.isEmpty()) {
                firstTwoLines = twoLineView.readFirstTwoLinesFromFile(content);
            }
            holder.storyText.setText(firstTwoLines);

            holder.storyImage.setImageResource(images[position % images.length]);

            if (currentStatus == 1) {
                holder.favoriteIcon.setImageResource(R.drawable.baseline_favorite_24);
            } else {
                holder.favoriteIcon.setImageResource(R.drawable.baseline_favorite_border_24);
            }

            holder.favoriteIcon.setOnClickListener(v -> {
                int newStatus = (currentStatus == 1) ? 0 : 1;
                databaseHelper.updateFav(id, newStatus);
                item.put("isFav", String.valueOf(newStatus));
                notifyItemChanged(position);
            });

            holder.cv_onClick.setOnClickListener(v -> {
                AdHelper.showAdIfNeeded(RsStoryListActivity.this, () -> {
                    Intent intent = new Intent(RsStoryListActivity.this, StoryViewActivity.class);
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
            TextView tv_rs_tittle, storyText, storySerialNumber;
            ImageView favoriteIcon, storyImage;
            View cv_onClick;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tv_rs_tittle = itemView.findViewById(R.id.storyTitle);
                storyText = itemView.findViewById(R.id.storyText);
                storySerialNumber = itemView.findViewById(R.id.storySerialNumber);
                storyImage = itemView.findViewById(R.id.storyImage);
                favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
                cv_onClick = itemView.findViewById(R.id.onclickNext);
            }
        }


    }


}