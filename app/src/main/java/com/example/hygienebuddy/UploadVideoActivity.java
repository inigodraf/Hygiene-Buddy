package com.example.hygienebuddy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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

import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadVideoActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTaskTitle;
    private VideoView videoPreview;
    private View videoContainer;
    private Button btnUploadVideo, btnSaveVideo;
    private Uri selectedVideoUri;
    private String taskName = "";

    // Background executor to prevent the app from freezing during compression
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

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
        videoContainer = findViewById(R.id.layoutVideoContainer);
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
        btnSaveVideo.setOnClickListener(v -> saveAndCompressVideo());

        // Resize container to 16:9 after layout
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

        // Show a loading dialog so the user knows it is compressing
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Compressing video... Please wait.");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Run the heavy compression task in the background
        executor.execute(() -> {
            try {
                // Directory for app-specific videos
                File directory = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "CustomInstructions");
                if (!directory.exists()) directory.mkdirs();

                // 1. Compress the video to ~720p light mp4
                // SiliCompressor returns the file path of the newly compressed video
                String compressedFilePath = SiliCompressor.with(UploadVideoActivity.this)
                        .compressVideo(selectedVideoUri, directory.getAbsolutePath());

                File compressedFile = new File(compressedFilePath);

                // 2. Rename the file to match your custom naming convention
                String fileName = (taskName != null ? taskName.toLowerCase().replace(" ", "_") : "instruction")
                        + "_" + System.currentTimeMillis() + ".mp4";
                File finalDestFile = new File(directory, fileName);

                compressedFile.renameTo(finalDestFile);

                // 3. Register it with the MediaStore (Optional, keeps your original logic)
                Uri finalUri = getVideoUri(finalDestFile);

                // 4. Update the UI back on the Main Thread
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(UploadVideoActivity.this, "Video successfully compressed and saved!", Toast.LENGTH_LONG).show();

                    // Preview the newly compressed video
                    previewVideo(Uri.fromFile(finalDestFile));
                });

            } catch (Exception e) {
                e.printStackTrace();

                // Handle errors gracefully on the Main Thread
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(UploadVideoActivity.this, "Error compressing video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
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

    private Uri getVideoUri(File file) {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.TITLE, file.getName());
        values.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }
}