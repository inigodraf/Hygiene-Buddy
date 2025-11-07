package com.example.hygienebuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create notification channel for reminders
        createNotificationChannel();

        // Only set up fragments on first creation (not on configuration changes or activity recreation)
        // The NavHostFragment will handle fragment restoration automatically
        if (savedInstanceState == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean facilitatorSetupCompleted = prefs.getBoolean("facilitator_setup_completed", false);
            boolean childSetupCompleted = prefs.getBoolean("child_setup_completed", false);

            // Check if we need to navigate to setup fragments
            // Note: The NavHostFragment handles navigation, so we should use NavController
            // But for now, if setup is not completed, the navigation graph should handle it
            // This logic is kept for backward compatibility
            if (!facilitatorSetupCompleted || !childSetupCompleted) {
                // Setup fragments will be shown via navigation graph
                // The NavHostFragment will handle the initial destination
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Activity won't be recreated, so fragments will automatically adapt to new orientation
        // No additional action needed - fragments handle their own lifecycle
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    "Hygiene Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for hygiene task reminders");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
