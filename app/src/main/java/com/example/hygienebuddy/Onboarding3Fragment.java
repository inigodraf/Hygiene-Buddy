package com.example.hygienebuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Onboarding3Fragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding3, container, false);

        Button btnGetStarted = view.findViewById(R.id.btnGetStarted);
        btnGetStarted.setOnClickListener(v -> {
            try {
                if (!isAdded() || getActivity() == null) {
                    android.util.Log.e("Onboarding3Fragment", "Fragment not attached to activity");
                    return;
                }

                // Use the activity's helper method to show facilitator setup
                if (getActivity() instanceof OnboardingActivity) {
                    OnboardingActivity activity = (OnboardingActivity) getActivity();
                    activity.showFacilitatorSetup();
                    android.util.Log.d("Onboarding3Fragment", "Called showFacilitatorSetup on activity");
                } else {
                    android.util.Log.e("Onboarding3Fragment", "Activity is not OnboardingActivity");
                    Toast.makeText(getContext(), "Error: Invalid activity", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                android.util.Log.e("Onboarding3Fragment", "Error navigating to FacilitatorSetupFragment: " + e.getMessage(), e);
                e.printStackTrace();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }
}
