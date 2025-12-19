package com.nerv.clock.ui;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.nerv.clock.NervClockWidget;

/**
 * Minimal launcher activity.
 * Some OEM launchers require an exported LAUNCHER activity for widgets
 * to appear in the picker. This activity triggers a widget refresh and finishes.
 */
public class MainActivity extends Activity {
    
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity started");
        
        triggerWidgetUpdate();
        finish();
    }
    
    private void triggerWidgetUpdate() {
        try {
            AppWidgetManager awm = AppWidgetManager.getInstance(this);
            ComponentName comp = new ComponentName(this, NervClockWidget.class);
            int[] ids = awm.getAppWidgetIds(comp);
            
            if (ids != null && ids.length > 0) {
                Log.d(TAG, "Triggering update for " + ids.length + " widgets");
                Intent intent = new Intent(this, NervClockWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering widget update: " + e.getMessage());
        }
    }
}
