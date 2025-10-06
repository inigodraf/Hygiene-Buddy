package com.example.hygienebuddy;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ManageProfileFragment extends Fragment {

    private RecyclerView rvProfiles;
    private MaterialButton btnManageProfiles;
    private ImageView btnBack;

    private ProfileAdapter profileAdapter;
    private List<ProfileModel> profileList = new ArrayList<>();

    public ManageProfileFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_profile, container, false);

        rvProfiles = view.findViewById(R.id.rvProfiles);
        btnManageProfiles = view.findViewById(R.id.btnManageProfiles);
        btnBack = view.findViewById(R.id.btnBack);

        // Grid layout: 2 columns for profile cards
        rvProfiles.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Temporary mock data
        profileList.add(new ProfileModel("Ethan", R.drawable.ic_toothbrush));
        profileList.add(new ProfileModel("Mia", R.drawable.ic_handwash));
        profileList.add(new ProfileModel("Add New", R.drawable.ic_add));

        profileAdapter = new ProfileAdapter(profileList, new ProfileAdapter.OnProfileClickListener() {
            @Override
            public void onProfileClick(ProfileModel profile, int position) {
                if ("Add New".equals(profile.getName())) {
                    showAddProfileDialog();
                } else {
                    Toast.makeText(getContext(), "Viewing profile: " + profile.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProfileLongClick(ProfileModel profile, int position) {
                if (!"Add New".equals(profile.getName())) {
                    showProfileOptionsDialog(profile, position);
                }
            }
        });

        rvProfiles.setAdapter(profileAdapter);

        btnManageProfiles.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Manage profiles feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void showAddProfileDialog() {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_profile, null);
        EditText etProfileName = dialogView.findViewById(R.id.etProfileName);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Buttons inside the dialog layout
        dialogView.findViewById(R.id.btnAddProfile).setOnClickListener(v -> {
            String name = etProfileName.getText().toString().trim();
            if (!name.isEmpty()) {
                profileList.add(profileList.size() - 1, new ProfileModel(name, R.drawable.ic_toothbrush));
                profileAdapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Profile added!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please enter a name.", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showProfileOptionsDialog(ProfileModel profile, int position) {
        String[] options = {"Edit", "Delete"};
        new AlertDialog.Builder(getContext())
                .setTitle(profile.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditProfileDialog(profile, position);
                    } else if (which == 1) {
                        profileList.remove(position);
                        profileAdapter.notifyItemRemoved(position);
                        Toast.makeText(getContext(), "Profile deleted.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showEditProfileDialog(ProfileModel profile, int position) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_profile, null);
        EditText etProfileName = dialogView.findViewById(R.id.etProfileName);
        etProfileName.setText(profile.getName());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btnAddProfile).setOnClickListener(v -> {
            String newName = etProfileName.getText().toString().trim();
            if (!newName.isEmpty()) {
                profile.setName(newName);
                profileAdapter.notifyItemChanged(position);
                Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please enter a name.", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
