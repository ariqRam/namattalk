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
    private String[] chosenChihous;




    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        chosenChihous = new String[Constants.CHIHOUS.length];
        for(int i = 0; i < Constants.CHIHOUS.length; i++) {
            String chihou = Constants.CHIHOUS[i];
            DialectState dialectState = getDialectState(context, chihou);
            if(dialectState.isEnabled) {
                chosenChihous[i] = chihou;
                Log.d("DB", chihou + " is Enabled");
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

    public Cursor searchWordForDialect(String word, int idx) {
        String chihou = Constants.CHIHOUS[idx];
        SQLiteDatabase db = this.getReadableDatabase();

        // Split the search word by '、' to handle multiple words
        String[] splitWords = word.split("、");

        DialectState dialectState = getDialectState(context, chihou);
        Log.d("ARIQ", chihou);
        String tableName = chihou;

        // Only proceed if the dialect is enabled for searching
        if (dialectState.isEnabled && tableName != null) {
            StringBuilder exactMatchQueryBuilder = new StringBuilder();
            StringBuilder partialMatchQueryBuilder = new StringBuilder();

            // Determine the column to search based on the mode ("学習" for non-native, "母語" for native)
            String searchColumn = "学習".equals(dialectState.mode) ? "trigger" : "hougen";

            // Build the exact match query
            exactMatchQueryBuilder.append("SELECT hougen, trigger, def, example, pos FROM ").append(tableName).append(" WHERE ");
            for (int i = 0; i < splitWords.length; i++) {
                exactMatchQueryBuilder.append(searchColumn).append(" = ?");
                if (i != splitWords.length - 1) {
                    exactMatchQueryBuilder.append(" OR ");
                }
            }

            // Build the partial match query
            partialMatchQueryBuilder.append("SELECT hougen, trigger, def, example, pos FROM ").append(tableName).append(" WHERE ");
            for (int i = 0; i < splitWords.length; i++) {
                partialMatchQueryBuilder.append(searchColumn).append(" LIKE ?");
                if (i != splitWords.length - 1) {
                    partialMatchQueryBuilder.append(" OR ");
                }
            }

            // Prepare query arguments for exact matching
            String[] exactQueryArgs = splitWords;

            // Prepare query arguments for partial matching (append % for LIKE)
            String[] partialQueryArgs = new String[splitWords.length];
            for (int i = 0; i < splitWords.length; i++) {
                partialQueryArgs[i] = "%" + splitWords[i] + "%";
            }

            // Try exact match first
            Cursor cursor = db.rawQuery(exactMatchQueryBuilder.toString(), exactQueryArgs);
            Log.d("searchWord", "Exact search for [" + word + "] in table " + tableName + " | count: " + cursor.getCount());

            // If exact match found, return the cursor
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(searchColumn);
                Log.d("searchWord", "Exact match found in " + tableName + ": " + cursor.getString(index));
            } else {
                // If no exact match, fallback to partial match
                cursor = db.rawQuery(partialMatchQueryBuilder.toString(), partialQueryArgs);
                Log.d("searchWord", "Partial search for [" + word + "] in table " + tableName + " | count: " + cursor.getCount());

                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(searchColumn);
                    Log.d("searchWord", "Partial match found in " + tableName + ": " + cursor.getString(index));
                }
            }

            return cursor;
        }

        return null;
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
                StringBuilder exactMatchQueryBuilder = new StringBuilder();
                StringBuilder partialMatchQueryBuilder = new StringBuilder();

                // Determine the column to search based on the mode ("学習" for non-native, "母語" for native)
                String searchColumn = "学習".equals(dialectState.mode) ? "trigger" : "hougen";

                // Build the exact match query
                exactMatchQueryBuilder.append("SELECT hougen, trigger, def, example, pos FROM ").append(tableName).append(" WHERE ");
                for (int i = 0; i < splitWords.length; i++) {
                    exactMatchQueryBuilder.append(searchColumn).append(" = ?");
                    if (i != splitWords.length - 1) {
                        exactMatchQueryBuilder.append(" OR ");
                    }
                }

                // Build the partial match query
                partialMatchQueryBuilder.append("SELECT hougen, trigger, def, example, pos FROM ").append(tableName).append(" WHERE ");
                for (int i = 0; i < splitWords.length; i++) {
                    partialMatchQueryBuilder.append(searchColumn).append(" LIKE ?");
                    if (i != splitWords.length - 1) {
                        partialMatchQueryBuilder.append(" OR ");
                    }
                }

                // Prepare query arguments for exact matching
                String[] exactQueryArgs = splitWords;

                // Prepare query arguments for partial matching (append % for LIKE)
                String[] partialQueryArgs = new String[splitWords.length];
                for (int i = 0; i < splitWords.length; i++) {
                    partialQueryArgs[i] = "%" + splitWords[i] + "%";
                }

                // Try exact match first
                Cursor cursor = db.rawQuery(exactMatchQueryBuilder.toString(), exactQueryArgs);
                Log.d("searchWord", "Exact search for [" + word + "] in table " + tableName + " | count: " + cursor.getCount());

                // If exact match found, return the cursor
                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(searchColumn);
                    Log.d("searchWord", "Exact match found in " + tableName + ": " + cursor.getString(index));
                } else {
                    // If no exact match, fallback to partial match
                    cursor = db.rawQuery(partialMatchQueryBuilder.toString(), partialQueryArgs);
                    Log.d("searchWord", "Partial search for [" + word + "] in table " + tableName + " | count: " + cursor.getCount());

                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(searchColumn);
                        Log.d("searchWord", "Partial match found in " + tableName + ": " + cursor.getString(index));
                    }
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

        for (String tableName : chosenChihous) {
            Log.d("KEn", tableName);
            if (tableName != null && !tableName.isEmpty()) {
                try {
                    // Query to get verbs where 'pos' column is '動詞' (verb)
                    String verbQuery = "SELECT hougen, trigger, pos FROM " + tableName + " WHERE pos = ?";
                    // Execute the query
                    String[] queryArgs = new String[]{"動詞"};
                    Cursor cursor = db.rawQuery(verbQuery, queryArgs);
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
