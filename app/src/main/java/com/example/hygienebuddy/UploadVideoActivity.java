package com.example.hygienebuddy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// LightCompressor Imports
import com.abedelazizshe.lightcompressorlibrary.CompressionListener;
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor;
import com.abedelazizshe.lightcompressorlibrary.VideoQuality;
import com.abedelazizshe.lightcompressorlibrary.config.Configuration;
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation;
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration;

import java.io.File;
import java.util.ArrayList;

public class UploadVideoActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTaskTitle;
    private VideoView videoPreview;
    private View videoContainer;
    private Button btnUploadVideo, btnSaveVideo;
    private Uri selectedVideoUri;
    private String taskName = "";

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

        btnBack = findViewById(R.id.btnBack);
        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        videoPreview = findViewById(R.id.videoPreview);
        videoContainer = findViewById(R.id.layoutVideoContainer);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnSaveVideo = findViewById(R.id.btnSaveVideo);

        taskName = getIntent().getStringExtra("TASK_NAME");
        if (taskName != null && !taskName.isEmpty()) {
            tvTaskTitle.setText("Upload " + taskName + " Video");
        }

        btnBack.setOnClickListener(v -> onBackPressed());
        btnUploadVideo.setOnClickListener(v -> openGallery());
        btnSaveVideo.setOnClickListener(v -> saveAndCompressVideo());

        if (videoContainer != null) {
            videoContainer.post(this::resizeVideoContainer);
        }
    }

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

    private void resizeVideoContainer() {
        if (videoContainer == null) return;
        int width = videoContainer.getWidth();
        if (width == 0) {
            videoContainer.post(this::resizeVideoContainer);
            return;
        }
        int height = Math.max((int)(width * 9f / 16f), 200);
        ViewGroup.LayoutParams lp = videoContainer.getLayoutParams();
        lp.height = height;
        videoContainer.setLayoutParams(lp);
    }

    private void saveAndCompressVideo() {
        if (selectedVideoUri == null) {
            Toast.makeText(this, "Please choose a video first", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Compressing video... 0%");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // LightCompressor requires a list of URIs
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(selectedVideoUri);

        // Configure the compression quality
        Configuration config = new Configuration(
                VideoQuality.MEDIUM, // 1. Quality
                true,                // 2. isMinBitrateCheckEnabled
                null,                // 3. custom bitrate (null = auto)
                false,               // 4. disableAudio
                false,               // 5. keepOriginalResolution
                null,                // 6. custom width (null = auto 720p)
                null,                // 7. custom height
                null                 // 8. videoNames (The missing 8th argument!)
        );

        // Automatically save to the Movies/CustomInstructions folder!
        SharedStorageConfiguration storageConfig = new SharedStorageConfiguration(
                SaveLocation.movies,
                "CustomInstructions"
        );

        // Start compression!
        VideoCompressor.start(
                this,
                uris,
                false, // isStreamable
                storageConfig,
                config,
                new CompressionListener() {
                    @Override
                    public void onStart(int index) {
                        // Background work started
                    }

                    @Override
                    public void onSuccess(int index, long size, @Nullable String path) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(UploadVideoActivity.this, "Video Compressed & Saved!", Toast.LENGTH_LONG).show();

                            // Play the newly compressed video
                            if (path != null) {
                                previewVideo(Uri.fromFile(new File(path)));
                            }
                        });
                    }

                    @Override
                    public void onFailure(int index, @NonNull String failureMessage) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(UploadVideoActivity.this, "Error: " + failureMessage, Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onProgress(int index, float percent) {
                        runOnUiThread(() -> {
                            // Update the dialog with actual percentage!
                            progressDialog.setMessage("Compressing video... " + (int) percent + "%");
                        });
                    }

                    @Override
                    public void onCancelled(int index) {
                        runOnUiThread(() -> progressDialog.dismiss());
                    }
                }
        );
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedVideoUri != null) {
            outState.putString("selectedVideoUri", selectedVideoUri.toString());
        }
        outState.putString("taskName", taskName);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String uri = savedInstanceState.getString("selectedVideoUri");
        taskName = savedInstanceState.getString("taskName", taskName);
        if (taskName != null && !taskName.isEmpty()) {
            tvTaskTitle.setText("Upload " + taskName + " Video");
        }
        if (uri != null) {
            selectedVideoUri = Uri.parse(uri);
            previewVideo(selectedVideoUri);
        }
    }
}