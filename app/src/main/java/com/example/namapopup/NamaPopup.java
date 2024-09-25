package com.example.namapopup;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NamaPopup extends AccessibilityService {
    private WindowManager windowManager;
    private View floatingButton;
    private TextView textView;
    private TextView chihouTextView;
    CharSequence composingText;
    private String convertedText = "";
    private static final long LONG_PRESS_THRESHOLD = 500;
    private DBHelper databaseHelper;
    private String searchResult = "";
    private List<List<String>> searchResults = new ArrayList<>();
    private String normalText = "";
    private boolean textViewSet = false;
    private List<CharacterPosition> characterPositions = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    public GlobalVariable.HougenInformation hougenInformation = new GlobalVariable.HougenInformation("", "", "", "", "", "", "");;
    private int currentResultIndex = 0; // Tracks which list within searchResults we are currently in
    private int currentItemIndex = 0;   // Tracks the item within the current list


    @Override
    public void onCreate() {
        super.onCreate();
        databaseHelper = new DBHelper(this);
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        for(String chihou : Constants.CHIHOUS) {
            Log.d("ONCREATE", "Prefs of " + chihou + " = " + sharedPreferences.getBoolean(chihou, false));
        }

        Log.d("ONCREATE", "Non-native mode = " + sharedPreferences.getBoolean(Constants.NON_NATIVE_MODE, false));

        // Check for the SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            // Permission is granted, create the floating button
            createFloatingButton();
        }
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

    // Custom class to store word and its position
    public class CharacterPosition {
        char character;
        int position;

        CharacterPosition(char character, int position) {
            this.character = character;
            this.position = position;
        }
    }

    private void showDialectSuggestions(String fullText) {
        String TAG = "showDialectSuggestions";
        int startIndex = 0;
        int endIndex = 0;
        int consecutiveNonMatchingChars = 0;

        // List to hold results grouped by chihou (region)
        searchResults = new ArrayList<>();

        while (endIndex < fullText.length()) {
            // Query for substring from startIndex to endIndex
            String queryText = fullText.substring(startIndex, endIndex + 1);
            Cursor[] cursors = databaseHelper.searchWord(queryText);

            boolean matchFound = false;
            List<String> currentChihouResults = new ArrayList<>(); // To hold results for the current `chihou`

            // Iterate over each cursor (one per chihou/region)
            for (int i = 0; i < cursors.length; i++) {
                Cursor cursor = cursors[i];
                if (cursor != null) {
                    Log.d("cursor", "Query: " + queryText + " Cursor: " + cursor.getCount());
                }

                if (cursor != null && cursor.getCount() > 0) {

                    // Move to the first valid row
                    if (cursor.moveToFirst()) {
                        // Get column indexes from the result
                        int hougenColumnIndex = cursor.getColumnIndex("hougen");
                        int triggerColumnIndex = cursor.getColumnIndex("trigger");
                        int prefColumnIndex = cursor.getColumnIndex("pref");
                        int areaColumnIndex = cursor.getColumnIndex("area");
                        int defColumnIndex = cursor.getColumnIndex("def");
                        int exampleColumnIndex = cursor.getColumnIndex("example");
                        Log.d("hougenColumnIndex", "hougenColumnIndex: " + hougenColumnIndex);
                        Log.d("triggerColumnIndex", "triggerColumnIndex: " + triggerColumnIndex);

                        // Check for an exact match of dialect word (hougen & trigger)
                        if (hougenColumnIndex != -1) {
                            String hougen = cursor.getString(hougenColumnIndex);
                            String trigger = (triggerColumnIndex != -1) ? cursor.getString(triggerColumnIndex) : "";

                            Log.d("hougenColumnIndex", "hougen: " + hougen + " Query: " + queryText + "trigger: " + trigger);

                            if (hougen != null && (hougen.equals(queryText) || trigger.equals(queryText))) {
                                // Exact match found, store result and update relevant info
                                currentChihouResults.add(hougen); // Add match to the list for the current `chihou`
                                Log.d(TAG, "Found exact match: " + hougen + " at " + startIndex + "-" + endIndex);

                                // Populate hougenInformation with data from cursor
                                hougenInformation.hougen = hougen;
                                hougenInformation.chihou = Constants.CHIHOUS_JP[i]; // Region from the cursor
                                hougenInformation.pref = "";
                                hougenInformation.area = "";
                                hougenInformation.def = cursor.getString(defColumnIndex);
                                hougenInformation.example = cursor.getString(exampleColumnIndex);

                                // Log hougen information
                                Log.d(TAG, "hougenInformation: " + hougenInformation.hougen + ", "
                                        + hougenInformation.pref + ", "
                                        + hougenInformation.area + ", "
                                        + hougenInformation.def + ", "
                                        + hougenInformation.example);

                                // Clear and update character positions for this result
                                characterPositions.clear();
                                int position = startIndex;
                                for (char c : hougen.toCharArray()) {
                                    characterPositions.add(new CharacterPosition(c, position));
                                    position++;
                                }

                                // Separate normal text outside of match
                                seperateNormalText(fullText, startIndex, endIndex);
                                Log.d(TAG, "characterPosition: " + characterPositions);

                                matchFound = true; // Mark as match found
                            }
                        }
                    }
                }

                if (cursor != null) {
                    cursor.close(); // Close cursor after use
                }
            }

            // Add all matches from the current `chihou` (if any) to the searchResults list
            if (!currentChihouResults.isEmpty()) {
                searchResults.add(currentChihouResults); // Add results for the current `chihou`
            }

            // Handle prefix-forming condition
            if (endIndex + 1 < fullText.length()) {
                String nextQueryText = fullText.substring(startIndex, endIndex + 2);
                Log.d(TAG, "nextQueryText: " + nextQueryText);
                Cursor[] nextCursors = databaseHelper.searchWord(nextQueryText);

                boolean nextPrefixValid = false;
                List<String> nextChihouResults = new ArrayList<>(); // To hold results for the next query

                for (int j = 0; j < nextCursors.length; j++) {
                    Cursor nextCursor = nextCursors[j];
                    if (nextCursor != null && nextCursor.getCount() > 0) {
                        nextPrefixValid = true;

                        // Move to the first row and handle exact match for next query
                        if (nextCursor.moveToFirst()) {
                            int nextHougenColumnIndex = nextCursor.getColumnIndex("hougen");
                            int nextTriggerColumnIndex = nextCursor.getColumnIndex("trigger");
                            int nextDefColumnIndex = nextCursor.getColumnIndex("def");
                            int nextExampleColumnIndex = nextCursor.getColumnIndex("example");

                            if (nextHougenColumnIndex != -1) {
                                String nextHougen = nextCursor.getString(nextHougenColumnIndex);
                                String nextTrigger = (nextTriggerColumnIndex != -1) ? nextCursor.getString(nextTriggerColumnIndex) : "";

                                if (nextHougen != null && (nextHougen.equals(nextQueryText) || nextTrigger.equals(nextQueryText))) {
                                    // Exact match found in next query
                                    nextChihouResults.add(nextHougen); // Add match to the next `chihou` results

                                    // Populate hougenInformation with next query match
                                    hougenInformation.hougen = nextHougen;
                                    hougenInformation.chihou = Constants.CHIHOUS_JP[j]; // Update this to reflect actual region if needed
                                    hougenInformation.pref = "";
                                    hougenInformation.area = "";
                                    hougenInformation.def = nextCursor.getString(nextDefColumnIndex);
                                    hougenInformation.example = nextCursor.getString(nextExampleColumnIndex);

                                    // Log next hougen information
                                    Log.d(TAG, "Found exact match in next query: " + nextHougen);

                                    // Update character positions
                                    characterPositions.clear();
                                    int position = startIndex;
                                    for (char c : nextHougen.toCharArray()) {
                                        characterPositions.add(new CharacterPosition(c, position));
                                        position++;
                                    }

                                    // Separate normal text for next match
                                    seperateNormalText(fullText, startIndex, endIndex + 1);
                                    Log.d(TAG, "characterPosition after next query match: " + characterPositions);

                                    matchFound = true; // Mark match found in the next query
                                }
                            }
                        }
                    }
                    if (nextCursor != null) {
                        nextCursor.close();
                    }
                }

                // Add next query results to searchResults
                if (!nextChihouResults.isEmpty()) {
                    searchResults.add(nextChihouResults);
                }

                if (!nextPrefixValid) {
                    // Next character doesn't form a valid prefix, move start index
                    startIndex = endIndex + 1;
                    endIndex = startIndex;
                    consecutiveNonMatchingChars = 0;
                } else {
                    // Both current and next character form a valid prefix
                    consecutiveNonMatchingChars = 0;
                    endIndex++;
                }
            } else {
                // At the last character, we've already checked for exact match
                break;
            }

            // If no match found after checking all cursors, increment indexes
            if (!matchFound) {
                if (endIndex == startIndex) {
                    startIndex++;
                    endIndex++;
                } else {
                    consecutiveNonMatchingChars++;
                    if (consecutiveNonMatchingChars > 2) {
                        // Stop searching after 2 non-matching chars
                        break;
                    }
                    endIndex++;
                }
            }
        }

        // Log the final searchResults structure
        Log.d(TAG, "Final searchResults: " + searchResults);

        // Update floating button text based on search results
        if (!searchResults.isEmpty()) {
            updateFloatingButtonText();
        } else {
            resetFloatingButtonText();
        }
    }





    private void updateFloatingButtonText() {
        textViewSet = true;
        String TAG = "updateFloatingButtonText";

        if (!searchResults.isEmpty() && currentResultIndex < searchResults.size()) {
            List<String> currentSublist = searchResults.get(currentResultIndex);

            if (!currentSublist.isEmpty() && currentItemIndex < currentSublist.size()) {
                String currentItem = currentSublist.get(currentItemIndex);
                textView.setText(currentItem);
                Log.d(TAG, "Showing: " + currentItem + " from searchResults[" + currentResultIndex + "][" + currentItemIndex + "]");

                // Update hougenInformation based on the current item
                updateHougenInformation(currentItem, currentResultIndex);
            } else {
                Log.d(TAG, "Inner list is empty");
            }
        } else {
            resetFloatingButtonText(); // Reset if out of bounds
        }

        // Set the current region (chihou)
        chihouTextView.setText(hougenInformation.chihou);
        chihouTextView.setVisibility(View.VISIBLE);
    }

    // Function to update the hougenInformation based on the current selected item and region index
    private void updateHougenInformation(String currentItem, int regionIndex) {
        // Set hougen and chihou information using the current item and region index
        hougenInformation.hougen = currentItem;
        hougenInformation.chihou = Constants.CHIHOUS_JP[regionIndex]; // Set the region (chihou)

        // Fetch more detailed information if available
        Cursor[] cursors = databaseHelper.searchWord(currentItem);  // Use the currentItem to get the relevant data

        if (cursors != null && cursors.length > 0) {
            for (Cursor cursor : cursors) {
                if (cursor != null && cursor.moveToFirst()) {
                    int hougenColumnIndex = cursor.getColumnIndex("hougen");
                    int defColumnIndex = cursor.getColumnIndex("def");
                    int exampleColumnIndex = cursor.getColumnIndex("example");

                    if (hougenColumnIndex != -1) {
                        hougenInformation.hougen = cursor.getString(hougenColumnIndex);
                    }
                    if (defColumnIndex != -1) {
                        hougenInformation.def = cursor.getString(defColumnIndex);
                    }
                    if (exampleColumnIndex != -1) {
                        hougenInformation.example = cursor.getString(exampleColumnIndex);
                    }

                    Log.d("hougenInformation", "Updated hougenInformation: " + hougenInformation.hougen + ", "
                            + hougenInformation.chihou + ", "
                            + hougenInformation.def + ", "
                            + hougenInformation.example);
                }

                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void resetFloatingButtonText() {
        textViewSet = false;
        textView.setText("な");
        chihouTextView.setText("");
        chihouTextView.setVisibility(View.GONE);
        hougenInformation = new GlobalVariable.HougenInformation("","", "", "", "", "", "");
        hougenInformation.chihou = "";
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
        if (!Settings.canDrawOverlays(this)) {
            Log.e("NamaPopup", "Permission to draw over other apps is not granted");
            return;
        }

        // Check if the floating button is already created
        if (floatingButton != null && floatingButton.isShown()) {
            Log.d("NamaPopup", "Floating button is already displayed");
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the floating button layout
        floatingButton = LayoutInflater.from(this).inflate(R.layout.accessibility_button_layout, null);
        textView = floatingButton.findViewById(R.id.accessibility_text);
        chihouTextView = floatingButton.findViewById(R.id.chihou);


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
        // Modify the touch listener within the `createFloatingButton()` method
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
                        if (textViewSet) v.postDelayed(longPressRunnable, LONG_PRESS_THRESHOLD);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                        // Detect swipe direction if textViewSet is true
                        if (textViewSet) {
                            // Swiping left
                            if (deltaX < -50 && Math.abs(deltaY) < 50) {
                                handleSwipeLeft();
                                return true; // Stop further event processing
                            }
                            // Swiping right
                            else if (deltaX > 50 && Math.abs(deltaY) < 50) {
                                handleSwipeRight();
                                return true; // Stop further event processing
                            }
                        }

                        // If textViewSet is false, allow moving the overlay
                        if (!textViewSet && distance > 10) { // Movement threshold
                            params.x = initialX + (int) deltaX;
                            params.y = initialY + (int) deltaY;
                            windowManager.updateViewLayout(floatingButton, params);
                        }
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
                    launchShousaiActivity();
                }
            };

            // Handle swipe left to move to the next item
            private void handleSwipeLeft() {
                String TAG = "handleSwipeLeft";
                Log.d(TAG, "Swiped left");

                if (!searchResults.isEmpty() && currentResultIndex < searchResults.size()) {
                    if (currentItemIndex < searchResults.get(currentResultIndex).size() - 1) {
                        // Move to the next item in the current sublist
                        currentItemIndex++;
                    } else if (currentResultIndex < searchResults.size() - 1) {
                        // Move to the next sublist if available
                        currentResultIndex++;
                        currentItemIndex = 0; // Reset item index for the new sublist
                    }
                }

                // Update the floating button with the new text and hougenInformation
                updateFloatingButtonText();
            }

            // Handle swipe right to move to the previous item
            private void handleSwipeRight() {
                String TAG = "handleSwipeRight";
                Log.d(TAG, "Swiped right");

                if (!searchResults.isEmpty()) {
                    if (currentItemIndex > 0) {
                        // Move to the previous item in the current sublist
                        currentItemIndex--;
                    } else if (currentResultIndex > 0) {
                        // Move to the previous sublist if available
                        currentResultIndex--;
                        currentItemIndex = searchResults.get(currentResultIndex).size() - 1; // Set to last item in the previous sublist
                    }
                }

                // Update the floating button with the new text and hougenInformation
                updateFloatingButtonText();
            }

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
        try {
            windowManager.addView(floatingButton, params);
        } catch (Exception e) {
            Log.e("NamaPopup", "Error adding view to WindowManager", e);
        }
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

    private void launchShousaiActivity() {
        Intent intent = new Intent(this, HougenInfoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("hougen", hougenInformation.hougen);
        intent.putExtra("chihou", hougenInformation.chihou);
        intent.putExtra("pref", hougenInformation.pref);
        intent.putExtra("area", hougenInformation.area);
        intent.putExtra("def", hougenInformation.def);
        intent.putExtra("example", hougenInformation.example);
        startService(intent);
    }

}