package com.example.namapopup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DictionaryInfoActivity extends AppCompatActivity {
    NamaPopup namaPopup = new NamaPopup();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout for the activity
        setContentView(R.layout.dictionaryinfo_activity_layout);

        // Retrieve any extras from the intent
        Intent intent = getIntent();
        if (intent != null) {
            String hougen = intent.getStringExtra("hougen");
            String chihou = intent.getStringExtra("chihou");
            String pref = intent.getStringExtra("pref");
            String area = intent.getStringExtra("area");
            String def = intent.getStringExtra("def");
            String example = intent.getStringExtra("example");

            // Find the TextViews and set their content
            TextView inputWordTextView = findViewById(R.id.inputWord);
            TextView inputChihouTextView = findViewById(R.id.inputChihou);
            TextView inputPrefTextView = findViewById(R.id.inputPref);
            TextView inputAreaTextView = findViewById(R.id.inputArea);
            TextView inputMeaningTextView = findViewById(R.id.inputMeaning);
            TextView inputExampleTextView = findViewById(R.id.inputExample);

            inputWordTextView.setText(hougen);
            inputChihouTextView.setText(chihou);
            inputPrefTextView.setText(pref);
            inputAreaTextView.setText(area);
            inputMeaningTextView.setText(def);
            inputExampleTextView.setText(example);
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
