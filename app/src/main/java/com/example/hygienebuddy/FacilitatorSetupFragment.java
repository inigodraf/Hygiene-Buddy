package com.example.hygienebuddy;

import android.content.Context;
import android.content.SharedPreferences;
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
        return inflater.inflate(R.layout.fragment_facilitator_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if facilitator setup is already completed
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean facilitatorSetupCompleted = prefs.getBoolean("facilitator_setup_completed", false);

        if (facilitatorSetupCompleted) {
            navigateAfterFacilitatorSetup();
            return;
        }

        // Bind views
        actvRelationship = view.findViewById(R.id.actvRelationship);
        etFacilitatorName = view.findViewById(R.id.etFacilitatorName);
        btnNext = view.findViewById(R.id.btnNext);
        btnBack = view.findViewById(R.id.btnBack);

        // Dropdown options
        String[] relationships = {"Parent", "Teacher"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                relationships
        );
        actvRelationship.setAdapter(adapter);

        // Back button → go back to onboarding if needed
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Next button → save facilitator setup and go to child setup
        btnNext.setOnClickListener(v -> saveFacilitatorSetup());
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

        // Save facilitator info here if needed (SharedPreferences or DB)
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("facilitator_setup_completed", true)
                .putString("facilitator_name", name)
                .putString("facilitator_relationship", relationship)
                .apply();

        navigateAfterFacilitatorSetup();
    }

    private void navigateAfterFacilitatorSetup() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean childSetupCompleted = prefs.getBoolean("child_setup_completed", false);

        if (!childSetupCompleted) {
            // Go to ChildProfileSetupFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChildProfileSetupFragment())
                    .commit();
        } else {
            // Already completed → go to Dashboard
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        }
    }
}
