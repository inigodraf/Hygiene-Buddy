package com.example.hygienebuddy;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UploadVideoActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTaskTitle;
    private VideoView videoPreview;
    private Button btnUploadVideo, btnSaveVideo;
    private Uri selectedVideoUri;
    private String taskName = "";

    // Launcher for selecting a video from gallery
    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    if (selectedVideoUri != null) {
                        previewVideo(selectedVideoUri);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);

        // Bind views
        btnBack = findViewById(R.id.btnBack);
        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        videoPreview = findViewById(R.id.videoPreview);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnSaveVideo = findViewById(R.id.btnSaveVideo);

        // Retrieve task name (optional)
        taskName = getIntent().getStringExtra("TASK_NAME");
        if (taskName != null && !taskName.isEmpty()) {
            tvTaskTitle.setText("Upload " + taskName + " Video");
        }

        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Upload video button
        btnUploadVideo.setOnClickListener(v -> openGallery());

        // Save video button
        btnSaveVideo.setOnClickListener(v -> saveVideoLocally());
    }

    /* private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        videoPickerLauncher.launch(intent);
    }
    */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        videoPickerLauncher.launch(Intent.createChooser(intent, "Select a video"));
    }


    private void previewVideo(Uri videoUri) {
        videoPreview.setVideoURI(videoUri);
        videoPreview.setOnPreparedListener(MediaPlayer::start);
        videoPreview.setOnCompletionListener(mp -> mp.seekTo(0));
    }

    private void saveVideoLocally() {
        if (selectedVideoUri == null) {
            Toast.makeText(this, "Please choose a video first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Directory for app-specific videos
            File directory = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "CustomInstructions");
            if (!directory.exists()) directory.mkdirs();

            // File name includes task name
            String fileName = (taskName != null ? taskName.toLowerCase().replace(" ", "_") : "instruction")
                    + "_" + System.currentTimeMillis() + ".mp4";

            File destFile = new File(directory, fileName);

            // Copy the video data to destination
            try (InputStream inputStream = getContentResolver().openInputStream(selectedVideoUri);
                 OutputStream outputStream = getContentResolver().openOutputStream(getVideoUri(destFile))) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                Toast.makeText(this, "Video saved to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                //finish();
                previewVideo(Uri.fromFile(destFile));
                videoPreview.setVideoURI(Uri.fromFile(destFile));
                videoPreview.start();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getVideoUri(File file) {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.TITLE, file.getName());
        values.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }
}
