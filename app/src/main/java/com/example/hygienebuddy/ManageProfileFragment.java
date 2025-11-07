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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
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

        try {
            rvProfiles = view.findViewById(R.id.rvProfiles);
            layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
            fabAddProfile = view.findViewById(R.id.fabAddProfile);

            if (rvProfiles == null || layoutEmptyState == null || fabAddProfile == null) {
                android.util.Log.e("ManageProfileFragment", "Required views not found in layout");
                return;
            }

            if (!isAdded() || getContext() == null) {
                android.util.Log.e("ManageProfileFragment", "Fragment not properly attached");
                return;
            }

            databaseHelper = new UserProfileDatabaseHelper(requireContext());

            setupRecyclerView();
            setupImagePicker();

            if (fabAddProfile != null) {
                fabAddProfile.setOnClickListener(v -> {
                    if (isAdded()) {
                        showProfileDialog(null);
                    }
                });
            }

            ShapeableImageView btnBack = view.findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    requireActivity()
                            .getSupportFragmentManager()
                            .popBackStack(); // goes back to the previous fragment
                });
            }

            loadProfiles();

            view.post(() -> {
                if (isAdded()) {
                    BottomNavHelper.setupBottomNav(this, "settings");
                }
            });
        } catch (Exception e) {
            android.util.Log.e("ManageProfileFragment", "Error in onViewCreated: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void setupRecyclerView() {
        adapter = new ManageProfileAdapter(requireContext(), this);
        // Use GridLayoutManager for landscape (2 columns) or LinearLayoutManager for portrait
        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            rvProfiles.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        } else {
            rvProfiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        rvProfiles.setAdapter(adapter);
    }

    private ImageView currentDialogImageView; // Store reference to update image when picked

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedImageUri = imageUri.toString();
                            // Update the dialog image view if it exists
                            if (currentDialogImageView != null) {
                                try {
                                    currentDialogImageView.setImageURI(imageUri);
                                } catch (Exception e) {
                                    android.util.Log.e("ManageProfileFragment", "Error setting picked image: " + e.getMessage(), e);
                                    currentDialogImageView.setImageResource(R.drawable.ic_default_user);
                                }
                            }
                        }
                    }
                });
    }

    private void loadProfiles() {
        if (databaseHelper == null || adapter == null) {
            android.util.Log.e("ManageProfileFragment", "Database helper or adapter not initialized");
            return;
        }

        try {
            profiles = databaseHelper.getAllProfiles();
            if (profiles == null) {
                profiles = new java.util.ArrayList<>();
            }

            adapter.setProfiles(profiles);

            // Load selected profile ID from SQLite
            AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
            int selectedProfileId = appDataDb.getIntSetting("selected_profile_id", -1);
            adapter.setSelectedProfileId(selectedProfileId);

            if (profiles.isEmpty()) {
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                if (rvProfiles != null) rvProfiles.setVisibility(View.GONE);
            } else {
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                if (rvProfiles != null) rvProfiles.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            android.util.Log.e("ManageProfileFragment", "Error loading profiles: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading profiles", Toast.LENGTH_SHORT).show();
        }
    }
    private void showProfileDialog(@Nullable UserProfile profile) {
        selectedImageUri = (profile != null) ? profile.getImageUri() : null;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_profile, null);

        ImageView ivProfileImage = dialogView.findViewById(R.id.ivProfileImage);
        currentDialogImageView = ivProfileImage; // Store reference for image picker callback

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

        // Set default avatar initially (for both new and existing profiles)
        ivProfileImage.setImageResource(R.drawable.default_avatar);

        // Pre-fill if editing
        if (profile != null) {
            etName.setText(profile.getName());
            actvAge.setText(String.valueOf(profile.getAge()), false);

            // Load conditions - handle both comma-separated and space-separated formats
            String conditionText = profile.getCondition();
            if (conditionText != null && !conditionText.trim().isEmpty() && !conditionText.equalsIgnoreCase("None")) {
                String lowerConditionText = conditionText.toLowerCase();
                // Check for conditions (case-insensitive, handles both comma and space separators)
                cbASD.setChecked(lowerConditionText.contains("asd"));
                cbADHD.setChecked(lowerConditionText.contains("adhd"));
                cbDownSyndrome.setChecked(lowerConditionText.contains("down"));
                android.util.Log.d("ManageProfileFragment", "Loaded conditions from profile: '" + conditionText + "' - ASD: " + cbASD.isChecked() + ", ADHD: " + cbADHD.isChecked() + ", Down: " + cbDownSyndrome.isChecked());
            } else {
                // No conditions or "None" - uncheck all
                cbASD.setChecked(false);
                cbADHD.setChecked(false);
                cbDownSyndrome.setChecked(false);
                android.util.Log.d("ManageProfileFragment", "No conditions found or 'None' - all checkboxes unchecked");
            }

            // Load profile image if available
            if (profile.hasImage() && profile.getImageUri() != null) {
                try {
                    Uri imageUri = Uri.parse(profile.getImageUri());
                    if (imageUri != null) {
                        ivProfileImage.setImageURI(imageUri);
                    } else {
                        ivProfileImage.setImageResource(R.drawable.default_avatar);
                    }
                } catch (Exception e) {
                    android.util.Log.e("ManageProfileFragment", "Error loading profile image in dialog: " + e.getMessage(), e);
                    ivProfileImage.setImageResource(R.drawable.default_avatar);
                }
            }
        } else {
            // For new profile, show default avatar
            ivProfileImage.setImageResource(R.drawable.default_avatar);
        }

        ivProfileImage.setOnClickListener(v -> pickImageFromGallery());

        // Create dialog and store reference to prevent early dismissal
        com.google.android.material.dialog.MaterialAlertDialogBuilder dialogBuilder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(profile != null ? "Edit Profile" : "Add New Profile")
                        .setView(dialogView)
                        .setNegativeButton("Cancel", null);

        // Set positive button with custom listener that prevents auto-dismiss
        dialogBuilder.setPositiveButton(profile != null ? "Update" : "Add", null);

        AlertDialog dialog = dialogBuilder.create();

        // Set custom click listener that reads values before dismissing
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> {
                    // Read all values from the dialog view BEFORE any dismissal
                    String name = etName.getText().toString().trim();
                    String ageText = actvAge.getText().toString().trim();

                    if (!validateInput(name, ageText)) {
                        return; // Don't dismiss if validation fails
                    }

                    // Read checkbox states BEFORE dialog dismisses
                    boolean hasASD = cbASD.isChecked();
                    boolean hasADHD = cbADHD.isChecked();
                    boolean hasDownSyndrome = cbDownSyndrome.isChecked();

                    android.util.Log.d("ManageProfileFragment", "Reading checkbox states - ASD: " + hasASD + ", ADHD: " + hasADHD + ", Down: " + hasDownSyndrome);

                    int age = Integer.parseInt(ageText);
                    StringBuilder conditionBuilder = new StringBuilder();
                    if (hasASD) conditionBuilder.append("ASD, ");
                    if (hasADHD) conditionBuilder.append("ADHD, ");
                    if (hasDownSyndrome) conditionBuilder.append("Down Syndrome, ");
                    String conditions = conditionBuilder.toString().trim();
                    if (conditions.endsWith(",")) {
                        conditions = conditions.substring(0, conditions.length() - 1).trim();
                    }
                    // If no conditions selected, use empty string
                    if (conditions.isEmpty()) {
                        conditions = "";
                    }

                    android.util.Log.d("ManageProfileFragment", "Saving conditions: '" + conditions + "' - ASD: " + hasASD + ", ADHD: " + hasADHD + ", Down: " + hasDownSyndrome);
                    android.util.Log.d("ManageProfileFragment", "ImageUri: " + (selectedImageUri != null ? selectedImageUri : "null"));

                    // Now dismiss the dialog and save
                    dialog.dismiss();

                    if (profile != null) {
                        updateProfile(profile.getId(), name, age, selectedImageUri, conditions);
                    } else {
                        addProfile(name, age, selectedImageUri, conditions);
                    }
                });
            }
        });

        dialog.show();
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
        if (databaseHelper == null) {
            Toast.makeText(requireContext(), "Database not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            android.util.Log.d("ManageProfileFragment", "addProfile called - name: " + name + ", age: " + age + ", conditions: '" + conditions + "', imageUri: " + (imageUri != null ? imageUri : "null"));

            long result = databaseHelper.insertProfile(name, age, imageUri, conditions);
            if (result != -1) {
                android.util.Log.d("ManageProfileFragment", "Profile added successfully with ID: " + result);

                // Verify the profile was saved correctly by reading it back
                UserProfile savedProfile = databaseHelper.getProfileById((int) result);
                if (savedProfile != null) {
                    android.util.Log.d("ManageProfileFragment", "Verified saved profile - conditions: '" + savedProfile.getCondition() + "'");
                } else {
                    android.util.Log.w("ManageProfileFragment", "Could not verify saved profile - getProfileById returned null");
                }

                Toast.makeText(requireContext(), "Profile added successfully!", Toast.LENGTH_SHORT).show();

                // Refresh profiles list immediately on main thread
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadProfiles(); // This will refresh the RecyclerView
                        if (adapter != null) {
                            adapter.notifyDataSetChanged(); // Force immediate refresh
                        }
                    });
                } else {
                    loadProfiles(); // Fallback if not on main thread
                }
            } else {
                android.util.Log.e("ManageProfileFragment", "Failed to add profile - insertProfile returned -1");
                Toast.makeText(requireContext(), "Failed to add profile", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("ManageProfileFragment", "Error adding profile: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error adding profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfile(int id, String name, int age, String imageUri, String conditions) {
        if (databaseHelper == null) {
            Toast.makeText(requireContext(), "Database not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            android.util.Log.d("ManageProfileFragment", "updateProfile called - id: " + id + ", name: " + name + ", age: " + age + ", conditions: '" + conditions + "', imageUri: " + (imageUri != null ? imageUri : "null"));

            int result = databaseHelper.updateProfile(id, name, age, imageUri, conditions);
            if (result > 0) {
                android.util.Log.d("ManageProfileFragment", "Profile updated successfully");

                // Verify the profile was updated correctly by reading it back
                UserProfile updatedProfile = databaseHelper.getProfileById(id);
                if (updatedProfile != null) {
                    android.util.Log.d("ManageProfileFragment", "Verified updated profile - conditions: '" + updatedProfile.getCondition() + "'");
                } else {
                    android.util.Log.w("ManageProfileFragment", "Could not verify updated profile - getProfileById returned null");
                }

                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                // Refresh profiles list immediately on main thread
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadProfiles(); // This will refresh the RecyclerView
                        if (adapter != null) {
                            adapter.notifyDataSetChanged(); // Force immediate refresh
                        }
                    });
                } else {
                    loadProfiles(); // Fallback if not on main thread
                }
            } else {
                android.util.Log.e("ManageProfileFragment", "Failed to update profile - updateProfile returned " + result);
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("ManageProfileFragment", "Error updating profile: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEditProfile(UserProfile profile) {
        showProfileDialog(profile);
    }

    @Override
    public void onDeleteProfile(UserProfile profile) {
        if (profile == null) {
            Toast.makeText(requireContext(), "Invalid profile", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete " + profile.getName() + "'s profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        int result = databaseHelper.deleteProfile(profile.getId());
                        if (result > 0) {
                            Toast.makeText(requireContext(), "Profile deleted successfully!", Toast.LENGTH_SHORT).show();
                            loadProfiles();
                        } else {
                            Toast.makeText(requireContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ManageProfileFragment", "Error deleting profile: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Error deleting profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onProfileSelected(UserProfile profile) {
        if (profile == null) return;

        // Save selected profile to SQLite for use across the app
        AppDataDatabaseHelper appDataDb = new AppDataDatabaseHelper(requireContext());
        appDataDb.setIntSetting("selected_profile_id", profile.getId());
        appDataDb.setIntSetting("current_profile_id", profile.getId()); // Store as current_profile_id for profile switching
        appDataDb.setSetting("child_name", profile.getName());
        appDataDb.setSetting("child_age", String.valueOf(profile.getAge()));
        appDataDb.setSetting("child_conditions", profile.getCondition() != null ? profile.getCondition() : "");

        // Update adapter to show selected state
        adapter.setSelectedProfileId(profile.getId());

        // Show feedback
        Toast.makeText(requireContext(), "Switched to " + profile.getName() + "'s profile", Toast.LENGTH_SHORT).show();

        // Small delay to ensure database is committed
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(() -> {
            // Navigate back to dashboard with smooth transition animation
            navigateToDashboardWithAnimation();
        }, 100);
    }

    /** Navigate back to dashboard with smooth transition */
    private void navigateToDashboardWithAnimation() {
        try {
            // Method 1: Try NavController with animation
            try {
                View view = getView();
                if (view != null) {
                    NavController navController = Navigation.findNavController(view);
                    // Pop back stack with animation
                    navController.popBackStack();
                    android.util.Log.d("ManageProfileFragment", "Navigated back via NavController");
                    return;
                }
            } catch (Exception e) {
                android.util.Log.d("ManageProfileFragment", "NavController not available: " + e.getMessage());
            }

            // Method 2: Use FragmentManager with smooth animations
            if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                // Pop with animation
                requireActivity().getSupportFragmentManager().popBackStack();
                android.util.Log.d("ManageProfileFragment", "Popped back stack");
            } else {
                // Replace with animation if no back stack
                HomeDashboardFragment homeFragment = new HomeDashboardFragment();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(
                                android.R.anim.slide_in_left,  // Enter animation
                                android.R.anim.slide_out_right, // Exit animation
                                android.R.anim.slide_in_left,  // Pop enter
                                android.R.anim.slide_out_right // Pop exit
                        )
                        .replace(R.id.fragment_container, homeFragment)
                        .addToBackStack(null)
                        .commit();
                android.util.Log.d("ManageProfileFragment", "Replaced with HomeDashboardFragment");
            }
        } catch (Exception e) {
            android.util.Log.e("ManageProfileFragment", "Error navigating back: " + e.getMessage(), e);
            // Fallback: use system back
            if (isAdded()) {
                requireActivity().onBackPressed();
            }
        }
    }
}
