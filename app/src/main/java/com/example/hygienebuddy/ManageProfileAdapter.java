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

import java.io.File;
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
            // Safety check: ensure conditions field doesn't contain image URI patterns
            if (conditionText != null && !conditionText.trim().isEmpty() && !conditionText.equalsIgnoreCase("None")) {
                String conditionsToDisplay = conditionText.trim();
                // Check if this looks like an image URI instead of conditions
                if (conditionsToDisplay.contains("/storage/") || conditionsToDisplay.contains("content://") ||
                        conditionsToDisplay.contains("file://") || conditionsToDisplay.contains("Android/data")) {
                    // This looks like an image URI, not conditions - treat as empty
                    android.util.Log.w("ManageProfileAdapter", "Conditions field appears to contain image URI for " + profile.getName() + ", treating as empty: " + conditionsToDisplay.substring(0, Math.min(50, conditionsToDisplay.length())));
                    tvProfileCondition.setText("No conditions specified");
                } else {
                    // Display conditions - handle both comma and space-separated formats
                    tvProfileCondition.setText("Conditions: " + conditionsToDisplay);
                    android.util.Log.d("ManageProfileAdapter", "Displaying conditions for " + profile.getName() + ": '" + conditionsToDisplay + "'");
                }
            } else {
                tvProfileCondition.setText("No conditions specified");
                android.util.Log.d("ManageProfileAdapter", "No conditions for " + profile.getName() + " (conditionText: '" + conditionText + "')");
            }

            // Load profile image - always set default first, then try to load custom image
            ivProfileImage.setImageResource(R.drawable.ic_default_user);
            ivProfileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            // Ensure ImageView never displays text - set proper contentDescription
            ivProfileImage.setContentDescription("Profile image for " + (profile.getName() != null ? profile.getName() : "user"));

            // Enable circular clipping for proper frame fitting
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                ivProfileImage.setClipToOutline(true);
                // Set outline provider for circular clipping
                ivProfileImage.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(android.view.View view, android.graphics.Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
            }

            // Try to load custom image if available
            if (profile.hasImage() && profile.getImageUri() != null && !profile.getImageUri().trim().isEmpty()) {
                try {
                    Uri imageUri = null;
                    String imageUriString = profile.getImageUri();

                    // Check if it's a file path (starts with /)
                    if (imageUriString.startsWith("/")) {
                        File imageFile = new File(imageUriString);
                        if (imageFile.exists()) {
                            // Use FileProvider for file paths
                            try {
                                imageUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        context.getPackageName() + ".fileprovider",
                                        imageFile
                                );
                            } catch (Exception e) {
                                imageUri = Uri.fromFile(imageFile);
                            }
                        }
                    } else if (imageUriString.startsWith("content://") || imageUriString.startsWith("file://")) {
                        // It's a content URI or file URI
                        imageUri = Uri.parse(imageUriString);
                    } else {
                        // Try to get from ImageManager
                        try {
                            ImageManager imageManager = new ImageManager(context);
                            Uri imageManagerUri = imageManager.getProfileImageUri(profile.getId());
                            if (imageManagerUri != null) {
                                imageUri = imageManagerUri;
                            }
                        } catch (Exception e) {
                            android.util.Log.w("ManageProfileAdapter", "ImageManager not available: " + e.getMessage());
                        }
                    }

                    if (imageUri != null) {
                        // Load image directly - setImageURI handles loading asynchronously
                        ivProfileImage.setImageURI(imageUri);
                        ivProfileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                        // Verify image loaded successfully with a post-delay check
                        Uri finalImageUri = imageUri;
                        ivProfileImage.postDelayed(() -> {
                            try {
                                if (ivProfileImage.getDrawable() == null) {
                                    // Image didn't load, use default
                                    ivProfileImage.setImageResource(R.drawable.ic_default_user);
                                    android.util.Log.w("ManageProfileAdapter", "Image failed to load for " + profile.getName() + ", using default");
                                } else {
                                    android.util.Log.d("ManageProfileAdapter", "Successfully loaded image URI: " + finalImageUri.toString() + " for profile: " + profile.getName());
                                }
                            } catch (Exception e) {
                                android.util.Log.e("ManageProfileAdapter", "Error verifying image load: " + e.getMessage());
                                ivProfileImage.setImageResource(R.drawable.ic_default_user);
                            }
                        }, 200);
                    } else {
                        // Could not resolve URI, use default
                        ivProfileImage.setImageResource(R.drawable.ic_default_user);
                        android.util.Log.w("ManageProfileAdapter", "Could not resolve image URI for " + profile.getName() + ": " + imageUriString);
                    }
                } catch (Exception e) {
                    android.util.Log.e("ManageProfileAdapter", "Error parsing image URI for " + profile.getName() + ": " + e.getMessage(), e);
                    // Ensure default avatar is set on error
                    ivProfileImage.setImageResource(R.drawable.ic_default_user);
                }
            } else {
                // No image URI or empty, use default
                ivProfileImage.setImageResource(R.drawable.ic_default_user);
                android.util.Log.d("ManageProfileAdapter", "No image URI for profile: " + profile.getName() + ", using default");
            }

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
