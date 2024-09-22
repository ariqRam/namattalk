package com.example.namapopup;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NamaPopup extends AccessibilityService {
    private WindowManager windowManager;
    private View floatingButton;
    private TextView textView;
    private TextView hougenchihouTextView;
    CharSequence composingText;
    private String convertedText = "";
    private static final long LONG_PRESS_THRESHOLD = 500;
    private DBHelper databaseHelper;
    private String searchResult = "";
    private String normalText = "";
    private String convertCandidate = "";
    private int mushiThreshold = 1;
    private int charNumAfterFound = 0;
    private int mushiStartIndex = 0;
    private boolean FOUND = false;
    private List<CharacterPosition> characterPositions = new ArrayList<>();
    public GlobalVariable.HougenInformation hougenInformation = new GlobalVariable.HougenInformation("", "", "", "", "", "");;

    // Custom class to store word and its position
    public class CharacterPosition {
        char character;
        int position;

        CharacterPosition(char character, int position) {
            this.character = character;
            this.position = position;
        }
    }



    @Override
    public void onCreate() {
        super.onCreate();
        databaseHelper = new DBHelper(this);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        // Configure the AccessibilityServiceInfo to listen for text changes
        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        serviceInfo.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        serviceInfo.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        serviceInfo.packageNames = null; // Listen to text changes in all apps

        createFloatingButton();

        setServiceInfo(serviceInfo);
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();

            if (source != null && source.isEditable()) {
                // Get the current text from the editable field
                CharSequence currentText = event.getText() != null && !event.getText().isEmpty() ? event.getText().get(0) : null;
                int positionStart = event.getFromIndex();
                int positionEnd = positionStart + event.getAddedCount();

                if (currentText != null) {
                    String fullText = currentText.toString();
                    Log.d("composingText", "Full Text: " + fullText);
                    Log.d("composingText", "Full Text Position: " + positionStart + " to " + positionEnd);

                    // Show suggestions for the new composing text
                    showDialectSuggestions(fullText);

                    if (positionStart == positionEnd) {
                        Log.d("composingText", "no text");
                        resetFloatingButtonText();
                    }
                }
            }

        }
    }


    private void showDialectSuggestions(String fullText) {
        String TAG = "showDialectSuggestions";
        int startIndex = 0;
        int endIndex = 0;
        int consecutiveNonMatchingChars = 0;

        while (endIndex < fullText.length()) {
            String queryText = fullText.substring(startIndex, endIndex + 1);
            Cursor cursor = databaseHelper.searchWord(queryText);


            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                int hougenColumnIndex = cursor.getColumnIndex("hougen");
                int prefColumnIndex = cursor.getColumnIndex("pref");
                int areaColumnIndex = cursor.getColumnIndex("area");
                int defColumnIndex = cursor.getColumnIndex("def");
                int exampleColumnIndex = cursor.getColumnIndex("example");

                if (hougenColumnIndex != -1) {
                    String hougen = cursor.getString(hougenColumnIndex);


                    if (hougen != null && hougen.equals(queryText)) {
                        // Exact match found
                        searchResult = hougen;
                        Log.d(TAG, "Found exact match in database: " + searchResult + " at " + startIndex + "-" + endIndex);

                        hougenInformation.hougen = searchResult;
                        hougenInformation.hougenchihou = "飛騨弁";
                        hougenInformation.pref = cursor.getString(prefColumnIndex);
                        hougenInformation.area = cursor.getString(areaColumnIndex);
                        hougenInformation.def = cursor.getString(defColumnIndex);
                        hougenInformation.example = cursor.getString(exampleColumnIndex);
                        Log.d(TAG, "hougenInformation: " + hougenInformation.hougen + ", " + hougenInformation.pref + ", " + hougenInformation.area + ", " + hougenInformation.def + ", " + hougenInformation.example);

                        characterPositions.clear();
                        int position = startIndex;
                        for (char c : searchResult.toCharArray()) {
                            characterPositions.add(new CharacterPosition(c, position));
                            position++;
                        }

                        seperateNormalText(fullText, startIndex, endIndex);
                        Log.d(TAG, "characterPosition" + characterPositions);
                    } else {
                        Log.d(TAG, "Found partial match in database");
                    }
                }

                // Check the next character
                if (endIndex + 1 < fullText.length()) {
                    String nextQueryText = fullText.substring(startIndex, endIndex + 2);
                    Log.d(TAG, "nextQueryText: " + nextQueryText);
                    Cursor nextCursor = databaseHelper.searchWord(nextQueryText);

                    if (nextCursor == null || nextCursor.getCount() == 0) {
                        // Next character doesn't form a valid prefix, move start index
                        startIndex = endIndex + 1;
                        endIndex = startIndex;
                        searchResult = "";
                        consecutiveNonMatchingChars = 0;
                    } else {
                        // Both current and next character form a valid prefix
                        consecutiveNonMatchingChars = 0;
                        endIndex++;
                    }

                    if (nextCursor != null) {
                        nextCursor.close();
                    }
                } else {
                    // At the last character, we've already checked for exact match
                    break;
                }
            } else {
                // No match found
                if (endIndex == startIndex) {
                    // Move to next character if no match at current position
                    startIndex++;
                    endIndex++;
                } else {
                    // No match for longer string, keep last matching result
                    consecutiveNonMatchingChars++;
                    if (consecutiveNonMatchingChars > 2) {
                        // User has typed 2 chars after last match, stop searching
                        break;
                    }
                    endIndex++;
                }
            }

            if (cursor != null) {
                cursor.close();
            }
        }

        if (!searchResult.isEmpty() && !searchResult.equals("な") && !fullText.isEmpty()) {
            updateFloatingButtonText();
        } else {
            resetFloatingButtonText();
        }

        Log.d(TAG, "Final result: " + (searchResult.isEmpty() ? "な" : searchResult));
    }

    private void updateFloatingButtonText() {
        textView.setText(searchResult);
        hougenchihouTextView.setText(hougenInformation.hougenchihou);
        hougenchihouTextView.setVisibility(View.VISIBLE);
    }

    private void resetFloatingButtonText() {
        textView.setText("な");
        hougenchihouTextView.setText("");
        hougenchihouTextView.setVisibility(View.GONE);
        hougenInformation = new GlobalVariable.HougenInformation("", "", "", "", "", "");
        hougenInformation.hougenchihou = "";
    }

    //this method can search the text that cannot be converted from fullText
    private void seperateNormalText(String fullText, int startIndex, int endIndex) {
        int position = 0;
        Log.d("array", "characterPosition_before" + characterPositions);

        for (char f : fullText.toCharArray()) {
            Log.d("DebugLoop", "Iteration: " + position + ", Char: " + f);
            if (position < startIndex || position > endIndex) {
                characterPositions.add(new CharacterPosition(f, position));
                Log.d("CharacterPositions", "Added: " + f + " at position " + position);
                normalText += f;
            }
            position++;
        }
        Log.d("normalText", "The text that cannot be converted: " + normalText);
        Log.d("array", "characterPosition_after" + characterPositions);
    }

    // Call this method when the user performs conversion
    private void onConvertText() {
        // Sort character positions by their start index
        if (characterPositions != null && !characterPositions.isEmpty()) {
            Collections.sort(characterPositions, new Comparator<CharacterPosition>() {
                @Override
                public int compare(CharacterPosition cp1, CharacterPosition cp2) {
                    return Integer.compare(cp1.position, cp2.position);
                }
            });
        }

        StringBuilder combinedWord = new StringBuilder();

        for (CharacterPosition cp : characterPositions) {
            combinedWord.append(cp.character);
        }

        convertedText = combinedWord.toString();

        Log.d("onConvertText", "Converted word: " + convertedText);

        // Clear the editable text and set it to the converted text
        // Assuming you have a method to clear and set the text
        setEditableText(convertedText);
        searchResult = "";
        convertedText = "";
        characterPositions.clear();
    }

    private void setEditableText(String text) {
        AccessibilityNodeInfo source = getRootInActiveWindow();
        if (source != null) {
            // Find the currently focused node
            AccessibilityNodeInfo focusedNode = source.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focusedNode != null && focusedNode.isEditable()) {
                // Set the new text (this may replace the entire content of the input field)
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            }
        }
    }



    private boolean isHiragana(char ch) {
        return (ch >= '\u3040' && ch <= '\u309F');
    }

    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the floating button layout
        floatingButton = LayoutInflater.from(this).inflate(R.layout.accessibility_button_layout, null);
        textView = floatingButton.findViewById(R.id.accessibility_text);
        hougenchihouTextView = floatingButton.findViewById(R.id.hougenchihou);


        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 50;
        params.y = 300;
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
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                        // If the movement is beyond a certain threshold, cancel the long press
                        if (distance > 10) { // You can adjust this threshold as needed
                            v.removeCallbacks(longPressRunnable);
                        }

                        params.x = initialX + (int) deltaX;
                        params.y = initialY + (int) deltaY;
                        windowManager.updateViewLayout(floatingButton, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.removeCallbacks(longPressRunnable);
                        long touchDuration = System.currentTimeMillis() - touchStartTime;
                        if (!isLongPress && touchDuration < LONG_PRESS_THRESHOLD) {
                            handleButtonClick();
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
                if (!searchResult.equals("")) {
                    Log.d("handleButtonClick", "Setting composing text to " + convertedText);

                    // Attempt to modify the text in the focused edit field
                    AccessibilityNodeInfo focusedNode = getRootInActiveWindow().findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                    if (focusedNode != null && focusedNode.isEditable()) {
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, convertedText);
                        focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                        onConvertText();
                        focusedNode.recycle();
                        composingText = "";
                        Log.d("ComposingText", "Resetting composingText " + composingText);
                        resetFloatingButtonText();
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

    public void updateFloatingPopupVisibility() {
        if (GlobalVariable.isMainActivityRunning) {
            hideFloatingPopup(); // Your method to hide the popup
        } else {
            showFloatingPopup(); // Your method to show the popup
        }
    }

    public void hideFloatingPopup() {
        if (floatingButton != null && floatingButton.getVisibility() != View.GONE) {
            floatingButton.setVisibility(View.GONE); // Hide the popup view
        }
    }

    public void showFloatingPopup() {
        if (floatingButton != null && floatingButton.getVisibility() != View.VISIBLE) {
            windowManager.addView(floatingButton, new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            ));
        }
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("hougen", hougenInformation.hougen);
        intent.putExtra("hougenchihou", hougenInformation.hougenchihou);
        intent.putExtra("pref", hougenInformation.pref);
        intent.putExtra("area", hougenInformation.area);
        intent.putExtra("def", hougenInformation.def);
        intent.putExtra("example", hougenInformation.example);
        startActivity(intent);
    }

}