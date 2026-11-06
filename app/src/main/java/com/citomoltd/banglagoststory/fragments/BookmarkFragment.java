package com.citomoltd.banglagoststory.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.citomoltd.banglagoststory.R;
import com.citomoltd.banglagoststory.StoryViewActivity;
import com.citomoltd.banglagoststory.utils.DatabaseHelper;
import com.citomoltd.banglagoststory.utils.TwoLineView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class BookmarkFragment extends Fragment {

    private ListView bookmarkList;
    private final ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private DatabaseHelper databaseHelper;

    // Empty state views
    private View txtBookmarkEmpty;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle, tvEmptySubtitle;

    // Search
    private EditText edSearch;

    // Tabs
    private MaterialCardView tabBookmark, tabFavorite;
    private ImageView ivTabBookmarkIcon, ivTabFavoriteIcon;
    private TextView tvTabBookmarkText, tvTabFavoriteText;

    // Current Mode (false = Bookmark, true = Favorite)
    private boolean isFavoriteMode = false;

    private BookAdapter bookAdapter;

    private final int[] images = {
            R.drawable.img_cat_horror,
            R.drawable.img_cat_mystery,
            R.drawable.img_cat_prime,
            R.drawable.img1,
            R.drawable.img2
    };
    private TwoLineView twoLineView;

    public BookmarkFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookmark, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views
        txtBookmarkEmpty = view.findViewById(R.id.txtBookmarkEmpty);
        ivEmptyIcon = view.findViewById(R.id.ivEmptyIcon);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);

        bookmarkList = view.findViewById(R.id.list_bookmark);
        edSearch = view.findViewById(R.id.edSearch);

        tabBookmark = view.findViewById(R.id.tabBookmark);
        tabFavorite = view.findViewById(R.id.tabFavorite);
        ivTabBookmarkIcon = view.findViewById(R.id.ivTabBookmarkIcon);
        ivTabFavoriteIcon = view.findViewById(R.id.ivTabFavoriteIcon);
        tvTabBookmarkText = view.findViewById(R.id.tvTabBookmarkText);
        tvTabFavoriteText = view.findViewById(R.id.tvTabFavoriteText);

        databaseHelper = new DatabaseHelper(requireContext());
        twoLineView = new TwoLineView(requireContext());

        bookAdapter = new BookAdapter();


        bookmarkList.setAdapter(bookAdapter);

        setupTabs();
        setupSearch();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupTabs() {
        tabBookmark.setOnClickListener(v -> {
            if (isFavoriteMode) {
                isFavoriteMode = false;
                updateTabUI();
                edSearch.setHint("বুকমার্ক করা গল্প খুঁজুন...");
                loadData();
            }
        });

        tabFavorite.setOnClickListener(v -> {
            if (!isFavoriteMode) {
                isFavoriteMode = true;
                updateTabUI();
                edSearch.setHint("ফেভারিট করা গল্প খুঁজুন...");
                loadData();
            }
        });

        updateTabUI();
    }

    private void updateTabUI() {
        int colorAccent = ContextCompat.getColor(requireContext(), R.color.accent_horror);
        int colorCardSurface = ContextCompat.getColor(requireContext(), R.color.card_surface);
        int colorCardSurfaceSec = ContextCompat.getColor(requireContext(), R.color.card_surface_secondary);
        int colorCardStroke = ContextCompat.getColor(requireContext(), R.color.card_stroke);
        int colorTextSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int colorTextTertiary = ContextCompat.getColor(requireContext(), R.color.text_tertiary);

        int activeStrokeWidth = (int) (2.0f * getResources().getDisplayMetrics().density);
        int inactiveStrokeWidth = (int) (1.0f * getResources().getDisplayMetrics().density);

        if (!isFavoriteMode) {
            // Bookmark is Active
            tabBookmark.setCardBackgroundColor(colorCardSurface);
            tabBookmark.setStrokeColor(colorAccent);
            tabBookmark.setStrokeWidth(activeStrokeWidth);
            ivTabBookmarkIcon.setColorFilter(colorAccent);
            tvTabBookmarkText.setTextColor(colorAccent);

            // Favorite is Inactive
            tabFavorite.setCardBackgroundColor(colorCardSurfaceSec);
            tabFavorite.setStrokeColor(colorCardStroke);
            tabFavorite.setStrokeWidth(inactiveStrokeWidth);
            ivTabFavoriteIcon.setColorFilter(colorTextTertiary);
            tvTabFavoriteText.setTextColor(colorTextSecondary);
        } else {
            // Favorite is Active
            tabFavorite.setCardBackgroundColor(colorCardSurface);
            tabFavorite.setStrokeColor(colorAccent);
            tabFavorite.setStrokeWidth(activeStrokeWidth);
            ivTabFavoriteIcon.setColorFilter(colorAccent);
            tvTabFavoriteText.setTextColor(colorAccent);

            // Bookmark is Inactive
            tabBookmark.setCardBackgroundColor(colorCardSurfaceSec);
            tabBookmark.setStrokeColor(colorCardStroke);
            tabBookmark.setStrokeWidth(inactiveStrokeWidth);
            ivTabBookmarkIcon.setColorFilter(colorTextTertiary);
            tvTabBookmarkText.setTextColor(colorTextSecondary);
        }

        updateTabBadgeCounts();
    }

    private void updateTabBadgeCounts() {
        if (databaseHelper != null && isAdded()) {
            int bmCount = databaseHelper.getBookmarkCount();
            int favCount = databaseHelper.getFavoriteCount();

            tvTabBookmarkText.setText(String.format(Locale.getDefault(), "বুকমার্ক (%d)", bmCount));
            tvTabFavoriteText.setText(String.format(Locale.getDefault(), "ফেভারিট (%d)", favCount));
        }
    }

    private void setupSearch() {
        edSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadData() {
        if (databaseHelper == null || !isAdded()) return;

        String query = (edSearch != null) ? edSearch.getText().toString().trim() : "";
        Cursor cursor;

        if (!isFavoriteMode) {
            // Bookmarks
            if (query.isEmpty()) {
                cursor = databaseHelper.getAllDataBookmark();
            } else {
                cursor = databaseHelper.searchDat(query);
            }
        } else {
            // Favorites
            if (query.isEmpty()) {
                cursor = databaseHelper.getAllFavorites();
            } else {
                cursor = databaseHelper.searchFavorites(query);
            }
        }

        allData(cursor);
        updateTabBadgeCounts();
    }

    public void allData(Cursor cursor) {
        arrayList.clear();
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("id", cursor.getString(0));
                    hashMap.put("name", cursor.getString(1));
                    hashMap.put("link", cursor.getString(2));
                    if (cursor.getColumnCount() > 3) {
                        hashMap.put("story_type", cursor.getString(3));
                    }
                    arrayList.add(hashMap);
                }
            } finally {
                cursor.close();
            }
        }

        Collections.reverse(arrayList);

        if (arrayList.isEmpty()) {
            txtBookmarkEmpty.setVisibility(View.VISIBLE);
            bookmarkList.setVisibility(View.GONE);

            if (!isFavoriteMode) {
                ivEmptyIcon.setImageResource(R.drawable.baseline_bookmark_border_24);
                tvEmptyTitle.setText("কোনো বুকমার্ক পাওয়া যায়নি");
                tvEmptySubtitle.setText("পছন্দের গল্প বুকমার্কে রাখতে গল্প পড়ার সময় উপরের বুকমার্ক আইকনে চাপ দিন।");
            } else {
                ivEmptyIcon.setImageResource(R.drawable.baseline_favorite_border_24);
                tvEmptyTitle.setText("কোনো ফেভারিট গল্প পাওয়া যায়নি");
                tvEmptySubtitle.setText("গল্পের তালিকায় থাকা হার্ট (❤️) আইকনে চাপ দিয়ে আপনার পছন্দের গল্পগুলো ফেভারিটে যোগ করুন।");
            }
        } else {
            txtBookmarkEmpty.setVisibility(View.GONE);
            bookmarkList.setVisibility(View.VISIBLE);
        }

        if (bookAdapter != null) {
            bookAdapter.notifyDataSetChanged();
        }
    }

    public class BookAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return arrayList.size();
        }

        @Override
        public Object getItem(int position) {
            return arrayList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.re_item_file, parent, false);
            }

            HashMap<String, String> item = arrayList.get(position);
            String stId = item.get("id");
            String title = item.get("name");
            String content = item.get("link");
            String storyType = item.get("story_type");

            TextView bmTitle = view.findViewById(R.id.storyTitle);
            TextView bmContent = view.findViewById(R.id.storyText);
            TextView bmSerialNumber = view.findViewById(R.id.storySerialNumber);
            ImageView bmImage = view.findViewById(R.id.storyImage);
            ImageView actionIcon = view.findViewById(R.id.favoriteIcon);
            View cv_onClick = view.findViewById(R.id.onclickNext);

            bmSerialNumber.setText(String.valueOf(position + 1));
            bmTitle.setText(title);

            String firstTwoLines = "";
            if (twoLineView != null && content != null && !content.isEmpty()) {
                firstTwoLines = twoLineView.readFirstTwoLinesFromFile(content);
            }
            bmContent.setText(firstTwoLines);
            bmImage.setImageResource(images[position % images.length]);

            if (!isFavoriteMode) {
                // Delete Icon for Bookmark mode
                actionIcon.setImageResource(R.drawable.baseline_delete_24);
                actionIcon.setContentDescription("বুকমার্ক মুছুন");
                actionIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_horror));

                actionIcon.setOnClickListener(v -> {
                    databaseHelper.deleteBookmark(stId);
                    arrayList.remove(position);
                    notifyDataSetChanged();
                    updateTabBadgeCounts();
                    Toast.makeText(requireContext(), "বুকমার্ক মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                    if (arrayList.isEmpty()) {
                        txtBookmarkEmpty.setVisibility(View.VISIBLE);
                        bookmarkList.setVisibility(View.GONE);
                    }
                });
            } else {
                // Filled Heart Icon for Favorite mode
                actionIcon.setImageResource(R.drawable.baseline_favorite_24);
                actionIcon.setContentDescription("ফেভারিট থেকে সরান");
                actionIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_horror));

                actionIcon.setOnClickListener(v -> {
                    databaseHelper.removeFavorite(stId, storyType, title);
                    arrayList.remove(position);
                    notifyDataSetChanged();
                    updateTabBadgeCounts();
                    Toast.makeText(requireContext(), "ফেভারিট থেকে সরানো হয়েছে", Toast.LENGTH_SHORT).show();
                    if (arrayList.isEmpty()) {
                        txtBookmarkEmpty.setVisibility(View.VISIBLE);
                        bookmarkList.setVisibility(View.GONE);
                    }
                });
            }

            cv_onClick.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), StoryViewActivity.class);
                intent.putExtra("story_title", title);
                intent.putExtra("story_content", content);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });

            return view;
        }
    }
}
