package com.example.namapopup;

import static android.media.AudioManager.ADJUST_RAISE;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.logging.Logger;

public class NamaPopup extends AccessibilityService {
    private AudioManager audioManager;
    private AccessibilityButtonController accessibilityButtonController;
    private AccessibilityButtonController
            .AccessibilityButtonCallback accessibilityButtonCallback;
    private boolean mIsAccessibilityButtonAvailable;


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("NAMA_POPUP", "Service connected");
        createOverlay();

        // Initialize Accessibility Button Controller
        accessibilityButtonController = getAccessibilityButtonController();

        if (accessibilityButtonController == null) {
            Log.e("NAMA_POPUP", "AccessibilityButtonController is null");
            return;
        }

        // Update AccessibilityServiceInfo with the button flag
        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        serviceInfo.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
        setServiceInfo(serviceInfo);

        // Set up Accessibility Button Callback
        accessibilityButtonCallback = new AccessibilityButtonController.AccessibilityButtonCallback() {
            @Override
            public void onClicked(AccessibilityButtonController controller) {
                Log.d("NAMA_POPUP", "Accessibility button clicked!");
                increaseVolume();
                // Add custom logic for the button press here
            }

            @Override
            public void onAvailabilityChanged(AccessibilityButtonController controller, boolean available) {
                mIsAccessibilityButtonAvailable = available;
                Log.d("NAMA_POPUP", "Accessibility button availability changed: " + available);
            }
        };

        // Register the callback immediately
        try {
            Log.d("NAMA_POPUP", "Attempting to register callback");
            accessibilityButtonController.registerAccessibilityButtonCallback(accessibilityButtonCallback);
            Log.d("NAMA_POPUP", "Callback registered successfully");
        } catch (NullPointerException e) {
            Log.e("NAMA_POPUP", "Failed to register callback: " + e.getMessage());
        }

        // Check button availability
        mIsAccessibilityButtonAvailable = accessibilityButtonController.isAccessibilityButtonAvailable();
        Log.d("NAMA_POPUP", "Initial accessibility button availability: " + mIsAccessibilityButtonAvailable);
    }    private void createOverlay() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View overlayView = inflater.inflate(R.layout.overlay_layout, null);

        // Add a touch listener to the overlay view
        overlayView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d("ARIQ", "TAPPED");
                increaseVolume();
            }
        });

        wm.addView(overlayView, params);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("NAMA_POPUP", "Service onCreate called");
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // Re-check the permission when the service starts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                createOverlay();
            }
        } else {
            createOverlay();
        }
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Log.d("ARQI", accessibilityEvent.toString());
    }

    private void increaseVolume() {
        Log.d("INCREASE VOLUME",  "clicked");
        if (audioManager != null) {
            // Adjust the volume for the STREAM_ACCESSIBILITY
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_ACCESSIBILITY,  // The stream type to adjust
                    ADJUST_RAISE,                          // Direction: ADJUST_RAISE, ADJUST_LOWER, or ADJUST_SAME
                    AudioManager.FLAG_SHOW_UI // Enable accessibility volume stream
            );
        }
    }

    @Override
    public void onInterrupt() {
        // Handle interruptions
    }



}