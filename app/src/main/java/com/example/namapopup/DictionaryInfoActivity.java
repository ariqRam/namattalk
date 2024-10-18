package com.example.namapopup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

public class DictionaryInfoActivity extends AppCompatActivity {
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dbHelper = new DBHelper(this);
        super.onCreate(savedInstanceState);

        // Set the layout for the activity
        setContentView(R.layout.dictionaryinfo_activity_layout);

        // Remove default action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Retrieve any extras from the intent
        Intent intent = getIntent();
        if (intent != null) {
            String hougen = intent.getStringExtra("hougen");
            String chihou = intent.getStringExtra("chihou");
            String pref = intent.getStringExtra("pref");
            String area = intent.getStringExtra("area");
            String def = intent.getStringExtra("def");
            String hinshi = intent.getStringExtra("pos");
            String example = intent.getStringExtra("example");

            // Find the TextViews and set their content
            TextView inputWordTextView = findViewById(R.id.inputWord);
            TextView inputChihouTextView = findViewById(R.id.inputChihou);
            TextView inputPrefTextView = findViewById(R.id.inputPref);
            TextView inputAreaTextView = findViewById(R.id.inputArea);
            TextView inputMeaningTextView = findViewById(R.id.inputMeaning);
            TextView inputHinshiTextView = findViewById(R.id.inputHinshi);
            TextView inputExampleTextView = findViewById(R.id.inputExample);

            inputWordTextView.setText(hougen);
            inputChihouTextView.setText(chihou);
            inputPrefTextView.setText(pref);
            inputAreaTextView.setText(area);
            inputMeaningTextView.setText(def);
            inputHinshiTextView.setText(hinshi);
            inputExampleTextView.setText(example);

            LinearLayout areaRow = findViewById(R.id.areaRow);
            if (inputAreaTextView.getText().toString().trim().isEmpty()) areaRow.setVisibility(View.GONE);

            LinearLayout meaningRow = findViewById(R.id.meaningRow);
            if (inputMeaningTextView.getText().toString().trim().isEmpty()) meaningRow.setVisibility(View.GONE);

            LinearLayout posRow = findViewById(R.id.posRow);
            if (inputHinshiTextView.getText().toString().trim().isEmpty()) posRow.setVisibility(View.GONE);

            LinearLayout exampleRow = findViewById(R.id.exampleRow);
            if (inputExampleTextView.getText().toString().trim().isEmpty()) exampleRow.setVisibility(View.GONE);
            boolean isBookmarked = dbHelper.isBookmarked(hougen);

            // Handle bookmark
            ImageView bookmarkButton = findViewById(R.id.bookmarkButton);
            bookmarkButton.setImageResource(isBookmarked ?  R.drawable.baseline_bookmark_24 : R.drawable.baseline_bookmark_border_24);
            bookmarkButton.setTag(isBookmarked ? R.drawable.baseline_bookmark_24 : R.drawable.baseline_bookmark_border_24);
            bookmarkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int currentImageResource = (int) bookmarkButton.getTag(); // Get the current tag (image resource)

                    if (currentImageResource == R.drawable.baseline_bookmark_border_24) {
                        bookmarkButton.setImageResource(R.drawable.baseline_bookmark_24); // Set filled bookmark
                        bookmarkButton.setTag(R.drawable.baseline_bookmark_24); // Update tag
                    } else {
                        bookmarkButton.setImageResource(R.drawable.baseline_bookmark_border_24); // Set border bookmark
                        bookmarkButton.setTag(R.drawable.baseline_bookmark_border_24); // Update tag
                    }

                    int regionIndex = Arrays.asList(Constants.CHIHOUS_JP).indexOf(chihou);
                    dbHelper.addWordToBookmarks(hougen, regionIndex);
                }
            });
        }

        // Handle the close button click if necessary
        ImageView closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the activity
            }
        });

    }
}
