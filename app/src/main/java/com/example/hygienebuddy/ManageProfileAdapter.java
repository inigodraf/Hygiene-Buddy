package com.example.hygienebuddy;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter for displaying user profiles in RecyclerView
 */
public class ManageProfileAdapter extends RecyclerView.Adapter<ManageProfileAdapter.ProfileViewHolder> {

    private Context context;
    private List<UserProfile> profiles;
    private OnProfileActionListener listener;

    public interface OnProfileActionListener {
        void onEditProfile(UserProfile profile);
        void onDeleteProfile(UserProfile profile);
    }

    public ManageProfileAdapter(Context context, OnProfileActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setProfiles(List<UserProfile> profiles) {
        this.profiles = profiles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_profile_card, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        UserProfile profile = profiles.get(position);
        holder.bind(profile);
    }

    @Override
    public int getItemCount() {
        return profiles != null ? profiles.size() : 0;
    }

    class ProfileViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfileImage;
        private TextView tvProfileName, tvProfileAge, tvProfileCondition;
        private MaterialButton btnEdit, btnDelete;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvProfileName = itemView.findViewById(R.id.tvProfileName);
            tvProfileAge = itemView.findViewById(R.id.tvProfileAge);
            tvProfileCondition = itemView.findViewById(R.id.tvProfileCondition);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEditProfile(profiles.get(position));
                }
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteProfile(profiles.get(position));
                }
            });
        }
        public void bind(UserProfile profile) {
            tvProfileName.setText(profile.getName());
            tvProfileAge.setText(profile.getAgeDisplayText());
            tvProfileCondition.setText(profile.getCondition() != null && !profile.getCondition().isEmpty()
                    ? "Conditions: " + profile.getCondition()
                    : "No conditions specified");

            if (profile.hasImage()) {
                try {
                    Uri imageUri = Uri.parse(profile.getImageUri());
                    ivProfileImage.setImageURI(imageUri);
                } catch (Exception e) {
                    ivProfileImage.setImageResource(R.drawable.default_avatar);
                }
            } else {
                ivProfileImage.setImageResource(R.drawable.default_avatar);
            }
        }
    }
}
