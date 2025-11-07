package com.example.hygienebuddy;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FacilitatorSetupFragment extends Fragment {

    private AutoCompleteTextView actvRelationship;
    private EditText etFacilitatorName;
    private Button btnNext;
    private ImageView btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_facilitator_setup, container, false);
        } catch (Exception e) {
            android.util.Log.e("FacilitatorSetupFragment", "Error inflating layout: " + e.getMessage(), e);
            e.printStackTrace();
            // Return a simple error view if layout fails
            android.widget.TextView errorView = new android.widget.TextView(getContext());
            errorView.setText("Error loading setup screen. Please restart the app.");
            errorView.setPadding(50, 50, 50, 50);
            return errorView;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            if (!isAdded() || getActivity() == null) {
                android.util.Log.e("FacilitatorSetupFragment", "Fragment not attached to activity");
                return;
            }

            // Check if facilitator setup is already completed (from SQLite)
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
            boolean facilitatorSetupCompleted = appDataDb.getBooleanSetting("facilitator_setup_completed", false);

            if (facilitatorSetupCompleted) {
                navigateAfterFacilitatorSetup();
                return;
            }

            // Bind views with null checks
            actvRelationship = view.findViewById(R.id.actvRelationship);
            etFacilitatorName = view.findViewById(R.id.etFacilitatorName);
            btnNext = view.findViewById(R.id.btnNext);
            btnBack = view.findViewById(R.id.btnBack);

            if (actvRelationship == null || etFacilitatorName == null || btnNext == null || btnBack == null) {
                android.util.Log.e("FacilitatorSetupFragment", "One or more views are null");
                Toast.makeText(getContext(), "Error: Setup screen layout issue", Toast.LENGTH_SHORT).show();
                return;
            }

            // Dropdown options
            String[] relationships = {"Parent", "Teacher"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    relationships
            );
            actvRelationship.setAdapter(adapter);

            // Back button → go back to onboarding if needed
            btnBack.setOnClickListener(v -> {
                try {
                    if (getActivity() instanceof OnboardingActivity) {
                        OnboardingActivity activity = (OnboardingActivity) getActivity();
                        // Switch back to onboarding adapter
                        activity.onboardingViewPager.setAdapter(activity.onboardingAdapter);
                        activity.onboardingViewPager.setCurrentItem(activity.onboardingAdapter.getItemCount() - 1, true); // Go to last onboarding screen
                        activity.onboardingViewPager.setUserInputEnabled(true);
                        if (activity.tabIndicator != null) {
                            activity.tabIndicator.setVisibility(android.view.View.VISIBLE);
                        }
                        android.util.Log.d("FacilitatorSetupFragment", "Switched back to onboarding");
                    }
                } catch (Exception e) {
                    android.util.Log.e("FacilitatorSetupFragment", "Error on back button: " + e.getMessage(), e);
                }
            });

            // Next button → save facilitator setup and go to child setup
            btnNext.setOnClickListener(v -> saveFacilitatorSetup());

            android.util.Log.d("FacilitatorSetupFragment", "Fragment view created successfully");
        } catch (Exception e) {
            android.util.Log.e("FacilitatorSetupFragment", "Error in onViewCreated: " + e.getMessage(), e);
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error loading setup screen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveFacilitatorSetup() {
        String name = etFacilitatorName.getText().toString().trim();
        String relationship = actvRelationship.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (relationship.isEmpty()) {
            Toast.makeText(requireContext(), "Please select your relationship to the child", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save facilitator info to SQLite
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
        appDataDb.setBooleanSetting("facilitator_setup_completed", true);
        appDataDb.setSetting("facilitator_name", name);
        appDataDb.setSetting("facilitator_relationship", relationship);

        navigateAfterFacilitatorSetup();
    }

    private void navigateAfterFacilitatorSetup() {
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
        boolean childSetupCompleted = appDataDb.getBooleanSetting("child_setup_completed", false);

        if (!childSetupCompleted) {
            // Navigate to next setup screen (Child Profile) using ViewPager2
            if (getActivity() instanceof OnboardingActivity) {
                OnboardingActivity activity = (OnboardingActivity) getActivity();
                activity.goToNextSetupScreen();
                android.util.Log.d("FacilitatorSetupFragment", "Navigated to Child Profile setup");
            }
        } else {
            // Already completed → go to MainActivity
            if (getActivity() instanceof OnboardingActivity) {
                OnboardingActivity activity = (OnboardingActivity) getActivity();
                activity.completeSetup();
            }
        }
    }
}
