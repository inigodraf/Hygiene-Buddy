package com.example.hygienebuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper class to manage profile image file operations
 * Handles saving images locally and retrieving stored image URIs
 */
public class ImageManager {

    private static final String TAG = "ImageManager";
    private static final String PREFS_NAME = "ImagePrefs";
    private static final String IMAGE_DIRECTORY = "profile_images";

    private Context context;
    private SharedPreferences preferences;

    public ImageManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves a profile image from URI to local storage and returns the file path
     * @param profileId The profile ID
     * @param imageUri The URI of the image to save
     * @return File path if successful, null otherwise
     */
    public String saveProfileImage(int profileId, Uri imageUri) {
        if (imageUri == null) {
            Log.w(TAG, "Image URI is null");
            return null;
        }

        try {
            // Create images directory if it doesn't exist
            File imagesDir = getImagesDirectory();
            if (!imagesDir.exists()) {
                if (!imagesDir.mkdirs()) {
                    Log.e(TAG, "Failed to create images directory");
                    return null;
                }
            }

            // Create file for this profile
            String fileName = "profile_" + profileId + ".jpg";
            File imageFile = new File(imagesDir, fileName);

            // Copy image from URI to file
            if (copyImageFile(imageUri, imageFile)) {
                String filePath = imageFile.getAbsolutePath();
                // Store path in preferences for quick lookup
                preferences.edit()
                        .putString("profile_" + profileId, filePath)
                        .apply();
                Log.d(TAG, "Successfully saved profile image: " + filePath);
                return filePath;
            } else {
                Log.e(TAG, "Failed to copy image file");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving profile image: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets the stored image URI for a profile
     * @param profileId The profile ID
     * @return Uri of the image file, or null if not found
     */
    public Uri getProfileImageUri(int profileId) {
        // First try to get from preferences
        String imagePath = preferences.getString("profile_" + profileId, null);

        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                try {
                    // Use FileProvider for proper URI handling
                    return FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider", imageFile);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating FileProvider URI: " + e.getMessage());
                    // Fallback to file URI
                    return Uri.fromFile(imageFile);
                }
            } else {
                // File doesn't exist, remove from preferences
                preferences.edit()
                        .remove("profile_" + profileId)
                        .apply();
                Log.w(TAG, "Profile image file not found, removed from preferences: " + imagePath);
            }
        }

        // Fallback: try to find file in images directory
        File imagesDir = getImagesDirectory();
        if (imagesDir.exists()) {
            String fileName = "profile_" + profileId + ".jpg";
            File imageFile = new File(imagesDir, fileName);
            if (imageFile.exists()) {
                try {
                    return FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider", imageFile);
                } catch (Exception e) {
                    return Uri.fromFile(imageFile);
                }
            }
        }

        return null;
    }

    /**
     * Gets the raw file path for a profile image
     * @param profileId The profile ID
     * @return File object, or null if not found
     */
    public File getProfileImageFile(int profileId) {
        String imagePath = preferences.getString("profile_" + profileId, null);
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                return imageFile;
            }
        }

        // Fallback: try to find file in images directory
        File imagesDir = getImagesDirectory();
        if (imagesDir.exists()) {
            String fileName = "profile_" + profileId + ".jpg";
            File imageFile = new File(imagesDir, fileName);
            if (imageFile.exists()) {
                return imageFile;
            }
        }

        return null;
    }

    /**
     * Deletes a profile image
     * @param profileId The profile ID
     * @return true if successful, false otherwise
     */
    public boolean deleteProfileImage(int profileId) {
        File imageFile = getProfileImageFile(profileId);
        if (imageFile != null && imageFile.exists()) {
            boolean deleted = imageFile.delete();
            if (deleted) {
                preferences.edit()
                        .remove("profile_" + profileId)
                        .apply();
                Log.d(TAG, "Successfully deleted profile image for profile: " + profileId);
            }
            return deleted;
        }
        return false;
    }

    /**
     * Gets the images directory path
     * @return File object representing the images directory
     */
    private File getImagesDirectory() {
        // Use external files directory for images
        File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (externalDir != null) {
            return new File(externalDir, IMAGE_DIRECTORY);
        } else {
            // Fallback to internal storage
            return new File(context.getFilesDir(), IMAGE_DIRECTORY);
        }
    }

    /**
     * Copies image file from URI to local file
     * @param sourceUri Source image URI
     * @param destinationFile Destination file
     * @return true if successful, false otherwise
     */
    private boolean copyImageFile(Uri sourceUri, File destinationFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream from URI: " + sourceUri);
                return false;
            }

            outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            Log.d(TAG, "Successfully copied image file to: " + destinationFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying image file: " + e.getMessage(), e);
            // Delete partial file if it exists
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams: " + e.getMessage());
            }
        }
    }

    /**
     * Converts a content URI or file path to a persistent file path
     * If it's already a file path, returns it. If it's a content URI, copies it to app storage.
     * @param profileId The profile ID
     * @param imageUriOrPath The URI or path to convert
     * @return File path if successful, original value otherwise
     */
    public String ensurePersistentImagePath(int profileId, String imageUriOrPath) {
        if (imageUriOrPath == null || imageUriOrPath.trim().isEmpty()) {
            return null;
        }

        // If it's already a file path (starts with /), return it
        if (imageUriOrPath.startsWith("/")) {
            File file = new File(imageUriOrPath);
            if (file.exists()) {
                return imageUriOrPath;
            }
        }

        // If it's a content URI, copy it to app storage
        if (imageUriOrPath.startsWith("content://")) {
            try {
                Uri uri = Uri.parse(imageUriOrPath);
                String savedPath = saveProfileImage(profileId, uri);
                if (savedPath != null) {
                    return savedPath;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error converting content URI to file path: " + e.getMessage(), e);
            }
        }

        // If it's a file URI, extract the path
        if (imageUriOrPath.startsWith("file://")) {
            String path = imageUriOrPath.replace("file://", "");
            File file = new File(path);
            if (file.exists()) {
                return path;
            }
        }

        // Return original if we can't convert it
        return imageUriOrPath;
    }
}

