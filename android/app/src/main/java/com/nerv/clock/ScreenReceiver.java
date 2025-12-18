package com.nerv.clock;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receives screen on broadcast to ensure widget is running when the user looks at the screen.
 * This handles cases where the widget may have stopped while the screen was off.
 */
public class ScreenReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NervClockScreen";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Screen receiver triggered: " + action);
        
        if (Intent.ACTION_SCREEN_ON.equals(action) ||
            Intent.ACTION_USER_PRESENT.equals(action)) {
            
            Log.d(TAG, "Screen on or user present, checking widget");
            checkAndRestartWidget(context);
        }
    }
    
    private void checkAndRestartWidget(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponent = new ComponentName(context, NervClockWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
            
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                Log.d(TAG, "Found " + appWidgetIds.length + " widgets, triggering wake update");
                
                // Send wake update broadcast to widget
                Intent updateIntent = new Intent(context, NervClockWidget.class);
                updateIntent.setAction("com.nerv.clock.ACTION_WAKE_UPDATE");
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                context.sendBroadcast(updateIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking widget: " + e.getMessage());
        }
    }
}
