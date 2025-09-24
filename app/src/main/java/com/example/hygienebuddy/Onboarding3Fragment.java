package com.example.hygienebuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
            // Hide onboarding UI
            requireActivity().findViewById(R.id.onboardingViewPager).setVisibility(View.GONE);
            requireActivity().findViewById(R.id.tabIndicator).setVisibility(View.GONE);

            // Show setup container
            View setupContainer = requireActivity().findViewById(R.id.fragment_container);
            setupContainer.setVisibility(View.VISIBLE);

            // Navigate to FacilitatorSetupFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new FacilitatorSetupFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}
