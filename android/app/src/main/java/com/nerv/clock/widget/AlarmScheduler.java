package com.nerv.clock.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * Handles scheduling and canceling alarms for widget updates.
 * Uses setExactAndAllowWhileIdle for Android 12+ to prevent process freezing.
 */
public final class AlarmScheduler {
    
    private static final String TAG = "AlarmScheduler";
    
    private AlarmScheduler() {} // Prevent instantiation
    
    /**
     * Schedule a periodic alarm to wake up the widget.
     * On Android 12+, uses setExactAndAllowWhileIdle for reliable updates.
     */
    public static void scheduleAlarm(Context context, Class<?> receiverClass) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager not available");
                return;
            }
            
            PendingIntent pendingIntent = createPendingIntent(context, receiverClass);
            
            // Cancel any existing alarm
            alarmManager.cancel(pendingIntent);
            
            // Schedule new alarm
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + WidgetConfig.ALARM_INTERVAL_MS,
                    pendingIntent
                );
                Log.d(TAG, "Alarm scheduled with setExactAndAllowWhileIdle (" + 
                          WidgetConfig.ALARM_INTERVAL_MS + "ms)");
            } else {
                alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + WidgetConfig.ALARM_INTERVAL_MS,
                    WidgetConfig.ALARM_INTERVAL_MS,
                    pendingIntent
                );
                Log.d(TAG, "Alarm scheduled with setRepeating");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm: " + e.getMessage());
        }
    }
    
    /**
     * Cancel the periodic alarm.
     */
    public static void cancelAlarm(Context context, Class<?> receiverClass) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            
            PendingIntent pendingIntent = createPendingIntent(context, receiverClass);
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarm cancelled");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling alarm: " + e.getMessage());
        }
    }
    
    private static PendingIntent createPendingIntent(Context context, Class<?> receiverClass) {
        Intent intent = new Intent(context, receiverClass);
        intent.setAction(WidgetConfig.ACTION_WAKE_UPDATE);
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
