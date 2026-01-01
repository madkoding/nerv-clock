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
 * This widget provider ONLY handles:
 * - Starting the WidgetUpdateService when widget is added
 * - Stopping the service when all widgets are removed
 * - Scheduling backup alarms to restart service if killed
 * 
 * ALL rendering and state management is handled by WidgetUpdateService
 * to prevent duplicate instances and state conflicts.
 */
public class NervClockWidget extends AppWidgetProvider {
    
    private static final String TAG = "NervClockWidget";
    private static final String ACTION_UPDATE = "com.nerv.clock.ACTION_UPDATE";
    private static final long ALARM_INTERVAL_MS = 5000; // 5 second backup alarm
    
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
        
        // Handle backup alarm - just restart the service
        if (ACTION_UPDATE.equals(action)) {
            Log.d(TAG, "Backup alarm triggered, ensuring service is running");
            WidgetUpdateService.start(context);
            scheduleAlarm(context);
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
