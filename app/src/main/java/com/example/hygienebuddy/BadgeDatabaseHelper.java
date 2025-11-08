package com.example.hygienebuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BadgeDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "badges.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_BADGES = "badges";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_EARNED_DATE = "earned_date";
    private static final String COLUMN_IS_UNLOCKED = "is_unlocked";
    private static final String COLUMN_BADGE_KEY = "badge_key";
    private static final String COLUMN_PROGRESS = "progress";
    private static final String COLUMN_GOAL = "goal";

    private static final String CREATE_TABLE_BADGES =
            "CREATE TABLE " + TABLE_BADGES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT NOT NULL, " +
                    COLUMN_DESCRIPTION + " TEXT NOT NULL, " +
                    COLUMN_EARNED_DATE + " TEXT, " +
                    COLUMN_IS_UNLOCKED + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_BADGE_KEY + " TEXT, " +
                    COLUMN_PROGRESS + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_GOAL + " INTEGER NOT NULL DEFAULT 0" +
                    ")";

    public BadgeDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BADGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BADGES);
        onCreate(db);
    }

    public void seedIfEmpty() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BADGES, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        if (count > 0) {
            db.close();
            return;
        }

        // Seed default badges
        insertBadge(db, "1-Day Streak", "Completed tasks 1 day in a row", 0, 1, "streak_1");
        insertBadge(db, "3-Day Streak", "Completed tasks 3 days in a row", 0, 3, "streak_3");
        insertBadge(db, "7-Day Streak", "Completed tasks 7 days in a row", 0, 7, "streak_7");
        insertBadge(db, "14-Day Streak", "Completed tasks 14 days in a row", 0, 14, "streak_14");
        insertBadge(db, "30-Day Streak", "Completed tasks 30 days in a row", 0, 30, "streak_30");
        insertBadge(db, "60-Day Streak", "Completed tasks 60 days in a row", 0, 60, "streak_60");
        insertBadge(db, "100-Day Streak", "Completed tasks 100 days in a row", 0, 100, "streak_100");

        insertBadge(db, "Handwashing Hero", "Completed 10 handwashing tasks", 0, 10, "handwashing_hero");
        insertBadge(db, "Toothbrushing Champ", "Completed 10 toothbrushing tasks", 0, 10, "toothbrushing_champ");
        insertBadge(db, "Routine Master", "Completed all daily tasks on time for 7 consecutive days", 0, 7, "routine_master");
        insertBadge(db, "Clean Habit Starter", "Completed first hygiene task", 0, 1, "clean_habit_starter");
        // insertBadge(db, "Super Streak", "Maintained streak for 30+ days", 0, 30, "super_streak");

        db.close();
    }

    private void insertBadge(SQLiteDatabase db, String title, String description, int progress, int goal, String key) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_PROGRESS, progress);
        values.put(COLUMN_GOAL, goal);
        values.put(COLUMN_BADGE_KEY, key);
        values.put(COLUMN_IS_UNLOCKED, 0);
        db.insert(TABLE_BADGES, null, values);
    }

    public List<BadgeModel> getAllBadges() {
        List<BadgeModel> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BADGES, null, null, null, null, null, COLUMN_TITLE + " ASC");
        if (cursor.moveToFirst()) {
            do {
                result.add(fromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return result;
    }

    public List<BadgeModel> getEarnedBadges() {
        List<BadgeModel> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BADGES, null, COLUMN_IS_UNLOCKED + "=1", null, null, null, COLUMN_TITLE + " ASC");
        if (cursor.moveToFirst()) {
            do {
                result.add(fromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return result;
    }

    public void unlockBadgeByKey(String badgeKey, String earnedDate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_UNLOCKED, 1);
        values.put(COLUMN_EARNED_DATE, earnedDate);
        db.update(TABLE_BADGES, values, COLUMN_BADGE_KEY + "=?", new String[]{badgeKey});
        db.close();
    }

    public void updateProgressByKey(String badgeKey, int progress) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROGRESS, progress);
        db.update(TABLE_BADGES, values, COLUMN_BADGE_KEY + "=?", new String[]{badgeKey});
        db.close();
    }

    /** Get a badge by its key */
    public BadgeModel getBadgeByKey(String badgeKey) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BADGES, null, COLUMN_BADGE_KEY + "=?",
                new String[]{badgeKey}, null, null, null);
        BadgeModel badge = null;
        if (cursor != null && cursor.moveToFirst()) {
            badge = fromCursor(cursor);
        }
        if (cursor != null) cursor.close();
        db.close();
        return badge;
    }

    private BadgeModel fromCursor(Cursor cursor) {
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
        boolean isUnlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_UNLOCKED)) == 1;
        String earnedDate = null;
        int earnedDateCol = cursor.getColumnIndex(COLUMN_EARNED_DATE);
        if (earnedDateCol >= 0 && !cursor.isNull(earnedDateCol)) {
            earnedDate = cursor.getString(earnedDateCol);
        }
        int progress = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS));
        int goal = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GOAL));
        String key = null;
        int keyCol = cursor.getColumnIndex(COLUMN_BADGE_KEY);
        if (keyCol >= 0 && !cursor.isNull(keyCol)) {
            key = cursor.getString(keyCol);
        }
        // Use the badge key as imageKey (can be overridden if needed)
        String imageKey = key;
        return new BadgeModel(key, title, description, isUnlocked, earnedDate, progress, goal, imageKey);
    }
}


