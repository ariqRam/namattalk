package com.example.namapopup;

import static android.content.Context.MODE_PRIVATE;

import static com.example.namapopup.Helper.getDialectState;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.compose.material3.AlertDialogDefaults;

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
    private String[] chosenChihous;




    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        chosenChihous = new String[Constants.CHIHOUS.length];
        for(int i = 0; i < Constants.CHIHOUS.length; i++) {
            String chihou = Constants.CHIHOUS[i];
            DialectState dialectState = getDialectState(context, chihou);
            if(dialectState.isEnabled()) {
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
        addFoundColumnToAllRegions();
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
        if (dialectState.isEnabled() && tableName != null) {
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
            if (dialectState.isEnabled() && tableName != null && !tableName.isEmpty()) {
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

    public void deleteRowUsingHougen() {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "DELETE FROM hida WHERE hougen = ?";
        db.execSQL(query, new String[]{"ん"});

        Log.d("Database", "Row deleted using raw query");

        db.close();
    }

    public void insertNewRow(String def, String trigger, String hougen, String pos) {
        // Get the writable database
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a ContentValues object to hold the data you want to insert
        ContentValues values = new ContentValues();
        values.put("def", def);
        values.put("trigger", trigger);
        values.put("hougen", hougen);
        values.put("pos", pos);

        // Insert the new row, the second argument is a nullColumnHack in case you don't have any data
        long newRowId = db.insert("hida", null, values);

        // Check if the insertion was successful
        if (newRowId != -1) {
            Log.d("Database", "Row inserted successfully with ID: " + newRowId);
        } else {
            Log.d("Database", "Failed to insert row");
        }

        // Close the database connection
        db.close();
    }

    public boolean addFoundColumnToAllRegions() {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean allColumnsAdded = true;

        Log.d("DB", "Starting to add 'found' column to all tables.");

        for (String tableName : Constants.CHIHOUS) {
            Log.d("DB", "Processing table: " + tableName);
            try {
                // Check if the 'found' column exists
                Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
                boolean foundColumnExists = false;

                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            do {
                                String columnName = cursor.getString(cursor.getColumnIndex("name"));
                                if ("found".equals(columnName)) {
                                    foundColumnExists = true;
                                    Log.d("DB", "'found' column already exists in table: " + tableName);
                                    break;
                                }
                            } while (cursor.moveToNext());
                        }
                    } finally {
                        cursor.close();
                    }
                }

                // If the column doesn't exist, add it
                if (!foundColumnExists) {
                    db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN found INTEGER DEFAULT 0");
                    Log.d("DB", "Successfully added 'found' column to table: " + tableName);
                }
            } catch (SQLException e) {
                Log.e("DB", "Error adding 'found' column to table: " + tableName, e);
                allColumnsAdded = false;
                // Continue with other tables even if one fails
            }
        }

        if (allColumnsAdded) {
            Log.d("DB", "Successfully added 'found' column to all tables.");
        } else {
            Log.e("DB", "Some tables failed to add 'found' column.");
        }



        return allColumnsAdded;
    }


    public boolean setWordToFound(String word, int regionIndex) {
        String TABLE_NAME = Constants.CHIHOUS[regionIndex];
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Ensure the 'found' column exists
            addFoundColumnToAllRegions();

            // Update the 'found' status for the word
            ContentValues values = new ContentValues();
            values.put("found", 1); // Using 1 for true, 0 for false

            int rowsAffected = db.update(
                    TABLE_NAME,
                    values,
                    "hougen = ?",
                    new String[]{word}
            );
            db.close();

            return rowsAffected > 0;
        } catch (SQLException e) {
            Log.e("DB", "Error setting word as found: " + word, e);
            return false;
        }
    }

    public List<String> getFoundWords(int regionIndex) {
        List<String> foundWords = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String TABLE_NAME = Constants.CHIHOUS[regionIndex];

        Cursor cursor = db.query(
                TABLE_NAME,
                new String[]{"hougen"},
                "found = 1",
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                foundWords.add(cursor.getString(0));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return foundWords;
    }

    public int getFoundWordsCount(int regionIndex) {
        List<String> foundWords = getFoundWords(regionIndex);
        int count = foundWords.size();
        for(String foundWord: foundWords) Log.d("GETFOUNDWORDSCOUNT", foundWord);

        return count;
    }

    public String getFoundWordsRatio(int regionIndex) {
        SQLiteDatabase db = this.getReadableDatabase();
        String TABLE_NAME = Constants.CHIHOUS[regionIndex];

        int foundCount = getFoundWordsCount(regionIndex);

        // Query to count total rows in the table
        Cursor totalCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_NAME,
                null
        );

        int totalCount = 0;
        if (totalCursor != null && totalCursor.moveToFirst()) {
            totalCount = totalCursor.getInt(0);
            totalCursor.close();
        }

        String resultText = foundCount + "/" + totalCount;
        Log.d(TABLE_NAME, resultText);

        return resultText;
    }
}
