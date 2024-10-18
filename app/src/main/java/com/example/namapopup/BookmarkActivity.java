package com.example.namapopup;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;

public class BookmarkActivity extends BaseDrawerActivity {
    private ViewPager2 viewPager;
    private FlashcardAdapter adapter;
    private List<GlobalVariable.Flashcard> flashcardList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookmark_layout);

        RecyclerView bookmarkRecyclerView = findViewById(R.id.bookmarkRecyclerView);

        // Sample data for demonstration
        List<GlobalVariable.Flashcard> bookmarks = new ArrayList<>();
        bookmarks.add(new GlobalVariable.Flashcard("Title 1"));
        bookmarks.add(new GlobalVariable.Flashcard("Title 2"));
        bookmarks.add(new GlobalVariable.Flashcard("Title 3"));

        FlashcardAdapter adapter = new FlashcardAdapter(bookmarks);
        bookmarkRecyclerView.setAdapter(adapter);

        // Set horizontal layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        bookmarkRecyclerView.setLayoutManager(layoutManager);
    }
}
