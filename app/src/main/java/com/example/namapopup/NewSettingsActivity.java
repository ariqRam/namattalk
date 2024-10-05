package com.example.namapopup;

import static com.example.namapopup.Helper.getDialectState;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewSettingsActivity extends BaseDrawerActivity {
    // Map to store state for each dialect
    Map<String, DialectState> dialectStates = new HashMap<>();
    Button modeButtonToyama;
    Button modeButtonHida;
    int colorGakushuBg = Color.argb(255, 5, 143, 106); // rgba(5, 143, 106, 1)
    int colorGakushuText = Color.WHITE;
    int colorBogoBg = Color.argb(255, 143, 71, 5); // rgba(143, 71, 5, 1)
    int colorBogoText = Color.WHITE;
    int colorUnusedBg = Color.parseColor("#D9D9D9"); // Background color
    int colorUnusedText = Color.parseColor("#8E8E8E"); // Text color
    private DBHelper db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_settings_layout);
        setupDrawer();
        setOnClickListenerForDropdowns();
        initButtonStates();
        setOnClickListenerForDialectButtons();
        checkPopupPermission();
        setUpCountText();

    }

    private void setUpCountText() {
        Log.d("NewSettingsAct", "SetupCountText");
        db = new DBHelper(this);
        TextView hidaCountText = findViewById(R.id.hida_count);
        String hidaText =  db.getFoundWordsRatio(0);
        hidaCountText.setText(hidaText);
        TextView toyamaCountText = findViewById(R.id.toyama_count);
        String toyamaText =  db.getFoundWordsRatio(1);
        toyamaCountText.setText(toyamaText);
    }

    private void checkPopupPermission() {
        if (!isAccessibilityServiceEnabled(NamaPopup.class)) {
            // Show a dialog or notification to the user
            new AlertDialog.Builder(this)
                    .setTitle("Enable Accessibility Service")
                    .setMessage("Please enable なまっtalk accessibility service for the app to function properly.")
                    .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            redirectToAccessibilitySettings();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        // Check if overlay permission is granted
        if (canDrawOverlays()) {
            // Permission is granted
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            // Proceed with your functionality
        } else {
            // Permission is not granted, prompt user to enable it
            Toast.makeText(this, "Permission not granted. Redirecting to settings.", Toast.LENGTH_SHORT).show();
            requestDrawOverlaysPermission();
        }
    }


    private void initButtonStates() {
        modeButtonToyama = findViewById(R.id.mode_button_toyama);

        modeButtonHida = findViewById(R.id.mode_button_hida);

        // Load saved states for dialects
        DialectState toyamaState = getDialectState(this, "toyama");
        DialectState hidaState = getDialectState(this, "hida");

        // Set initial states for toyama buttons
        if (toyamaState.isEnabled()) {
            modeButtonToyama.setText(toyamaState.mode);
            modeButtonToyama.setBackground(createRoundedRectangleDrawable(
                    toyamaState.mode.equals("学習") ? colorGakushuBg : colorBogoBg));
            modeButtonToyama.setTextColor(colorGakushuText);
        } else {
            modeButtonToyama.setText("未使用");
            modeButtonToyama.setBackground(createRoundedRectangleDrawable(colorUnusedBg));
            modeButtonToyama.setTextColor(colorUnusedText);
        }

        if (hidaState.isEnabled()) {
            modeButtonHida.setText(hidaState.mode);
            modeButtonHida.setBackground(createRoundedRectangleDrawable(
                    hidaState.mode.equals("学習") ? colorGakushuBg : colorBogoBg));
            modeButtonHida.setTextColor(colorGakushuText);
        } else {
            modeButtonHida.setText("未使用");
            modeButtonHida.setBackground(createRoundedRectangleDrawable(colorUnusedBg));
            modeButtonHida.setTextColor(colorUnusedText);

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
    private void saveDialectState(String dialect, String mode) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save the mode and enabled state with unique keys
        editor.putString(dialect + "_mode", mode);


        editor.apply(); // Save changes
    }


    private void setOnClickListenerForDialectButtons() {

        dialectStates.put("toyama", new DialectState());
        dialectStates.put("hida", new DialectState());

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
            } else if(state.mode.equals("母語")) {
                state.mode = "未使用";
                button.setText(state.mode);
                button.setBackground(createRoundedRectangleDrawable(colorUnusedBg));
                button.setTextColor(colorUnusedText);
            } else if (state.mode.equals("未使用")) {
                state.mode = "学習";
                button.setText(state.mode);
                button.setBackground(createRoundedRectangleDrawable(colorGakushuBg));
                button.setTextColor(colorGakushuText);
            }

            // Save updated state
            saveDialectState(dialect, state.mode);
        };



        // Assign tags to buttons and set listeners
        modeButtonToyama.setTag("toyama");
        modeButtonToyama.setOnClickListener(modeClickListener);

        modeButtonHida.setTag("hida");
        modeButtonHida.setOnClickListener(modeClickListener);
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

    private boolean isAccessibilityServiceEnabled(Class<? extends AccessibilityService> service) {
        AccessibilityManager accessibilityManager =
                (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            Log.d("AccessibilityService", "Enabled Service ID: " + enabledService.getId());
            if (enabledService.getId().contains("namapopup")) {
                return true; // Service is enabled
            }
        }
        return false; // Service is not enabled
    }


    private void redirectToAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // Method to check if the permission is granted
    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    // Method to request the permission
    private void requestDrawOverlaysPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 100); // You can choose any request code
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check if the permission has been granted after returning from settings
        if (requestCode == 100) {
            if (canDrawOverlays()) {
                Toast.makeText(this, "Permission granted after returning from settings", Toast.LENGTH_SHORT).show();
                // Proceed with your functionality
            } else {
                Toast.makeText(this, "Permission still not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

