package com.example.hygienebuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
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

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean facilitatorSetupCompleted = prefs.getBoolean("facilitator_setup_completed", false);
        boolean childSetupCompleted = prefs.getBoolean("child_setup_completed", false);

        if (!facilitatorSetupCompleted) {
            // Show facilitator setup
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new FacilitatorSetupFragment())
                    .commit();
        } else if (!childSetupCompleted) {
            // Show child setup
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChildProfileSetupFragment())
                    .commit();
        } else {
            // All setup done → dashboard
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        }
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
