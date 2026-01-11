package com.limelight.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import com.limelight.R;

/**
 * QuickBarMenu - A popup menu that appears near the Quick Bar FAB.
 * Automatically adjusts its position based on available screen space.
 */
public class QuickBarMenu {

    private final Context context;
    private final PopupWindow popupWindow;
    private final View menuView;
    private MenuCallbacks callbacks;

    public interface MenuCallbacks {
        void onKeyboardToggle();
        void onZoomToggle();
        void onHudToggle();
        void onControllerToggle();
        void onDisconnect();
        void onLockKeyboardToggle();
        boolean isLockKeyboardEnabled();
    }

    public QuickBarMenu(Context context, MenuCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;

        // Inflate menu layout
        LayoutInflater inflater = LayoutInflater.from(context);
        menuView = inflater.inflate(R.layout.quick_bar_menu, null);

        // Create popup window - NOT focusable to prevent closing keyboard
        popupWindow = new PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false // NOT focusable - allows keyboard to stay open
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setTouchable(true);
        // Non-focusable so keyboard stays open - toggle handled by isQuickBarMenuOpen flag
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setElevation(20f);

        setupButtons();
    }

    private void setupButtons() {
        ImageButton zoomBtn = menuView.findViewById(R.id.quickBarZoom);
        ImageButton hudBtn = menuView.findViewById(R.id.quickBarHud);
        ImageButton controllerBtn = menuView.findViewById(R.id.quickBarController);
        ImageButton disconnectBtn = menuView.findViewById(R.id.quickBarDisconnect);

        // Apply white tint to icons
        int[] buttonIds = {R.id.quickBarZoom, R.id.quickBarHud,
                          R.id.quickBarController, R.id.quickBarDisconnect};
        for (int id : buttonIds) {
            ImageButton btn = menuView.findViewById(id);
            if (btn != null) {
                btn.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            }
        }

        if (zoomBtn != null) {
            zoomBtn.setOnClickListener(v -> {
                if (callbacks != null) callbacks.onZoomToggle();
                dismiss();
            });
        }

        if (hudBtn != null) {
            hudBtn.setOnClickListener(v -> {
                if (callbacks != null) callbacks.onHudToggle();
                dismiss();
            });
        }

        if (controllerBtn != null) {
            controllerBtn.setOnClickListener(v -> {
                if (callbacks != null) callbacks.onControllerToggle();
                dismiss();
            });
        }

        if (disconnectBtn != null) {
            disconnectBtn.setOnClickListener(v -> {
                if (callbacks != null) callbacks.onDisconnect();
                dismiss();
            });
        }
    }


    /**
     * Show the menu near the anchor view, automatically adjusting direction
     * based on available screen space.
     */
    public void show(View anchor) {
        if (popupWindow == null || anchor == null) return;

        // Measure the popup
        menuView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int menuWidth = menuView.getMeasuredWidth();
        int menuHeight = menuView.getMeasuredHeight();

        // Get anchor position on screen
        int[] anchorLocation = new int[2];
        anchor.getLocationOnScreen(anchorLocation);
        int anchorX = anchorLocation[0];
        int anchorY = anchorLocation[1];
        int anchorWidth = anchor.getWidth();
        int anchorHeight = anchor.getHeight();

        // Get screen dimensions
        int screenWidth = anchor.getRootView().getWidth();
        int screenHeight = anchor.getRootView().getHeight();

        // Calculate available space in each direction
        int spaceAbove = anchorY;
        int spaceBelow = screenHeight - (anchorY + anchorHeight);
        int spaceLeft = anchorX;
        int spaceRight = screenWidth - (anchorX + anchorWidth);

        int xOffset = 0;
        int yOffset = 0;

        // Determine vertical position (PREFER ABOVE - upward)
        // Note: showAsDropDown positions relative to anchor's BOTTOM
        if (spaceAbove >= menuHeight) {
            // Show above - go up by menu height + anchor height + small gap
            yOffset = -menuHeight - anchorHeight - 8;
        } else if (spaceBelow >= menuHeight) {
            // Show below - just a small gap below anchor
            yOffset = 8;
        } else {
            // Not enough space, show above anyway
            yOffset = -menuHeight - anchorHeight - 8;
        }

        // Determine horizontal position (center on anchor, but stay on screen)
        int desiredX = anchorX + (anchorWidth / 2) - (menuWidth / 2);
        if (desiredX < 0) {
            xOffset = -anchorX;
        } else if (desiredX + menuWidth > screenWidth) {
            xOffset = screenWidth - menuWidth - anchorX - anchorWidth;
        } else {
            xOffset = (anchorWidth / 2) - (menuWidth / 2);
        }

        popupWindow.showAsDropDown(anchor, xOffset, yOffset, Gravity.START | Gravity.TOP);
    }

    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        if (popupWindow != null) {
            popupWindow.setOnDismissListener(listener);
        }
    }
}
