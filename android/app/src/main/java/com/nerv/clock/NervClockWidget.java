package com.nerv.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import com.nerv.clock.ui.ClockViewRenderer;
import com.nerv.clock.ui.FontManager;

/**
 * NERV Clock Widget Provider - Pure Java/Canvas implementation
 * Uses AlarmManager for reliable updates even when process is frozen
 */
public class NervClockWidget extends AppWidgetProvider {
    
    private static final String TAG = "NervClockWidget";
    private static final String ACTION_UPDATE = "com.nerv.clock.ACTION_UPDATE";
    private static final String ACTION_STOP = "com.nerv.clock.ACTION_STOP";
    private static final String ACTION_SLOW = "com.nerv.clock.ACTION_SLOW";
    private static final String ACTION_NORMAL = "com.nerv.clock.ACTION_NORMAL";
    private static final String ACTION_RACING = "com.nerv.clock.ACTION_RACING";
    private static final long UPDATE_INTERVAL_MS = 40; // ~25 FPS
    private static final long ALARM_INTERVAL_MS = 1000; // 1 second alarm wakeup
    
    private static Handler handler;
    private static ClockViewRenderer clockRenderer;
    private static boolean isRunning = false;
    private static Context appContext;
    private static long lastAlarmTime = 0;
    
    // Current dimensions
    private static int currentWidth = 400;
    private static int currentHeight = 150;
    
    private static synchronized Handler getHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate: " + appWidgetIds.length + " widgets");
        
        // Keep application context
        appContext = context.getApplicationContext();
        
        // Initialize fonts once
        FontManager.initialize(appContext);
        
        // Initialize renderer
        if (clockRenderer == null) {
            clockRenderer = new ClockViewRenderer(appContext);
        }
        
        // Get dimensions from first widget
        if (appWidgetIds.length > 0) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetIds[0]);
            updateDimensions(appContext, options);
        }
        
        // Start update loop and alarm
        startUpdates();
        scheduleAlarm(context);
    }
    
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        
        Log.d(TAG, "Widget resized");
        
        // Update dimensions
        updateDimensions(context, newOptions);
        
        // Force immediate update
        updateAllWidgets(context);
    }
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");
        appContext = context.getApplicationContext();
        FontManager.initialize(appContext);
        scheduleAlarm(context);
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widget disabled");
        stopUpdates();
        cancelAlarm(context);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        String action = intent.getAction();
        if (action == null) return;
        
        // Store app context
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
        
        // Handle alarm wakeup - this is the key to surviving process freeze
        if (ACTION_UPDATE.equals(action)) {
            Log.d(TAG, "Alarm wakeup received");
            
            // Re-initialize if needed
            if (clockRenderer == null) {
                FontManager.initialize(appContext);
                clockRenderer = new ClockViewRenderer(appContext);
            }
            
            // Ensure updates are running
            startUpdates();
            
            // Schedule next alarm
            scheduleAlarm(context);
            return;
        }
        
        // Handle mode changes - initialize renderer if needed
        if (ACTION_STOP.equals(action) || ACTION_SLOW.equals(action) || 
            ACTION_NORMAL.equals(action) || ACTION_RACING.equals(action)) {
            
            // Initialize renderer if needed
            if (clockRenderer == null) {
                FontManager.initialize(appContext);
                clockRenderer = new ClockViewRenderer(appContext);
            }
            
            switch (action) {
                case ACTION_STOP:
                    clockRenderer.getClockLogic().setMode(com.nerv.clock.ui.ClockLogic.Mode.STOP);
                    Log.d(TAG, "Mode changed to STOP");
                    break;
                case ACTION_SLOW:
                    clockRenderer.getClockLogic().setMode(com.nerv.clock.ui.ClockLogic.Mode.SLOW);
                    Log.d(TAG, "Mode changed to SLOW");
                    break;
                case ACTION_NORMAL:
                    clockRenderer.getClockLogic().setMode(com.nerv.clock.ui.ClockLogic.Mode.NORMAL);
                    Log.d(TAG, "Mode changed to NORMAL");
                    break;
                case ACTION_RACING:
                    clockRenderer.getClockLogic().setMode(com.nerv.clock.ui.ClockLogic.Mode.RACING);
                    Log.d(TAG, "Mode changed to RACING");
                    break;
            }
            
            // Force immediate update after mode change
            startUpdates();
            updateAllWidgets(context);
        }
    }
    
    private PendingIntent createPendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, NervClockWidget.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
    
    private void scheduleAlarm(Context context) {
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
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            }
            lastAlarmTime = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm: " + e.getMessage());
        }
    }
    
    private void cancelAlarm(Context context) {
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
    }
    
    private void updateDimensions(Context context, Bundle options) {
        float density = context.getResources().getDisplayMetrics().density;
        
        int minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200);
        int maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 400);
        int minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100);
        int maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 150);
        
        // Convert to pixels
        int widthPx = (int)(maxW * density);
        int heightPx = (int)(maxH * density);
        
        // Use actual dimensions reported by Android - no manipulation
        // This ensures the bitmap matches the widget space exactly
        currentWidth = Math.max(widthPx, 400);
        currentHeight = Math.max(heightPx, 150);
        
        Log.d(TAG, "Dimensions: " + currentWidth + "x" + currentHeight + " (density: " + density + ")");
    }
    
    private void startUpdates() {
        if (isRunning) return;
        isRunning = true;
        
        scheduleNextUpdate();
    }
    
    private void stopUpdates() {
        isRunning = false;
        getHandler().removeCallbacksAndMessages(null);
    }
    
    private void scheduleNextUpdate() {
        if (!isRunning || appContext == null) return;
        
        getHandler().postDelayed(() -> {
            if (!isRunning || appContext == null) return;
            
            updateAllWidgets(appContext);
            scheduleNextUpdate();
        }, UPDATE_INTERVAL_MS);
    }
    
    private boolean isCharging(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        if (batteryStatus == null) return false;
        
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }
    
    private void updateAllWidgets(Context context) {
        try {
            if (clockRenderer == null) {
                FontManager.initialize(context);
                clockRenderer = new ClockViewRenderer(context);
            }
            
            // Check charging state and update renderer
            clockRenderer.setCharging(isCharging(context));
            
            // Render to bitmap
            Bitmap bitmap = renderClock();
            if (bitmap == null) return;
            
            // Update all widgets
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, NervClockWidget.class);
            int[] ids = mgr.getAppWidgetIds(widget);
            
            if (ids.length == 0) {
                Log.d(TAG, "No widgets found, stopping updates");
                stopUpdates();
                return;
            }
            
            for (int id : ids) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_canvas);
                views.setImageViewBitmap(R.id.widget_image, bitmap);
                
                // Calculate button container height (approximately 15% of widget height)
                // setViewLayoutHeight is only available in API 31+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    int buttonHeight = (int)(currentHeight * 0.15f);
                    views.setViewLayoutHeight(R.id.button_container, buttonHeight, android.util.TypedValue.COMPLEX_UNIT_PX);
                }
                
                // Set up click handlers for buttons
                views.setOnClickPendingIntent(R.id.btn_stop, createPendingIntent(context, ACTION_STOP, 1));
                views.setOnClickPendingIntent(R.id.btn_slow, createPendingIntent(context, ACTION_SLOW, 2));
                views.setOnClickPendingIntent(R.id.btn_normal, createPendingIntent(context, ACTION_NORMAL, 3));
                views.setOnClickPendingIntent(R.id.btn_racing, createPendingIntent(context, ACTION_RACING, 4));
                
                mgr.updateAppWidget(id, views);
            }
        } catch (Exception e) {
            Log.e(TAG, "Update error: " + e.getMessage());
        }
    }
    
    private Bitmap renderClock() {
        try {
            if (currentWidth <= 0 || currentHeight <= 0) return null;
            
            Bitmap bitmap = Bitmap.createBitmap(currentWidth, currentHeight, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            clockRenderer.drawClock(canvas, currentWidth, currentHeight);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Render error: " + e.getMessage());
            return null;
        }
    }
}
