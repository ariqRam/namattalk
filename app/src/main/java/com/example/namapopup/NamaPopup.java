package com.example.namapopup;

import static com.example.namapopup.Helper.getDialectState;
import static com.example.namapopup.Helper.isNonNativeMode;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class NamaPopup extends AccessibilityService {
    private WindowManager windowManager;
    private View floatingButton;
    private TextView indicatorTextView;
    private TextView textView;
    private TextView chihouTextView;
    private String convertedText = "";
    private static final long LONG_PRESS_THRESHOLD = 500;
    private DBHelper databaseHelper;
    private List<GlobalVariable.HougenInformation> searchResults = new ArrayList<>();
    private GlobalVariable.HougenInformation currentHougenInformation = new GlobalVariable.HougenInformation("", "", "", "", "", "", "");
    private String normalText = "";
    private boolean textViewSet = false;
    private List<CharacterPosition> characterPositions = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    public GlobalVariable.HougenInformation hougenInformation = new GlobalVariable.HougenInformation("", "", "", "", "", "", "");
    private int currentResultIndex = 0; // Tracks which list within searchResults we are currently in
    private WindowManager.LayoutParams params;
    private String indicator = "";
    private VerbConjugator verbConjugator;
    private HashMap<String, List<String>> verbMap;


    @Override
    public void onCreate() {
        super.onCreate();
        databaseHelper = new DBHelper(this);
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        Log.d("ONCREATE", "Non-native mode = " + sharedPreferences.getBoolean(Constants.NON_NATIVE_MODE, false));

        //initialize verbConjugator
        verbConjugator = new VerbConjugator(this);
        verbMap = verbConjugator.getVerbs();

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
                    if (positionStart == positionEnd) {
                        Log.d("composingText", "no text");
                        resetFloatingButtonText();
                    } else {
                        // Show suggestions for the new composing text
                        showDialectSuggestions(fullText);
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

        // List to hold results grouped by chihou (region)
        searchResults = new ArrayList<>();

        // Scan through the input text
        while (startIndex < fullText.length()) {
            boolean matchFound = false;
            endIndex = startIndex; // Reset endIndex for each new startIndex

            while (endIndex < fullText.length()) {
                String baseText = fullText.substring(startIndex, endIndex + 1);
                String queryText = verbConjugator.reconjugate(baseText, verbMap);
                Log.d(TAG, "reconjugated queryText: " + queryText);
                Cursor[] cursors = databaseHelper.searchWord(queryText);


                // Iterate over each region
                for (int i = 0; i < cursors.length; i++) {
                    Cursor cursor = cursors[i];
//
                    DialectState dialectState = getDialectState(this, Constants.CHIHOUS[i]);
                    if (cursor != null && cursor.getCount() > 0) {
                        if (cursor.moveToFirst()) {
                            int hougenColumnIndex = cursor.getColumnIndex("hougen");
                            int triggerColumnIndex = cursor.getColumnIndex("trigger");

                            // Check for dialect word match
                            String hougen = cursor.getString(hougenColumnIndex);
                            String triggers = (triggerColumnIndex != -1) && isNonNativeMode(dialectState) ? cursor.getString(triggerColumnIndex) : "";
                            String[] splitTriggers = triggers.split("、");
                            boolean isExactMatch = hougen.equals(queryText) || Arrays.asList(splitTriggers).contains(queryText);

                            if (isExactMatch) {
                                Log.d(TAG, "Exact match found: " + hougen);
                                Log.d(TAG, "endIndex: " + endIndex + " fulltextLength: " + (fullText.length() -1));
                                hougen = verbConjugator.conjugate(hougen, VerbConjugator.getVerbForm(baseText), verbMap);
                                Log.d(TAG, "Conjugated hougen: " + hougen + " baseText: " + VerbConjugator.getVerbForm(baseText));

                                // Add to character positions, update relevant information
                                updateHougenInformation(cursor, i, hougen);
                                Log.d("updateHougenInfo", "QUERYTEXT:" + queryText);

                                // After scanning the entire substring from startIndex
                                addCurrentResultToSearchResults(hougenInformation);

                                addCharacterPositions(hougen, startIndex);
//                                separateNormalText(fullText, startIndex, endIndex);

                                if (endIndex == fullText.length() - 1) {
                                    matchFound = true;
                                    Log.d(TAG, "match founded!");
                                    break; // Stop scanning if exact match found
                                }
                            }
                        }
                    }
                    if (cursor != null) cursor.close();
                }

                if (matchFound) {
                    Log.d(TAG, "exit the endIndex loop");
                    break; // Stop expanding endIndex once a match is found
                }
                endIndex++;
            }

            if (matchFound) break;
            // Move startIndex to the next position
            startIndex++;
        }

        Log.d(TAG, "showAllResults: " + searchResults);
        // Update UI with final results
        updateSuggestionsUI();
    }

    private boolean isConjugationExist(Cursor cursor) {
        if(cursor.getCount() <= 0 && cursor != null) return false;
        int posIdx = cursor.getColumnIndex("pos"); // CURRENLTY cursor is NULL
        String pos = cursor.getString(posIdx);
        return ("動詞".equals(pos) && cursor.getCount() > 0);
    }


    private void addCurrentResultToSearchResults(GlobalVariable.HougenInformation hougenInformation) {
        // Add all matches from the current chihou (if any) to the searchResults list
        if (hougenInformation != null && !searchResults.contains(hougenInformation)) {
            searchResults.add(hougenInformation); // Add results for the current chihou
        }
    }

    private void updateSuggestionsUI() {
        if (!searchResults.isEmpty()) {
            // Update floating button text or suggestions UI based on search results
            updateFloatingButtonText(); // Update with the collected results
        } else {
            // Reset the suggestions UI if no results were found
            resetFloatingButtonText();
        }
    }


    private void addCharacterPositions(String matchedWord, int startIndex) {
        characterPositions.clear(); // Clear existing character positions before adding new ones
        int position = startIndex;

        // Populate characterPositions with the character and its position
        for (char c : matchedWord.toCharArray()) {
            characterPositions.add(new CharacterPosition(c, position));
            position++;
        }
    }



    private void updateIndicator() {
        indicator = (currentResultIndex + 1) + "/" + searchResults.size();
        indicatorTextView.setText(indicator);
    }

    private void updateFloatingButtonText() {
        textViewSet = true;
        String TAG = "updateFloatingButtonText";

        if (!searchResults.isEmpty() && currentResultIndex <= searchResults.size()) {
            currentHougenInformation = searchResults.get(currentResultIndex);

            if (currentHougenInformation != null) {
                if (currentHougenInformation.hougen != null) {
                    textView.setText(currentHougenInformation.hougen);
                } else {
                    textView.setText("な");
                }

                chihouTextView.setText(currentHougenInformation.chihou);
                Log.d(TAG, "Showing: " + currentHougenInformation.hougen + " from searchResults[" + currentResultIndex + "]");

                if (indicatorTextView == null) {
                    createIndicator(indicator);
                    Log.d(TAG, "Created indicator");
                } else {
                    updateIndicator();
                    Log.d(TAG, "Updated indicator");

                }


            } else {
                Log.d(TAG, "Inner list is empty");
            }
        } else {
            resetFloatingButtonText(); // Reset if out of bounds
        }

        // Set the current region (chihou)
        chihouTextView.setVisibility(View.VISIBLE);
    }

    // Function to update the hougenInformation based on the current selected item and region index
    private void updateHougenInformation(Cursor cursor, int regionIndex, String hougen) {
        int defColumnIndex = cursor.getColumnIndex("def");
        int exampleColumnIndex = cursor.getColumnIndex("example");

        hougenInformation.hougen = hougen;
        hougenInformation.chihou = Constants.CHIHOUS_JP[regionIndex];// Region from the cursor
        hougenInformation.pref = Constants.PREFS[regionIndex];
        hougenInformation.area = Constants.AREAS[regionIndex];
        hougenInformation.def = cursor.getString(defColumnIndex);
        hougenInformation.example = cursor.getString(exampleColumnIndex);
    }

    private void resetFloatingButtonText() {
        textViewSet = false;
        textView.setText("な");
        chihouTextView.setText("");
        chihouTextView.setVisibility(View.GONE);
        characterPositions.clear();
        hougenInformation = new GlobalVariable.HougenInformation("","", "", "", "", "", "");
        hougenInformation.chihou = "";
        if (indicatorTextView != null) {
            windowManager.removeView(indicatorTextView);
            indicatorTextView = null;
        }
    }


    //this method can search the text that cannot be converted from fullText
    private void separateNormalText(String fullText, int startIndex, int endIndex) {
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


        params = new WindowManager.LayoutParams(
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
                        if (textViewSet && searchResults.size() > 1) {
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
                    //implement left swiping animation
                    floatingButton.animate()
                            .translationXBy(-800)
                            .alpha(0)
                            .setDuration(150)
                            .withEndAction(new Runnable() {
                                   @Override
                                   public void run() {
                                       floatingButton.setTranslationX(800); // Move view to the right of the screen
                                       floatingButton.animate()
                                               .translationX(0)  // Slide the new view into position
                                               .alpha(1)         // Fade in the new view
                                               .setDuration(200)
                                               .start();
                                   }
                            })
                            .start();


                    if (currentResultIndex < searchResults.size() - 1) {
                        // Move to the next sublist if available
                        currentResultIndex++;
                        updateIndicator();
                        Log.d("swipeLeft", "Update indicatorTextView to " + indicator);
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
                    //implement right swiping animation
                    floatingButton.animate()
                            .translationXBy(800) // Move the view out of screen to the right
                            .alpha(0)             // Fade out the current view
                            .setDuration(100)     // Duration of the animation
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    floatingButton.setTranslationX(-800); // Move view to the left of the screen
                                    floatingButton.animate()
                                            .translationX(0)  // Slide the new view into position
                                            .alpha(1)         // Fade in the new view
                                            .setDuration(150)
                                            .start();
                                }
                            })
                            .start();

                    if (currentResultIndex > 0) {
                        // Move to the previous sublist if available
                        currentResultIndex--;
                        updateIndicator();
                        Log.d("swipeRight", "Update indicatorTextView to " + indicator);
                    }
                }

                // Update the floating button with the new text and hougenInformation
                updateFloatingButtonText();
            }

            private void handleButtonClick() {
                Log.d("handleButtonClick", "Button clicked");
                if (!searchResults.get(currentResultIndex).equals("")) {
                    Log.d("handleButtonClick", "Setting composing text to " + convertedText);

                    // Attempt to modify the text in the focused edit field
                    AccessibilityNodeInfo focusedNode = getRootInActiveWindow().findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                    if (focusedNode != null && focusedNode.isEditable()) {
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, convertedText);
                        focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                        onConvertText();
                        focusedNode.recycle();
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

    private void createIndicator(String indicator) {
        // Check if indicatorTextView is already created
        if (indicatorTextView != null && indicatorTextView.isShown()) {
            Log.d("NamaPopup", "indicatorTextView is already displayed");
            return;
        }

        //create indicator textView
        indicatorTextView = new TextView(this);
        indicatorTextView.setTextSize(14);
        indicatorTextView.setTextColor(Color.BLACK);
        indicatorTextView.setPadding(5, 5, 5, 5);

        updateIndicator();  //update indicatorTextView with current resultsIndex value

        //set layout parameters for indicatorTextView
        WindowManager.LayoutParams indicatorParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // Position the new TextView below the existing floating layout
        int indicatorOffset = floatingButton.getHeight() * 23 / 10;

        indicatorParams.gravity = Gravity.TOP | Gravity.LEFT;
        indicatorParams.x = params.x; // Keep same X position
        indicatorParams.y = params.y + indicatorOffset; // Position below the floating layout

        // Add the new TextView to the WindowManager
        windowManager.addView(indicatorTextView, indicatorParams);
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
        intent.putExtra("hougen", verbConjugator.reconjugate(hougenInformation.hougen, verbMap));
        intent.putExtra("chihou", hougenInformation.chihou);
        intent.putExtra("pref", hougenInformation.pref);
        intent.putExtra("area", hougenInformation.area);
        intent.putExtra("def", hougenInformation.def);
        intent.putExtra("example", hougenInformation.example);
        startService(intent);
    }

}