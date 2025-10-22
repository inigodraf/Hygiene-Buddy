package com.example.hygienebuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class UserProfileDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "user_profiles.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_USER_PROFILES = "user_profiles";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_AGE = "age";
    private static final String COLUMN_IMAGE_URI = "image_uri";
    private static final String COLUMN_CONDITIONS = "conditions";

    private static final String CREATE_TABLE_USER_PROFILES =
            "CREATE TABLE " + TABLE_USER_PROFILES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_AGE + " INTEGER NOT NULL, " +
                    COLUMN_IMAGE_URI + " TEXT, " +
                    COLUMN_CONDITIONS + " TEXT" +
                    ")";

    public UserProfileDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER_PROFILES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_PROFILES);
        onCreate(db);
    }

    public long insertProfile(String name, int age, String imageUri, String conditions) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_AGE, age);
        values.put(COLUMN_IMAGE_URI, imageUri);
        values.put(COLUMN_CONDITIONS, conditions);
        long result = db.insert(TABLE_USER_PROFILES, null, values);
        db.close();
        return result;
    }

    public List<UserProfile> getAllProfiles() {
        List<UserProfile> profiles = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER_PROFILES, null, null, null, null, null, COLUMN_NAME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));
                String imageUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URI));
                String conditions = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONDITIONS));
                profiles.add(new UserProfile(id, name, age, imageUri, conditions));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return profiles;
    }

    public int updateProfile(int id, String name, int age, String imageUri, String conditions) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_AGE, age);
        values.put(COLUMN_IMAGE_URI, imageUri);
        values.put(COLUMN_CONDITIONS, conditions);
        int result = db.update(TABLE_USER_PROFILES, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    public int deleteProfile(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_USER_PROFILES, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }
}
