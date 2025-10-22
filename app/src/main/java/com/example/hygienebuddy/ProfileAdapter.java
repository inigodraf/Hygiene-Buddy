package com.example.hygienebuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    private final List<ProfileModel> profiles;
    private final OnProfileClickListener listener;

    public interface OnProfileClickListener {
        void onProfileClick(ProfileModel profile, int position);
        void onEditClick(ProfileModel profile, int position);
        void onDeleteClick(ProfileModel profile, int position);
    }

    public ProfileAdapter(List<ProfileModel> profiles, OnProfileClickListener listener) {
        this.profiles = profiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_card, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        ProfileModel profile = profiles.get(position);

        holder.tvProfileName.setText(profile.getName());
        holder.tvProfileAge.setText("Age: " + profile.getAge());
        holder.tvProfileCondition.setText("Condition: " + profile.getCondition());

        // Set the profile image (if any)
        if (profile.getImageUri() != null) {
            holder.ivProfileImage.setImageURI(profile.getImageUri());
        } else {
            holder.ivProfileImage.setImageResource(R.drawable.default_avatar);
        }

        // Card click (optional if you want it to open details)
        holder.itemView.setOnClickListener(v -> listener.onProfileClick(profile, position));

        // Edit button click
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(profile, position));

        // Delete button click
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(profile, position));
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    public static class ProfileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfileImage;
        TextView tvProfileName, tvProfileAge, tvProfileCondition;
        MaterialButton btnEdit, btnDelete;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvProfileName = itemView.findViewById(R.id.tvProfileName);
            tvProfileAge = itemView.findViewById(R.id.tvProfileAge);
            tvProfileCondition = itemView.findViewById(R.id.tvProfileCondition);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
