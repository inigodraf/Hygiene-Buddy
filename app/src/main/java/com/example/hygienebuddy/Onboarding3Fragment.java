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

        Button btnNext = view.findViewById(R.id.btnGetStarted); // re-use same id, change text in XML to "Get Started"

        if (getActivity() instanceof OnboardingActivity) {
            OnboardingActivity host = (OnboardingActivity) getActivity();

            if (btnNext != null) {
                btnNext.setOnClickListener(v -> host.goToMain());
            }
        }

        return view;
    }
}
