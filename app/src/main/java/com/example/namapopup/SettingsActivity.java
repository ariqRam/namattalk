package com.example.namapopup;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class SettingsActivity extends BaseDrawerActivity {

    private SharedPreferences sharedPreferences;
    private LinearLayout switchLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        switchLayout = findViewById(R.id.switch_layout);

        setupDrawer();

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        drawChihousSwitchView();

        // making mode swtich button(true: non-native mode, false: native mode)
        Switch modeSwitch = findViewById(R.id.modeSwitch);

        //Load previous State
        modeSwitch.setChecked(sharedPreferences.getBoolean(Constants.NON_NATIVE_MODE, false));

        // Listen for changes
        modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Log.d("Chihou Switch Toggled",  Constants.NON_NATIVE_MODE + " pref toggled: " + Boolean.toString(isChecked));
            editor.putBoolean(Constants.NON_NATIVE_MODE, isChecked);
            editor.apply();
        });

        ImageView backToMainButton = findViewById(R.id.backToMainButton);
        backToMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSettingsActivity();
            }
        });
    }

    private void closeSettingsActivity() {
        Intent intent = new Intent(SettingsActivity.this, HougenInfoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Only needed if starting from non-activity context
        startService(intent); // Start the service if it's not running
        finish(); // Close SettingsActivity
    }

    private void drawChihousSwitchView() {
        for(int i = 0; i < Constants.CHIHOUS.length; i++) {
            String chihou = Constants.CHIHOUS[i];
            Switch newSwitch = new Switch(this);
            newSwitch.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            newSwitch.setId(View.generateViewId());
            newSwitch.setTextSize(24);
            newSwitch.setText(chihou); // Set the text to the prefecture name
            switchLayout.addView(newSwitch, i);

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
    }
}


