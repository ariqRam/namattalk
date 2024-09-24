package com.example.namapopup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private Switch toyamaSwitch;
    private SharedPreferences sharedPreferences;
    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity_layout);
        layout = findViewById(R.id.settings_layout);

        ImageView menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("SettingsActivity", "Menu button clicked");
            }
        });
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);


        for(int i = 0; i < Constants.CHIHOUS.length; i++) {
            String chihou = Constants.CHIHOUS[i];
            Switch newSwitch = new Switch(this);
            newSwitch.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            newSwitch.setId(View.generateViewId());
            newSwitch.setTextSize(24);
            newSwitch.setText(chihou); // Set the text to the prefecture name
            layout.addView(newSwitch, i);

            // Load the previous state
            boolean switchValue = sharedPreferences.getBoolean(chihou, false);
            newSwitch.setChecked(switchValue);

            // Listen for changes
            newSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Log.d("Chihou Switch Toggled", chihou + " pref toggled: " + Boolean.toString(isChecked));
                editor.putBoolean(chihou, isChecked);
                editor.apply();
            });
        }



        ImageView backToMainButton = findViewById(R.id.backToMainButton);
        backToMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, HougenInfoActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Only needed if starting from non-activity context
                startService(intent); // Start the service if it's not running
                finish(); // Close SettingsActivity
            }
        });
    }
}


