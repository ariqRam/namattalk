package com.example.namapopup;

import static android.content.Context.MODE_PRIVATE;

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

        if(chosenChihous[0].isEmpty()) chosenChihous[0] = "hida";

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
    public Cursor searchWord(String word) {
        SQLiteDatabase db = this.getReadableDatabase();
        String TABLE_NAME = chosenChihous[0];
        String COLUMN_NAME = "hougen";

        String queryString = "SELECT hougen, pref, area, def, example FROM " + TABLE_NAME + " WHERE " + COLUMN_NAME + " LIKE ?";

        Cursor cursor = db.rawQuery(queryString, new String[]{ word + "%" });

        return cursor;
    }
}
