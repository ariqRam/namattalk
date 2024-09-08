package com.example.namapopup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);

        TextView inputWordTextView = findViewById(R.id.inputWord);
        TextView inputMeaningTextView = findViewById(R.id.inputMeaning);


        // Get the text passed from the NamaPopup service
        String popupText = getIntent().getStringExtra("POPUP_TEXT");

        if (popupText != null && !popupText.isEmpty()) {
            inputWordTextView.setText(popupText);
            if (popupText.equals("方言")) {
                inputMeaningTextView.setText("ほうげん");
            } else {
                inputMeaningTextView.setText("なし");
            }
        } else {
            // Default text if no text was passed
            inputWordTextView.setText("入力して");
            inputMeaningTextView.setText("なし");
        }

        ImageView settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the SettingsActivity when the settings button is clicked
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });


        ImageView closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // This will close the current activity
                System.exit(0); // This will ensure the app fully closes
            }
        });
    }
}