package com.nerv.clock.ui;

/**
 * Configuration for UI layout and sizing
 * These values ensure the Java implementation matches the HTML exactly
 */
public class UIConfig {
    
    // Responsive sizing (all relative to screen dimensions)
    
    // Margins (as % of screen)
    public static final float MARGIN_TOP = 0.01f;          // 1% of height
    public static final float MARGIN_BOTTOM = 0.01f;       // 1% of height
    public static final float MARGIN_SIDES = 0.02f;        // 2% of width
    
    // Sections (as % of height)
    public static final float TOP_BAR_HEIGHT = 0.15f;      // 15% of height
    public static final float CONTROL_BAR_HEIGHT = 0.12f;  // 12% of height
    
    // Gaps (as % of width)
    public static final float GAP_BUTTONS = 0.01f;         // 1% of width between buttons
    public static final float GAP_DIGITS = 0.005f;         // 0.5% of width between digit groups
    
    // Text sizing (relative to container)
    public static final float TEXT_SIZE_JAPANESE = 0.4f;   // 40% of top bar height
    public static final float TEXT_SIZE_LABEL = 0.3f;      // 30% of top bar height
    public static final float TEXT_SIZE_MODE = 0.3f;       // 30% of top bar height
    public static final float TEXT_SIZE_BUTTON = 0.4f;     // 40% of button height
    
    // Digit sizing (relative to available space)
    public static final float DIGIT_SIZE_RATIO = 0.8f;     // 80% of display area height
    public static final float DIGIT_SMALL_RATIO = 0.7f;    // 70% of normal digit
    
    // Warning box sizing (relative to screen)
    public static final float WARNING_BOX_WIDTH = 0.12f;   // 12% of width
    public static final float WARNING_BOX_HEIGHT = 0.6f;   // 60% of top bar height
    public static final float WARNING_STRIPE_WIDTH = 0.15f; // 15% of box width
    
    // Corner accents
    public static final float CORNER_SIZE = 0.02f;         // 2% of width
    
    // Border and stroke widths
    public static final float STROKE_BORDER = 2;           // pixels
    public static final float STROKE_BUTTON_BORDER = 2;    // pixels
    public static final float STROKE_DIVIDER = 1;          // pixels
    public static final float STROKE_CORNER = 2;           // pixels
    
    // Animation durations (milliseconds)
    public static final long ANIMATION_COLON_BLINK = 1000;
    public static final long ANIMATION_CRITICAL_PULSE = 500;
    public static final long ANIMATION_DEPLETED_FLASH = 1000;
    
    // Update interval
    public static final long UPDATE_INTERVAL_MS = 40;      // 40ms = 25 FPS equivalent
    
    // Glow effects
    public static final float GLOW_OPACITY_NORMAL = 0.5f;  // 50%
    public static final float GLOW_OPACITY_DIM = 0.15f;    // 15%
    public static final float GLOW_BLUR_RADIUS = 0.005f;   // as % of width
    
    // Status light sizing
    public static final float STATUS_LIGHT_WIDTH = 0.04f;  // 4% of width (min 12px)
    
    // Touch feedback
    public static final long BUTTON_PRESS_FEEDBACK_MS = 100;
}
