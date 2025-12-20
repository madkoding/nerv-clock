package com.nerv.clock.ui;

import android.graphics.Color;

/**
 * NERV Chronometer color scheme
 * Defines all colors used in the digital clock display
 * Supports multiple color themes
 */
public class ColorScheme {
    
    // Theme names
    public static final String[] THEME_NAMES = {
        "orange", "cyan", "green", "purple", "pink", "yellow", "red", "white"
    };
    
    // Current theme index (0 = orange/default)
    private static int currentThemeIndex = 0;
    
    // Theme primary colors
    private static final int[] THEME_PRIMARY_COLORS = {
        Color.parseColor("#FF6A00"),  // Orange (default)
        Color.parseColor("#00CCFF"),  // Cyan
        Color.parseColor("#00FF00"),  // Green
        Color.parseColor("#AA00FF"),  // Purple
        Color.parseColor("#FF69B4"),  // Pink
        Color.parseColor("#FFFF00"),  // Yellow
        Color.parseColor("#FF0000"),  // Red
        Color.parseColor("#FFFFFF"),  // White
    };
    
    // Theme dim colors (for off segments)
    private static final int[] THEME_DIM_COLORS = {
        Color.parseColor("#260D00"),  // Orange dim
        Color.parseColor("#002233"),  // Cyan dim
        Color.parseColor("#003300"),  // Green dim
        Color.parseColor("#1A0033"),  // Purple dim
        Color.parseColor("#331122"),  // Pink dim
        Color.parseColor("#333300"),  // Yellow dim
        Color.parseColor("#330000"),  // Red dim
        Color.parseColor("#333333"),  // White dim
    };
    
    // Theme border colors
    private static final int[] THEME_BORDER_COLORS = {
        Color.parseColor("#FF6A00"),  // Orange
        Color.parseColor("#00CCFF"),  // Cyan
        Color.parseColor("#00FF00"),  // Green
        Color.parseColor("#AA00FF"),  // Purple
        Color.parseColor("#FF69B4"),  // Pink
        Color.parseColor("#FFFF00"),  // Yellow
        Color.parseColor("#FF0000"),  // Red
        Color.parseColor("#FFFFFF"),  // White
    };
    
    // Set current theme
    public static void setTheme(int themeIndex) {
        if (themeIndex >= 0 && themeIndex < THEME_PRIMARY_COLORS.length) {
            currentThemeIndex = themeIndex;
        }
    }
    
    // Get current theme index
    public static int getThemeIndex() {
        return currentThemeIndex;
    }
    
    // Cycle to next theme
    public static int nextTheme() {
        currentThemeIndex = (currentThemeIndex + 1) % THEME_PRIMARY_COLORS.length;
        return currentThemeIndex;
    }
    
    // Get theme name
    public static String getThemeName() {
        return THEME_NAMES[currentThemeIndex];
    }
    
    // Dynamic colors based on current theme
    public static int getPrimaryColor() {
        return THEME_PRIMARY_COLORS[currentThemeIndex];
    }
    
    public static int getDimColor() {
        return THEME_DIM_COLORS[currentThemeIndex];
    }
    
    public static int getBorderColor() {
        return THEME_BORDER_COLORS[currentThemeIndex];
    }
    
    // NERV Official Colors (legacy - still used for some elements)
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
