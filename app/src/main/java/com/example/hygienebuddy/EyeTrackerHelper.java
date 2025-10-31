package com.example.hygienebuddy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;

public class EyeTrackerHelper {

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private FaceDetector detector;
    private boolean userIsLooking = true;
    private EyeTrackerListener listener;

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

    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        detector = FaceDetection.getClient(options);
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void startEyeTracking(SurfaceView previewView) {
        ProcessCameraProvider cameraProvider;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return;
        }

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), imageProxy -> {
            analyzeImage(imageProxy);
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                imageAnalysis
        );
    }

    private void analyzeImage(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        detector.process(image)
                .addOnSuccessListener(faces -> handleFaces(faces))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleFaces(java.util.List<Face> faces) {
        if (faces.isEmpty()) {
            if (userIsLooking) {
                userIsLooking = false;
                listener.onUserLookAway();
            }
        } else {
            Face face = faces.get(0);
            float rotY = face.getHeadEulerAngleY(); // Y-axis rotation
            if (Math.abs(rotY) > 25) { // Threshold for looking away
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
    }

    public void stop() {
        detector.close();
    }
}
