package com.example.namapopup;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;

public class BookmarkActivity extends BaseDrawerActivity {
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);

        setContentView(R.layout.bookmark_layout);
        TextView title = findViewById(R.id.screenTitle);
        title.setText("ブックマーク");

        // Remove default action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        RecyclerView bookmarkRecyclerView = findViewById(R.id.bookmarkRecyclerView);

        List<List<GlobalVariable.HougenInformation>> allBookmarks = dbHelper.getAllBookmarks();

        List<GlobalVariable.HougenInformation> bookmarks = new ArrayList<>();
        for (int i = 0; i < allBookmarks.size(); i++) {
            bookmarks.addAll(allBookmarks.get(i));
        }

        FlashcardAdapter adapter = new FlashcardAdapter(bookmarks);
        bookmarkRecyclerView.setAdapter(adapter);

        // Set horizontal layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        bookmarkRecyclerView.setLayoutManager(layoutManager);

        // Attach a SnapHelper to enable one-by-one paging scroll
        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(bookmarkRecyclerView);



        setupDrawer();
    }
}
