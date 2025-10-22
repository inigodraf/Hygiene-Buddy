package com.example.hygienebuddy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 onboardingViewPager;
    private TabLayout tabIndicator;
    private OnboardingAdapter onboardingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        onboardingViewPager = findViewById(R.id.onboardingViewPager);
        tabIndicator = findViewById(R.id.tabIndicator);

        onboardingViewPager.setUserInputEnabled(true);

        onboardingAdapter = new OnboardingAdapter(this);
        onboardingViewPager.setAdapter(onboardingAdapter);
        onboardingViewPager.setOffscreenPageLimit(onboardingAdapter.getItemCount());

        new TabLayoutMediator(tabIndicator, onboardingViewPager,
                (tab, position) -> { /* no labels */ }).attach();
    }

    /** Called by the last onboarding fragment */
    public void completeOnboarding() {
        // Mark onboarding completed
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_completed", true).apply();

        // Navigate to facilitator setup
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /** Called by fragments to jump to last page */
    public void skipToLastPage() {
        if (onboardingAdapter == null) return;
        onboardingViewPager.setCurrentItem(onboardingAdapter.getItemCount() - 1, true);
    }

    /** move to next page programmatically */
    public void goToNextPage() {
        int current = onboardingViewPager.getCurrentItem();
        if (current < onboardingAdapter.getItemCount() - 1) {
            onboardingViewPager.setCurrentItem(current + 1, true);
        }
    }
}