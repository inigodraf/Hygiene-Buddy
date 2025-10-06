package com.example.hygienebuddy;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class UploadVideoActivity extends AppCompatActivity {

    private TextView tvTaskTitle;
    private VideoView videoPreview;
    private Button btnUploadVideo, btnSaveVideo;

    private Uri selectedVideoUri;
    private String taskName;

    private ActivityResultLauncher<Intent> videoPickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);

        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        videoPreview = findViewById(R.id.videoPreview);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnSaveVideo = findViewById(R.id.btnSaveVideo);

        // Get the task name passed from SettingsFragment
        taskName = getIntent().getStringExtra("TASK_NAME");
        tvTaskTitle.setText("Upload Video for " + taskName);

        setupVideoPicker();

        btnUploadVideo.setOnClickListener(v -> openVideoPicker());
        btnSaveVideo.setOnClickListener(v -> saveVideoMetadata());
    }

    private void setupVideoPicker() {
        videoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedVideoUri = result.getData().getData();
                        if (selectedVideoUri != null) {
                            videoPreview.setVideoURI(selectedVideoUri);
                            videoPreview.start();
                            Toast.makeText(this, "Video selected successfully", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        videoPickerLauncher.launch(intent);
    }

    private void saveVideoMetadata() {
        if (selectedVideoUri == null) {
            Toast.makeText(this, "Please upload a video first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Placeholder for saving video metadata to database
        // e.g., video title, URI string, timestamp, etc.
        Toast.makeText(this, "Video saved for " + taskName, Toast.LENGTH_LONG).show();

        // Return to Settings screen
        finish();
    }
}
