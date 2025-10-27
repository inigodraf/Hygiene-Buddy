package com.example.hygienebuddy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;

public class ReminderManager {

    public static void scheduleReminder(Context context, int reminderId, String taskName, String time, String frequency) {
        ReminderDatabaseHelper dbHelper = new ReminderDatabaseHelper(context);
        ReminderModel reminder = dbHelper.getReminderById(reminderId);
        if (reminder == null) return;

        scheduleReminder(context, reminder);
    }

    public static void scheduleReminder(Context context, ReminderModel reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String frequency = reminder.getFrequency();
        String time = reminder.getTime();

        // Handle multiple times per day
        if ("multiple".equals(frequency) && reminder.getTimesPerDay() != null) {
            String[] times = reminder.getTimesPerDay().split(",");
            for (int i = 0; i < times.length; i++) {
                String singleTime = times[i].trim();
                scheduleSingleReminder(context, reminder, singleTime, i);
            }
            return;
        }

        // Handle weekly reminders
        if ("weekly".equals(frequency) && reminder.getDaysOfWeek() != null) {
            String[] days = reminder.getDaysOfWeek().split(",");
            for (String day : days) {
                scheduleWeeklyReminder(context, reminder, day.trim(), time);
            }
            return;
        }

        // Handle other frequencies (once, daily, custom)
        scheduleSingleReminder(context, reminder, time, 0);
    }

    private static void scheduleSingleReminder(Context context, ReminderModel reminder, String time, int instance) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("taskName", reminder.getTaskName());
        intent.putExtra("reminderId", reminder.getId());
        intent.putExtra("time", time);
        intent.putExtra("frequency", reminder.getFrequency());

        int requestCode = reminder.getId() * 1000 + instance; // Unique request code
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        String frequency = reminder.getFrequency();

        // If time has passed today, schedule for tomorrow (or next occurrence)
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            if ("daily".equals(frequency)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            } else if ("custom".equals(frequency)) {
                calendar.add(Calendar.DAY_OF_YEAR, reminder.getCustomInterval());
            } else if ("once".equals(frequency)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        // Schedule based on frequency
        if ("daily".equals(frequency)) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else if ("custom".equals(frequency) && reminder.getCustomInterval() > 0) {
            long intervalMillis = reminder.getCustomInterval() * 24 * 60 * 60 * 1000L;
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    intervalMillis,
                    pendingIntent
            );
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private static void scheduleWeeklyReminder(Context context, ReminderModel reminder, String dayOfWeek, String time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("taskName", reminder.getTaskName());
        intent.putExtra("reminderId", reminder.getId());
        intent.putExtra("time", time);
        intent.putExtra("frequency", "weekly");
        intent.putExtra("dayOfWeek", dayOfWeek);

        int dayCode = getDayOfWeekCode(dayOfWeek);
        int requestCode = reminder.getId() * 1000 + dayCode; // Unique request code for each day
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        int targetDay = getCalendarDayOfWeek(dayOfWeek);

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, targetDay);

        // If this week's occurrence has passed, schedule for next week
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_WEEK, 7);
        }

        // Schedule weekly repeating
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
        );
    }

    private static int getCalendarDayOfWeek(String day) {
        switch (day.toLowerCase()) {
            case "sun": return Calendar.SUNDAY;
            case "mon": return Calendar.MONDAY;
            case "tue": return Calendar.TUESDAY;
            case "wed": return Calendar.WEDNESDAY;
            case "thu": return Calendar.THURSDAY;
            case "fri": return Calendar.FRIDAY;
            case "sat": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

    private static int getDayOfWeekCode(String day) {
        switch (day.toLowerCase()) {
            case "sun": return 0;
            case "mon": return 1;
            case "tue": return 2;
            case "wed": return 3;
            case "thu": return 4;
            case "fri": return 5;
            case "sat": return 6;
            default: return 1;
        }
    }

    public static void cancelReminder(Context context, int reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        ReminderDatabaseHelper dbHelper = new ReminderDatabaseHelper(context);
        ReminderModel reminder = dbHelper.getReminderById(reminderId);
        if (reminder == null) return;

        String frequency = reminder.getFrequency();

        // Cancel multiple reminders if this was a multiple-times-per-day reminder
        if ("multiple".equals(frequency) && reminder.getTimesPerDay() != null) {
            String[] times = reminder.getTimesPerDay().split(",");
            for (int i = 0; i < times.length; i++) {
                cancelSingleReminder(alarmManager, context, reminderId, i);
            }
            return;
        }

        // Cancel weekly reminders for all days
        if ("weekly".equals(frequency) && reminder.getDaysOfWeek() != null) {
            String[] days = reminder.getDaysOfWeek().split(",");
            for (String day : days) {
                cancelWeeklyReminder(alarmManager, context, reminderId, day.trim());
            }
            return;
        }

        // Cancel single reminder
        cancelSingleReminder(alarmManager, context, reminderId, 0);
    }

    private static void cancelSingleReminder(AlarmManager alarmManager, Context context, int reminderId, int instance) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        int requestCode = reminderId * 1000 + instance;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    private static void cancelWeeklyReminder(AlarmManager alarmManager, Context context, int reminderId, String day) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        int dayCode = getDayOfWeekCode(day);
        int requestCode = reminderId * 1000 + dayCode;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    public static void rescheduleAllReminders(Context context) {
        ReminderDatabaseHelper dbHelper = new ReminderDatabaseHelper(context);
        List<ReminderModel> reminders = dbHelper.getActiveReminders();

        for (ReminderModel reminder : reminders) {
            scheduleReminder(context, reminder.getId(), reminder.getTaskName(), reminder.getTime(), reminder.getFrequency());
        }
    }
}
