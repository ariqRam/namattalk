package com.example.namapopup;

import android.os.Bundle;
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
    }
}