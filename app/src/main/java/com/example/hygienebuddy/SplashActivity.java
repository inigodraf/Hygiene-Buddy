package com.example.hygienebuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3500; // ~3.5s

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoImage = findViewById(R.id.logoImage);
        TextView appName = findViewById(R.id.appName);

        // Slide logo after 1s
        new Handler().postDelayed(() -> {
            TranslateAnimation slideLeft = new TranslateAnimation(0, -logoImage.getWidth(), 0, 0);
            slideLeft.setDuration(800);
            slideLeft.setFillAfter(true);
            logoImage.startAnimation(slideLeft);

            new Handler().postDelayed(() -> {
                AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setDuration(800);
                appName.startAnimation(fadeIn);
                appName.setAlpha(1);
            }, 300);

        }, 1000);

        // After splash, navigate properly
        new Handler().postDelayed(this::navigateNext, SPLASH_DURATION);
    }

    private void navigateNext() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);

        // Log the onboarding status for debugging
        android.util.Log.d("SplashActivity", "onboarding_completed flag: " + onboardingCompleted);

        // Also check if setup is completed to ensure we go directly to dashboard
        boolean facilitatorSetupCompleted = prefs.getBoolean("facilitator_setup_completed", false);
        boolean childSetupCompleted = prefs.getBoolean("child_setup_completed", false);
        boolean setupCompleted = facilitatorSetupCompleted && childSetupCompleted;

        android.util.Log.d("SplashActivity", "Setup status - Facilitator: " + facilitatorSetupCompleted + ", Child: " + childSetupCompleted);

        Intent nextIntent;
        // Only show onboarding if it hasn't been completed yet
        // Fallback: If setup is completed, skip onboarding even if flag is missing (safety check)
        // Once onboarding is completed OR setup is completed, always go to MainActivity
        if (!onboardingCompleted && !setupCompleted) {
            android.util.Log.d("SplashActivity", "Navigating to OnboardingActivity (first time user)");
            nextIntent = new Intent(this, OnboardingActivity.class);
        } else {
            // Onboarding completed OR setup completed - go directly to MainActivity
            // MainActivity will handle showing dashboard or setup screens based on setup completion status
            if (onboardingCompleted) {
                android.util.Log.d("SplashActivity", "Navigating to MainActivity (onboarding already completed)");
            } else {
                android.util.Log.d("SplashActivity", "Navigating to MainActivity (setup completed, skipping onboarding)");
            }
            nextIntent = new Intent(this, MainActivity.class);
        }

        startActivity(nextIntent);
        finish();
    }
}
