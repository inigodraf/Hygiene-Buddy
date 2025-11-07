package com.example.hygienebuddy;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Adapter for displaying user profiles in RecyclerView (Netflix-style)
 */
public class ManageProfileAdapter extends RecyclerView.Adapter<ManageProfileAdapter.ProfileViewHolder> {

    private Context context;
    private List<UserProfile> profiles;
    private OnProfileActionListener listener;
    private int selectedProfileId = -1;

    public interface OnProfileActionListener {
        void onEditProfile(UserProfile profile);
        void onDeleteProfile(UserProfile profile);
        void onProfileSelected(UserProfile profile);
    }

    public ManageProfileAdapter(Context context, OnProfileActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setProfiles(List<UserProfile> profiles) {
        this.profiles = profiles;
        notifyDataSetChanged();
    }

    public void setSelectedProfileId(int profileId) {
        this.selectedProfileId = profileId;
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
        private MaterialCardView cardView;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvProfileName = itemView.findViewById(R.id.tvProfileName);
            tvProfileAge = itemView.findViewById(R.id.tvProfileAge);
            tvProfileCondition = itemView.findViewById(R.id.tvProfileCondition);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Profile card click - select profile
            cardView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && profiles != null) {
                    UserProfile profile = profiles.get(position);
                    listener.onProfileSelected(profile);
                    setSelectedProfileId(profile.getId());
                }
            });

            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && profiles != null) {
                    listener.onEditProfile(profiles.get(position));
                }
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && profiles != null) {
                    listener.onDeleteProfile(profiles.get(position));
                }
            });
        }

        public void bind(UserProfile profile) {
            if (profile == null) return;

            tvProfileName.setText(profile.getName() != null ? profile.getName() : "Unknown");
            tvProfileAge.setText(profile.getAgeDisplayText());

            String conditionText = profile.getCondition();
            // Handle null, empty string, or "None" - all mean no conditions
            if (conditionText != null && !conditionText.trim().isEmpty() && !conditionText.equalsIgnoreCase("None")) {
                // Display conditions - handle both comma and space-separated formats
                String displayText = conditionText.trim();
                tvProfileCondition.setText("Conditions: " + displayText);
                android.util.Log.d("ManageProfileAdapter", "Displaying conditions for " + profile.getName() + ": '" + displayText + "'");
            } else {
                tvProfileCondition.setText("No conditions specified");
                android.util.Log.d("ManageProfileAdapter", "No conditions for " + profile.getName() + " (conditionText: '" + conditionText + "')");
            }

            // Load profile image - always set default first, then try to load custom image
            ivProfileImage.setImageResource(R.drawable.ic_default_user);

            if (profile.hasImage() && profile.getImageUri() != null && !profile.getImageUri().isEmpty()) {
                try {
                    Uri imageUri = Uri.parse(profile.getImageUri());
                    if (imageUri != null) {
                        // Try to load the image URI
                        ivProfileImage.setImageURI(imageUri);

                        // Verify the image was actually loaded by checking if drawable changed
                        // If setImageURI fails silently, the default avatar will remain
                        android.util.Log.d("ManageProfileAdapter", "Loading image URI: " + imageUri.toString());
                    }
                } catch (Exception e) {
                    android.util.Log.e("ManageProfileAdapter", "Error loading profile image: " + e.getMessage(), e);
                    // Default avatar is already set, so no need to set it again
                }
            }

            // Highlight selected profile (Netflix-style)
            if (selectedProfileId == profile.getId()) {
                cardView.setAlpha(0.9f);
                cardView.setCardElevation(8f);
                cardView.setStrokeWidth(4);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    cardView.setStrokeColor(context.getResources().getColor(R.color.blue, null));
                } else {
                    cardView.setStrokeColor(context.getResources().getColor(R.color.blue));
                }
            } else {
                cardView.setAlpha(1.0f);
                cardView.setCardElevation(4f);
                cardView.setStrokeWidth(0);
            }
        }
    }
}
