package com.nerv.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

/**
 * Receives boot completed broadcast to restart the widget after device reboot.
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NervClockBoot";
    private static final String ACTION_RETRY_RESTART = "com.nerv.clock.action.RETRY_RESTART";
    private static final String EXTRA_RETRY_COUNT = "retryCount";
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 15_000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int retryCount = intent.getIntExtra(EXTRA_RETRY_COUNT, 0);
        Log.d(TAG, "Boot receiver triggered: " + action + " retry=" + retryCount);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_USER_UNLOCKED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            ACTION_RETRY_RESTART.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            Log.d(TAG, "Device boot/system event, restarting widget");
            restartWidget(context, retryCount);
        }
    }
    
    private void restartWidget(Context context, int retryCount) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponent = new ComponentName(context, NervClockWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
            
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                Log.d(TAG, "Found " + appWidgetIds.length + " widgets, triggering update");
                
                // Get current widget options to preserve dimensions
                Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetIds[0]);
                Log.d(TAG, "Widget options after boot: " + options);
                
                // Send update broadcast to widget
                Intent updateIntent = new Intent(context, NervClockWidget.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                context.sendBroadcast(updateIntent);
                
                // Also trigger dimensions update via options changed
                // This ensures proper scaling after reboot
                for (int widgetId : appWidgetIds) {
                    Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(widgetId);
                    if (widgetOptions != null && !widgetOptions.isEmpty()) {
                        Intent optionsIntent = new Intent(context, NervClockWidget.class);
                        optionsIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED);
                        optionsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                        optionsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, widgetOptions);
                        context.sendBroadcast(optionsIntent);
                    }
                }
            } else {
                Log.d(TAG, "No widgets found (retry=" + retryCount + ")");
                scheduleRetry(context, retryCount);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restarting widget: " + e.getMessage());
        }
    }
    
    private void scheduleRetry(Context context, int retryCount) {
        if (retryCount >= MAX_RETRY) {
            Log.w(TAG, "Max retry reached, giving up");
            return;
        }
        try {
            Intent retryIntent = new Intent(context, BootReceiver.class);
            retryIntent.setAction(ACTION_RETRY_RESTART);
            retryIntent.putExtra(EXTRA_RETRY_COUNT, retryCount + 1);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                long triggerAt = SystemClock.elapsedRealtime() + RETRY_DELAY_MS;
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
                Log.d(TAG, "Scheduled widget restart retry " + (retryCount + 1));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule retry: " + e.getMessage());
        }
    }
}
