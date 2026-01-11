package com.limelight.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.limelight.R;
import com.limelight.binding.input.KeyboardTranslator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for managing the horizontal shortcut bar with keyboard shortcuts.
 * Has a dedicated drag handle at the end for repositioning.
 * Shortcuts are customizable via settings. Position is remembered.
 */
public class CircularQuickBar {

    private static final String PREF_POSITION_X = "shortcut_bar_pos_x";
    private static final String PREF_POSITION_Y = "shortcut_bar_pos_y";

    /**
     * Represents a keyboard shortcut with its keys, icon, and label.
     */
    public static class Shortcut {
        public final String id;
        public final String label;
        public final int iconResId;
        public final short[] keys;

        public Shortcut(String id, String label, int iconResId, short[] keys) {
            this.id = id;
            this.label = label;
            this.iconResId = iconResId;
            this.keys = keys;
        }
    }

    // All available shortcuts - productivity focused
    public static Map<String, Shortcut> getAllShortcuts(Context context) {
        Map<String, Shortcut> shortcuts = new HashMap<>();

        // Close window
        shortcuts.put("alt_f4", new Shortcut(
            "alt_f4",
            context.getString(R.string.shortcut_alt_f4),
            R.drawable.ic_shortcut_close,
            new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_F4}
        ));

        // Close tab
        shortcuts.put("ctrl_w", new Shortcut(
            "ctrl_w",
            context.getString(R.string.shortcut_ctrl_w),
            R.drawable.ic_shortcut_close_tab,
            new short[]{KeyboardTranslator.VK_LCONTROL, (short)87} // VK_W = 87
        ));

        // Show desktop
        shortcuts.put("win_d", new Shortcut(
            "win_d",
            context.getString(R.string.shortcut_win_d),
            R.drawable.ic_shortcut_desktop,
            new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_D}
        ));

        // Windows key
        shortcuts.put("win", new Shortcut(
            "win",
            context.getString(R.string.shortcut_win),
            R.drawable.ic_shortcut_windows,
            new short[]{KeyboardTranslator.VK_LWIN}
        ));

        // Task view
        shortcuts.put("win_tab", new Shortcut(
            "win_tab",
            context.getString(R.string.shortcut_win_tab),
            R.drawable.ic_shortcut_taskview,
            new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_TAB}
        ));

        // Escape
        shortcuts.put("escape", new Shortcut(
            "escape",
            context.getString(R.string.shortcut_escape),
            R.drawable.ic_shortcut_escape,
            new short[]{KeyboardTranslator.VK_ESCAPE}
        ));

        // Task Manager
        shortcuts.put("task_mgr", new Shortcut(
            "task_mgr",
            context.getString(R.string.shortcut_task_mgr),
            R.drawable.ic_shortcut_taskmgr,
            new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_ESCAPE}
        ));

        // Ctrl+Alt+Del
        shortcuts.put("cad", new Shortcut(
            "cad",
            context.getString(R.string.shortcut_cad),
            R.drawable.ic_shortcut_security,
            new short[]{(short)162, (short)164, (short)46}
        ));

        // Alt+Tab (switch windows)
        shortcuts.put("alt_tab", new Shortcut(
            "alt_tab",
            context.getString(R.string.shortcut_alt_tab),
            R.drawable.ic_shortcut_switch,
            new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_TAB}
        ));

        // Ctrl+C (copy) - VK_C = 67
        shortcuts.put("copy", new Shortcut(
            "copy",
            "Copy (Ctrl+C)",
            R.drawable.ic_shortcut_copy,
            new short[]{KeyboardTranslator.VK_LCONTROL, (short)67}
        ));

        // Ctrl+V (paste) - VK_V = 86
        shortcuts.put("paste", new Shortcut(
            "paste",
            "Paste (Ctrl+V)",
            R.drawable.ic_shortcut_paste,
            new short[]{KeyboardTranslator.VK_LCONTROL, (short)86}
        ));

        // Ctrl+Z (undo) - VK_Z = 90
        shortcuts.put("undo", new Shortcut(
            "undo",
            "Undo (Ctrl+Z)",
            R.drawable.ic_shortcut_undo,
            new short[]{KeyboardTranslator.VK_LCONTROL, (short)90}
        ));

        // Ctrl+S (save) - VK_S = 83
        shortcuts.put("save", new Shortcut(
            "save",
            "Save (Ctrl+S)",
            R.drawable.ic_shortcut_save,
            new short[]{KeyboardTranslator.VK_LCONTROL, (short)83}
        ));

        // Ctrl+A (select all) - VK_A = 65
        shortcuts.put("select_all", new Shortcut(
            "select_all",
            "Select All (Ctrl+A)",
            R.drawable.ic_shortcut_select_all,
            new short[]{KeyboardTranslator.VK_LCONTROL, (short)65}
        ));

        // Win+E (file explorer) - VK_E = 69
        shortcuts.put("explorer", new Shortcut(
            "explorer",
            "File Explorer (Win+E)",
            R.drawable.ic_shortcut_explorer,
            new short[]{KeyboardTranslator.VK_LWIN, (short)69}
        ));

        // Win+L (lock screen) - VK_L = 76
        shortcuts.put("lock", new Shortcut(
            "lock",
            "Lock Screen (Win+L)",
            R.drawable.ic_shortcut_lock,
            new short[]{KeyboardTranslator.VK_LWIN, (short)76}
        ));

        // Back (Alt+Left) - VK_LEFT = 37
        shortcuts.put("back", new Shortcut(
            "back",
            "Back (Alt+Left)",
            R.drawable.ic_shortcut_back,
            new short[]{KeyboardTranslator.VK_LMENU, (short)37}
        ));

        // Forward (Alt+Right) - VK_RIGHT = 39
        shortcuts.put("forward", new Shortcut(
            "forward",
            "Forward (Alt+Right)",
            R.drawable.ic_shortcut_forward,
            new short[]{KeyboardTranslator.VK_LMENU, (short)39}
        ));

        return shortcuts;
    }

    // Get shortcuts based on user settings
    public static List<Shortcut> getSelectedShortcuts(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> selectedIds = prefs.getStringSet("shortcut_bar_actions", getDefaultShortcutIds());

        Map<String, Shortcut> allShortcuts = getAllShortcuts(context);
        List<Shortcut> selected = new ArrayList<>();

        // Maintain order
        String[] order = {"alt_f4", "ctrl_w", "win_d", "win", "win_tab", "alt_tab", "escape", "task_mgr", "cad",
                          "copy", "paste", "undo", "save", "select_all", "explorer", "lock", "back", "forward"};
        for (String id : order) {
            if (selectedIds.contains(id) && allShortcuts.containsKey(id)) {
                selected.add(allShortcuts.get(id));
            }
        }

        return selected;
    }

    private static Set<String> getDefaultShortcutIds() {
        Set<String> defaults = new HashSet<>();
        defaults.add("alt_f4");
        defaults.add("win_d");
        defaults.add("win");
        defaults.add("win_tab");
        return defaults;
    }

    public interface ShortcutListener {
        void onShortcutPressed(short[] keys);
    }

    private final Context context;
    private final View container;
    private final ImageButton[] shortcutButtons = new ImageButton[10];
    private final int[] shortcutButtonIds = {
        R.id.shortcut0, R.id.shortcut1, R.id.shortcut2, R.id.shortcut3,
        R.id.shortcut4, R.id.shortcut5, R.id.shortcut6, R.id.shortcut7,
        R.id.shortcut8, R.id.shortcut9
    };

    private List<Shortcut> shortcuts;
    private ShortcutListener listener;

    // Drag handling
    private float dX, dY;

    @SuppressLint("ClickableViewAccessibility")
    public CircularQuickBar(View rootView, Context context, ShortcutListener listener) {
        this.context = context;
        this.listener = listener;
        this.shortcuts = getSelectedShortcuts(context);

        container = rootView.findViewById(R.id.circularQuickBarContainer);

        // Initialize shortcut buttons
        for (int i = 0; i < shortcutButtonIds.length; i++) {
            shortcutButtons[i] = rootView.findViewById(shortcutButtonIds[i]);
        }

        if (container == null) {
            return;
        }

        // Restore saved position
        restorePosition();

        // Set up shortcuts
        setupShortcuts();
    }

    private void savePosition() {
        if (container == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putFloat(PREF_POSITION_X, container.getX())
            .putFloat(PREF_POSITION_Y, container.getY())
            .apply();
    }

    private void restorePosition() {
        if (container == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        float x = prefs.getFloat(PREF_POSITION_X, -1);
        float y = prefs.getFloat(PREF_POSITION_Y, -1);

        if (x >= 0 && y >= 0) {
            // Validate position is within screen bounds
            ViewGroup parent = (ViewGroup) container.getParent();
            if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
                x = Math.max(0, Math.min(x, parent.getWidth() - container.getWidth()));
                y = Math.max(0, Math.min(y, parent.getHeight() - container.getHeight()));
            }
            container.setX(x);
            container.setY(y);
        }
    }

    private boolean isDraggingShortcut = false;
    private static final int LONG_PRESS_TIMEOUT = 300; // ms

    @SuppressLint("ClickableViewAccessibility")
    private void setupShortcuts() {
        for (int i = 0; i < shortcutButtons.length; i++) {
            ImageButton btn = shortcutButtons[i];
            if (btn == null) continue;

            if (i < shortcuts.size()) {
                Shortcut shortcut = shortcuts.get(i);
                btn.setImageResource(shortcut.iconResId);
                btn.setContentDescription(shortcut.label);
                btn.setVisibility(View.VISIBLE);
                btn.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

                final int index = i;
                final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                final Runnable longPressRunnable = () -> {
                    isDraggingShortcut = true;
                    // Vibrate to signal drag mode
                    android.os.Vibrator v = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null) {
                        v.vibrate(50);
                    }
                    container.animate().scaleX(1.1f).scaleY(1.1f).alpha(0.8f).setDuration(150).start();
                };

                btn.setOnTouchListener(new View.OnTouchListener() {
                    private float startX, startY;
                    private boolean moved = false;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                startX = event.getRawX();
                                startY = event.getRawY();
                                dX = container.getX() - event.getRawX();
                                dY = container.getY() - event.getRawY();
                                moved = false;
                                isDraggingShortcut = false;
                                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                                v.setPressed(true);
                                // Bring to full opacity when touched
                                container.animate().alpha(1.0f).setDuration(150).start();
                                return true;

                            case MotionEvent.ACTION_MOVE:
                                if (!isDraggingShortcut) {
                                    if (Math.abs(event.getRawX() - startX) > 10 || Math.abs(event.getRawY() - startY) > 10) {
                                        moved = true;
                                        handler.removeCallbacks(longPressRunnable);
                                    }
                                } else {
                                    float newX = event.getRawX() + dX;
                                    float newY = event.getRawY() + dY;
                                    ViewGroup parent = (ViewGroup) container.getParent();
                                    if (parent != null) {
                                        newX = Math.max(0, Math.min(newX, parent.getWidth() - container.getWidth()));
                                        newY = Math.max(0, Math.min(newY, parent.getHeight() - container.getHeight()));
                                    }
                                    container.setX(newX);
                                    container.setY(newY);
                                }
                                return true;

                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                handler.removeCallbacks(longPressRunnable);
                                v.setPressed(false);
                                if (isDraggingShortcut) {
                                    container.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.4f).setDuration(200).start();
                                    savePosition();
                                } else if (!moved && event.getAction() == MotionEvent.ACTION_UP) {
                                    if (listener != null) {
                                        listener.onShortcutPressed(shortcuts.get(index).keys);
                                    }
                                    // Fade back to idle opacity after a click
                                    container.animate().alpha(0.4f).setDuration(300).setStartDelay(500).start();
                                } else {
                                    // Fade back if it was just a touch or cancelled
                                    container.animate().alpha(0.4f).setDuration(200).start();
                                }
                                isDraggingShortcut = false;
                                return true;
                        }
                        return false;
                    }
                });
            } else {
                btn.setVisibility(View.GONE);
                btn.setOnTouchListener(null);
            }
        }
    }

    public void show() {
        if (container != null) {
            container.setVisibility(View.VISIBLE);
            container.bringToFront();
            // Defer position restore until view is laid out
            container.post(() -> {
                restorePosition();
            });
        }
    }

    public void hide() {
        if (container != null) {
            container.setVisibility(View.GONE);
        }
    }

    public boolean isVisible() {
        return container != null && container.getVisibility() == View.VISIBLE;
    }

    public void setShortcuts(List<Shortcut> shortcuts) {
        this.shortcuts = shortcuts;
        setupShortcuts();
    }

    public void refreshShortcuts(Context context) {
        this.shortcuts = getSelectedShortcuts(context);
        setupShortcuts();
    }
}
