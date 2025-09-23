package com.example.hygienebuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Onboarding1Fragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding1, container, false);

        Button btnNext = view.findViewById(R.id.btnNext);
        TextView btnSkip = view.findViewById(R.id.btnSkip);

        if (getActivity() instanceof OnboardingActivity) {
            OnboardingActivity host = (OnboardingActivity) getActivity();

            if (btnNext != null) {
                btnNext.setOnClickListener(v -> host.goToNextPage());
            }

            if (btnSkip != null) {
                btnSkip.setOnClickListener(v -> host.skipToLastPage());
            }
        }

        return view;
    }
}
