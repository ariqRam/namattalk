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
import android.widget.ImageView;
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
    private ImageView logoView;
    private TextView chihouTextView;
    private String convertedText = "";
    private static final long LONG_PRESS_THRESHOLD = 500;
    private DBHelper databaseHelper;
    private List<GlobalVariable.HougenInformation> searchResults = new ArrayList<>();
    private GlobalVariable.HougenInformation currentHougenInformation = new GlobalVariable.HougenInformation("", "", "", "", "", "", "", new ArrayList<>(), "");
    private String normalText = "";
    private boolean textViewSet = false;
    private int currentResultIndex = 0; // Tracks which list within searchResults we are currently in
    private WindowManager.LayoutParams params;
    private String indicator = "";
    private VerbConjugator verbConjugator;
    private HashMap<String, List<String>> verbMap;

    @Override
    public void onCreate() {
        super.onCreate();
        databaseHelper = new DBHelper(this);

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

        // Configure the AccessibilityServiceInfo to listen for various events
        AccessibilityServiceInfo serviceInfo = getServiceInfo();

        // Listen to more events than just text changes
        serviceInfo.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;

        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        // Include views that are not important for accessibility (e.g., certain input fields)
        serviceInfo.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        // Listen to events in all apps
        serviceInfo.packageNames = null;

        // Set service info to make it active
        setServiceInfo(serviceInfo);

        // Optionally create any floating UI you need
        createFloatingButton();
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        AccessibilityNodeInfo currentNode;

        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                AccessibilityNodeInfo source = event.getSource();
                if (source != null && source.isEditable()) {
                    // Get the current text from the editable field
                    CharSequence currentText = event.getText() != null && !event.getText().isEmpty() ? event.getText().get(0) : null;
                    int positionStart = event.getFromIndex() - 1;
                    int positionEnd = positionStart + event.getAddedCount();

                    if (currentText != null) {
                        String fullText = currentText.toString();
                        Log.d("composingText", "Full Text: " + fullText);
                        if (positionStart == positionEnd) {
                            Log.d("composingText", "No text in textField");
                            resetFloatingButtonText();
                        } else {
                            // Show suggestions for the new composing text
                            showDialectSuggestions(fullText);
                        }
                    }
                }
                break;


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
                String selectedText = fullText.substring(startIndex, endIndex + 1);
                String queryText = verbConjugator.reconjugate(selectedText, verbMap);
                Log.d(TAG, "reconjugated selectedText to: " + queryText);
                Cursor[] cursors = databaseHelper.searchWord(queryText);


                // Iterate over each region
                for (int i = 0; i < cursors.length; i++) {
                    Cursor cursor = cursors[i];
                    List<CharacterPosition> characterPositions = new ArrayList<>();
                    normalText = "";

                    DialectState dialectState = getDialectState(this, Constants.CHIHOUS[i]);
                    if (cursor != null && cursor.getCount() > 0) {
                        if (cursor.moveToFirst()) {
                            int hougenColumnIndex = cursor.getColumnIndex("hougen");
                            int triggerColumnIndex = cursor.getColumnIndex("trigger");

                            // Check for dialect word match
                            String hougen = cursor.getString(hougenColumnIndex);
                            GlobalVariable.HougenInformation hougenInformation = new GlobalVariable.HougenInformation("", "", "", "", "", "", "", new ArrayList<>(), "");
                            String triggers = (triggerColumnIndex != -1) && isNonNativeMode(dialectState) ? cursor.getString(triggerColumnIndex) : "";
                            String[] splitTriggers = triggers.split("、");
                            boolean isExactMatch = hougen.equals(queryText) || Arrays.asList(splitTriggers).contains(queryText);

                            if (isExactMatch && endIndex == fullText.length() - 1) {
                                matchFound = true;
                                Log.d(TAG, "Exact match found: " + hougen + " in " + Constants.CHIHOUS[i] + " at position: " + startIndex + " to " + endIndex);
                                String conjugatedHougen = verbConjugator.conjugate(hougen, VerbConjugator.getVerbForm(selectedText), verbMap);
                                Log.d(TAG, "Conjugated hougen: " + hougen + " in form of: " + VerbConjugator.getVerbForm(selectedText) + " to " + conjugatedHougen);

                                // Add to character positions, update relevant information
                                updateHougenInformation(cursor, i, conjugatedHougen, hougenInformation, characterPositions);
                                // After scanning the entire substring from startIndex
                                addCurrentResultToSearchResults(hougenInformation);
                                addCharacterPositions(hougenInformation.characterPositions, conjugatedHougen, startIndex);
                                separateNormalText(hougenInformation.characterPositions, fullText, startIndex, endIndex);
                            }

                        }
                    }
                    if (cursor != null) cursor.close();
                }

                if (matchFound) {
                    break; // Stop expanding endIndex once a match is found
                }
                endIndex++;
                Log.d(TAG, "move endIndex to " + endIndex);
            }

            if (matchFound) {
                break;
            }
            // Move startIndex to the next position
            startIndex++;
            Log.d(TAG, "move startIndex to " + endIndex);
        }

        Log.d(TAG, "numbers of result: " + searchResults.size());
        // Update UI with final results
        updateSuggestionsUI();
    }

    private void addCurrentResultToSearchResults(GlobalVariable.HougenInformation hougenInformation) {
        // Add all matches from the current chihou (if any) to the searchResults list
        if (hougenInformation != null) {
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


    private void addCharacterPositions(List<CharacterPosition> characterPositions, String matchedWord, int startIndex) {
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
                    setOverlayActive();
                    textView.setText(currentHougenInformation.hougen);
                } else {
                    setOverlayIdle();
                }

                chihouTextView.setText(currentHougenInformation.chihou);

                if (indicatorTextView == null) {
                    createIndicator(indicator);
                } else {
                    updateIndicator();
                }

                Log.d(TAG, "Update FloatingButtonText to " + currentHougenInformation.hougen + " in " + currentHougenInformation.chihou + " (" + indicator + ")");


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
    private void updateHougenInformation(Cursor cursor, int regionIndex, String hougen, GlobalVariable.HougenInformation hougenInformation, List<CharacterPosition> characterPositions) {
        int defColumnIndex = cursor.getColumnIndex("def");
        int exampleColumnIndex = cursor.getColumnIndex("example");
        int posColumnIndex = cursor.getColumnIndex("pos");

        hougenInformation.hougen = hougen;
        hougenInformation.chihou = Constants.CHIHOUS_JP[regionIndex];// Region from the cursor
        hougenInformation.pref = Constants.PREFS[regionIndex];
        hougenInformation.area = Constants.AREAS[regionIndex];
        hougenInformation.def = cursor.getString(defColumnIndex);
        hougenInformation.example = cursor.getString(exampleColumnIndex);
        hougenInformation.characterPositions = characterPositions;
        hougenInformation.pos = cursor.getString(posColumnIndex);
    }

    private void resetFloatingButtonText() {
        textViewSet = false;
        setOverlayIdle();
        chihouTextView.setText("");
        chihouTextView.setVisibility(View.GONE);
        currentHougenInformation.characterPositions.clear();
        normalText = "";
        currentResultIndex = 0;
        if (indicatorTextView != null) {
            windowManager.removeView(indicatorTextView);
            indicatorTextView = null;
        }
    }

    private void setOverlayIdle() {
        textView.setVisibility(View.GONE);
        logoView.setVisibility(View.VISIBLE);
    }

    private void setOverlayActive() {
        textView.setVisibility(View.VISIBLE);
        logoView.setVisibility(View.GONE);
    }


    //this method can search the text that cannot be converted from fullText
    private void separateNormalText(List<CharacterPosition> characterPositions, String fullText, int startIndex, int endIndex) {
        String TAG = "separateNormalText";
        int position = 0;

        for (char f : fullText.toCharArray()) {
            if (position < startIndex || position > endIndex) {
                characterPositions.add(new CharacterPosition(f, position));
                normalText += f;
            }
            position++;
        }
        Log.d(TAG, "The text that cannot be converted: " + normalText);
        Log.d(TAG, "numbers of character in textfield: " + characterPositions.size());
    }

    // Call this method when the user performs conversion
    private void onConvertText() {
        String TAG = "onConvertText";
        // Sort character positions by their start index
        if (currentHougenInformation.characterPositions != null && !currentHougenInformation.characterPositions.isEmpty()) {
            Collections.sort(currentHougenInformation.characterPositions, new Comparator<CharacterPosition>() {
                @Override
                public int compare(CharacterPosition cp1, CharacterPosition cp2) {
                    return Integer.compare(cp1.position, cp2.position);
                }
            });
        }

        StringBuilder combinedWord = new StringBuilder();

        for (CharacterPosition cp : currentHougenInformation.characterPositions) {
            combinedWord.append(cp.character);
        }

        convertedText = combinedWord.toString();

        Log.d(TAG, "Converted selectedText to: " + convertedText);

        // Clear the editable text and set it to the converted text
        // Assuming you have a method to clear and set the text
        setEditableText(convertedText);
        convertedText = "";
        currentHougenInformation.characterPositions.clear();

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

    private void createFloatingButton() {
        String TAG = "createFloatingButton";
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Permission to draw over other apps is not granted");
            return;
        }

        // Check if the floating button is already created
        if (floatingButton != null && floatingButton.isShown()) {
            Log.d(TAG, "Floating button is already displayed");
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the floating button layout
        floatingButton = LayoutInflater.from(this).inflate(R.layout.accessibility_button_layout, null);
        textView = floatingButton.findViewById(R.id.accessibility_text);
        logoView = floatingButton.findViewById(R.id.accessibility_logo);
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
            private boolean isSwipe = false;

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
                                isSwipe = true;
                                return true; // Stop further event processing
                            }
                            // Swiping right
                            else if (deltaX > 50 && Math.abs(deltaY) < 50) {
                                handleSwipeRight();
                                isSwipe = true;
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
                        if (!isLongPress && touchDuration < LONG_PRESS_THRESHOLD && !isSwipe) {
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
                    Log.d("longPressRunnable", "Long press detected");
                    launchShousaiActivity(currentHougenInformation);
                }
            };

            // Handle swipe left to move to the next item
            private void handleSwipeLeft() {
                String TAG = "handleSwipeLeft";
                Log.d(TAG, "SwipeLeft detected");

                if (!searchResults.isEmpty() && currentResultIndex < searchResults.size()) {
                    //implement left swiping animation
                    floatingButton.animate()
                            .translationXBy(-500)
                            .alpha(0)
                            .setDuration(150)
                            .withEndAction(new Runnable() {
                                   @Override
                                   public void run() {
                                       floatingButton.setTranslationX(100); // Move view to the right of the screen
                                       floatingButton.animate()
                                               .translationX(0)  // Slide the new view into position
                                               .alpha(1)         // Fade in the new view
                                               .setDuration(100)
                                               .withEndAction(new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       isSwipe = false;
                                                   }
                                               });
                                   }
                            })
                            .start();


                    if (currentResultIndex < searchResults.size() - 1) {
                        // Move to the next sublist if available
                        currentResultIndex++;
                        // Update the floating button with the new text and hougenInformation
                        updateFloatingButtonText();
                    }
                }

                // Update the floating button with the new text and hougenInformation
                updateFloatingButtonText();
            }

            // Handle swipe right to move to the previous item
            private void handleSwipeRight() {
                String TAG = "handleSwipeRight";
                Log.d(TAG, "SwipeRight detected");

                if (!searchResults.isEmpty()) {
                    //implement right swiping animation
                    floatingButton.animate()
                            .translationXBy(500) // Move the view out of screen to the right
                            .alpha(0)             // Fade out the current view
                            .setDuration(150)     // Duration of the animation
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    floatingButton.setTranslationX(-100); // Move view to the left of the screen
                                    floatingButton.animate()
                                            .translationX(0)  // Slide the new view into position
                                            .alpha(1)         // Fade in the new view
                                            .setDuration(100)
                                            .withEndAction(new Runnable() {
                                                @Override
                                                public void run() {
                                                    isSwipe = false;
                                                }
                                            });
                                }
                            })
                            .start();

                    if (currentResultIndex > 0) {
                        // Move to the previous sublist if available
                        currentResultIndex--;
                        // Update the floating button with the new text and hougenInformation
                        updateFloatingButtonText();
                    }
                }
            }

            private void handleButtonClick() {
                String TAG = "handleButtonClick";
                Log.d(TAG, "Button clicked");
                if (!searchResults.isEmpty()) {

                    // Attempt to modify the text in the focused edit field
                    AccessibilityNodeInfo focusedNode = getRootInActiveWindow().findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                    if (focusedNode != null && focusedNode.isEditable()) {
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, convertedText);
                        focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                        onConvertText();
                        int regionIndex = Arrays.asList(Constants.CHIHOUS_JP).indexOf(currentHougenInformation.chihou);
                        databaseHelper.setWordToFound(currentHougenInformation.hougen, regionIndex);
                        List<String> foundWords = databaseHelper.getFoundWords(regionIndex);
                        Log.d("FoundWords", "FOUND WORD " + foundWords + " in " + Constants.CHIHOUS[regionIndex]);
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
            Log.e(TAG, "Error adding view to WindowManager", e);
        }
    }

    private void createIndicator(String indicator) {
        String TAG = "createIndicator";
        // Check if indicatorTextView is already created
        if (indicatorTextView != null && indicatorTextView.isShown()) {
            Log.d(TAG, "indicatorTextView is already displayed");
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
        int indicatorOffsetX = 20;
        int indicatorOffsetY = floatingButton.getHeight() * 17/10;

        indicatorParams.gravity = Gravity.TOP | Gravity.LEFT;
        indicatorParams.x = params.x + indicatorOffsetX; // Keep same X position
        indicatorParams.y = params.y + indicatorOffsetY; // Position below the floating layout

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

    private void launchShousaiActivity(GlobalVariable.HougenInformation hougenInformation) {
        Log.d("launchShousaiActivity", "lauched ShousaiActivity");
        Intent intent = new Intent(this, HougenInfoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("hougen", verbConjugator.reconjugate(hougenInformation.hougen, verbMap));
        intent.putExtra("chihou", hougenInformation.chihou);
        intent.putExtra("pref", hougenInformation.pref);
        intent.putExtra("area", hougenInformation.area);
        intent.putExtra("def", hougenInformation.def);
        intent.putExtra("example", hougenInformation.example);
        intent.putExtra("pos", hougenInformation.pos);
        startService(intent);
    }

}