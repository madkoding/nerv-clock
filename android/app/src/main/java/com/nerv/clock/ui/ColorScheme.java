package com.nerv.clock.ui;

import android.graphics.Color;

/**
 * NERV Chronometer color scheme
 * Defines all colors used in the digital clock display
 */
public class ColorScheme {
    
    // NERV Official Colors
    public static final int NERV_ORANGE = Color.parseColor("#FF6A00");
    public static final int NERV_ORANGE_BRIGHT = Color.parseColor("#FF8C00");
    public static final int NERV_ORANGE_DIM = Color.parseColor("#260D00"); // ~15% opacity version
    public static final int NERV_ORANGE_GLOW = Color.parseColor("#FF6A00"); // With 50% alpha
    
    public static final int NERV_RED = Color.parseColor("#CC0000");
    public static final int NERV_RED_DARK = Color.parseColor("#880000");
    public static final int NERV_RED_STRIPE = Color.parseColor("#AA0000");
    
    public static final int NERV_GREEN = Color.parseColor("#00CC00");
    
    public static final int NERV_DARK = Color.parseColor("#0A0800");
    public static final int NERV_DARK_BROWN = Color.parseColor("#1A1400");
    
    public static final int SEGMENT_OFF = Color.parseColor("#260D00"); // Dim orange segments
    
    // Warning states
    public static final int WARNING_YELLOW = Color.parseColor("#FFCC00");
    public static final int CRITICAL_RED = Color.parseColor("#FF0000");
    
    // UI Elements
    public static final int BUTTON_BACKGROUND_TOP = Color.parseColor("#252520");
    public static final int BUTTON_BACKGROUND_BOTTOM = Color.parseColor("#151510");
    public static final int BUTTON_BORDER = Color.parseColor("#3a3a30");
    public static final int BUTTON_BORDER_HOVER = Color.parseColor("#555555");
    
    public static final int ACTIVE_BUTTON_BG_TOP = Color.parseColor("#200800");
    public static final int ACTIVE_BUTTON_BG_BOTTOM = Color.parseColor("#301000");
    
    // Status light
    public static final int STATUS_LIGHT_BRIGHT = Color.parseColor("#ff3333");
    public static final int STATUS_LIGHT_DARK = Color.parseColor("#800000");
    
    // Background gradient (semitransparent - 85% opacity)
    public static final int BG_TOP = Color.argb(217, 26, 20, 0);       // #1a1400 with 85% alpha
    public static final int BG_BOTTOM = Color.argb(217, 13, 9, 0);     // #0d0900 with 85% alpha
    
    public static final int BORDER_COLOR = NERV_ORANGE;
    
    // Alphas for various effects
    public static final int ALPHA_GLOW = 128;      // 50%
    public static final int ALPHA_DIM = 39;        // 15%
    public static final int ALPHA_WEAK = 25;       // ~10%
    
    // Mode colors
    public static int getModeColor(String mode) {
        switch (mode.toLowerCase()) {
            case "racing":
                return NERV_RED;
            case "slow":
                return WARNING_YELLOW;
            case "stop":
            case "paused":
                return Color.parseColor("#888888");
            case "normal":
            default:
                return NERV_GREEN;
        }
    }
    
    // Mode text shadow colors (with glow)
    public static int getModeShadowColor(String mode) {
        return getModeColor(mode);
    }
}
