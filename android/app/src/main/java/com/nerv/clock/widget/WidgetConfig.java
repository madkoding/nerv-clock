package com.nerv.clock.widget;

/**
 * Configuration constants for the NERV Clock widget.
 */
public final class WidgetConfig {
    
    private WidgetConfig() {} // Prevent instantiation
    
    // Version
    public static final String BUILD_VERSION = "v2.1.0";
    
    // Timing constants
    public static final int UPDATE_INTERVAL_MS = 50;
    public static final int PAGE_LOAD_TIMEOUT_MS = 10000;
    public static final long ALARM_INTERVAL_MS = 10000;
    public static final long FREEZE_DETECTION_THRESHOLD_MS = 5000;
    
    // Retry configuration
    public static final int MAX_RETRY_COUNT = 3;
    public static final int RETRY_RESET_DELAY_MS = 30000;
    public static final int CONSECUTIVE_FAILURES_THRESHOLD = 5;
    public static final int NATIVE_UPDATE_INTERVAL_MS = 60000;
    
    // Default render dimensions (in dp)
    public static final int BASE_WIDTH_DP = 400;
    public static final int BASE_HEIGHT_DP = 120;
    public static final int MIN_WIDTH_DP = 180;
    public static final int MIN_HEIGHT_DP = 60;
    
    // Colors
    public static final String COLOR_BACKGROUND = "#0d0900";
    public static final String COLOR_ORANGE = "#FF6A00";
    public static final String COLOR_YELLOW = "#FFB800";
    public static final String COLOR_RED = "#CC0000";
    public static final String COLOR_DARK = "#1a1a1a";
    
    // SharedPreferences
    public static final String PREFS_NAME = "NervClockWidget";
    public static final String PREF_USE_NATIVE_ONLY = "use_native_only";
    
    // Actions
    public static final String ACTION_STOP = "com.nerv.clock.ACTION_STOP";
    public static final String ACTION_SLOW = "com.nerv.clock.ACTION_SLOW";
    public static final String ACTION_NORMAL = "com.nerv.clock.ACTION_NORMAL";
    public static final String ACTION_RACING = "com.nerv.clock.ACTION_RACING";
    public static final String ACTION_WAKE_UPDATE = "com.nerv.clock.ACTION_WAKE_UPDATE";
}
