package com.nerv.clock;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receives boot completed broadcast to restart the widget after device reboot.
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NervClockBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Boot receiver triggered: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            Log.d(TAG, "Device booted, restarting widget");
            restartWidget(context);
        }
    }
    
    private void restartWidget(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponent = new ComponentName(context, NervClockWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
            
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                Log.d(TAG, "Found " + appWidgetIds.length + " widgets, triggering update");
                
                // Send update broadcast to widget
                Intent updateIntent = new Intent(context, NervClockWidget.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                context.sendBroadcast(updateIntent);
            } else {
                Log.d(TAG, "No widgets found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restarting widget: " + e.getMessage());
        }
    }
}
