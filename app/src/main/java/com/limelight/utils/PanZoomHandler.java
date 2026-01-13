package com.limelight.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.widget.Toast;

import com.limelight.Game;
import com.limelight.LimeLog;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.R;

public class PanZoomHandler {
    static private final float MAX_SCALE = 10.0f;

    private final Game game;
    private final View streamView;
    private final PreferenceConfiguration prefConfig;
    private final boolean isTopMode;
    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;
    private View parent;
    private View boundaryView;
    private View warningView;
    private View gameScreenBorderView;
    private float scaleFactor = 1.0f;
    private float childX, childY = 0;
    private float parentWidth, parentHeight = 0;
    private float childWidth, childHeight = 0;
    private long lastWarningTime = 0;
    private static final long WARNING_THRESHOLD = 2000; // 2 seconds

    // Temporary zoom mode state
    private boolean isTemporaryZoomModeActive = false;
    private float initialScaleFactor = 1.0f;
    private float initialChildX, initialChildY = 0;

    // Two-thumb hold detection state
    private long twoThumbDownTime = 0;
    private static final long HOLD_THRESHOLD = 1000; // 1 second
    private static final float MOVE_THRESHOLD = 50.0f; // tolerance for jitter
    private float firstFingerDownX, firstFingerDownY;
    private float secondFingerDownX, secondFingerDownY;

    public PanZoomHandler(Context context, Game game, View streamView, View parent, PreferenceConfiguration prefConfig) {
        this.game = game;
        this.streamView = streamView;
        this.parent = parent;
        this.prefConfig = prefConfig;
        this.isTopMode = prefConfig.alignDisplayTopCenter;
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        // Everything gets easier with 0,0 as the pivot point
        streamView.setPivotX(0);
        streamView.setPivotY(0);
    }

    public void handleTouchEvent(MotionEvent motionEvent) {
        if (prefConfig.twoThumbZoom) {
            detectTwoThumbHold(motionEvent);
        }

        if (isTemporaryZoomModeActive || game.isZoomModeEnabled()) {
            scaleGestureDetector.onTouchEvent(motionEvent);
            gestureDetector.onTouchEvent(motionEvent);

            int action = motionEvent.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                game.saveZoomPan();
            }
        }
    }

    private void detectTwoThumbHold(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        if (isTemporaryZoomModeActive) {
            return;
        }

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount == 2) {
                    twoThumbDownTime = System.currentTimeMillis();
                    firstFingerDownX = event.getX(0);
                    firstFingerDownY = event.getY(0);
                    secondFingerDownX = event.getX(1);
                    secondFingerDownY = event.getY(1);
                } else {
                    twoThumbDownTime = 0;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (twoThumbDownTime != 0 && pointerCount == 2) {
                    float dx1 = event.getX(0) - firstFingerDownX;
                    float dy1 = event.getY(0) - firstFingerDownY;
                    float dx2 = event.getX(1) - secondFingerDownX;
                    float dy2 = event.getY(1) - secondFingerDownY;

                    if (Math.abs(dx1) > MOVE_THRESHOLD || Math.abs(dy1) > MOVE_THRESHOLD ||
                            Math.abs(dx2) > MOVE_THRESHOLD || Math.abs(dy2) > MOVE_THRESHOLD) {
                        twoThumbDownTime = 0;
                    } else if (System.currentTimeMillis() - twoThumbDownTime > HOLD_THRESHOLD) {
                        activateTemporaryZoomMode();
                        twoThumbDownTime = 0;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                twoThumbDownTime = 0;
                break;
        }
    }

    private void activateTemporaryZoomMode() {
        isTemporaryZoomModeActive = true;
        initialScaleFactor = scaleFactor;
        initialChildX = childX;
        initialChildY = childY;

        Vibrator vibrator = (Vibrator) game.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }

        // We sync boundary visibility in Game.java by checking isPanZoomMode,
        // so we might need a way to tell Game that we are in temp mode too.
        game.updateZoomStatus();
    }

    private void deactivateTemporaryZoomMode() {
        if (!isTemporaryZoomModeActive) return;
        isTemporaryZoomModeActive = false;

        scaleFactor = initialScaleFactor;
        childX = initialChildX;
        childY = initialChildY;
        syncViewPositions();

        game.updateZoomStatus();
    }

    public boolean isTemporaryZoomModeActive() {
        return isTemporaryZoomModeActive;
    }

    public void setBoundaryView(View boundaryView) {
        this.boundaryView = boundaryView;
    }

    public void setWarningView(View warningView) {
        this.warningView = warningView;
    }

    public void setGameScreenBorderView(View gameScreenBorderView) {
        this.gameScreenBorderView = gameScreenBorderView;
        if (gameScreenBorderView != null) {
            gameScreenBorderView.setPivotX(0);
            gameScreenBorderView.setPivotY(0);
        }
    }


    private void updateDimensions() {
        childHeight = streamView.getHeight() * scaleFactor;
        childWidth = streamView.getWidth() * scaleFactor;
        parentWidth = parent.getWidth();
        parentHeight = parent.getHeight();
    }

    private void constrainToScreen() {
        updateDimensions();

        // Don't constrain if we don't have valid dimensions yet
        if (parentWidth <= 0 || parentHeight <= 0 || streamView.getWidth() <= 0 || streamView.getHeight() <= 0) {
            return;
        }

        // Keep at least 50% of the stream visible on screen
        float halfChildWidth = childWidth * 0.5f;
        float halfChildHeight = childHeight * 0.5f;

        // X constraint: keep at least half the stream visible
        float minX = halfChildWidth - childWidth;  // Left edge can go off-screen by half
        float maxX = parentWidth - halfChildWidth; // Right edge can go off-screen by half
        if (childWidth > parentWidth) {
            // If zoomed in larger than screen, allow panning within content
            minX = parentWidth - childWidth;
            maxX = 0;
        }

        boolean hitX = childX < minX || childX > maxX;
        childX = Math.max(minX, Math.min(childX, maxX));

        // Y constraint: keep at least half the stream visible
        float minY = halfChildHeight - childHeight; // Top edge can go off-screen by half
        float maxY = parentHeight - halfChildHeight; // Bottom edge can go off-screen by half
        if (childHeight > parentHeight) {
            // If zoomed in larger than screen, allow panning within content
            minY = parentHeight - childHeight;
            maxY = 0;
        }

        boolean hitY = childY < minY || childY > maxY;
        childY = Math.max(minY, Math.min(childY, maxY));

        syncViewPositions();

        if (boundaryView != null) {
            if (hitX || hitY) {
                // Flash red or intensify border when hitting limit
                boundaryView.setBackgroundResource(R.drawable.pan_zoom_border_hit);
                boundaryView.setAlpha(1.0f);

                if (warningView != null) {
                    warningView.setVisibility(View.VISIBLE);
                }

                // Provide haptic feedback and warn user
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastWarningTime > WARNING_THRESHOLD) {
                    Vibrator vibrator = (Vibrator) game.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(50);
                        }
                    }
                    lastWarningTime = currentTime;
                }
            } else {
                // Default bright red border
                boundaryView.setBackgroundResource(R.drawable.pan_zoom_border);
                boundaryView.setAlpha(0.8f);

                if (warningView != null) {
                    warningView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void syncViewPositions() {
        streamView.setX(childX);
        streamView.setY(childY);
        streamView.setScaleX(scaleFactor);
        streamView.setScaleY(scaleFactor);

        if (gameScreenBorderView != null) {
            // First ensure the base size matches the streamView
            if (gameScreenBorderView.getWidth() != streamView.getWidth() ||
                gameScreenBorderView.getHeight() != streamView.getHeight()) {
                android.view.ViewGroup.LayoutParams params = gameScreenBorderView.getLayoutParams();
                params.width = streamView.getWidth();
                params.height = streamView.getHeight();
                gameScreenBorderView.setLayoutParams(params);
            }

            // Sync transformation
            gameScreenBorderView.setX(childX);
            gameScreenBorderView.setY(childY);
            gameScreenBorderView.setScaleX(scaleFactor);
            gameScreenBorderView.setScaleY(scaleFactor);
        }
    }

    public void handleSurfaceChange() {
        if (childWidth == 0 || parent == null) {
            // Retrieve parent, should handle both built-in display and external display
            parent = (View)streamView.getParent();
            return;
        }

        float prevChildWidth = childWidth;
        float prevChildHeight = childHeight;
        float prevParentWidth = parentWidth;
        float prevParentHeight = parentHeight;

        updateDimensions();

        float viewScaleX = childWidth / prevChildWidth;
        float viewScaleY = childHeight / prevChildHeight;

        float dPivotX1 = childX - prevParentWidth / 2;
        float dPivotY1 = childY - prevParentHeight / 2;

        float dPivotX2 = dPivotX1 * viewScaleX;
        float dPivotY2 = dPivotY1 * viewScaleY;

        childX = dPivotX2 + parentWidth / 2;
        childY = dPivotY2 + parentHeight / 2;

        constrainToScreen();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float lastScaleFactor = scaleFactor;
            float newScaleFactor = scaleFactor * detector.getScaleFactor();

            if (isTemporaryZoomModeActive) {
                if (newScaleFactor < initialScaleFactor) {
                    newScaleFactor = initialScaleFactor;
                }
            } else {
                newScaleFactor = Math.max(1, Math.min(newScaleFactor, MAX_SCALE));
            }

            // Calculate pivot point
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            float dPivotX = (childX - focusX) / scaleFactor * newScaleFactor;
            float dPivotY = (childY - focusY) / scaleFactor * newScaleFactor;

            childX = focusX + dPivotX;
            childY = focusY + dPivotY;

            scaleFactor = newScaleFactor;

            if (isTemporaryZoomModeActive && lastScaleFactor > initialScaleFactor && scaleFactor <= initialScaleFactor) {
                deactivateTemporaryZoomMode();
            } else {
                constrainToScreen();
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            game.updatePipAutoEnter();
            game.saveZoomPan();
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (isTemporaryZoomModeActive && e2.getPointerCount() < 2) {
                return false;
            }
            childX -= distanceX;
            childY -= distanceY;

            constrainToScreen();
            return true;
        }
    }

    public void setInitialZoomAndPan(float scale, float offsetX, float offsetY) {
        this.scaleFactor = scale;
        this.childX = offsetX;
        this.childY = offsetY;

        // Also initialize initial values so temp zoom resets to this restored state
        this.initialScaleFactor = scale;
        this.initialChildX = offsetX;
        this.initialChildY = offsetY;

        syncViewPositions();
    }

    public float getScaleFactor() { return scaleFactor; }
    public float getChildX() { return childX; }
    public float getChildY() { return childY; }

    public float getInitialScaleFactor() { return initialScaleFactor; }
    public float getInitialChildX() { return initialChildX; }
    public float getInitialChildY() { return initialChildY; }
}
