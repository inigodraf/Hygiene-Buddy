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
        if (name == null || name.trim().isEmpty()) {
            android.util.Log.e("UserProfileDatabaseHelper", "Cannot insert profile with empty name");
            return -1;
        }

        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, name.trim());
            values.put(COLUMN_AGE, age);
            if (imageUri != null) {
                values.put(COLUMN_IMAGE_URI, imageUri);
            }
            if (conditions != null) {
                values.put(COLUMN_CONDITIONS, conditions);
            }
            long result = db.insert(TABLE_USER_PROFILES, null, values);
            if (result == -1) {
                android.util.Log.e("UserProfileDatabaseHelper", "Failed to insert profile: " + name);
            } else {
                android.util.Log.d("UserProfileDatabaseHelper", "Successfully inserted profile with ID: " + result);
            }
            return result;
        } catch (Exception e) {
            android.util.Log.e("UserProfileDatabaseHelper", "Error inserting profile: " + e.getMessage(), e);
            return -1;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public List<UserProfile> getAllProfiles() {
        List<UserProfile> profiles = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = getReadableDatabase();
            cursor = db.query(TABLE_USER_PROFILES, null, null, null, null, null, COLUMN_NAME + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                    int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));

                    int imageUriCol = cursor.getColumnIndex(COLUMN_IMAGE_URI);
                    String imageUri = (imageUriCol >= 0 && !cursor.isNull(imageUriCol))
                            ? cursor.getString(imageUriCol) : null;

                    int conditionsCol = cursor.getColumnIndex(COLUMN_CONDITIONS);
                    String conditions = (conditionsCol >= 0 && !cursor.isNull(conditionsCol))
                            ? cursor.getString(conditionsCol) : null;

                    profiles.add(new UserProfile(id, name, age, imageUri, conditions));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            android.util.Log.e("UserProfileDatabaseHelper", "Error getting all profiles: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return profiles;
    }

    public UserProfile getProfileById(int id) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = getReadableDatabase();
            cursor = db.query(TABLE_USER_PROFILES, null, COLUMN_ID + "=?",
                    new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));

                int imageUriCol = cursor.getColumnIndex(COLUMN_IMAGE_URI);
                String imageUri = (imageUriCol >= 0 && !cursor.isNull(imageUriCol))
                        ? cursor.getString(imageUriCol) : null;

                int conditionsCol = cursor.getColumnIndex(COLUMN_CONDITIONS);
                String conditions = (conditionsCol >= 0 && !cursor.isNull(conditionsCol))
                        ? cursor.getString(conditionsCol) : null;

                return new UserProfile(id, name, age, imageUri, conditions);
            }
        } catch (Exception e) {
            android.util.Log.e("UserProfileDatabaseHelper", "Error getting profile by ID: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return null;
    }

    public int updateProfile(int id, String name, int age, String imageUri, String conditions) {
        if (name == null || name.trim().isEmpty()) {
            android.util.Log.e("UserProfileDatabaseHelper", "Cannot update profile with empty name");
            return 0;
        }

        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, name.trim());
            values.put(COLUMN_AGE, age);
            if (imageUri != null) {
                values.put(COLUMN_IMAGE_URI, imageUri);
            } else {
                values.putNull(COLUMN_IMAGE_URI);
            }
            if (conditions != null) {
                values.put(COLUMN_CONDITIONS, conditions);
            } else {
                values.putNull(COLUMN_CONDITIONS);
            }
            int result = db.update(TABLE_USER_PROFILES, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
            if (result > 0) {
                android.util.Log.d("UserProfileDatabaseHelper", "Successfully updated profile with ID: " + id);
            } else {
                android.util.Log.e("UserProfileDatabaseHelper", "Failed to update profile with ID: " + id);
            }
            return result;
        } catch (Exception e) {
            android.util.Log.e("UserProfileDatabaseHelper", "Error updating profile: " + e.getMessage(), e);
            return 0;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public int deleteProfile(int id) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            int result = db.delete(TABLE_USER_PROFILES, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
            if (result > 0) {
                android.util.Log.d("UserProfileDatabaseHelper", "Successfully deleted profile with ID: " + id);
            } else {
                android.util.Log.e("UserProfileDatabaseHelper", "Failed to delete profile with ID: " + id);
            }
            return result;
        } catch (Exception e) {
            android.util.Log.e("UserProfileDatabaseHelper", "Error deleting profile: " + e.getMessage(), e);
            return 0;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
