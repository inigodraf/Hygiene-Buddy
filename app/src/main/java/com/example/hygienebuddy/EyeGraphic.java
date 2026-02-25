package com.example.hygienebuddy;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;

/**
 * Draws a bounding box around the face and highly accurate markers directly on the pupils.
 * The artificial offset has been removed so it aligns perfectly with the camera preview.
 */
public class EyeGraphic extends GraphicOverlay.Graphic {
    private final List<NormalizedLandmark> faceLandmarks;

    private final Paint faceBoxStrokePaint;
    private final Paint faceBoxFillPaint;
    private final Paint pupilPaint;

    public EyeGraphic(GraphicOverlay overlay, List<NormalizedLandmark> faceLandmarks) {
        super(overlay);
        this.faceLandmarks = faceLandmarks;

        // Glowing Outline for the face
        faceBoxStrokePaint = new Paint();
        faceBoxStrokePaint.setColor(Color.parseColor("#00E5FF")); // Cyan
        faceBoxStrokePaint.setStyle(Paint.Style.STROKE);
        faceBoxStrokePaint.setStrokeWidth(6.0f);
        faceBoxStrokePaint.setAntiAlias(true);
        faceBoxStrokePaint.setStrokeCap(Paint.Cap.ROUND);

        // Faint Inner Fill
        faceBoxFillPaint = new Paint();
        faceBoxFillPaint.setColor(Color.parseColor("#00E5FF"));
        faceBoxFillPaint.setAlpha(25);
        faceBoxFillPaint.setStyle(Paint.Style.FILL);
        faceBoxFillPaint.setAntiAlias(true);

        // Solid Pupil Dots
        pupilPaint = new Paint();
        pupilPaint.setColor(Color.parseColor("#FF0055")); // Hot Pink/Red so it stands out!
        pupilPaint.setStyle(Paint.Style.FILL);
        pupilPaint.setAntiAlias(true);
        pupilPaint.setShadowLayer(4.0f, 0f, 0f, Color.BLACK); // Shadow to pop against the eye
    }

    @Override
    public void draw(Canvas canvas) {
        if (faceLandmarks == null || faceLandmarks.isEmpty()) return;

        // 1. Find the edges of the face
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (NormalizedLandmark landmark : faceLandmarks) {
            minX = Math.min(minX, landmark.x());
            maxX = Math.max(maxX, landmark.x());
            minY = Math.min(minY, landmark.y());
            maxY = Math.max(maxY, landmark.y());
        }

        // 2. Convert to screen pixels
        float screenX1 = translateX(minX);
        float screenX2 = translateX(maxX);
        float screenTop = translateY(minY);
        float screenBottom = translateY(maxY);

        float left = Math.min(screenX1, screenX2);
        float right = Math.max(screenX1, screenX2);
        float top = Math.min(screenTop, screenBottom);
        float bottom = Math.max(screenTop, screenBottom);

        // 3. Scale the box down slightly so it frames the face tightly (80% size)
        float centerX = (left + right) / 2.0f;
        float centerY = (top + bottom) / 2.0f;
        float scaledWidth = (right - left) * 0.80f;
        float scaledHeight = (bottom - top) * 0.80f;

        left = centerX - (scaledWidth / 2.0f);
        right = centerX + (scaledWidth / 2.0f);
        top = centerY - (scaledHeight / 2.0f);
        bottom = centerY + (scaledHeight / 2.0f);

        // NO MORE SHIFT TRANSLATION HERE! It will draw exactly where you are.

        float cornerRadius = 40.0f;

        // Draw the face framing box
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, faceBoxFillPaint);
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, faceBoxStrokePaint);

        // 4. Draw the exact Pupil Centers (Iris Tracking)
        drawPupil(canvas, 468); // Left Pupil Center
        drawPupil(canvas, 473); // Right Pupil Center
    }

    private void drawPupil(Canvas canvas, int landmarkIndex) {
        if (landmarkIndex >= 0 && landmarkIndex < faceLandmarks.size()) {
            NormalizedLandmark landmark = faceLandmarks.get(landmarkIndex);
            float cx = translateX(landmark.x());
            float cy = translateY(landmark.y());

            // Draw a solid dot exactly on the pupil
            canvas.drawCircle(cx, cy, 10.0f, pupilPaint);
        }
    }
}