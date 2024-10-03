package com.example.namapopup;

import static com.example.namapopup.Helper.getDialectState;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryActivity extends BaseDrawerActivity {
    private SearchResultAdapter adapter;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dictionary_activity_layout);
        setupDrawer();
        setupSearchResultView();
        setupSearchInputView();

    }

    private void setupSearchInputView() {
        // Initialize the search input
        searchInput = findViewById(R.id.search_input);
        // Method 1: Listen for text changes
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String searchQuery = s.toString();
                performSearch(searchQuery);
            }
        });

        // Method 2: Listen for "submit" action
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String searchQuery = searchInput.getText().toString();
                performSearch(searchQuery);

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                return true;
            }
            return false;
        });

        // Show keyboard when EditText is focused
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void performSearch(String query) {
        // TODO : Implement search
        // Implement your search logic here
        // This is just an example:
        List<HashMap<String,String>> searchResults = new ArrayList<>();

        if (!query.isEmpty()) {
            // Replace this with your actual dictionary search logic
            // When you have search results:
            HashMap<String, String> result = new HashMap<>();
            result.put("text", "おる");
            result.put("meaning", "いる、います");
            result.put("region", "飛騨弁");

            HashMap<String, String> result3 = new HashMap<>();
            result3.put("text", "べらぼう");
            result3.put("meaning", "とても、非常に");
            result3.put("region", "江戸弁");
            searchResults.add(result);
            searchResults.add(result3);
        }

        // Update the RecyclerView with results
        adapter.updateResults(searchResults);
    }

    private void setupSearchResultView() {
        RecyclerView recyclerView = findViewById(R.id.results_recycler_view);
        adapter = new SearchResultAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // When you have search results:
        HashMap<String, String> result1 = new HashMap<>();
        result1.put("text", "おる");
        result1.put("meaning", "いる、います");
        result1.put("region", "飛騨弁");

        // Result 2
        HashMap<String, String> result2 = new HashMap<>();
        result2.put("text", "あかん");
        result2.put("meaning", "だめ、いけない");
        result2.put("region", "関西弁");

        // Result 3
        HashMap<String, String> result3 = new HashMap<>();
        result3.put("text", "べらぼう");
        result3.put("meaning", "とても、非常に");
        result3.put("region", "江戸弁");

        List<HashMap<String, String>> searchResults = Arrays.asList(result1);
        adapter.updateResults(searchResults);
    }
}

