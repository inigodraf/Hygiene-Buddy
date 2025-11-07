package com.example.hygienebuddy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class OnboardingActivity extends AppCompatActivity {

    ViewPager2 onboardingViewPager; // Package-private for FacilitatorSetupFragment access
    TabLayout tabIndicator; // Package-private for FacilitatorSetupFragment access
    OnboardingAdapter onboardingAdapter; // Package-private for FacilitatorSetupFragment access
    SetupAdapter setupAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if onboarding was already completed - if so, skip directly to MainActivity
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(this);
        boolean onboardingCompleted = appDataDb.getBooleanSetting("onboarding_completed", false);

        android.util.Log.d("OnboardingActivity", "onCreate - onboarding_completed flag: " + onboardingCompleted);

        if (onboardingCompleted) {
            // Onboarding already completed - skip and go directly to MainActivity
            android.util.Log.d("OnboardingActivity", "Onboarding already completed, redirecting to MainActivity");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        android.util.Log.d("OnboardingActivity", "Showing onboarding screens (first time user)");

        setContentView(R.layout.activity_onboarding);

        onboardingViewPager = findViewById(R.id.onboardingViewPager);
        tabIndicator = findViewById(R.id.tabIndicator);

        // Verify fragment_container exists after setting content view
        android.view.View testContainer = findViewById(R.id.fragment_container);
        if (testContainer != null) {
            android.util.Log.d("OnboardingActivity", "fragment_container found in onCreate - ID: " + testContainer.getId());
        } else {
            android.util.Log.e("OnboardingActivity", "fragment_container NOT found in onCreate!");
        }

        onboardingViewPager.setUserInputEnabled(true);

        onboardingAdapter = new OnboardingAdapter(this);
        onboardingViewPager.setAdapter(onboardingAdapter);
        onboardingViewPager.setOffscreenPageLimit(onboardingAdapter.getItemCount());

        new TabLayoutMediator(tabIndicator, onboardingViewPager,
                (tab, position) -> { /* no labels */ }).attach();
    }

    /** Called by the last onboarding fragment */
    public void completeOnboarding() {
        // Mark onboarding completed in SQLite
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(this);
        appDataDb.setBooleanSetting("onboarding_completed", true);

        android.util.Log.d("OnboardingActivity", "completeOnboarding called - flag saved to SQLite");

        // Verify the flag was saved
        boolean verifyFlag = appDataDb.getBooleanSetting("onboarding_completed", false);
        android.util.Log.d("OnboardingActivity", "Verification - onboarding_completed flag after save: " + verifyFlag);

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

    /** Show facilitator setup fragment - called from Onboarding3Fragment
     * Uses the same ViewPager2 approach as onboarding for consistent navigation */
    public void showFacilitatorSetup() {
        try {
            android.util.Log.d("OnboardingActivity", "showFacilitatorSetup called - switching to setup adapter");

            // Mark onboarding as completed when transitioning to setup (in SQLite)
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(this);
            appDataDb.setBooleanSetting("onboarding_completed", true);
            android.util.Log.d("OnboardingActivity", "Onboarding marked as completed in SQLite");

            // Hide tab indicator (setup screens don't need indicators)
            if (tabIndicator != null) {
                tabIndicator.setVisibility(android.view.View.GONE);
                android.util.Log.d("OnboardingActivity", "TabIndicator hidden");
            }

            // Switch ViewPager2 to use SetupAdapter (same pattern as onboarding)
            if (onboardingViewPager != null) {
                // Create setup adapter
                setupAdapter = new SetupAdapter(this);

                // Replace the adapter - this will show FacilitatorSetupFragment as the first page
                onboardingViewPager.setAdapter(setupAdapter);
                onboardingViewPager.setCurrentItem(0, false); // Go to first setup screen (Facilitator)
                onboardingViewPager.setUserInputEnabled(false); // Disable swiping for setup screens

                android.util.Log.d("OnboardingActivity", "ViewPager2 switched to SetupAdapter - showing FacilitatorSetupFragment");
            } else {
                android.util.Log.e("OnboardingActivity", "onboardingViewPager is null!");
                android.widget.Toast.makeText(this, "Error: ViewPager not available", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("OnboardingActivity", "Error showing facilitator setup: " + e.getMessage(), e);
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /** Navigate to next setup screen (e.g., from Facilitator to Child Profile) */
    public void goToNextSetupScreen() {
        if (onboardingViewPager != null && setupAdapter != null) {
            int current = onboardingViewPager.getCurrentItem();
            if (current < setupAdapter.getItemCount() - 1) {
                onboardingViewPager.setCurrentItem(current + 1, true);
                android.util.Log.d("OnboardingActivity", "Navigated to setup screen: " + (current + 1));
            }
        }
    }

    /** Navigate to previous setup screen */
    public void goToPreviousSetupScreen() {
        if (onboardingViewPager != null) {
            int current = onboardingViewPager.getCurrentItem();
            if (current > 0) {
                onboardingViewPager.setCurrentItem(current - 1, true);
                android.util.Log.d("OnboardingActivity", "Navigated back to setup screen: " + (current - 1));
            }
        }
    }

    /** Complete setup and navigate to MainActivity */
    public void completeSetup() {
        android.util.Log.d("OnboardingActivity", "Setup completed - navigating to MainActivity");
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}