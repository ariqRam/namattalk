package com.example.namapopup;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.HashMap;
import java.util.Map;

public class NewSettingsActivity extends BaseDrawerActivity {
    // Map to store state for each dialect
    Map<String, DialectState> dialectStates = new HashMap<>();
    Button modeButtonToyama;
    Button toggleButtonToyama;
    Button modeButtonHida;
    Button toggleButtonHida;
    int colorGakushuBg = Color.argb(255, 5, 143, 106); // rgba(5, 143, 106, 1)
    int colorGakushuText = Color.WHITE;
    int colorBogoBg = Color.argb(255, 143, 71, 5); // rgba(143, 71, 5, 1)
    int colorBogoText = Color.WHITE;
    int colorMinusBg = Color.argb(255, 161, 43, 43); // rgba(161, 43, 43, 1)
    int colorMinusText = Color.WHITE;
    int colorPlusBg = Color.parseColor("#268D58"); // Hex color
    int colorPlusText = Color.WHITE;
    int colorUnusedBg = Color.parseColor("#D9D9D9"); // Background color
    int colorUnusedText = Color.parseColor("#8E8E8E"); // Text color


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_settings_layout);
        setupDrawer();

        setOnClickListenerForDropdowns();
        initButtonStates();
        setOnClickListenerForDialectButtons();

    }

    private void initButtonStates() {
        modeButtonToyama = findViewById(R.id.mode_button_toyama);
        toggleButtonToyama = findViewById(R.id.toggle_button_toyama);

        modeButtonHida = findViewById(R.id.mode_button_hida);
        toggleButtonHida = findViewById(R.id.toggle_button_hida);

        // Load saved states for dialects
        DialectState toyamaState = getDialectState("富山弁");
        DialectState hidaState = getDialectState("飛騨弁");

        // Set initial states for 富山弁 buttons
        if (toyamaState.isEnabled) {
            modeButtonToyama.setText(toyamaState.mode);
            modeButtonToyama.setBackground(createRoundedRectangleDrawable(
                    toyamaState.mode.equals("学習") ? colorGakushuBg : colorBogoBg));
            modeButtonToyama.setTextColor(colorGakushuText);
            toggleButtonToyama.setText("-");
            toggleButtonToyama.setBackground(createCircleDrawable(colorMinusBg));
            toggleButtonToyama.setTextColor(colorMinusText);
        } else {
            modeButtonToyama.setText("未使用");
            modeButtonToyama.setBackground(createRoundedRectangleDrawable(colorUnusedBg));
            modeButtonToyama.setTextColor(colorUnusedText);
            toggleButtonToyama.setText("+");
            toggleButtonToyama.setBackground(createCircleDrawable(colorPlusBg));
            toggleButtonToyama.setTextColor(colorPlusText);
        }

        if (hidaState.isEnabled) {
            modeButtonHida.setText(hidaState.mode);
            modeButtonHida.setBackground(createRoundedRectangleDrawable(
                    hidaState.mode.equals("学習") ? colorGakushuBg : colorBogoBg));
            modeButtonHida.setTextColor(colorGakushuText);
            toggleButtonHida.setText("-");
            toggleButtonHida.setBackground(createCircleDrawable(colorMinusBg));
            toggleButtonHida.setTextColor(colorMinusBg);
        } else {
            modeButtonHida.setText("未使用");
            modeButtonHida.setBackground(createRoundedRectangleDrawable(colorUnusedBg));
            modeButtonHida.setTextColor(colorUnusedText);
            toggleButtonHida.setText("+");
            toggleButtonHida.setBackground(createCircleDrawable(colorPlusBg));
            toggleButtonHida.setTextColor(colorPlusText);
        }
    }

    private void setOnClickListenerForDropdowns() {
        TextView dropdownChubu = findViewById(R.id.dropdown_chubu);
        LinearLayout itemsChubu = findViewById(R.id.items_chubu);

        // Set initial arrow pointing right
        dropdownChubu.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_right, 0);

        dropdownChubu.setOnClickListener(v -> {
            if (itemsChubu.getVisibility() == View.GONE) {
                itemsChubu.setVisibility(View.VISIBLE);
                // Change arrow to point down when items are visible
                dropdownChubu.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
            } else {
                itemsChubu.setVisibility(View.GONE);
                // Change arrow to point right when items are hidden
                dropdownChubu.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_right, 0);
            }
        });
    }

    // Save dialect state in SharedPreferences
    private void saveDialectState(String dialect, String mode, boolean isEnabled) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save the mode and enabled state with unique keys
        editor.putString(dialect + "_mode", mode);
        editor.putBoolean(dialect + "_enabled", isEnabled);

        editor.apply(); // Save changes
    }


    // Retrieve dialect state from SharedPreferences
    private DialectState getDialectState(String dialect) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        // Get mode and enabled state, with defaults if not found
        String mode = sharedPreferences.getString(dialect + "_mode", "学習");
        boolean isEnabled = sharedPreferences.getBoolean(dialect + "_enabled", false);

        DialectState state = new DialectState();
        state.mode = mode;
        state.isEnabled = isEnabled;

        return state;
    }


    private void setOnClickListenerForDialectButtons() {

        dialectStates.put("富山弁", new DialectState());
        dialectStates.put("飛騨弁", new DialectState());

        // Click listener for mode buttons (rounded rectangle)
        View.OnClickListener modeClickListener = v -> {
            String dialect = (String) v.getTag();
            DialectState state = dialectStates.get(dialect);
            Button button = (Button) v;

            // Toggle mode between 学習 and 母語
            if (state.mode.equals("学習")) {
                state.mode = "母語";
                button.setText(state.mode);
                button.setBackground(createRoundedRectangleDrawable(colorBogoBg));
                button.setTextColor(colorBogoText);
            } else {
                state.mode = "学習";
                button.setText(state.mode);
                button.setBackground(createRoundedRectangleDrawable(colorGakushuBg));
                button.setTextColor(colorGakushuText);
            }

            // Save updated state
            saveDialectState(dialect, state.mode, state.isEnabled);
        };


        // Click listener for toggle buttons (circle)
        View.OnClickListener toggleClickListener = v -> {
            String dialect = (String) v.getTag();
            DialectState state = dialectStates.get(dialect);
            Button toggleButton = (Button) v;

            // Find the corresponding mode button
            Button modeButton;
            if (dialect.equals("富山弁")) {
                modeButton = findViewById(R.id.mode_button_toyama);
            } else { // 飛騨弁
                modeButton = findViewById(R.id.mode_button_hida);
            }

            // Toggle between + and -
            state.isEnabled = !state.isEnabled;

            if (state.isEnabled) {
                // If toggle is +, revert mode button to 学習 or 母語
                toggleButton.setText("+");
                toggleButton.setBackground(createCircleDrawable(colorPlusBg));
                toggleButton.setTextColor(colorPlusText);

                // Restore the original mode background and text
                modeButton.setText(state.mode);
                if (state.mode.equals("学習")) {
                    modeButton.setBackground(createRoundedRectangleDrawable(colorGakushuBg));
                    modeButton.setTextColor(colorGakushuText);
                } else {
                    modeButton.setBackground(createRoundedRectangleDrawable(colorBogoBg));
                    modeButton.setTextColor(colorBogoText);
                }
            } else {
                // If toggle is -, set mode button to 未使用 with corresponding colors
                toggleButton.setText("-");
                toggleButton.setBackground(createCircleDrawable(colorMinusBg));
                toggleButton.setTextColor(colorMinusText);

                // Update mode button to "未使用"
                modeButton.setText("未使用");
                modeButton.setBackground(createRoundedRectangleDrawable(colorUnusedBg));
                modeButton.setTextColor(colorUnusedText);
            }

            // Save updated state
            saveDialectState(dialect, state.mode, state.isEnabled);
        };


        // Assign tags to buttons and set listeners
        modeButtonToyama.setTag("富山弁");
        modeButtonToyama.setOnClickListener(modeClickListener);

        toggleButtonToyama.setTag("富山弁");
        toggleButtonToyama.setOnClickListener(toggleClickListener);

        modeButtonHida.setTag("飛騨弁");
        modeButtonHida.setOnClickListener(modeClickListener);

        toggleButtonHida.setTag("飛騨弁");
        toggleButtonHida.setOnClickListener(toggleClickListener);
    }

    // Create rounded rectangle drawable
    private GradientDrawable createRoundedRectangleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(16f); // Set the desired corner radius for the rounded rectangle
        return drawable;
    }


    // Create circle drawable
    private GradientDrawable createCircleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

}

// Create a data class to hold the state for each dialect
class DialectState {
    String mode = "学習"; // Default mode
    boolean isEnabled = false; // Default toggle state
}

