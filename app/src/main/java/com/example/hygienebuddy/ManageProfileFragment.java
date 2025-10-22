package com.example.hygienebuddy;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

/**
 * Fragment for managing user profiles with CRUD operations.
 */
public class ManageProfileFragment extends Fragment implements ManageProfileAdapter.OnProfileActionListener {

    private RecyclerView rvProfiles;
    private LinearLayout layoutEmptyState;
    private FloatingActionButton fabAddProfile;
    private ManageProfileAdapter adapter;
    private UserProfileDatabaseHelper databaseHelper;
    private List<UserProfile> profiles;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String selectedImageUri = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvProfiles = view.findViewById(R.id.rvProfiles);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        fabAddProfile = view.findViewById(R.id.fabAddProfile);

        databaseHelper = new UserProfileDatabaseHelper(requireContext());

        setupRecyclerView();
        setupImagePicker();

        fabAddProfile.setOnClickListener(v -> showProfileDialog(null));

        ShapeableImageView btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack(); // goes back to the previous fragment
        });

        loadProfiles();

        view.post(() -> BottomNavHelper.setupBottomNav(this, "settings"));
    }

    private void setupRecyclerView() {
        adapter = new ManageProfileAdapter(requireContext(), this);
        rvProfiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProfiles.setAdapter(adapter);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) selectedImageUri = imageUri.toString();
                    }
                });
    }

    private void loadProfiles() {
        profiles = databaseHelper.getAllProfiles();
        adapter.setProfiles(profiles);
        if (profiles.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvProfiles.setVisibility(View.GONE); }
        else { layoutEmptyState.setVisibility(View.GONE);
            rvProfiles.setVisibility(View.VISIBLE); }
    }
    private void showProfileDialog(@Nullable UserProfile profile) {
        selectedImageUri = (profile != null) ? profile.getImageUri() : null;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_profile, null);

        ImageView ivProfileImage = dialogView.findViewById(R.id.ivProfileImage);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilName);
        TextInputLayout tilAge = dialogView.findViewById(R.id.tilAge);
        CheckBox cbASD = dialogView.findViewById(R.id.cbASD);
        CheckBox cbADHD = dialogView.findViewById(R.id.cbADHD);
        CheckBox cbDownSyndrome = dialogView.findViewById(R.id.cbDownSyndrome);
        AutoCompleteTextView actvAge = dialogView.findViewById(R.id.actvAge);

        EditText etName = tilName.getEditText();

        // Populate age dropdown 4-7
        String[] ages = {"4", "5", "6", "7"};
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ages
        );
        actvAge.setAdapter(ageAdapter);

        // Pre-fill if editing
        if (profile != null) {
            etName.setText(profile.getName());
            actvAge.setText(String.valueOf(profile.getAge()), false);

            String conditionText = profile.getCondition() != null ? profile.getCondition().toLowerCase() : "";
            cbASD.setChecked(conditionText.contains("asd"));
            cbADHD.setChecked(conditionText.contains("adhd"));
            cbDownSyndrome.setChecked(conditionText.contains("down"));

            if (profile.hasImage()) {
                try {
                    ivProfileImage.setImageURI(Uri.parse(profile.getImageUri()));
                } catch (Exception e) {
                    ivProfileImage.setImageResource(R.drawable.default_avatar);
                }
            }
        }

        ivProfileImage.setOnClickListener(v -> pickImageFromGallery());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(profile != null ? "Edit Profile" : "Add New Profile")
                .setView(dialogView)
                .setPositiveButton(profile != null ? "Update" : "Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String ageText = actvAge.getText().toString().trim();

                    if (!validateInput(name, ageText)) return;

                    int age = Integer.parseInt(ageText);
                    StringBuilder conditionBuilder = new StringBuilder();
                    if (cbASD.isChecked()) conditionBuilder.append("ASD, ");
                    if (cbADHD.isChecked()) conditionBuilder.append("ADHD, ");
                    if (cbDownSyndrome.isChecked()) conditionBuilder.append("Down Syndrome, ");
                    String conditions = conditionBuilder.toString().trim();
                    if (conditions.endsWith(",")) {
                        conditions = conditions.substring(0, conditions.length() - 1);
                    }

                    if (profile != null) {
                        updateProfile(profile.getId(), name, age, selectedImageUri, conditions);
                    } else {
                        addProfile(name, age, selectedImageUri, conditions);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean validateInput(String name, String ageText) {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ageText.isEmpty() || !Arrays.asList("4", "5", "6", "7").contains(ageText)) {
            Toast.makeText(requireContext(), "Please select an age between 4–7", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void addProfile(String name, int age, String imageUri, String conditions) {
        long result = databaseHelper.insertProfile(name, age, imageUri, conditions);
        if (result != -1) {
            Toast.makeText(requireContext(), "Profile added successfully!", Toast.LENGTH_SHORT).show();
            loadProfiles();
        } else {
            Toast.makeText(requireContext(), "Failed to add profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfile(int id, String name, int age, String imageUri, String conditions) {
        int result = databaseHelper.updateProfile(id, name, age, imageUri, conditions);
        if (result > 0) {
            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            loadProfiles();
        } else {
            Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEditProfile(UserProfile profile) {
        showProfileDialog(profile);
    }

    @Override
    public void onDeleteProfile(UserProfile profile) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete " + profile.getName() + "'s profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int result = databaseHelper.deleteProfile(profile.getId());
                    if (result > 0) {
                        Toast.makeText(requireContext(), "Profile deleted successfully!", Toast.LENGTH_SHORT).show();
                        loadProfiles();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
