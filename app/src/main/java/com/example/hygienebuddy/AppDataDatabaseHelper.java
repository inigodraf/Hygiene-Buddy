package com.example.hygienebuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Centralized database helper for all app data previously stored in SharedPreferences.
 * Manages: current profile ID, task completions, streak days, badge progress, and app settings.
 */
public class AppDataDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "app_data.db";
    private static final int DATABASE_VERSION = 1;

    // Table: App Settings (current profile ID, onboarding flags, etc.)
    private static final String TABLE_APP_SETTINGS = "app_settings";
    private static final String COLUMN_SETTING_KEY = "setting_key";
    private static final String COLUMN_SETTING_VALUE = "setting_value";

    // Table: Task Completions (date, profile_id, task_type)
    private static final String TABLE_TASK_COMPLETIONS = "task_completions";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PROFILE_ID = "profile_id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_TASK_TYPE = "task_type";

    // Table: Streak Days (date, profile_id)
    private static final String TABLE_STREAK_DAYS = "streak_days";
    private static final String COLUMN_STREAK_DATE = "streak_date";

    // Table: Badge Progress (badge_key, profile_id, progress, unlocked, earned_date)
    private static final String TABLE_BADGE_PROGRESS = "badge_progress";
    private static final String COLUMN_BADGE_KEY = "badge_key";
    private static final String COLUMN_PROGRESS = "progress";
    private static final String COLUMN_UNLOCKED = "unlocked";
    private static final String COLUMN_EARNED_DATE = "earned_date";

    // Create table statements
    private static final String CREATE_TABLE_APP_SETTINGS =
            "CREATE TABLE " + TABLE_APP_SETTINGS + " (" +
                    COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, " +
                    COLUMN_SETTING_VALUE + " TEXT NOT NULL" +
                    ")";

    private static final String CREATE_TABLE_TASK_COMPLETIONS =
            "CREATE TABLE " + TABLE_TASK_COMPLETIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PROFILE_ID + " INTEGER NOT NULL, " +
                    COLUMN_DATE + " TEXT NOT NULL, " +
                    COLUMN_TASK_TYPE + " TEXT NOT NULL, " +
                    "UNIQUE(" + COLUMN_PROFILE_ID + ", " + COLUMN_DATE + ", " + COLUMN_TASK_TYPE + ")" +
                    ")";

    private static final String CREATE_TABLE_STREAK_DAYS =
            "CREATE TABLE " + TABLE_STREAK_DAYS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PROFILE_ID + " INTEGER NOT NULL, " +
                    COLUMN_STREAK_DATE + " TEXT NOT NULL, " +
                    "UNIQUE(" + COLUMN_PROFILE_ID + ", " + COLUMN_STREAK_DATE + ")" +
                    ")";

    private static final String CREATE_TABLE_BADGE_PROGRESS =
            "CREATE TABLE " + TABLE_BADGE_PROGRESS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PROFILE_ID + " INTEGER NOT NULL, " +
                    COLUMN_BADGE_KEY + " TEXT NOT NULL, " +
                    COLUMN_PROGRESS + " INTEGER DEFAULT 0, " +
                    COLUMN_UNLOCKED + " INTEGER DEFAULT 0, " +
                    COLUMN_EARNED_DATE + " TEXT, " +
                    "UNIQUE(" + COLUMN_PROFILE_ID + ", " + COLUMN_BADGE_KEY + ")" +
                    ")";

    public AppDataDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_APP_SETTINGS);
        db.execSQL(CREATE_TABLE_TASK_COMPLETIONS);
        db.execSQL(CREATE_TABLE_STREAK_DAYS);
        db.execSQL(CREATE_TABLE_BADGE_PROGRESS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle future migrations here
        if (oldVersion < 1) {
            // Future migrations
        }
    }

    // ==================== App Settings Methods ====================

    public void setSetting(String key, String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETTING_KEY, key);
        values.put(COLUMN_SETTING_VALUE, value);
        db.replace(TABLE_APP_SETTINGS, null, values);
        db.close();
    }

    public String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_APP_SETTINGS, new String[]{COLUMN_SETTING_VALUE},
                COLUMN_SETTING_KEY + "=?", new String[]{key}, null, null, null);
        String value = defaultValue;
        if (cursor != null && cursor.moveToFirst()) {
            value = cursor.getString(0);
        }
        if (cursor != null) cursor.close();
        db.close();
        return value;
    }

    public void setIntSetting(String key, int value) {
        setSetting(key, String.valueOf(value));
    }

    public int getIntSetting(String key, int defaultValue) {
        String value = getSetting(key, null);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setBooleanSetting(String key, boolean value) {
        setSetting(key, String.valueOf(value));
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        String value = getSetting(key, null);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    // ==================== Task Completions Methods ====================

    public void addTaskCompletion(int profileId, String date, String taskType) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_ID, profileId);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_TASK_TYPE, taskType.toLowerCase());
        db.insertWithOnConflict(TABLE_TASK_COMPLETIONS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public Set<String> getTaskCompletionsForDate(int profileId, String date) {
        SQLiteDatabase db = getReadableDatabase();
        Set<String> tasks = new HashSet<>();
        Cursor cursor = db.query(TABLE_TASK_COMPLETIONS, new String[]{COLUMN_TASK_TYPE},
                COLUMN_PROFILE_ID + "=? AND " + COLUMN_DATE + "=?",
                new String[]{String.valueOf(profileId), date}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                tasks.add(cursor.getString(0));
            }
            cursor.close();
        }
        db.close();
        return tasks;
    }

    public boolean hasTaskCompletion(int profileId, String date, String taskType) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASK_COMPLETIONS, new String[]{COLUMN_ID},
                COLUMN_PROFILE_ID + "=? AND " + COLUMN_DATE + "=? AND " + COLUMN_TASK_TYPE + "=?",
                new String[]{String.valueOf(profileId), date, taskType.toLowerCase()}, null, null, null);
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    /** Get the earliest task completion date for a profile */
    public String getEarliestTaskDate(int profileId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASK_COMPLETIONS, new String[]{COLUMN_DATE},
                COLUMN_PROFILE_ID + "=?", new String[]{String.valueOf(profileId)},
                null, null, COLUMN_DATE + " ASC", "1");
        String earliestDate = null;
        if (cursor != null && cursor.moveToFirst()) {
            earliestDate = cursor.getString(0);
        }
        if (cursor != null) cursor.close();
        db.close();
        return earliestDate;
    }

    // ==================== Streak Days Methods ====================

    public void addStreakDay(int profileId, String date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_ID, profileId);
        values.put(COLUMN_STREAK_DATE, date);
        db.insertWithOnConflict(TABLE_STREAK_DAYS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public Set<String> getStreakDays(int profileId) {
        SQLiteDatabase db = getReadableDatabase();
        Set<String> days = new HashSet<>();
        Cursor cursor = db.query(TABLE_STREAK_DAYS, new String[]{COLUMN_STREAK_DATE},
                COLUMN_PROFILE_ID + "=?", new String[]{String.valueOf(profileId)}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                days.add(cursor.getString(0));
            }
            cursor.close();
        }
        db.close();
        return days;
    }

    public boolean isStreakDay(int profileId, String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_STREAK_DAYS, new String[]{COLUMN_ID},
                COLUMN_PROFILE_ID + "=? AND " + COLUMN_STREAK_DATE + "=?",
                new String[]{String.valueOf(profileId), date}, null, null, null);
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    // ==================== Badge Progress Methods ====================

    public void setBadgeProgress(int profileId, String badgeKey, int progress) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_ID, profileId);
        values.put(COLUMN_BADGE_KEY, badgeKey);
        values.put(COLUMN_PROGRESS, progress);
        db.insertWithOnConflict(TABLE_BADGE_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public int getBadgeProgress(int profileId, String badgeKey) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BADGE_PROGRESS, new String[]{COLUMN_PROGRESS},
                COLUMN_PROFILE_ID + "=? AND " + COLUMN_BADGE_KEY + "=?",
                new String[]{String.valueOf(profileId), badgeKey}, null, null, null);
        int progress = 0;
        if (cursor != null && cursor.moveToFirst()) {
            progress = cursor.getInt(0);
        }
        if (cursor != null) cursor.close();
        db.close();
        return progress;
    }

    public void setBadgeUnlocked(int profileId, String badgeKey, boolean unlocked, String earnedDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_ID, profileId);
        values.put(COLUMN_BADGE_KEY, badgeKey);
        values.put(COLUMN_UNLOCKED, unlocked ? 1 : 0);
        if (earnedDate != null) {
            values.put(COLUMN_EARNED_DATE, earnedDate);
        }
        db.insertWithOnConflict(TABLE_BADGE_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public boolean isBadgeUnlocked(int profileId, String badgeKey) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BADGE_PROGRESS, new String[]{COLUMN_UNLOCKED},
                COLUMN_PROFILE_ID + "=? AND " + COLUMN_BADGE_KEY + "=?",
                new String[]{String.valueOf(profileId), badgeKey}, null, null, null);
        boolean unlocked = false;
        if (cursor != null && cursor.moveToFirst()) {
            unlocked = cursor.getInt(0) == 1;
        }
        if (cursor != null) cursor.close();
        db.close();
        return unlocked;
    }

    public String getBadgeEarnedDate(int profileId, String badgeKey) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BADGE_PROGRESS, new String[]{COLUMN_EARNED_DATE},
                COLUMN_PROFILE_ID + "=? AND " + COLUMN_BADGE_KEY + "=?",
                new String[]{String.valueOf(profileId), badgeKey}, null, null, null);
        String date = null;
        if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
            date = cursor.getString(0);
        }
        if (cursor != null) cursor.close();
        db.close();
        return date;
    }

    public List<BadgeProgress> getAllBadgeProgress(int profileId) {
        SQLiteDatabase db = getReadableDatabase();
        List<BadgeProgress> badges = new ArrayList<>();
        Cursor cursor = db.query(TABLE_BADGE_PROGRESS, null,
                COLUMN_PROFILE_ID + "=?", new String[]{String.valueOf(profileId)}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String badgeKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BADGE_KEY));
                int progress = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS));
                boolean unlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_UNLOCKED)) == 1;
                String earnedDate = cursor.isNull(cursor.getColumnIndex(COLUMN_EARNED_DATE)) ? null :
                        cursor.getString(cursor.getColumnIndex(COLUMN_EARNED_DATE));
                badges.add(new BadgeProgress(badgeKey, progress, unlocked, earnedDate));
            }
            cursor.close();
        }
        db.close();
        return badges;
    }

    // ==================== Helper Class ====================

    public static class BadgeProgress {
        public String badgeKey;
        public int progress;
        public boolean unlocked;
        public String earnedDate;

        public BadgeProgress(String badgeKey, int progress, boolean unlocked, String earnedDate) {
            this.badgeKey = badgeKey;
            this.progress = progress;
            this.unlocked = unlocked;
            this.earnedDate = earnedDate;
        }
    }
}

