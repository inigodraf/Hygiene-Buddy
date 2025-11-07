package com.example.hygienebuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
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

        // Ensure onboarding is not shown - verify the flag (from SQLite)
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(this);
        boolean onboardingCompleted = appDataDb.getBooleanSetting("onboarding_completed", false);
        android.util.Log.d("MainActivity", "onCreate - onboarding_completed: " + onboardingCompleted);

        // Only set up fragments on first creation (not on configuration changes or activity recreation)
        // The NavHostFragment will handle fragment restoration automatically
        // The navigation graph's startDestination is homeDashboardFragment, so it will show the dashboard
        if (savedInstanceState == null) {
            boolean facilitatorSetupCompleted = appDataDb.getBooleanSetting("facilitator_setup_completed", false);
            boolean childSetupCompleted = appDataDb.getBooleanSetting("child_setup_completed", false);

            android.util.Log.d("MainActivity", "Setup status - Facilitator: " + facilitatorSetupCompleted + ", Child: " + childSetupCompleted);

            // The NavHostFragment will automatically show homeDashboardFragment as the start destination
            // Setup fragments (if needed) will be handled by their respective fragments checking the flags
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
