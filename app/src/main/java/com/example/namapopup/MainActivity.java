package com.example.namapopup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    NamaPopup namaPopup = new NamaPopup();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);

        TextView inputWordTextView = findViewById(R.id.inputWord);
        TextView inputHougenchihouTextView = findViewById(R.id.inputHougenchihou);
        TextView inputPrefTextView = findViewById(R.id.inputPref);
        TextView inputAreaTextView = findViewById(R.id.inputArea);
        TextView inputMeaningTextView = findViewById(R.id.inputMeaning);
        TextView inputExampleTextView = findViewById(R.id.inputExample);

        String hougen = getIntent().getStringExtra("hougen");
        String hougenchihou = getIntent().getStringExtra("hougenchihou");
        String pref = getIntent().getStringExtra("pref");
        String area = getIntent().getStringExtra("area");
        String def = getIntent().getStringExtra("def");
        String example = getIntent().getStringExtra("example");

        //setText in TextView
        inputWordTextView.setText(hougen);
        inputHougenchihouTextView.setText(hougenchihou);
        inputPrefTextView.setText(pref);
        inputAreaTextView.setText(area);
        inputMeaningTextView.setText(def);
        inputExampleTextView.setText(example);
        Log.d("MainActivity", "hougen: " + hougen);

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
//                GlobalVariable.isMainActivityRunning = false;
                namaPopup.updateFloatingPopupVisibility();
            }
        });
    }
}

