package com.example.hygienebuddy;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SetupAdapter extends FragmentStateAdapter {

    public SetupAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new FacilitatorSetupFragment();
            case 1: return new ChildProfileSetupFragment();
            default: return new FacilitatorSetupFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Facilitator setup and Child profile setup
    }
}

