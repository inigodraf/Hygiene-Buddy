package com.example.hygienebuddy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Handles real-time eye tracking using CameraX and ML Kit.
 * Draws landmarks using GraphicOverlay for visualization.
 */
public class EyeTrackerHelper {

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private FaceDetector detector;
    private boolean userIsLooking = true;
    private final EyeTrackerListener listener;

    public interface EyeTrackerListener {
        void onUserLookAway();
        void onUserLookBack();
    }

    public EyeTrackerHelper(Context context, LifecycleOwner lifecycleOwner, EyeTrackerListener listener) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.listener = listener;
        setupFaceDetector();
    }

    /** Configure ML Kit face detector */
    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build();

        detector = FaceDetection.getClient(options);
    }

    /** Start camera + ML Kit detection with overlay */
    @SuppressLint("UnsafeOptInUsageError")
    public void startEyeTracking(PreviewView previewView, GraphicOverlay graphicOverlay) {
        ProcessCameraProvider cameraProvider;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return;
        }

        // Camera Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // ML Kit Analysis
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context),
                imageProxy -> analyzeImage(imageProxy, graphicOverlay));

        // Unbind previous sessions
        cameraProvider.unbindAll();

        // Front camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        // Bind camera to lifecycle
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);

        // ✅ Sync overlay dimensions and mirroring
        previewView.post(() -> {
            graphicOverlay.setCameraInfo(
                    previewView.getWidth(),
                    previewView.getHeight(),
                    true // front camera mirror
            );
        });
    }

    /** Analyze image frames and detect faces */
    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(ImageProxy imageProxy, GraphicOverlay graphicOverlay) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        detector.process(image)
                .addOnSuccessListener(faces -> handleFaces(faces, graphicOverlay))
                .addOnFailureListener(e -> Log.e("EyeTrackerHelper", "Detection failed: " + e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    /** Handle detected faces and draw overlays */
    private void handleFaces(List<Face> faces, GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();

        if (faces.isEmpty()) {
            if (userIsLooking) {
                userIsLooking = false;
                listener.onUserLookAway();
            }
            return;
        }

        for (Face face : faces) {
            // Draw face and eyes
            graphicOverlay.add(new EyeGraphic(graphicOverlay, face));

            // Get rotation and eye open probabilities
            float rotY = face.getHeadEulerAngleY();
            Float leftEyeOpen = face.getLeftEyeOpenProbability();
            Float rightEyeOpen = face.getRightEyeOpenProbability();

            boolean eyesClosed = (leftEyeOpen != null && rightEyeOpen != null)
                    && (leftEyeOpen < 0.3f && rightEyeOpen < 0.3f);
            boolean lookingAway = Math.abs(rotY) > 25;

            if (eyesClosed || lookingAway) {
                if (userIsLooking) {
                    userIsLooking = false;
                    listener.onUserLookAway();
                }
            } else {
                if (!userIsLooking) {
                    userIsLooking = true;
                    listener.onUserLookBack();
                }
            }
        }

        // Redraw overlay
        graphicOverlay.postInvalidate();
    }

    /** Stop face detection */
    public void stop() {
        if (detector != null) {
            detector.close();
        }
    }
}

