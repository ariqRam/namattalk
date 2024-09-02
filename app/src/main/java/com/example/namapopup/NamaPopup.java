package com.example.namapopup;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.Random;

public class NamaPopup extends AccessibilityService {
    private WindowManager windowManager;
    private View floatingButton;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("NAMA_POPUP", "Service connected");

        // Configure the AccessibilityServiceInfo to listen for text changes
        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        serviceInfo.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        serviceInfo.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        serviceInfo.packageNames = null; // Listen to text changes in all apps

        createFloatingButton();

        setServiceInfo(serviceInfo);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log and process the text whenever it changes
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            CharSequence text = event.getText().toString();
            Log.d("NAMA_POPUP", "Text changed: " + text);

            // Change the button's color to a random color
            View button = floatingButton.findViewById(R.id.accessibility_button);
            int randomColor = getRandomColor();
            button.setBackgroundColor(randomColor);
            Log.d("NAMA_POPUP", "Color set to: " + Integer.toString(randomColor));
        }
    }

    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the floating button layout
        floatingButton = LayoutInflater.from(this).inflate(R.layout.accessibility_button_layout, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;

        // Enable dragging
        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingButton, params);
                        return true;
                }
                return false;
            }
        });

        // Set an action when the button is clicked
        floatingButton.setOnClickListener(v -> {
            Log.d("NAMA_POPUP", "Floating button clicked");
            // Trigger your accessibility functionality here
        });

        // Add the floating button to the window
        windowManager.addView(floatingButton, params);
    }

    @Override
    public void onInterrupt() {
        // Handle any cleanup or interruptions here
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingButton != null) {
            windowManager.removeView(floatingButton);
        }
    }

    private int getRandomColor() {
        Random random = new Random();
        return Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
}