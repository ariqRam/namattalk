package com.example.namapopup;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import java.util.Random;

public class NamaPopup extends AccessibilityService {
    private WindowManager windowManager;
    private View floatingButton;
    private TextView textView;
    CharSequence composingText;
    private static final long LONG_PRESS_THRESHOLD = 500;

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
        String TAG = "onAccessibilityEvent";

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            // Check if the event source is an editable text field
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.isEditable()) {
                // Get the text from the event
                String preText = event.getText().toString();
                String text = preText.substring(1, preText.length() - 1);
                Log.d(TAG, "isEditable " + text);

                // Extract the composing text
                Log.d(TAG, "Setting composingText to " + text);
                composingText = text;
                if(composingText.equals("ほうげん")) textView.setText("方言");
            }
        }

    }

    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the floating button layout
        floatingButton = LayoutInflater.from(this).inflate(R.layout.accessibility_button_layout, null);
        textView = floatingButton.findViewById(R.id.accessibility_text);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;
        floatingButton.setClickable(true);

        // Enable dragging
        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long touchStartTime;
            private boolean isLongPress = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        v.removeCallbacks(longPressRunnable);
                        long touchDuration = System.currentTimeMillis() - touchStartTime;
                        // Consider it a click if the touch was short and the movement was minimal
                        if (!isLongPress && touchDuration < LONG_PRESS_THRESHOLD) {
                            handleButtonClick();
                        }
//                        if (touchDuration < 200 &&
//                                Math.abs(event.getRawX() - initialTouchX) < 10 &&
//                                Math.abs(event.getRawY() - initialTouchY) < 10) {
//                            Log.d("onTouch", "Floating button clicked");
//                            // Handle the click action
//                            if (composingText != null && composingText.equals("ほうげん")) {
//                                Log.d("onTouch", "Setting textView to " + composingText);
//                                textView.setText("方言");
//                            }
//                        }
//                        handleButtonClick();
                        return true;
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        isLongPress = false;
                        v.postDelayed(longPressRunnable, LONG_PRESS_THRESHOLD);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!isLongPress) {
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingButton, params);
                        }

                        return true;
                }

                return false;
            }

            private Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    isLongPress = true;
                    launchMainActivity();
                }
            };

            private void handleButtonClick() {
                Log.d("handleButtonClick", "Button clicked");
                if (composingText != null && composingText.toString().equals("ほうげん")) {
                    Log.d("handleButtonClick", "Setting composing text to 方言");

                    // Attempt to modify the text in the focused edit field
                    AccessibilityNodeInfo focusedNode = getRootInActiveWindow().findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                    if (focusedNode != null && focusedNode.isEditable()) {
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "方言");
                        focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                        focusedNode.recycle();
                        composingText = "";
                        textView.setText("な");
                    }
                }
            }
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

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Pass the current text of the popup button
        intent.putExtra("POPUP_TEXT", textView.getText().toString());
        startActivity(intent);
    }

}