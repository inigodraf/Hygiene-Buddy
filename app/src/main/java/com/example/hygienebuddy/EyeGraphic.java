package com.example.hygienebuddy;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

/**
 * Draws a smaller bounding box around the face and markers for the eyes.
 * Keeps your original offset (up 130%, right 30%).
 */
public class EyeGraphic extends GraphicOverlay.Graphic {
    private final Face face;
    private final Paint faceBoxPaint;
    private final Paint eyePaint;

    public EyeGraphic(GraphicOverlay overlay, Face face) {
        super(overlay);
        this.face = face;

        // Paint for the face box
        faceBoxPaint = new Paint();
        faceBoxPaint.setColor(Color.GREEN);
        faceBoxPaint.setStyle(Paint.Style.STROKE);
        faceBoxPaint.setStrokeWidth(5.0f);

        // Paint for the eyes
        eyePaint = new Paint();
        eyePaint.setColor(Color.CYAN);
        eyePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        if (face == null) return;

        // Get bounding box info
        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());
        float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
        float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);

        // ✅ Make the box smaller (about 70% of original size)
        float boxScaleFactor = 0.55f;
        xOffset *= boxScaleFactor;
        yOffset *= boxScaleFactor;

        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

        // ✅ Keep your custom offsets
        float verticalOffsetFactor = .9f; // same as your 130%
        float shiftY = canvas.getHeight() * verticalOffsetFactor;
        float horizontalOffsetFactor = 0.45f; // same as your 30%
        float shiftX = canvas.getWidth() * horizontalOffsetFactor;

        canvas.translate(shiftX, -shiftY);

        // Draw bounding box and eyes
        canvas.drawRect(left, top, right, bottom, faceBoxPaint);
        //drawEye(canvas, FaceLandmark.LEFT_EYE);
        //drawEye(canvas, FaceLandmark.RIGHT_EYE);

        // Reset translation
        canvas.translate(-shiftX, shiftY);
    }

    private void drawEye(Canvas canvas, int eyeType) {
        FaceLandmark landmark = face.getLandmark(eyeType);
        if (landmark != null) {
            PointF point = landmark.getPosition();
            canvas.drawCircle(translateX(point.x), translateY(point.y), 9.0f, eyePaint);
        }
    }
}
