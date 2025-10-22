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

        Intent nextIntent;
        if (!onboardingCompleted) {
            nextIntent = new Intent(this, OnboardingActivity.class);
        } else {
            nextIntent = new Intent(this, MainActivity.class);
        }

        startActivity(nextIntent);
        finish();
    }
}
