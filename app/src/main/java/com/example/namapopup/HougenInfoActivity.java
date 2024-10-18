package com.example.namapopup;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;

public class HougenInfoActivity extends Service {
    private WindowManager mWindowManager;
    private View mFloatingView;
    DBHelper dbHelper;
    ImageView bookmarkButton;


    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(this);

        showFloatingView();
        bookmarkButton = mFloatingView.findViewById(R.id.bookmarkButton);

        ImageView settingsButton = mFloatingView.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the SettingsActivity when the settings button is clicked
                Log.d("FloatingView", "Settings button clicked");
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                stopSelf();
                if (mFloatingView != null) {
                    mWindowManager.removeView(mFloatingView);
                }
            }
        });




        ImageView closeButton = mFloatingView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf(); // Stops the service and removes the floating view
                if (mFloatingView != null) {
                    mWindowManager.removeView(mFloatingView);
                }
            }
        });
    }

    private void showFloatingView() {
        if (mFloatingView == null) {
            // Inflate the floating view layout we created

            mFloatingView = LayoutInflater.from(this).inflate(R.layout.hougeninfo_activity_layout, null);

            // Setup the layout parameters
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            // Specify the initial position of the window
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = 0;
            params.y = 0;

            // Add the view to the window
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mFloatingView, params);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if the intent is not null and contains the expected data
        if (intent != null) {
            String hougen = intent.getStringExtra("hougen");
            String chihou = intent.getStringExtra("chihou");
            String pref = intent.getStringExtra("pref");
            String area = intent.getStringExtra("area");
            String def = intent.getStringExtra("def");
            String example = intent.getStringExtra("example");
            String hinshi = intent.getStringExtra("pos");

            // Update the floating view TextViews with the received data
            if (mFloatingView != null) {
                TextView inputWordTextView = mFloatingView.findViewById(R.id.inputWord);
                TextView inputChihouTextView = mFloatingView.findViewById(R.id.inputChihou);
                TextView inputPrefTextView = mFloatingView.findViewById(R.id.inputPref);
                TextView inputAreaTextView = mFloatingView.findViewById(R.id.inputArea);
                TextView inputMeaningTextView = mFloatingView.findViewById(R.id.inputMeaning);
                TextView inputHinshiTextView = mFloatingView.findViewById(R.id.inputHinshi);
                TextView inputExampleTextView = mFloatingView.findViewById(R.id.inputExample);

                inputWordTextView.setText(hougen);
                inputChihouTextView.setText(chihou);
                inputPrefTextView.setText(pref);
                inputAreaTextView.setText(area);
                inputMeaningTextView.setText(def);
                inputHinshiTextView.setText(hinshi);
                inputExampleTextView.setText(example);

                LinearLayout areaRow = mFloatingView.findViewById(R.id.areaRow);
                if (inputAreaTextView.getText().toString().trim().isEmpty()) areaRow.setVisibility(View.GONE);

                LinearLayout meaningRow = mFloatingView.findViewById(R.id.meaningRow);
                if (inputMeaningTextView.getText().toString().trim().isEmpty()) meaningRow.setVisibility(View.GONE);

                LinearLayout posRow = mFloatingView.findViewById(R.id.posRow);
                if (inputHinshiTextView.getText().toString().trim().isEmpty()) posRow.setVisibility(View.GONE);

                LinearLayout exampleRow = mFloatingView.findViewById(R.id.exampleRow);
                if (inputExampleTextView.getText().toString().trim().isEmpty()) exampleRow.setVisibility(View.GONE);
            }

            // Handle bookmark
            int regionIndex = Arrays.asList(Constants.CHIHOUS_JP).indexOf(chihou); // []
            boolean isBookmarked = dbHelper.isBookmarked(hougen, regionIndex);
            Log.d("TOYANMA", "isBookmarked: " + isBookmarked);
            bookmarkButton.setImageResource(isBookmarked ? R.drawable.baseline_bookmark_24 : R.drawable.baseline_bookmark_border_24);
            bookmarkButton.setTag(isBookmarked ? R.drawable.baseline_bookmark_24 : R.drawable.baseline_bookmark_border_24);
            bookmarkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int currentImageResource = (int) bookmarkButton.getTag(); // Get the current tag (image resource)

                    if (currentImageResource == R.drawable.baseline_bookmark_border_24) {
                        bookmarkButton.setImageResource(R.drawable.baseline_bookmark_24); // Set filled bookmark
                        dbHelper.addWordToBookmarks(hougen, regionIndex);
                        bookmarkButton.setTag(R.drawable.baseline_bookmark_24); // Update tag
                    } else {
                        bookmarkButton.setImageResource(R.drawable.baseline_bookmark_border_24); // Set border bookmark
                        bookmarkButton.setTag(R.drawable.baseline_bookmark_border_24); // Update tag
                        dbHelper.removeWordFromBookmarks(hougen, regionIndex);
                    }

                }
            });
        }

        // Return START_STICKY to keep the service running
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null && mFloatingView.isAttachedToWindow()) {
            try {
                mWindowManager.removeView(mFloatingView);
            } catch (IllegalArgumentException e) {
                // This exception occurs if the view is not attached to the window
                e.printStackTrace();
            }
        }
    }
}

