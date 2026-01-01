package com.nerv.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

/**
 * NERV Clock Widget Provider
 * 
 * This widget provider handles:
 * - Starting the WidgetUpdateService when widget is added (pre-Android 12)
 * - Direct widget updates on Android 12+ where foreground services are restricted
 * - Scheduling backup alarms to keep widget updated
 * - Handling button clicks for mode changes
 * 
 * On Android 12+, the widget updates directly without a foreground service
 * to comply with background execution restrictions.
 */
public class NervClockWidget extends AppWidgetProvider {
    
    private static final String TAG = "NervClockWidget";
    private static final String ACTION_UPDATE = "com.nerv.clock.ACTION_UPDATE";
    private static final long ALARM_INTERVAL_MS = 100; // 100ms for smooth animation
    
    // Button actions
    public static final String ACTION_MODE_STOP = "com.nerv.clock.ACTION_STOP";
    public static final String ACTION_MODE_SLOW = "com.nerv.clock.ACTION_SLOW";
    public static final String ACTION_MODE_NORMAL = "com.nerv.clock.ACTION_NORMAL";
    public static final String ACTION_MODE_RACING = "com.nerv.clock.ACTION_RACING";
    public static final String ACTION_THEME = "com.nerv.clock.ACTION_THEME";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate: " + appWidgetIds.length + " widgets");
        
        // Start the service - it handles everything
        WidgetUpdateService.start(context);
        
        // Schedule backup alarm in case service dies
        scheduleAlarm(context);
    }
    
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        Log.d(TAG, "Widget resized");
        
        // Notify service to refresh dimensions
        WidgetUpdateService.start(context);
    }
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled (first widget added)");
        
        // Start service
        WidgetUpdateService.start(context);
        scheduleAlarm(context);
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widget disabled (all widgets removed)");
        
        // Stop service and cancel alarms
        WidgetUpdateService.stop(context);
        cancelAlarm(context);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        String action = intent.getAction();
        if (action == null) return;
        
        Log.d(TAG, "onReceive: " + action);
        
        // Handle button clicks and mode changes
        switch (action) {
            case ACTION_UPDATE:
                Log.d(TAG, "Update alarm triggered");
                WidgetUpdateService.start(context);
                scheduleAlarm(context);
                break;
            case ACTION_MODE_STOP:
            case ACTION_MODE_SLOW:
            case ACTION_MODE_NORMAL:
            case ACTION_MODE_RACING:
            case ACTION_THEME:
                // Forward to service for mode handling
                WidgetUpdateService.handleAction(context, action);
                break;
        }
    }
    
    private void scheduleAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            
            Intent intent = new Intent(context, NervClockWidget.class);
            intent.setAction(ACTION_UPDATE);
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
            
            long triggerTime = SystemClock.elapsedRealtime() + ALARM_INTERVAL_MS;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm: " + e.getMessage());
        }
    }
    
    private void cancelAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            
            Intent intent = new Intent(context, NervClockWidget.class);
            intent.setAction(ACTION_UPDATE);
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel alarm: " + e.getMessage());
        }
    }
}
