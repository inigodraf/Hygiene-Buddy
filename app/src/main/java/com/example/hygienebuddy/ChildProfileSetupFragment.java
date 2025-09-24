package com.example.hygienebuddy;

import android.app.Activity;
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

        // Here you can save to database or shared preferences
        StringBuilder summary = new StringBuilder();
        summary.append("Child's Name: ").append(childName).append("\n");
        summary.append("Age: ").append(childAge).append("\n");
        summary.append("Conditions: ");
        if (hasASD) summary.append("ASD ");
        if (hasADHD) summary.append("ADHD ");
        if (hasDownSyndrome) summary.append("Down Syndrome ");
        if (!hasASD && !hasADHD && !hasDownSyndrome) summary.append("None");

        Toast.makeText(requireContext(), summary.toString(), Toast.LENGTH_LONG).show();

        // TODO: Navigate to next fragment (e.g., Dashboard or Routine setup)
    }
}
