package com.example.hygienebuddy;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A view that overlays graphics on top of a camera preview.
 * Handles scaling, rotation, offsets, and mirroring for MediaPipe's normalized coordinates.
 */
public class GraphicOverlay extends View {

    private final Object lock = new Object();
    private int imageWidth;
    private int imageHeight;
    private boolean isFrontFacing = true;

    // Calculated values for mapping MediaPipe coordinates to the screen
    private float scaleFactor = 1.0f;
    private float postScaleWidthOffset = 0f;
    private float postScaleHeightOffset = 0f;

    private final List<Graphic> graphics = new ArrayList<>();

    public abstract static class Graphic {
        private final GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        /**
         * Converts MediaPipe's normalized X coordinate (0.0 to 1.0) to screen pixels.
         * Also handles horizontal flipping for the front camera.
         */
        public float translateX(float normalizedX) {
            float x = (normalizedX * overlay.imageWidth * overlay.scaleFactor) + overlay.postScaleWidthOffset;
            if (overlay.isFrontFacing) {
                // Flip horizontally for front camera
                return overlay.getWidth() - x;
            } else {
                return x;
            }
        }

        /**
         * Converts MediaPipe's normalized Y coordinate (0.0 to 1.0) to screen pixels.
         */
        public float translateY(float normalizedY) {
            return (normalizedY * overlay.imageHeight * overlay.scaleFactor) + overlay.postScaleHeightOffset;
        }

        public void postInvalidate() {
            overlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
        postInvalidate();
    }

    /**
     * Called when a new frame is processed.
     * @param imageWidth The width of the frame MediaPipe processed
     * @param imageHeight The height of the frame MediaPipe processed
     * @param isFrontFacing True if using the selfie camera
     */
    public void setCameraInfo(int imageWidth, int imageHeight, boolean isFrontFacing) {
        synchronized (lock) {
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.isFrontFacing = isFrontFacing;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (lock) {
            if (imageWidth > 0 && imageHeight > 0) {
                // Calculate scaling and offsets assuming the PreviewView is set to FILL_CENTER
                float viewAspectRatio = (float) getWidth() / getHeight();
                float imageAspectRatio = (float) imageWidth / imageHeight;

                if (viewAspectRatio > imageAspectRatio) {
                    // The view is wider than the image's aspect ratio
                    scaleFactor = (float) getWidth() / imageWidth;
                    postScaleHeightOffset = (getHeight() - (imageHeight * scaleFactor)) / 2f;
                    postScaleWidthOffset = 0f;
                } else {
                    // The view is taller than the image's aspect ratio
                    scaleFactor = (float) getHeight() / imageHeight;
                    postScaleWidthOffset = (getWidth() - (imageWidth * scaleFactor)) / 2f;
                    postScaleHeightOffset = 0f;
                }
            }

            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }
}