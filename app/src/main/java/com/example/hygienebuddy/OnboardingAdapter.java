package com.example.hygienebuddy;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingAdapter extends FragmentStateAdapter {

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new Onboarding1Fragment();
            case 1: return new Onboarding2Fragment();
            case 2: return new Onboarding3Fragment();
            default: return new Onboarding1Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
