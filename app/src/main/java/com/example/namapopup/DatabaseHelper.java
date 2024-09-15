package com.example.namapopup;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "hougen_dictionary.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "hougen_entries";  // Changed from "hougen_hida"
    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "definition TEXT,"
                + "trigger_word TEXT,"
                + "hougen TEXT,"
                + "part_of_speech TEXT,"
                + "situation TEXT,"
                + "prefecture TEXT,"
                + "area TEXT,"
                + "example TEXT,"
                + "frequency TEXT"
                + ")";
        db.execSQL(CREATE_TABLE);
        loadHougenData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    private void loadHougenData(SQLiteDatabase db) {
        try {
            InputStream is = context.getAssets().open("hougenDatabase_hida.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            String insertQuery = "INSERT INTO " + TABLE_NAME +
                    " (definition, trigger_word, hougen, part_of_speech, situation, prefecture, area, example, frequency) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            SQLiteStatement statement = db.compileStatement(insertQuery);

            db.beginTransaction();
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] rowData = line.split(",");
                if (rowData.length >= 9) {
                    for (int i = 0; i < 9; i++) {
                        statement.bindString(i + 1, rowData[i].trim());
                    }
                    statement.executeInsert();
                    statement.clearBindings();
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Cursor searchHougen(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = new String[]{"_id", "definition", "hougen", "part_of_speech", "example"};
        String selection = "hougen LIKE ? OR definition LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        return db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);
    }

    public Cursor getAllWords() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, new String[]{"_id", "hougen"}, null, null, null, null, "hougen ASC");
    }
}