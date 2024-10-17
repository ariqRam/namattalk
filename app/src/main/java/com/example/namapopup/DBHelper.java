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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "hogen.db";
    private static final int DATABASE_VERSION = 6;
    private final Context context;
    private SQLiteDatabase db;
    private String[] chosenChihous;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        this.db = getWritableDatabase(); // Initialize the database here
        chosenChihous = new String[Constants.CHIHOUS.length];
        for (int i = 0; i < Constants.CHIHOUS.length; i++) {
            String chihou = Constants.CHIHOUS[i];
            DialectState dialectState = getDialectState(context, chihou);
            if (dialectState.isEnabled()) {
                chosenChihous[i] = chihou;
                Log.d("DB", chihou + " database is Enabled");
            }
        }
        try {
            copyDatabaseFromAssets();
        } catch (IOException e) {
            throw new Error("Error copying database from assets", e);
        }
        addFoundColumnToAllRegions(db);
    }

    // Access the db variable instead of calling getReadableDatabase() or getWritableDatabase()
    public SQLiteDatabase getDatabase() {
        if (db == null || !db.isOpen()) {
            db = getWritableDatabase();
        }
        return db;
    }



    private void copyDatabaseFromAssets() throws IOException {
        File databaseFile = context.getDatabasePath(DATABASE_NAME);

        // Check if the database file already exists
        if (!databaseFile.exists() || !tablesExist()) {
            InputStream inputStream = context.getAssets().open(DATABASE_NAME);
            String outFileName = databaseFile.getPath();
            OutputStream outputStream = new FileOutputStream(outFileName);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.d("DBHelper", "Database copied from assets.");
        } else {
            Log.d("DBHelper", "Database already exists and contains required tables. No need to copy.");
        }
    }

    // Method to check if both "hida" and "toyama" tables exist
    private boolean tablesExist() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openDatabase(context.getDatabasePath(DATABASE_NAME).getPath(), null, SQLiteDatabase.OPEN_READONLY);

            // Check if "hida" table exists
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='hida'", null);
            boolean hidaExists = cursor != null && cursor.moveToFirst();
            cursor.close();

            // Check if "toyama" table exists
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='toyama'", null);
            boolean toyamaExists = cursor != null && cursor.moveToFirst();
            cursor.close();

            return hidaExists && toyamaExists;
        } catch (Exception e) {
            Log.e("DBHelper", "Error checking tables: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }



    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DB", "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 6) {
            for (String tableName : Constants.CHIHOUS) {
                // Add 'found' column if it doesn't exist
                try {
                    Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
                    boolean foundColumnExists = false;
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                do {
                                    String columnName = cursor.getString(cursor.getColumnIndex("name"));
                                    if ("found".equals(columnName)) {
                                        foundColumnExists = true;
                                        break;
                                    }
                                } while (cursor.moveToNext());
                            }
                        } finally {
                            cursor.close();
                        }
                    }

                    // Add the 'found' column if it doesn't exist
                    if (!foundColumnExists) {
                        db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN found INTEGER DEFAULT 0");
                        Log.d("DB Upgrade", "'found' column added to table: " + tableName);
                    }
                } catch (SQLException e) {
                    Log.e("DB", "Error adding 'found' column to table: " + tableName, e);
                }
            }
        }
    }


    public Cursor[] searchDictionary(String searchTerm) {
        SQLiteDatabase db = getDatabase(); // Use the private variable here
        String wildcardTerm = "%" + searchTerm + "%";
        Cursor[] resultCursors = new Cursor[Constants.CHIHOUS.length];
        for (int i = 0; i < Constants.CHIHOUS.length; i++) {
            String chihou = Constants.CHIHOUS[i];
            String rawQuery =
                    "SELECT hougen, trigger, yomikata, candidate, def, example, pos FROM " + chihou +
                            " WHERE trigger LIKE ? OR yomikata LIKE ? OR candidate LIKE ? OR def LIKE ?";

            String[] selectionArgs = new String[]{wildcardTerm, wildcardTerm};

            Cursor cursor = db.rawQuery(rawQuery, selectionArgs);
            resultCursors[i] = cursor;
        }
        return resultCursors;
    }

    // Add methods to query your dictionary here

    public Cursor[] searchWord(String word) {
        SQLiteDatabase db = getDatabase();
        List<Cursor> allResults = new ArrayList<>();

        // Split the search word by '|' to handle multiple words
        String[] splitWords = word.split(Constants.SEPARATOR);

        for (String chihou : Constants.CHIHOUS) {
            DialectState dialectState = getDialectState(context, chihou);
            String tableName = chihou;

            // Only proceed if the dialect is enabled for searching
            if (dialectState.isEnabled() && tableName != null && !tableName.isEmpty()) {
                StringBuilder exactMatchQueryBuilder = new StringBuilder();
                StringBuilder partialMatchQueryBuilder = new StringBuilder();

                // Determine the column to search based on the mode ("学習" for non-native, "母語" for native)
                String searchColumn = "学習".equals(dialectState.mode) ? "trigger" : "yomikata";

                // Build the exact match query
                exactMatchQueryBuilder.append("SELECT hougen, trigger, yomikata, candidate, def, example, pos FROM ").append(tableName).append(" WHERE ");
                for (int i = 0; i < splitWords.length; i++) {
                    exactMatchQueryBuilder.append(searchColumn).append(" = ?");
                    if (i != splitWords.length - 1) {
                        exactMatchQueryBuilder.append(" OR ");
                    }
                }

                // Build the partial match query
                partialMatchQueryBuilder.append("SELECT hougen, trigger, yomikata, candidate, def, example, pos FROM ").append(tableName).append(" WHERE ");
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

                Log.d("exactMatchQuery", exactMatchQueryBuilder.toString());
                // Try exact match first
                Cursor cursor = db.rawQuery(exactMatchQueryBuilder.toString(), exactQueryArgs);

                // If exact match found, return the cursor
                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(searchColumn);
                } else {
                    // If no exact match, fallback to partial match
                    cursor = db.rawQuery(partialMatchQueryBuilder.toString(), partialQueryArgs);

                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(searchColumn);
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
        SQLiteDatabase db = getDatabase();
        List<Cursor> allResults = new ArrayList<>();

        for (String tableName : chosenChihous) {
            if (tableName != null && !tableName.isEmpty()) {
                try {
                    // Query to get verbs where 'pos' column is '動詞' (verb)
                    String verbQuery = "SELECT trigger, yomikata, candidate, pos FROM " + tableName + " WHERE pos = ?";
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

    public boolean addFoundColumnToAllRegions(SQLiteDatabase db) {
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
        SQLiteDatabase db = getDatabase();

        try {
            // Ensure the 'found' column exists
//            addFoundColumnToAllRegions();

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
        SQLiteDatabase db = getDatabase();
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
        return count;
    }

    public String getFoundWordsRatio(int regionIndex) {
        SQLiteDatabase db = getDatabase();
        String TABLE_NAME = Constants.CHIHOUS[regionIndex];

        int foundCount = getFoundWordsCount(regionIndex);
        List<String> foundWords = getFoundWords(regionIndex);

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

        String resultCount = foundCount + "/" + totalCount;
        Log.d("getFoundWordsRatio", "word that was found in " + TABLE_NAME + ": " + foundWords + " (" + foundCount + "/" + totalCount + ")");

        return resultCount;
    }
}
