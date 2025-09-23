package com.example.hygienebuddy;

import android.content.Intent;
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

        // ensure swiping allowed
        onboardingViewPager.setUserInputEnabled(true);

        // adapter
        onboardingAdapter = new OnboardingAdapter(this);
        onboardingViewPager.setAdapter(onboardingAdapter);

        // keep pages alive (optional)
        onboardingViewPager.setOffscreenPageLimit(onboardingAdapter.getItemCount());

        // attach dots
        new TabLayoutMediator(tabIndicator, onboardingViewPager,
                (tab, position) -> { /* no labels */ }
        ).attach();
    }

    /** Called by fragments to go to next page */
    public void goToNextPage() {
        if (onboardingAdapter == null) return;
        int current = onboardingViewPager.getCurrentItem();
        int total = onboardingAdapter.getItemCount();
        if (current < total - 1) {
            onboardingViewPager.setCurrentItem(current + 1, true);
        }
    }

    /** Called by fragments to jump to last page */
    public void skipToLastPage() {
        if (onboardingAdapter == null) return;
        onboardingViewPager.setCurrentItem(onboardingAdapter.getItemCount() - 1, true);
    }

    /** Called by the last fragment to start MainActivity */
    public void goToMain() {
        startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
        finish();
    }
}
