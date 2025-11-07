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

        // Note: The NavHostFragment in activity_main.xml will automatically load
        // the start destination (homeDashboardFragment) from nav_graph.xml
        // So we don't need to manually load fragments here unless we're in setup mode

        // For setup fragments, we need to replace the NavHostFragment temporarily
        if (!facilitatorSetupCompleted) {
            // Show facilitator setup - replace the entire NavHostFragment
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, new FacilitatorSetupFragment())
                    .commit();
        } else if (!childSetupCompleted) {
            // Show child setup - replace the entire NavHostFragment
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, new ChildProfileSetupFragment())
                    .commit();
        }
        // else: NavHostFragment will automatically show homeDashboardFragment (start destination)
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
