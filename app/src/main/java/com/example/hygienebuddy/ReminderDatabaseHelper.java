package com.example.hygienebuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ReminderDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "reminders.db";
    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_REMINDERS = "reminders";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TASK_NAME = "task_name";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_FREQUENCY = "frequency";
    private static final String COLUMN_IS_ACTIVE = "is_active";
    private static final String COLUMN_CUSTOM_INTERVAL = "custom_interval";
    private static final String COLUMN_DAYS_OF_WEEK = "days_of_week";
    private static final String COLUMN_TIMES_PER_DAY = "times_per_day";
    private static final String COLUMN_PROFILE_ID = "profile_id";

    private static final String CREATE_TABLE_REMINDERS =
            "CREATE TABLE " + TABLE_REMINDERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TASK_NAME + " TEXT NOT NULL, " +
                    COLUMN_TIME + " TEXT NOT NULL, " +
                    COLUMN_FREQUENCY + " TEXT NOT NULL, " +
                    COLUMN_IS_ACTIVE + " INTEGER NOT NULL, " +
                    COLUMN_CUSTOM_INTERVAL + " INTEGER, " +
                    COLUMN_DAYS_OF_WEEK + " TEXT, " +
                    COLUMN_TIMES_PER_DAY + " TEXT, " +
                    COLUMN_PROFILE_ID + " INTEGER DEFAULT 0" +
                    ")";

    public ReminderDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_TABLE_REMINDERS);
            android.util.Log.d("ReminderDatabaseHelper", "Database table created successfully");
        } catch (Exception e) {
            android.util.Log.e("ReminderDatabaseHelper", "Error creating database table: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle migrations from older versions to current version
        if (oldVersion < 2) {
            // Migration from version 1 to 2: Add new columns
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_CUSTOM_INTERVAL + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_DAYS_OF_WEEK + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_TIMES_PER_DAY + " TEXT");
            oldVersion = 2; // Update oldVersion for next migration step
        }

        if (oldVersion < 3) {
            // Migration from version 2 to 3: Ensure all columns exist (in case they were missing)
            // Check and add columns only if they don't exist
            try {
                db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_CUSTOM_INTERVAL + " INTEGER");
            } catch (Exception e) {
                // Column already exists, ignore
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_DAYS_OF_WEEK + " TEXT");
            } catch (Exception e) {
                // Column already exists, ignore
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_TIMES_PER_DAY + " TEXT");
            } catch (Exception e) {
                // Column already exists, ignore
            }
            oldVersion = 3; // Update for next migration step
        }

        if (oldVersion < 4) {
            // Migration from version 3 to 4: Add profile_id column and set default values
            try {
                // Check if column exists by trying to query it
                android.database.Cursor testCursor = db.rawQuery("PRAGMA table_info(" + TABLE_REMINDERS + ")", null);
                boolean columnExists = false;
                if (testCursor != null) {
                    while (testCursor.moveToNext()) {
                        String columnName = testCursor.getString(1); // Column name is at index 1
                        if (COLUMN_PROFILE_ID.equals(columnName)) {
                            columnExists = true;
                            break;
                        }
                    }
                    testCursor.close();
                }

                if (!columnExists) {
                    // Add profile_id column with default 0
                    db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_PROFILE_ID + " INTEGER DEFAULT 0");
                    android.util.Log.d("ReminderDatabaseHelper", "Added profile_id column");
                } else {
                    // Column exists but might have NULL values - update them to 0
                    db.execSQL("UPDATE " + TABLE_REMINDERS + " SET " + COLUMN_PROFILE_ID + " = 0 WHERE " + COLUMN_PROFILE_ID + " IS NULL");
                    android.util.Log.d("ReminderDatabaseHelper", "Updated NULL profile_id values to 0");
                }
            } catch (Exception e) {
                android.util.Log.e("ReminderDatabaseHelper", "Error adding/updating profile_id column: " + e.getMessage(), e);
                // Try to add column anyway - might fail if it exists, but that's okay
                try {
                    db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_PROFILE_ID + " INTEGER DEFAULT 0");
                } catch (Exception e2) {
                    // Column already exists, that's fine
                    android.util.Log.d("ReminderDatabaseHelper", "profile_id column already exists");
                }
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database downgrade (should rarely happen, but handle gracefully)
        // Instead of crashing, we'll keep the existing schema
        // This preserves user data even if version number goes backwards
        android.util.Log.w("ReminderDatabaseHelper",
                "Database downgrade detected from version " + oldVersion + " to " + newVersion +
                        ". Keeping existing schema to preserve data.");
        // Don't modify the database - just log the warning
    }

    public long insertReminder(ReminderModel reminder) {
        if (reminder == null) {
            android.util.Log.e("ReminderDatabaseHelper", "Cannot insert null reminder");
            return -1;
        }

        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();

            // Verify table exists
            if (!tableExists(db, TABLE_REMINDERS)) {
                android.util.Log.e("ReminderDatabaseHelper", "Table does not exist, recreating...");
                onCreate(db);
            }

            // Validate required fields
            if (reminder.getTaskName() == null || reminder.getTaskName().isEmpty()) {
                android.util.Log.e("ReminderDatabaseHelper", "Task name is null or empty");
                return -1;
            }
            if (reminder.getTime() == null || reminder.getTime().isEmpty()) {
                android.util.Log.e("ReminderDatabaseHelper", "Time is null or empty");
                return -1;
            }
            if (reminder.getFrequency() == null || reminder.getFrequency().isEmpty()) {
                android.util.Log.e("ReminderDatabaseHelper", "Frequency is null or empty");
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put(COLUMN_TASK_NAME, reminder.getTaskName());
            values.put(COLUMN_TIME, reminder.getTime());
            values.put(COLUMN_FREQUENCY, reminder.getFrequency());
            values.put(COLUMN_IS_ACTIVE, reminder.isActive() ? 1 : 0);

            // Set profile_id - use reminder's profileId if set, otherwise default to 0
            int profileId = (reminder.getProfileId() != null && reminder.getProfileId() > 0)
                    ? reminder.getProfileId() : 0;
            values.put(COLUMN_PROFILE_ID, profileId);

            // Optional fields - only add if they have values
            if (reminder.getCustomInterval() > 0) {
                values.put(COLUMN_CUSTOM_INTERVAL, reminder.getCustomInterval());
            }
            if (reminder.getDaysOfWeek() != null && !reminder.getDaysOfWeek().isEmpty()) {
                values.put(COLUMN_DAYS_OF_WEEK, reminder.getDaysOfWeek());
            }
            if (reminder.getTimesPerDay() != null && !reminder.getTimesPerDay().isEmpty()) {
                values.put(COLUMN_TIMES_PER_DAY, reminder.getTimesPerDay());
            }

            long result = db.insert(TABLE_REMINDERS, null, values);

            if (result == -1) {
                android.util.Log.e("ReminderDatabaseHelper", "Failed to insert reminder: " + reminder.getTaskName());
            } else {
                android.util.Log.d("ReminderDatabaseHelper", "Successfully inserted reminder with ID: " + result);
            }

            return result;
        } catch (Exception e) {
            android.util.Log.e("ReminderDatabaseHelper", "Error inserting reminder: " + e.getMessage(), e);
            return -1;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public List<ReminderModel> getAllReminders() {
        List<ReminderModel> reminders = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_REMINDERS, null, null, null, null, null, COLUMN_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                reminders.add(createReminderFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return reminders;
    }

    /**
     * Get all reminders (active and inactive) filtered by profile ID
     * @param profileId The profile ID to filter by. Use 0 for global reminders, or -1 to get all reminders
     * @return List of reminders for the specified profile (includes global reminders with profile_id = 0)
     */
    public List<ReminderModel> getAllRemindersByProfile(int profileId) {
        List<ReminderModel> reminders = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String selection;
        String[] selectionArgs;

        if (profileId > 0) {
            // Get reminders for specific profile OR global reminders (profile_id = 0)
            selection = COLUMN_PROFILE_ID + " = ? OR " + COLUMN_PROFILE_ID + " = 0";
            selectionArgs = new String[]{String.valueOf(profileId)};
        } else if (profileId == 0) {
            // Get only global reminders
            selection = COLUMN_PROFILE_ID + " = 0";
            selectionArgs = null;
        } else {
            // Get all reminders (profileId = -1)
            return getAllReminders();
        }

        Cursor cursor = db.query(TABLE_REMINDERS, null, selection, selectionArgs, null, null, COLUMN_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                reminders.add(createReminderFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return reminders;
    }

    public List<ReminderModel> getActiveReminders() {
        List<ReminderModel> reminders = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String selection = COLUMN_IS_ACTIVE + " = ?";
        String[] selectionArgs = {"1"};
        Cursor cursor = db.query(TABLE_REMINDERS, null, selection, selectionArgs, null, null, COLUMN_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                reminders.add(createReminderFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return reminders;
    }

    /**
     * Get active reminders filtered by profile ID
     * @param profileId The profile ID to filter by. Use 0 for global reminders, or -1 to get all reminders
     * @return List of reminders for the specified profile (includes global reminders with profile_id = 0)
     */
    public List<ReminderModel> getActiveRemindersByProfile(int profileId) {
        List<ReminderModel> reminders = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String selection;
        String[] selectionArgs;

        if (profileId > 0) {
            // Get reminders for specific profile OR global reminders (profile_id = 0)
            selection = COLUMN_IS_ACTIVE + " = ? AND (" + COLUMN_PROFILE_ID + " = ? OR " + COLUMN_PROFILE_ID + " = 0)";
            selectionArgs = new String[]{"1", String.valueOf(profileId)};
        } else if (profileId == 0) {
            // Get only global reminders
            selection = COLUMN_IS_ACTIVE + " = ? AND " + COLUMN_PROFILE_ID + " = 0";
            selectionArgs = new String[]{"1"};
        } else {
            // Get all active reminders (profileId = -1)
            return getActiveReminders();
        }

        Cursor cursor = db.query(TABLE_REMINDERS, null, selection, selectionArgs, null, null, COLUMN_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                reminders.add(createReminderFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return reminders;
    }

    public int updateReminder(ReminderModel reminder) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_NAME, reminder.getTaskName());
        values.put(COLUMN_TIME, reminder.getTime());
        values.put(COLUMN_FREQUENCY, reminder.getFrequency());
        values.put(COLUMN_IS_ACTIVE, reminder.isActive() ? 1 : 0);

        // Set profile_id - use reminder's profileId if set, otherwise default to 0
        int profileId = (reminder.getProfileId() != null && reminder.getProfileId() > 0)
                ? reminder.getProfileId() : 0;
        values.put(COLUMN_PROFILE_ID, profileId);

        if (reminder.getCustomInterval() > 0) {
            values.put(COLUMN_CUSTOM_INTERVAL, reminder.getCustomInterval());
        }
        if (reminder.getDaysOfWeek() != null) {
            values.put(COLUMN_DAYS_OF_WEEK, reminder.getDaysOfWeek());
        }
        if (reminder.getTimesPerDay() != null) {
            values.put(COLUMN_TIMES_PER_DAY, reminder.getTimesPerDay());
        }
        int result = db.update(TABLE_REMINDERS, values, COLUMN_ID + "=?", new String[]{String.valueOf(reminder.getId())});
        db.close();
        return result;
    }

    public int deleteReminder(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_REMINDERS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    public ReminderModel getReminderById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_REMINDERS, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);

        ReminderModel reminder = null;
        if (cursor.moveToFirst()) {
            reminder = createReminderFromCursor(cursor);
        }

        cursor.close();
        db.close();
        return reminder;
    }

    private ReminderModel createReminderFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String taskName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_NAME));
        String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME));
        String frequency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY));
        boolean isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1;

        ReminderModel reminder = new ReminderModel(id, taskName, time, frequency, isActive);

        // Read profile_id if it exists
        int profileIdCol = cursor.getColumnIndex(COLUMN_PROFILE_ID);
        if (profileIdCol >= 0 && !cursor.isNull(profileIdCol)) {
            reminder.setProfileId(cursor.getInt(profileIdCol));
        } else {
            reminder.setProfileId(0); // Default to 0 if not set
        }

        int customIntervalCol = cursor.getColumnIndex(COLUMN_CUSTOM_INTERVAL);
        if (customIntervalCol >= 0 && !cursor.isNull(customIntervalCol)) {
            reminder.setCustomInterval(cursor.getInt(customIntervalCol));
        }
        int daysOfWeekCol = cursor.getColumnIndex(COLUMN_DAYS_OF_WEEK);
        if (daysOfWeekCol >= 0 && !cursor.isNull(daysOfWeekCol)) {
            reminder.setDaysOfWeek(cursor.getString(daysOfWeekCol));
        }
        int timesPerDayCol = cursor.getColumnIndex(COLUMN_TIMES_PER_DAY);
        if (timesPerDayCol >= 0 && !cursor.isNull(timesPerDayCol)) {
            reminder.setTimesPerDay(cursor.getString(timesPerDayCol));
        }

        return reminder;
    }

    /**
     * Check if a table exists in the database
     */
    private boolean tableExists(SQLiteDatabase db, String tableName) {
        if (db == null || !db.isOpen()) {
            return false;
        }

        try {
            String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
            android.database.Cursor cursor = db.rawQuery(query, new String[]{tableName});
            boolean exists = cursor.getCount() > 0;
            cursor.close();
            return exists;
        } catch (Exception e) {
            android.util.Log.e("ReminderDatabaseHelper", "Error checking if table exists: " + e.getMessage(), e);
            return false;
        }
    }
}

