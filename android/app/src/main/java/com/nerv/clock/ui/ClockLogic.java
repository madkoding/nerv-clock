package com.nerv.clock.ui;

import android.util.Log;
import java.util.Calendar;

/**
 * Clock logic handler for NERV Chronometer
 * Manages time calculation, modes, and state
 */
public class ClockLogic {
    
    private static final String TAG = "ClockLogic";
    
    // Clock modes
    public enum Mode {
        NORMAL,      // Current system time
        RACING,      // Stopwatch
        SLOW,        // Pomodoro (25min or 5min)
        STOP         // Paused (used with RACING or SLOW)
    }
    
    // Current state
    private Mode currentMode = Mode.NORMAL;
    private boolean isPaused = false;
    private boolean isDepleted = false;
    
    // Time values
    private int hour, minute, second, centisecond;
    
    // Stopwatch state
    private long stopwatchTime = 0;
    private long lastUpdateTime = System.currentTimeMillis();
    
    // Pomodoro state
    private static final long POMODORO_25MIN = 25 * 60 * 1000;
    private static final long POMODORO_5MIN = 5 * 60 * 1000;
    private long[] pomoDurations = { POMODORO_25MIN, POMODORO_5MIN };
    private int pomoDurationIndex = 0;
    private long pomodoroDuration = POMODORO_25MIN;
    private long pomodoroRemaining = POMODORO_25MIN;
    
    // Display state for warning/critical
    private WarningState warningState = WarningState.NORMAL;
    
    public enum WarningState {
        NORMAL,      // Orange
        WARNING,     // Yellow (4+ minutes remaining)
        CRITICAL,    // Red (1+ minutes remaining)
        DEPLETED     // Flashing red (0 time remaining)
    }
    
    // Listener for updates
    public interface OnClockUpdateListener {
        void onTimeUpdate(int h, int m, int s, int cs);
        void onModeChanged(Mode mode);
        void onWarningStateChanged(WarningState state);
        void onDepletedStateChanged(boolean isDepleted);
    }
    
    private OnClockUpdateListener listener;
    
    public ClockLogic() {
        updateNormalTime();
    }
    
    public void setUpdateListener(OnClockUpdateListener listener) {
        this.listener = listener;
    }
    
    /**
     * Update the clock - call this regularly (every ~40ms)
     */
    public void update() {
        long now = System.currentTimeMillis();
        
        switch (currentMode) {
            case NORMAL:
                updateNormalTime();
                warningState = WarningState.NORMAL;
                isDepleted = false;
                break;
                
            case RACING:
                if (!isPaused) {
                    stopwatchTime += (now - lastUpdateTime);
                }
                updateFromStopwatch();
                warningState = WarningState.NORMAL;
                isDepleted = false;
                break;
                
            case SLOW:
                if (!isPaused) {
                    pomodoroRemaining -= (now - lastUpdateTime);
                    if (pomodoroRemaining < 0) {
                        pomodoroRemaining = 0;
                    }
                }
                updateFromPomodoro();
                updatePomodoroWarningState();
                break;
                
            case STOP:
                // Paused state - no time update
                break;
        }
        
        lastUpdateTime = now;
        
        if (listener != null) {
            listener.onTimeUpdate(hour, minute, second, centisecond);
        }
    }
    
    private void updateNormalTime() {
        Calendar cal = Calendar.getInstance();
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        second = cal.get(Calendar.SECOND);
        centisecond = cal.get(Calendar.MILLISECOND) / 10;
    }
    
    private void updateFromStopwatch() {
        long total = stopwatchTime;
        hour = (int) ((total / 3600000) % 100);
        minute = (int) ((total / 60000) % 60);
        second = (int) ((total / 1000) % 60);
        centisecond = (int) ((total % 1000) / 10);
    }
    
    private void updateFromPomodoro() {
        long remaining = pomodoroRemaining;
        hour = (int) ((remaining / 3600000) % 100);
        minute = (int) ((remaining / 60000) % 60);
        second = (int) ((remaining / 1000) % 60);
        centisecond = (int) ((remaining % 1000) / 10);
    }
    
    private void updatePomodoroWarningState() {
        long remainingMinutes = pomodoroRemaining / 60000;
        
        if (pomodoroRemaining <= 0) {
            if (!isDepleted) {
                isDepleted = true;
                if (listener != null) {
                    listener.onDepletedStateChanged(true);
                }
            }
            warningState = WarningState.DEPLETED;
        } else if (remainingMinutes <= 1) {
            warningState = WarningState.CRITICAL;
        } else if (remainingMinutes <= 4) {
            warningState = WarningState.WARNING;
        } else {
            warningState = WarningState.NORMAL;
        }
        
        if (listener != null) {
            listener.onWarningStateChanged(warningState);
        }
    }
    
    /**
     * Set the clock mode
     */
    public void setMode(Mode mode) {
        if (currentMode == mode && mode != Mode.SLOW) {
            return; // No change
        }
        
        // Special case: SLOW mode toggles duration on repeated click
        if (mode == Mode.SLOW && currentMode == Mode.SLOW) {
            pomoDurationIndex = (pomoDurationIndex + 1) % 2;
            pomodoroDuration = pomoDurations[pomoDurationIndex];
            pomodoroRemaining = pomodoroDuration;
            isDepleted = false;
            isPaused = false;
            lastUpdateTime = System.currentTimeMillis();
            if (listener != null) {
                listener.onModeChanged(mode);
            }
            return;
        }
        
        currentMode = mode;
        isPaused = false;
        isDepleted = false;
        lastUpdateTime = System.currentTimeMillis();
        
        // Reset mode-specific timers
        if (mode == Mode.RACING) {
            stopwatchTime = 0;
        } else if (mode == Mode.SLOW) {
            pomoDurationIndex = 0;
            pomodoroDuration = POMODORO_25MIN;
            pomodoroRemaining = pomodoroDuration;
        }
        
        warningState = WarningState.NORMAL;
        
        if (listener != null) {
            listener.onModeChanged(mode);
        }
    }
    
    /**
     * Toggle pause/play for RACING or SLOW modes
     */
    public void togglePause() {
        if (currentMode == Mode.NORMAL) {
            return; // Can't pause normal mode
        }
        
        isPaused = !isPaused;
        if (!isPaused) {
            lastUpdateTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Get padded time string (always 2 digits)
     */
    public String getHourString() {
        return String.format("%02d", hour);
    }
    
    public String getMinuteString() {
        return String.format("%02d", minute);
    }
    
    public String getSecondString() {
        return String.format("%02d", second);
    }
    
    public String getCentisecondString() {
        return String.format("%02d", centisecond);
    }
    
    // Getters
    public Mode getCurrentMode() {
        return currentMode;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public boolean isDepleted() {
        return isDepleted;
    }
    
    public WarningState getWarningState() {
        return warningState;
    }
    
    public int getHour() {
        return hour;
    }
    
    public int getMinute() {
        return minute;
    }
    
    public int getSecond() {
        return second;
    }
    
    public int getCentisecond() {
        return centisecond;
    }
    
    public long getPomodoroDuration() {
        return pomodoroDuration;
    }
    
    public long getPomodoroRemaining() {
        return pomodoroRemaining;
    }
    
    public int getPomoDurationIndex() {
        return pomoDurationIndex;
    }
}
