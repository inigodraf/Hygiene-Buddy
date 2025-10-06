package com.example.hygienebuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    private List<ProfileModel> profiles;
    private OnProfileClickListener listener;

    public interface OnProfileClickListener {
        void onProfileClick(ProfileModel profile, int position);
        void onProfileLongClick(ProfileModel profile, int position);
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
        holder.ivProfileAvatar.setImageResource(profile.getAvatarResId());

        // Show "+" overlay if it's the "Add New" card
        holder.ivAddIcon.setVisibility(profile.getName().equals("Add New") ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onProfileClick(profile, position));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onProfileLongClick(profile, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    public static class ProfileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfileAvatar, ivAddIcon;
        TextView tvProfileName;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfileAvatar = itemView.findViewById(R.id.ivProfileAvatar);
            ivAddIcon = itemView.findViewById(R.id.ivAddIcon);
            tvProfileName = itemView.findViewById(R.id.tvProfileName);
        }
    }
}
