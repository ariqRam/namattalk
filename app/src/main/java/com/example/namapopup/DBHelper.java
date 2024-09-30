package com.example.namapopup;

import static android.content.Context.MODE_PRIVATE;

import static com.example.namapopup.Helper.getDialectState;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "hougen.db";
    private static final int DATABASE_VERSION = 1;
    private final Context context;
    private SQLiteDatabase database;
    private SharedPreferences sharedPreferences;
    private String[] chosenChihous;



    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        chosenChihous = new String[Constants.CHIHOUS.length];
        sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        for(int i = 0; i < Constants.CHIHOUS.length; i++) {
            if(sharedPreferences.getBoolean(Constants.CHIHOUS[i], false)) {
                chosenChihous[i] = Constants.CHIHOUS[i];
            }
        }

        // Check if the database exists, if not, copy it from assets
        if (!checkDatabase()) {
            try {
                copyDatabaseFromAssets();
            } catch (IOException e) {
                throw new Error("Error copying database from assets", e);
            }
        }
    }

    private boolean checkDatabase() {
        SQLiteDatabase checkDB = null;
        try {
            String path = context.getDatabasePath(DATABASE_NAME).getPath();
            checkDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            // Database doesn't exist yet
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null;
    }

    private void copyDatabaseFromAssets() throws IOException {
        InputStream inputStream = context.getAssets().open(DATABASE_NAME);
        String outFileName = context.getDatabasePath(DATABASE_NAME).getPath();
        OutputStream outputStream = new FileOutputStream(outFileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // We don't need to create the database here as we're copying it from assets
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades if needed
    }

    // Add methods to query your dictionary here

    public Cursor[] searchWord(String word) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Cursor> allResults = new ArrayList<>();


        // Split the search word by '、' to handle multiple words
        String[] splitWords = word.split("、");

        for (String chihou : Constants.CHIHOUS) {
            DialectState dialectState = getDialectState(context, chihou);
            String tableName = chihou;

            // Only proceed if the dialect is enabled for searching
            if (dialectState.isEnabled && tableName != null && !tableName.isEmpty()) {
                StringBuilder queryBuilder = new StringBuilder();

                // Branch based on the mode ("学習" for non-native, "母語" for native)
                if ("学習".equals(dialectState.mode)) {
                    Log.d("searchWord", tableName + " is in non-native mode");
                    queryBuilder.append("SELECT hougen, trigger, def, example, pos FROM ").append(tableName).append(" WHERE ");

                    // Add dynamic LIKE conditions for each split word in 'trigger' column
                    for (int i = 0; i < splitWords.length; i++) {
                        queryBuilder.append("trigger LIKE ?");
                        if (i != splitWords.length - 1) {
                            queryBuilder.append(" OR ");
                        }
                    }
                } else if ("母語".equals(dialectState.mode)) {
                    Log.d("searchWord", tableName + " is in native mode");
                    queryBuilder.append("SELECT hougen, trigger, def, example, pos FROM ").append(tableName).append(" WHERE ");

                    // Add dynamic LIKE conditions for each split word in 'hougen' column
                    for (int i = 0; i < splitWords.length; i++) {
                        queryBuilder.append("hougen LIKE ?");
                        if (i != splitWords.length - 1) {
                            queryBuilder.append(" OR ");
                        }
                    }
                } else {
                    // If mode is neither "学習" nor "母語", skip to the next dialect
                    Log.d("searchWord", "Unrecognized mode for " + tableName + ": " + dialectState.mode);
                    continue;
                }

                // Prepare query arguments (for each split word, append % to enable partial matching)
                String[] queryArgs = new String[splitWords.length];
                for (int i = 0; i < splitWords.length; i++) {
                    queryArgs[i] = "%" + splitWords[i] + "%"; // Partial matching with LIKE
                }

                // Execute the raw query
                Cursor cursor = db.rawQuery(queryBuilder.toString(), queryArgs);
                Log.d("searchWord", "Searching for [" + word + "] in table " + tableName + " | count: " + cursor.getCount());

                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex("hougen");
                    Log.d("searchWord", "Found in " + tableName + ": " + cursor.getString(index));
                }
                allResults.add(cursor);
            } else {
                allResults.add(null);
            }
        }

        return allResults.toArray(new Cursor[0]);
    }


    public Cursor[] getVerbs() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Cursor> allResults = new ArrayList<>();
        StringBuilder verbQuery = new StringBuilder();

        for (String tableName : chosenChihous) {
            if (tableName != null && !tableName.isEmpty()) {
                try {
                    // Query to get verbs where 'pos' column is '動詞' (verb)
                    verbQuery.append("SELECT hougen, trigger, def, example, pos FROM ").append(tableName).append(" WHERE pos = ?");
                    // Execute the query
                    String[] queryArgs = new String[]{"動詞"};
                    Cursor cursor = db.rawQuery(verbQuery.toString(), queryArgs);
                    if (cursor.moveToFirst()) {
                        Log.d("getVerbs", "Verbs found in the database.");
                    } else {
                        Log.d("getVerbs", "No verbs found.");
                    }

                    allResults.add(cursor);
                } catch (Exception e) {
                    Log.e("DBHelper", "Error fetching verbs", e);
                    allResults.add(null);
                }

            }
        }

        return allResults.toArray(new Cursor[0]);
    }
    
}
