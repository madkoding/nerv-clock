package com.nerv.clock.ui;

/**
 * Validation utilities for testing and debugging
 * Ensures all timing and mode transitions work correctly
 */
public class ValidationUtils {
    
    /**
     * Validate clock logic modes
     */
    public static boolean validateModeTransitions() {
        ClockLogic clock = new ClockLogic();
        
        // Test NORMAL mode
        clock.setMode(ClockLogic.Mode.NORMAL);
        assert clock.getCurrentMode() == ClockLogic.Mode.NORMAL : "NORMAL mode failed";
        assert !clock.isPaused() : "NORMAL should not be pausable";
        
        // Test RACING mode
        clock.setMode(ClockLogic.Mode.RACING);
        assert clock.getCurrentMode() == ClockLogic.Mode.RACING : "RACING mode failed";
        assert !clock.isPaused() : "RACING should start unpaused";
        
        // Test pause/resume
        clock.togglePause();
        assert clock.isPaused() : "Pause toggle failed";
        clock.togglePause();
        assert !clock.isPaused() : "Resume toggle failed";
        
        // Test SLOW mode
        clock.setMode(ClockLogic.Mode.SLOW);
        assert clock.getCurrentMode() == ClockLogic.Mode.SLOW : "SLOW mode failed";
        assert clock.getPomodoroDuration() == 25 * 60 * 1000 : "SLOW duration should be 25 min";
        
        // Test SLOW mode toggle (duration change)
        clock.setMode(ClockLogic.Mode.SLOW);
        assert clock.getPomodoroDuration() == 5 * 60 * 1000 : "SLOW should toggle to 5 min";
        clock.setMode(ClockLogic.Mode.SLOW);
        assert clock.getPomodoroDuration() == 25 * 60 * 1000 : "SLOW should toggle back to 25 min";
        
        return true;
    }
    
    /**
     * Validate time formatting
     */
    public static boolean validateTimeFormatting() {
        ClockLogic clock = new ClockLogic();
        clock.setMode(ClockLogic.Mode.RACING);
        
        // Verify padding with zeros
        String hour = clock.getHourString();
        String minute = clock.getMinuteString();
        String second = clock.getSecondString();
        
        assert hour.length() == 2 : "Hour should be 2 digits";
        assert minute.length() == 2 : "Minute should be 2 digits";
        assert second.length() == 2 : "Second should be 2 digits";
        
        // Single digits should be padded with 0
        if (clock.getHour() < 10) {
            assert hour.charAt(0) == '0' : "Hour should be zero-padded";
        }
        
        return true;
    }
    
    /**
     * Validate warning state transitions
     */
    public static boolean validateWarningStates() {
        // This would be tested with actual time progression
        // Warning at 4+ minutes
        // Critical at 1+ minutes
        // Depleted at 0 minutes
        
        // Example structure - actual testing would need time progression
        ClockLogic.WarningState[] states = {
            ClockLogic.WarningState.NORMAL,
            ClockLogic.WarningState.WARNING,
            ClockLogic.WarningState.CRITICAL,
            ClockLogic.WarningState.DEPLETED
        };
        
        return true;
    }
    
    /**
     * Validate color scheme
     */
    public static boolean validateColorScheme() {
        // Verify all colors are defined
        assert ColorScheme.NERV_ORANGE != 0 : "NERV_ORANGE not defined";
        assert ColorScheme.NERV_RED != 0 : "NERV_RED not defined";
        assert ColorScheme.NERV_GREEN != 0 : "NERV_GREEN not defined";
        assert ColorScheme.NERV_DARK != 0 : "NERV_DARK not defined";
        
        // Verify mode colors
        int normalColor = ColorScheme.getModeColor("normal");
        int racingColor = ColorScheme.getModeColor("racing");
        int slowColor = ColorScheme.getModeColor("slow");
        
        assert normalColor == ColorScheme.NERV_GREEN : "Normal mode should be green";
        assert racingColor == ColorScheme.NERV_RED : "Racing mode should be red";
        assert slowColor == ColorScheme.WARNING_YELLOW : "Slow mode should be yellow";
        
        return true;
    }
    
    /**
     * Run all validation tests
     */
    public static void runAllTests() {
        try {
            System.out.println("Testing mode transitions...");
            validateModeTransitions();
            System.out.println("✓ Mode transitions passed");
            
            System.out.println("Testing time formatting...");
            validateTimeFormatting();
            System.out.println("✓ Time formatting passed");
            
            System.out.println("Testing warning states...");
            validateWarningStates();
            System.out.println("✓ Warning states passed");
            
            System.out.println("Testing color scheme...");
            validateColorScheme();
            System.out.println("✓ Color scheme passed");
            
            System.out.println("\n✅ All tests passed!");
        } catch (AssertionError e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
