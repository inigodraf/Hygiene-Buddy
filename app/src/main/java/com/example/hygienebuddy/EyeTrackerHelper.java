package com.example.hygienebuddy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optimized Eye Tracker using MediaPipe.
 * Features: 2-second distraction delay, smoothed blink detection, and UI thread safety.
 */
public class EyeTrackerHelper {

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private FaceLandmarker faceLandmarker;
    private final EyeTrackerListener listener;
    private GraphicOverlay currentOverlay;

    // --- Logic & Timing ---
    private boolean userIsLooking = true;
    private long distractionStartTime = 0;
    private boolean isCurrentlyFlagged = false;
    private static final long DISTRACTION_THRESHOLD_MS = 1200; // 2 Seconds

    // --- Performance Optimizations ---
    private final Matrix rotationMatrix = new Matrix();
    private boolean isProcessing = false; // Add this flag
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private long lastTimestampMs = 0;

    private volatile boolean isStopped = false;


    public interface EyeTrackerListener {
        void onUserLookAway();

        void onUserLookBack();
    }

    public EyeTrackerHelper(Context context, LifecycleOwner lifecycleOwner, EyeTrackerListener listener) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.listener = listener;
        setupFaceLandmarker();
    }

    private void setupFaceLandmarker() {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build();

        FaceLandmarker.FaceLandmarkerOptions options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setOutputFaceBlendshapes(true)
                .setResultListener(this::onLandmarkerResult)
                .setErrorListener(e -> {
                    isProcessing = false; // Release lock on error
                    Log.e("EyeTrackerHelper", "MediaPipe Error: ", e);
                })
                .build();

        faceLandmarker = FaceLandmarker.createFromOptions(context, options);
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void startEyeTracking(PreviewView previewView, GraphicOverlay graphicOverlay) {
        isStopped = false;
        this.currentOverlay = graphicOverlay;

        backgroundExecutor.execute(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(context).get();
                Preview preview = new Preview.Builder().build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                imageAnalysis.setAnalyzer(backgroundExecutor, this::analyzeImage);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                mainHandler.post(() -> {
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);
                });

            } catch (Exception e) {
                Log.e("EyeTrackerHelper", "Camera initialization failed", e);
            }
        });
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        // 1. Drop the frame if MediaPipe is still busy or closed

        if (isStopped) {
            imageProxy.close();
            return;
        }

        if (faceLandmarker == null || isProcessing) {
            imageProxy.close();
            return;
        }

        isProcessing = true; // 2. Lock the pipeline

        Bitmap bitmap = imageProxy.toBitmap();
        if (bitmap == null) {
            isProcessing = false; // Release lock if bitmap fails
            imageProxy.close();
            return;
        }

        rotationMatrix.reset();
        rotationMatrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotationMatrix, true);

        MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();

        // 3. Ensure strictly increasing timestamps (MediaPipe will crash otherwise)
        long currentTimestampMs = android.os.SystemClock.uptimeMillis();
        if (currentTimestampMs <= lastTimestampMs) {
            currentTimestampMs = lastTimestampMs + 1;
        }
        lastTimestampMs = currentTimestampMs;

        // 4. Safely process the frame
        try {
            faceLandmarker.detectAsync(mpImage, currentTimestampMs);
        } catch (RuntimeException e) {
            // Catch graph initialization errors or closed graph errors
            Log.e("EyeTrackerHelper", "MediaPipe skipped frame: " + e.getMessage());
            isProcessing = false; // CRITICAL: Release the lock so the camera doesn't freeze!
        } finally {
            // ALWAYS close the proxy so CameraX can send the next frame
            imageProxy.close();
        }
    }

    private void onLandmarkerResult(FaceLandmarkerResult result, MPImage inputImage) {
        // UI Updates MUST happen on the Main Thread
        isProcessing = false;
        mainHandler.post(() -> {
            if (currentOverlay == null) return;
            currentOverlay.clear();

            if (result.faceLandmarks().isEmpty()) {
                handleDistraction(true); // Treat "no face" as a distraction
                return;
            }

            // Sync coordinate mapping
            currentOverlay.setCameraInfo(inputImage.getWidth(), inputImage.getHeight(), true);

            List<NormalizedLandmark> face = result.faceLandmarks().get(0);
            currentOverlay.add(new EyeGraphic(currentOverlay, face));
            currentOverlay.postInvalidate();

            processFaceDistractionLogic(face, result);
        });
    }

    private void processFaceDistractionLogic(List<NormalizedLandmark> face, FaceLandmarkerResult result) {
        boolean eyesClosed = false;
        boolean eyesDarting = false; // NEW: Tracks pupil movement

        Optional<List<List<Category>>> blendshapesOpt = result.faceBlendshapes();

        if (blendshapesOpt.isPresent() && !blendshapesOpt.get().isEmpty()) {
            List<Category> blendshapes = blendshapesOpt.get().get(0);

            // 1. Blink Detection
            float avgBlinkScore = (blendshapes.get(9).score() + blendshapes.get(10).score()) / 2f;
            eyesClosed = avgBlinkScore > 0.5f;

            // 2. Eye Gaze Detection (Darting left, right, up, or down)
            // Indices: 11(DownL), 12(DownR), 13(InL), 14(InR), 15(OutL), 16(OutR), 17(UpL), 18(UpR)
            float lookLeftRight = Math.max(
                    Math.max(blendshapes.get(13).score(), blendshapes.get(14).score()),
                    Math.max(blendshapes.get(15).score(), blendshapes.get(16).score())
            );

            float lookUpDown = Math.max(
                    Math.max(blendshapes.get(11).score(), blendshapes.get(12).score()),
                    Math.max(blendshapes.get(17).score(), blendshapes.get(18).score())
            );

            // If any eye direction scores higher than 0.55, the user is looking away
            if (lookLeftRight > 0.55f || lookUpDown > 0.55f) {
                eyesDarting = true;
            }
        }

        // 3. Head Rotation Check (Nose ratio)
        float noseX = face.get(1).x();
        float leftCheekX = face.get(234).x();
        float rightCheekX = face.get(454).x();
        float noseRatio = (noseX - leftCheekX) / (rightCheekX - leftCheekX);
        boolean headTurned = (noseRatio < 0.28f || noseRatio > 0.72f);

        // Flag the user if ANY of these 3 conditions are met!
        handleDistraction(eyesClosed || headTurned || eyesDarting);
    }

    private void handleDistraction(boolean isDistracted) {
        long currentTime = System.currentTimeMillis();

        if (isDistracted) {
            if (distractionStartTime == 0) {
                distractionStartTime = currentTime; // Start the timer
            } else if (currentTime - distractionStartTime >= DISTRACTION_THRESHOLD_MS) {
                if (!isCurrentlyFlagged) {
                    isCurrentlyFlagged = true;
                    listener.onUserLookAway();
                }
            }
        } else {
            // User is back!
            if (isCurrentlyFlagged) {
                isCurrentlyFlagged = false;
                listener.onUserLookBack();
            }
            distractionStartTime = 0; // Reset timer
        }
    }

    public void stop() {
        // 1. Instantly block all new frames permanently
        isStopped = true;

        // Lock the pipeline so no new frames enter analyzeImage
        isProcessing = true;

        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdownNow();
        }

        if (faceLandmarker != null) {
            try {
                faceLandmarker.close();
            } catch (Exception e) {
                Log.e("EyeTrackerHelper", "Error closing FaceLandmarker", e);
            } finally {
                faceLandmarker = null;
            }
        }
    }

}