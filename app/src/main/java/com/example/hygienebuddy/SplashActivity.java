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

    private static final int SPLASH_DURATION = 3500; // total ~3.5s

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoImage = findViewById(R.id.logoImage);
        TextView appName = findViewById(R.id.appName);

        // Wait 1s, then slide logo to the left
        new Handler().postDelayed(() -> {
            TranslateAnimation slideLeft = new TranslateAnimation(0, -logoImage.getWidth(), 0, 0);
            slideLeft.setDuration(800);
            slideLeft.setFillAfter(true);
            logoImage.startAnimation(slideLeft);

            // Fade in app name
            new Handler().postDelayed(() -> {
                AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setDuration(800);
                appName.startAnimation(fadeIn);
                appName.setAlpha(1);
            }, 300);

        }, 1000);

        // Fade out app name, then decide next screen
        new Handler().postDelayed(() -> {
            AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(600);
            appName.startAnimation(fadeOut);
            appName.setAlpha(0);

            // After fade-out ends, check setup status
            new Handler().postDelayed(() -> {
                // Retrieve setup state (default: false = not set up)
                SharedPreferences prefs = getSharedPreferences("HygieneBuddyPrefs", MODE_PRIVATE);
                boolean isSetup = prefs.getBoolean("isSetup", false);

                Intent nextIntent;
                if (isSetup) {
                    nextIntent = new Intent(SplashActivity.this, OnboardingActivity.class); // replace with your dashboard
                } else {
                    nextIntent = new Intent(SplashActivity.this, OnboardingActivity.class); // your onboarding/setup
                }

                startActivity(nextIntent);
                finish();
            }, 600);

        }, SPLASH_DURATION);
    }
}
