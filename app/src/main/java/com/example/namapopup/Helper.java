package com.example.namapopup;
import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.namapopup.Constants;
// Retrieve dialect state from SharedPreferences
public class Helper {
    public static DialectState getDialectState(Context context, String dialect) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        // Get mode and enabled state, with defaults if not found
        String mode = sharedPreferences.getString(dialect + "_mode", "学習");
        boolean isEnabled = sharedPreferences.getBoolean(dialect + "_enabled", false);

        DialectState state = new DialectState();
        state.mode = mode;
        state.isEnabled = isEnabled;

        return state;
    }

    public static boolean isNonNativeMode(DialectState dialectState) {
        return "学習".equals(dialectState.mode);
    }
}
