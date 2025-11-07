package com.example.hygienebuddy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.IOException;

public class ChildProfileSetupFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private ImageView ivChildAvatar;
    private ImageButton btnEditAvatar;
    private EditText etChildName;
    private AutoCompleteTextView actvAge;
    private MaterialCheckBox cbASD, cbADHD, cbDownSyndrome;
    private Button btnSaveContinue;
    private ImageView btnBack;

    private Uri selectedImageUri = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_child_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if setup is already completed (from SQLite)
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
        boolean setupCompleted = appDataDb.getBooleanSetting("child_setup_completed", false);
        if (setupCompleted) {
            // Skip setup and go directly to dashboard
            navigateToDashboard();
            return;
        }

        // Bind views
        ivChildAvatar = view.findViewById(R.id.ivChildAvatar);
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        etChildName = view.findViewById(R.id.etChildName);
        actvAge = view.findViewById(R.id.actvAge);
        cbASD = view.findViewById(R.id.cbASD);
        cbADHD = view.findViewById(R.id.cbADHD);
        cbDownSyndrome = view.findViewById(R.id.cbDownSyndrome);
        btnSaveContinue = view.findViewById(R.id.btnSaveContinue);
        btnBack = view.findViewById(R.id.btnBack);

        // Set default avatar
        ivChildAvatar.setImageResource(R.drawable.default_avatar);

        // Avatar edit click → open gallery
        btnEditAvatar.setOnClickListener(v -> openImageChooser());

        // Populate age dropdown (4–7 years old)
        String[] ages = {"4", "5", "6", "7"};
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ages
        );
        actvAge.setAdapter(ageAdapter);

        // Save & Continue button click
        btnSaveContinue.setOnClickListener(v -> saveChildProfile());

        // Back button → go back to previous setup screen
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof OnboardingActivity) {
                OnboardingActivity activity = (OnboardingActivity) getActivity();
                activity.goToPreviousSetupScreen();
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                ivChildAvatar.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveChildProfile() {
        String childName = etChildName.getText().toString().trim();
        String childAge = actvAge.getText().toString().trim();

        boolean hasASD = cbASD.isChecked();
        boolean hasADHD = cbADHD.isChecked();
        boolean hasDownSyndrome = cbDownSyndrome.isChecked();

        if (childName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your child's name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (childAge.isEmpty()) {
            Toast.makeText(requireContext(), "Please select your child's age", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build conditions string - use comma-separated format (consistent with ManageProfileFragment)
        StringBuilder conditionBuilder = new StringBuilder();
        if (hasASD) conditionBuilder.append("ASD, ");
        if (hasADHD) conditionBuilder.append("ADHD, ");
        if (hasDownSyndrome) conditionBuilder.append("Down Syndrome, ");
        String conditions = conditionBuilder.toString().trim();
        if (conditions.endsWith(",")) {
            conditions = conditions.substring(0, conditions.length() - 1).trim();
        }
        // If no conditions selected, use empty string (not "None")
        if (conditions.isEmpty()) {
            conditions = "";
        }

        android.util.Log.d("ChildProfileSetup", "Saving conditions: '" + conditions + "'");

        // Save to database so Manage Profile sees it
        UserProfileDatabaseHelper dbHelper = new UserProfileDatabaseHelper(requireContext());
        long result = dbHelper.insertProfile(childName, Integer.parseInt(childAge),
                selectedImageUri != null ? selectedImageUri.toString() : null, conditions);
        if (result != -1) {
            Toast.makeText(requireContext(), "Profile saved successfully!", Toast.LENGTH_SHORT).show();

            // Save profile data to SQLite app settings
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
            appDataDb.setIntSetting("current_profile_id", (int) result);
            appDataDb.setIntSetting("selected_profile_id", (int) result);
            appDataDb.setSetting("child_name", childName);
            appDataDb.setSetting("child_age", childAge);
            appDataDb.setSetting("child_conditions", conditions);
        } else {
            Toast.makeText(requireContext(), "Failed to save profile to database", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mark setup as completed in SQLite
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
        appDataDb.setBooleanSetting("child_setup_completed", true);

        // Confirmation
        Toast.makeText(requireContext(), "Child profile saved successfully!", Toast.LENGTH_SHORT).show();

        // Navigate to Dashboard
        navigateToDashboard();
    }

    private void navigateToDashboard() {
        // Navigate to MainActivity which will show the dashboard
        if (getActivity() instanceof OnboardingActivity) {
            OnboardingActivity activity = (OnboardingActivity) getActivity();
            activity.completeSetup();
        } else {
            // Fallback: navigate to MainActivity directly
            android.content.Intent intent = new android.content.Intent(requireActivity(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }
}
