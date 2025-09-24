package com.example.hygienebuddy;

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

        // Find views
        actvRelationship = view.findViewById(R.id.actvRelationship);
        etFacilitatorName = view.findViewById(R.id.etFacilitatorName);
        btnNext = view.findViewById(R.id.btnNext);
        btnBack = view.findViewById(R.id.btnBack);

        // Dropdown options
        String[] relationships = {"Parent", "Teacher"};

        // Attach adapter to AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                relationships
        );
        actvRelationship.setAdapter(adapter);

        // Back button click → goes to Onboarding3Fragment (or last fragment in back stack)
        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Next button click → goes to ChildSetupFragment
        btnNext.setOnClickListener(v -> {
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

            // Navigate to ChildSetupFragment
            ChildProfileSetupFragment childSetupFragment = new ChildProfileSetupFragment();

            // Pass data via Bundle
            Bundle bundle = new Bundle();
            bundle.putString("facilitatorName", name);
            bundle.putString("relationship", relationship);
            childSetupFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, childSetupFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
}
