package com.example.namapopup;

import static com.example.namapopup.Helper.getDialectState;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dictionary_activity_layout);
        db = new DBHelper(this);
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
        List<GlobalVariable.HougenInformation> searchResults = new ArrayList<>();

        if (!query.isEmpty()) {
            Cursor[] cursors = db.searchDictionary(query);
            for(int i = 0; i < cursors.length; i++) {
                Cursor cursor = cursors[i];
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        GlobalVariable.HougenInformation hougenInformation = new GlobalVariable.HougenInformation("", "", "", "", "", "", "", new ArrayList<>(), "");
                        String hougen = cursor.getString(cursor.getColumnIndexOrThrow("hougen"));
                        String trigger = cursor.getString(cursor.getColumnIndexOrThrow("trigger"));
                        String def = cursor.getString(cursor.getColumnIndexOrThrow("def"));
                        String example = cursor.getString(cursor.getColumnIndexOrThrow("example"));
                        String pos = cursor.getString(cursor.getColumnIndexOrThrow("pos"));
                        hougenInformation.hougen = hougen;
                        hougenInformation.chihou = Constants.CHIHOUS_JP[i];// Region from the cursor
                        hougenInformation.pref = Constants.PREFS[i];
                        hougenInformation.area = Constants.AREAS[i];
                        hougenInformation.def = def;
                        hougenInformation.example = example;
                        hougenInformation.pos = pos;

                        // When search results found:
                        searchResults.add(hougenInformation);

                        // Use the retrieved data as needed
                    } while (cursor.moveToNext());
                }

                if (cursor != null) {
                    cursor.close();
                }

            }


        }

        // Update the RecyclerView with results
        adapter.updateResults(searchResults);
    }

    private void setupSearchResultView() {
        RecyclerView recyclerView = findViewById(R.id.results_recycler_view);
        adapter = new SearchResultAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void launchDictinfoActivity(GlobalVariable.HougenInformation hougenInformation) {
        Intent intent = new Intent(this, DictionaryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("hougen", hougenInformation.hougen);
        intent.putExtra("chihou", hougenInformation.chihou);
        intent.putExtra("pref", hougenInformation.pref);
        intent.putExtra("area", hougenInformation.area);
        intent.putExtra("def", hougenInformation.def);
        intent.putExtra("example", hougenInformation.example);
        startService(intent);
    }

}

