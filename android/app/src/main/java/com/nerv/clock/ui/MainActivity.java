package com.nerv.clock.ui;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.nerv.clock.NervClockWidget;

/**
 * Minimal launcher activity. Some OEM launchers require an exported LAUNCHER activity
 * for widgets to appear in the picker (MIUI/HyperOS). This activity immediately
 * triggers a widget refresh and finishes.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity started, triggering widget update");
        // Write a short persistent diagnostic so we can verify on-device logs/files
        try {
            String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            String msg = ts + " - MainActivity started: " + System.currentTimeMillis() + "\n";
            // write internal file (best-effort)
            try (java.io.FileOutputStream fos = openFileOutput("nerv_debug.log", MODE_APPEND)) {
                fos.write(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
            // also write to external files dir so we can adb pull it without run-as
            try {
                java.io.File ext = getExternalFilesDir(null);
                if (ext != null) {
                    java.io.File ef = new java.io.File(ext, "nerv_debug_ext.log");
                    try (java.io.FileOutputStream efos = new java.io.FileOutputStream(ef, true)) {
                        efos.write(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to write external nerv_debug_ext.log: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to write nerv_debug.log: " + e.getMessage());
        }
        try {
            AppWidgetManager awm = AppWidgetManager.getInstance(this);
            ComponentName comp = new ComponentName(this, NervClockWidget.class);
            int[] ids = awm.getAppWidgetIds(comp);
            if (ids != null && ids.length > 0) {
                Intent intent = new Intent(this, NervClockWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);
                // Diagnostic toast so user can see MainActivity was invoked
                android.widget.Toast.makeText(this, "NERV: triggered widget update", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering widget update: " + e.getMessage());
        }
        finish();
    }
}
