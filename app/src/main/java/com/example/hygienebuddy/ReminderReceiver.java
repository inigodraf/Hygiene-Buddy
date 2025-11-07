package com.example.hygienebuddy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "reminder_channel";
    private static final String CHANNEL_NAME = "Hygiene Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || context == null) {
            android.util.Log.e("ReminderReceiver", "Received null intent or context");
            return;
        }

        String taskName = intent.getStringExtra("taskName");
        int reminderId = intent.getIntExtra("reminderId", -1);

        if (taskName == null || taskName.isEmpty()) {
            android.util.Log.e("ReminderReceiver", "Task name is null or empty");
            taskName = "Hygiene Task"; // Fallback
        }

        if (reminderId == -1) {
            android.util.Log.e("ReminderReceiver", "Invalid reminder ID");
            return;
        }

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create intent to open Task Steps screen
        Intent taskIntent = new Intent(context, MainActivity.class);
        taskIntent.putExtra("openTask", taskName.toLowerCase());
        taskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                reminderId,
                taskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle("Reminder: " + taskName)
                .setContentText("It's time to " + taskName.toLowerCase() + "!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(reminderId, builder.build());
        }

        // Handle one-time reminders - mark as inactive after firing
        String frequency = intent.getStringExtra("frequency");
        if ("once".equals(frequency)) {
            ReminderDatabaseHelper dbHelper = new ReminderDatabaseHelper(context);
            ReminderModel reminder = dbHelper.getReminderById(reminderId);
            if (reminder != null) {
                reminder.setActive(false);
                dbHelper.updateReminder(reminder);
            }
        }
        // Note: Daily, weekly, and custom repeating reminders are handled by AlarmManager.setRepeating()
    }
}
