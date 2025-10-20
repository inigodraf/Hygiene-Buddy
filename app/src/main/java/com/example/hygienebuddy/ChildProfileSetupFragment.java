package com.example.hygienebuddy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

        btnBack.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack(); // goes back to FacilitatorSetupFragment
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

        // Build conditions string
        StringBuilder conditions = new StringBuilder();
        if (hasASD) conditions.append("ASD ");
        if (hasADHD) conditions.append("ADHD ");
        if (hasDownSyndrome) conditions.append("Down Syndrome ");
        if (conditions.length() == 0) conditions.append("None");

        // Save to SharedPreferences
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("ChildProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("child_name", childName);
        editor.putString("child_age", childAge);
        editor.putString("child_conditions", conditions.toString().trim());
        editor.apply();

        // Confirmation
        Toast.makeText(requireContext(), "Child profile saved successfully!", Toast.LENGTH_SHORT).show();

        // Navigate directly to DashboardFragment
        HomeDashboardFragment dashboardFragment = new HomeDashboardFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, dashboardFragment)
                .commit();
    }
}
