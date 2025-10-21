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
 * Helper class to manage video file operations for hygiene tasks
 * Handles saving videos locally and retrieving stored video URIs
 */
public class VideoManager {

    private static final String TAG = "VideoManager";
    private static final String PREFS_NAME = "VideoPrefs";
    private static final String VIDEO_DIRECTORY = "videos";

    private Context context;
    private SharedPreferences preferences;

    public VideoManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves a video file from URI to local storage and stores the path
     * @param taskType The hygiene task type (e.g., "handwashing", "toothbrushing")
     * @param videoUri The URI of the video to save
     * @return true if successful, false otherwise
     */
    public boolean saveVideo(String taskType, Uri videoUri) {
        try {
            // Create videos directory if it doesn't exist
            File videosDir = getVideosDirectory();
            if (!videosDir.exists()) {
                videosDir.mkdirs();
            }

            // Generate filename based on task type
            String fileName = taskType.toLowerCase() + "_instruction.mp4";
            File videoFile = new File(videosDir, fileName);

            // Copy video from URI to local file
            boolean copySuccess = copyVideoFile(videoUri, videoFile);

            if (copySuccess) {
                // Store the local file path in SharedPreferences
                String localPath = videoFile.getAbsolutePath();
                preferences.edit()
                        .putString(taskType.toLowerCase() + "_video_uri", localPath)
                        .apply();

                Log.d(TAG, "Video saved successfully: " + localPath);
                return true;
            } else {
                Log.e(TAG, "Failed to copy video file");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving video: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the stored video URI for a specific task
     * @param taskType The hygiene task type
     * @return Uri of the video file, or null if not found
     */
    public Uri getVideoUri(String taskType) {
        String videoPath = preferences.getString(taskType.toLowerCase() + "_video_uri", null);

        if (videoPath != null) {
            File videoFile = new File(videoPath);
            if (videoFile.exists()) {
                return Uri.fromFile(videoFile);
            } else {
                // File doesn't exist, remove from preferences
                preferences.edit()
                        .remove(taskType.toLowerCase() + "_video_uri")
                        .apply();
                Log.w(TAG, "Video file not found, removed from preferences: " + videoPath);
            }
        }

        return null;
    }

    /**
     * Checks if a video exists for the given task type
     * @param taskType The hygiene task type
     * @return true if video exists, false otherwise
     */
    public boolean hasVideo(String taskType) {
        Uri videoUri = getVideoUri(taskType);
        return videoUri != null;
    }

    /**
     * Deletes the video file for a specific task
     * @param taskType The hygiene task type
     * @return true if successful, false otherwise
     */
    public boolean deleteVideo(String taskType) {
        try {
            String videoPath = preferences.getString(taskType.toLowerCase() + "_video_uri", null);

            if (videoPath != null) {
                File videoFile = new File(videoPath);
                boolean deleted = videoFile.delete();

                if (deleted) {
                    preferences.edit()
                            .remove(taskType.toLowerCase() + "_video_uri")
                            .apply();
                    Log.d(TAG, "Video deleted successfully: " + videoPath);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting video: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the videos directory path
     * @return File object representing the videos directory
     */
    private File getVideosDirectory() {
        // Use external files directory for videos
        File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (externalDir != null) {
            return new File(externalDir, VIDEO_DIRECTORY);
        } else {
            // Fallback to internal storage
            return new File(context.getFilesDir(), VIDEO_DIRECTORY);
        }
    }

    /**
     * Copies video file from URI to local file
     * @param sourceUri Source video URI
     * @param destinationFile Destination file
     * @return true if successful, false otherwise
     */
    private boolean copyVideoFile(Uri sourceUri, File destinationFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying video file: " + e.getMessage());
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
     * Saves a video file from URI to local storage for a specific task step
     * @param taskType The hygiene task type (e.g., "handwashing", "toothbrushing")
     * @param stepNumber The step number within the task
     * @param videoUri The URI of the video to save
     * @return true if successful, false otherwise
     */
    public boolean saveStepVideo(String taskType, int stepNumber, Uri videoUri) {
        try {
            // Create videos directory if it doesn't exist
            File videosDir = getVideosDirectory();
            if (!videosDir.exists()) {
                videosDir.mkdirs();
            }

            // Generate filename based on task type and step number
            String fileName = taskType.toLowerCase() + "_step_" + stepNumber + ".mp4";
            File videoFile = new File(videosDir, fileName);

            // Copy video from URI to local file
            boolean copySuccess = copyVideoFile(videoUri, videoFile);

            if (copySuccess) {
                // Store the local file path in SharedPreferences
                String localPath = videoFile.getAbsolutePath();
                String videoKey = taskType.toLowerCase() + "_step_" + stepNumber;
                preferences.edit()
                        .putString(videoKey, localPath)
                        .apply();

                Log.d(TAG, "Step video saved successfully: " + localPath);
                Log.d(TAG, "Video key stored: " + videoKey);
                Log.d(TAG, "File exists: " + videoFile.exists());
                Log.d(TAG, "File size: " + videoFile.length() + " bytes");
                return true;
            } else {
                Log.e(TAG, "Failed to copy step video file");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving step video: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the stored video URI for a specific task step
     * @param taskType The hygiene task type
     * @param stepNumber The step number within the task
     * @return Uri of the video file, or null if not found
     */
    public Uri getStepVideoUri(String taskType, int stepNumber) {
        String videoKey = taskType.toLowerCase() + "_step_" + stepNumber;
        String videoPath = preferences.getString(videoKey, null);

        if (videoPath != null) {
            File videoFile = new File(videoPath);
            if (videoFile.exists()) {
                try {
                    // Use FileProvider for proper URI handling
                    return FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider", videoFile);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating FileProvider URI: " + e.getMessage());
                    // Fallback to file URI
                    return Uri.fromFile(videoFile);
                }
            } else {
                // File doesn't exist, remove from preferences
                preferences.edit()
                        .remove(videoKey)
                        .apply();
                Log.w(TAG, "Step video file not found, removed from preferences: " + videoPath);
            }
        }

        return null;
    }

    /**
     * Gets the raw file path for a step video (for internal use)
     * @param taskType The hygiene task type
     * @param stepNumber The step number within the task
     * @return File object, or null if not found
     */
    public File getStepVideoFile(String taskType, int stepNumber) {
        String videoKey = taskType.toLowerCase() + "_step_" + stepNumber;
        String videoPath = preferences.getString(videoKey, null);

        if (videoPath != null) {
            File videoFile = new File(videoPath);
            if (videoFile.exists() && videoFile.length() > 0) {
                Log.d(TAG, "Video file found: " + videoPath + " (size: " + videoFile.length() + " bytes)");
                return videoFile;
            } else {
                // File doesn't exist or is empty, remove from preferences
                preferences.edit()
                        .remove(videoKey)
                        .apply();
                Log.w(TAG, "Step video file not found or empty, removed from preferences: " + videoPath);
            }
        }

        Log.d(TAG, "No video file found for " + taskType + " step " + stepNumber);
        return null;
    }

    /**
     * Checks if a video exists for the given task step
     * @param taskType The hygiene task type
     * @param stepNumber The step number within the task
     * @return true if video exists, false otherwise
     */
    public boolean hasStepVideo(String taskType, int stepNumber) {
        String videoKey = taskType.toLowerCase() + "_step_" + stepNumber;
        String videoPath = preferences.getString(videoKey, null);

        Log.d(TAG, "Checking video for key: " + videoKey);
        Log.d(TAG, "Stored path: " + videoPath);

        if (videoPath != null) {
            File videoFile = new File(videoPath);
            boolean exists = videoFile.exists();
            Log.d(TAG, "File exists: " + exists);
            if (exists) {
                Log.d(TAG, "File size: " + videoFile.length() + " bytes");
            }
            return exists;
        }

        Log.d(TAG, "No video path found for key: " + videoKey);
        return false;
    }

    /**
     * Deletes the video file for a specific task step
     * @param taskType The hygiene task type
     * @param stepNumber The step number within the task
     * @return true if successful, false otherwise
     */
    public boolean deleteStepVideo(String taskType, int stepNumber) {
        try {
            String videoKey = taskType.toLowerCase() + "_step_" + stepNumber;
            String videoPath = preferences.getString(videoKey, null);

            if (videoPath != null) {
                File videoFile = new File(videoPath);
                boolean deleted = videoFile.delete();

                if (deleted) {
                    preferences.edit()
                            .remove(videoKey)
                            .apply();
                    Log.d(TAG, "Step video deleted successfully: " + videoPath);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting step video: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the size of the video file for a specific task step
     * @param taskType The hygiene task type
     * @param stepNumber The step number within the task
     * @return File size in bytes, or -1 if file doesn't exist
     */
    public long getStepVideoFileSize(String taskType, int stepNumber) {
        String videoKey = taskType.toLowerCase() + "_step_" + stepNumber;
        String videoPath = preferences.getString(videoKey, null);

        if (videoPath != null) {
            File videoFile = new File(videoPath);
            if (videoFile.exists()) {
                return videoFile.length();
            }
        }

        return -1;
    }

    /**
     * Gets all stored video steps for a specific task
     * @param taskType The hygiene task type
     * @return Array of step numbers that have videos
     */
    public int[] getStoredStepVideos(String taskType) {
        java.util.List<Integer> stepsWithVideos = new java.util.ArrayList<>();

        // Check for steps 1-10 (adjust range as needed)
        for (int step = 1; step <= 10; step++) {
            if (hasStepVideo(taskType, step)) {
                stepsWithVideos.add(step);
            }
        }

        return stepsWithVideos.stream().mapToInt(Integer::intValue).toArray();
    }
}
