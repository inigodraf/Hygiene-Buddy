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
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_REMINDERS = "reminders";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TASK_NAME = "task_name";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_FREQUENCY = "frequency";
    private static final String COLUMN_IS_ACTIVE = "is_active";
    private static final String COLUMN_CUSTOM_INTERVAL = "custom_interval";
    private static final String COLUMN_DAYS_OF_WEEK = "days_of_week";
    private static final String COLUMN_TIMES_PER_DAY = "times_per_day";

    private static final String CREATE_TABLE_REMINDERS =
            "CREATE TABLE " + TABLE_REMINDERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TASK_NAME + " TEXT NOT NULL, " +
                    COLUMN_TIME + " TEXT NOT NULL, " +
                    COLUMN_FREQUENCY + " TEXT NOT NULL, " +
                    COLUMN_IS_ACTIVE + " INTEGER NOT NULL, " +
                    COLUMN_CUSTOM_INTERVAL + " INTEGER, " +
                    COLUMN_DAYS_OF_WEEK + " TEXT, " +
                    COLUMN_TIMES_PER_DAY + " TEXT" +
                    ")";

    public ReminderDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_REMINDERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_CUSTOM_INTERVAL + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_DAYS_OF_WEEK + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN " + COLUMN_TIMES_PER_DAY + " TEXT");
        }
    }

    public long insertReminder(ReminderModel reminder) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_NAME, reminder.getTaskName());
        values.put(COLUMN_TIME, reminder.getTime());
        values.put(COLUMN_FREQUENCY, reminder.getFrequency());
        values.put(COLUMN_IS_ACTIVE, reminder.isActive() ? 1 : 0);
        if (reminder.getCustomInterval() > 0) {
            values.put(COLUMN_CUSTOM_INTERVAL, reminder.getCustomInterval());
        }
        if (reminder.getDaysOfWeek() != null) {
            values.put(COLUMN_DAYS_OF_WEEK, reminder.getDaysOfWeek());
        }
        if (reminder.getTimesPerDay() != null) {
            values.put(COLUMN_TIMES_PER_DAY, reminder.getTimesPerDay());
        }
        long result = db.insert(TABLE_REMINDERS, null, values);
        db.close();
        return result;
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

    public int updateReminder(ReminderModel reminder) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_NAME, reminder.getTaskName());
        values.put(COLUMN_TIME, reminder.getTime());
        values.put(COLUMN_FREQUENCY, reminder.getFrequency());
        values.put(COLUMN_IS_ACTIVE, reminder.isActive() ? 1 : 0);
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
}
