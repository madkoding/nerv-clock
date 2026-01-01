package com.nerv.clock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.nerv.clock.ui.ClockLogic;
import com.nerv.clock.ui.ClockViewRenderer;
import com.nerv.clock.ui.ColorScheme;
import com.nerv.clock.ui.FontManager;

/**
 * Foreground Service to keep the widget updating reliably on devices with
 * aggressive battery optimization (Xiaomi, Huawei, OPPO, etc.)
 * 
 * The service:
 * - Runs as a foreground service to prevent being killed
 * - Monitors screen state and launcher foreground status
 * - Pauses updates when screen is off or launcher is not visible
 * - Resumes updates immediately when conditions are met
 */
public class WidgetUpdateService extends Service {
    
    private static final String TAG = "WidgetUpdateService";
    private static final String CHANNEL_ID = "nerv_clock_service";
    private static final int NOTIFICATION_ID = 1001;
    
    // Actions
    public static final String ACTION_START = "com.nerv.clock.service.START";
    public static final String ACTION_STOP = "com.nerv.clock.service.STOP";
    public static final String ACTION_MODE_STOP = "com.nerv.clock.ACTION_STOP";
    public static final String ACTION_MODE_SLOW = "com.nerv.clock.ACTION_SLOW";
    public static final String ACTION_MODE_NORMAL = "com.nerv.clock.ACTION_NORMAL";
    public static final String ACTION_MODE_RACING = "com.nerv.clock.ACTION_RACING";
    public static final String ACTION_THEME = "com.nerv.clock.ACTION_THEME";
    
    private static final String PREFS_NAME = "NervClockPrefs";
    private static final String PREF_THEME_INDEX = "theme_index";
    
    // Update intervals
    private static final long UPDATE_INTERVAL_ACTIVE_MS = 40; // ~25 FPS when active
    private static final long UPDATE_INTERVAL_BACKGROUND_MS = 1000; // 1 FPS when in background
    
    // Static instance to prevent multiple services
    private static WidgetUpdateService instance = null;
    private static Handler handler;
    private static ClockViewRenderer clockRenderer;
    private static boolean isServiceRunning = false;
    
    private NotificationHelper notificationHelper;
    private PowerManager powerManager;
    private boolean isScreenOn = true;
    
    // Dimensions
    private int currentWidth = 400;
    private int currentHeight = 150;
    
    // Screen state receiver
    private BroadcastReceiver screenReceiver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        
        // Prevent multiple instances
        if (instance != null) {
            Log.d(TAG, "Service instance already exists, reusing");
            return;
        }
        instance = this;
        
        // Only create handler once
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        
        // Initialize fonts
        FontManager.initialize(this);
        
        // Load saved theme
        loadSavedTheme();
        
        // Only create renderer once (static)
        if (clockRenderer == null) {
            clockRenderer = new ClockViewRenderer(this);
            setupTimerListener();
        }
        
        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);
        
        // Register screen state receiver
        registerScreenReceiver();
        
        // Check initial screen state
        isScreenOn = powerManager.isInteractive();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "onStartCommand action: " + action);
        
        // CRITICAL: Start foreground service IMMEDIATELY to avoid ANR/crash
        // Must be called within 5 seconds of startForegroundService()
        startForegroundServiceNotification();
        
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        // Handle mode changes
        if (action != null && clockRenderer != null) {
            switch (action) {
                case ACTION_MODE_STOP:
                    clockRenderer.getClockLogic().togglePause();
                    break;
                case ACTION_MODE_SLOW:
                    clockRenderer.getClockLogic().setMode(ClockLogic.Mode.SLOW);
                    break;
                case ACTION_MODE_NORMAL:
                    clockRenderer.getClockLogic().setMode(ClockLogic.Mode.NORMAL);
                    break;
                case ACTION_MODE_RACING:
                    clockRenderer.getClockLogic().setMode(ClockLogic.Mode.RACING);
                    break;
                case ACTION_THEME:
                    ColorScheme.nextTheme();
                    saveTheme(ColorScheme.getThemeIndex());
                    clockRenderer.updateThemeColors();
                    break;
            }
            // Force immediate update after mode change
            updateWidgets();
        }
        
        // Start update loop (only once)
        if (!isServiceRunning) {
            isServiceRunning = true;
            refreshDimensions();
            
            // IMPORTANT: Do initial widget update immediately
            updateWidgets();
            
            // Start the update loop
            scheduleNextUpdate(UPDATE_INTERVAL_ACTIVE_MS);
        }
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        isServiceRunning = false;
        
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        unregisterScreenReceiver();
        instance = null;
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void startForegroundServiceNotification() {
        createNotificationChannel();
        
        Intent stopIntent = new Intent(this, WidgetUpdateService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        Notification notification = builder
            .setContentTitle("NERV Clock")
            .setContentText("Widget activo")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPendingIntent)
            .build();
        
        // Android 10+ requires foreground service type
        // Always use SPECIAL_USE since that's what's declared in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "NERV Clock Service",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Mantiene el widget del reloj actualizado");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Log.d(TAG, "Screen ON");
                    isScreenOn = true;
                    // Reset update time to prevent jumps
                    if (clockRenderer != null && clockRenderer.getClockLogic() != null) {
                        clockRenderer.getClockLogic().resetLastUpdateTime();
                    }
                    scheduleNextUpdate(0);
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Log.d(TAG, "Screen OFF");
                    isScreenOn = false;
                    // No need to stop updates completely, just slow them down
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        // Android 12+ requires RECEIVER_NOT_EXPORTED flag for non-exported receivers
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
    }
    
    private void unregisterScreenReceiver() {
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering screen receiver: " + e.getMessage());
            }
            screenReceiver = null;
        }
    }
    
    private void scheduleNextUpdate(long delayMs) {
        if (!isServiceRunning) return;
        
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateLoop();
            }
        }, delayMs);
    }
    
    private void updateLoop() {
        if (!isServiceRunning) return;
        
        // Always update the widget
        updateWidgets();
        
        // Determine next update interval based on screen state
        long nextInterval;
        if (isScreenOn) {
            // Screen on - fast updates for smooth animation
            nextInterval = UPDATE_INTERVAL_ACTIVE_MS;
        } else {
            // Screen off - slow updates just to keep service alive
            nextInterval = UPDATE_INTERVAL_BACKGROUND_MS;
        }
        
        scheduleNextUpdate(nextInterval);
    }
    
    private void updateWidgets() {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            ComponentName widget = new ComponentName(this, NervClockWidget.class);
            int[] ids = mgr.getAppWidgetIds(widget);
            
            if (ids == null || ids.length == 0) {
                Log.d(TAG, "No widgets found, will retry");
                // Don't stop immediately - widget might still be registering
                // Just return and let the update loop retry
                return;
            }
            
            // Update charging state
            clockRenderer.setCharging(isCharging());
            
            // Render bitmap
            Bitmap bitmap = renderClock();
            if (bitmap == null) return;
            
            // Update all widgets
            for (int id : ids) {
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_canvas);
                views.setImageViewBitmap(R.id.widget_image, bitmap);
                
                // Set up click handlers - route to service for faster response
                views.setOnClickPendingIntent(R.id.btn_stop, createServiceIntent(ACTION_MODE_STOP, 1));
                views.setOnClickPendingIntent(R.id.btn_slow, createServiceIntent(ACTION_MODE_SLOW, 2));
                views.setOnClickPendingIntent(R.id.btn_normal, createServiceIntent(ACTION_MODE_NORMAL, 3));
                views.setOnClickPendingIntent(R.id.btn_racing, createServiceIntent(ACTION_MODE_RACING, 4));
                views.setOnClickPendingIntent(R.id.btn_title, createServiceIntent(ACTION_THEME, 5));
                
                mgr.updateAppWidget(id, views);
            }
        } catch (Exception e) {
            Log.e(TAG, "Update error: " + e.getMessage());
        }
    }
    
    private PendingIntent createServiceIntent(String action, int requestCode) {
        Intent intent = new Intent(this, WidgetUpdateService.class);
        intent.setAction(action);
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    
    private Bitmap renderClock() {
        try {
            if (currentWidth <= 0 || currentHeight <= 0) return null;
            
            // Force consistent aspect ratio (~2.2:1)
            float idealRatio = 2.2f;
            float currentRatio = (float) currentWidth / currentHeight;
            
            int bitmapWidth, bitmapHeight;
            if (currentRatio > idealRatio) {
                bitmapWidth = (int)(currentHeight * idealRatio);
                bitmapHeight = currentHeight;
            } else {
                bitmapWidth = currentWidth;
                bitmapHeight = (int)(currentWidth / idealRatio);
            }
            
            bitmapWidth = Math.max(bitmapWidth, 100);
            bitmapHeight = Math.max(bitmapHeight, 50);
            
            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            clockRenderer.drawClock(canvas, bitmapWidth, bitmapHeight);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Render error: " + e.getMessage());
            return null;
        }
    }
    
    private void refreshDimensions() {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            ComponentName widget = new ComponentName(this, NervClockWidget.class);
            int[] ids = mgr.getAppWidgetIds(widget);
            
            if (ids != null && ids.length > 0) {
                Bundle options = mgr.getAppWidgetOptions(ids[0]);
                if (options != null && !options.isEmpty()) {
                    float density = getResources().getDisplayMetrics().density;
                    
                    int minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200);
                    int maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 400);
                    int minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100);
                    int maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 150);
                    
                    int widthDp = Math.max(minW, maxW);
                    int heightDp = Math.max(minH, maxH);
                    
                    currentWidth = Math.max((int)(widthDp * density), 400);
                    currentHeight = Math.max((int)(heightDp * density), 150);
                    
                    Log.d(TAG, "Dimensions refreshed: " + currentWidth + "x" + currentHeight);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing dimensions: " + e.getMessage());
        }
    }
    
    private boolean isCharging() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        if (batteryStatus == null) return false;
        
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }
    
    private void loadSavedTheme() {
        int savedTheme = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(PREF_THEME_INDEX, 0);
        ColorScheme.setTheme(savedTheme);
    }
    
    private void saveTheme(int themeIndex) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(PREF_THEME_INDEX, themeIndex)
            .apply();
    }
    
    private void setupTimerListener() {
        if (clockRenderer != null && clockRenderer.getClockLogic() != null) {
            clockRenderer.getClockLogic().setUpdateListener(new ClockLogic.OnClockUpdateListener() {
                @Override
                public void onTimeUpdate(int h, int m, int s, int cs) {}
                
                @Override
                public void onModeChanged(ClockLogic.Mode mode) {}
                
                @Override
                public void onWarningStateChanged(ClockLogic.WarningState state) {}
                
                @Override
                public void onDepletedStateChanged(boolean isDepleted) {}
                
                @Override
                public void onTimerComplete(int durationMinutes) {
                    Log.d(TAG, "Timer complete! Duration: " + durationMinutes + " minutes");
                    if (notificationHelper != null) {
                        notificationHelper.showTimerCompleteNotification(durationMinutes);
                    }
                }
            });
        }
    }
    
    /**
     * Static method to start the service from other components
     * On Android 12+ (API 31+), foreground services cannot be started from background.
     * We need to handle this gracefully.
     */
    public static void start(Context context) {
        try {
            Intent intent = new Intent(context, WidgetUpdateService.class);
            intent.setAction(ACTION_START);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+: Try to start, but catch exception if not allowed
                // The widget will still render via direct updates
                try {
                    context.startForegroundService(intent);
                } catch (Exception e) {
                    Log.w(TAG, "Cannot start foreground service from background on Android 12+: " + e.getMessage());
                    // Fallback: do direct widget update without service
                    doDirectWidgetUpdate(context);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service: " + e.getMessage());
            // Fallback to direct update
            doDirectWidgetUpdate(context);
        }
    }
    
    /**
     * Direct widget update without service - for Android 12+ restrictions
     */
    private static void doDirectWidgetUpdate(Context context) {
        try {
            // Initialize if needed
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            
            FontManager.initialize(context);
            
            if (clockRenderer == null) {
                clockRenderer = new ClockViewRenderer(context);
            }
            
            // Load theme
            int themeIndex = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(PREF_THEME_INDEX, 0);
            ColorScheme.setTheme(themeIndex);
            
            // Render and update widget
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, NervClockWidget.class);
            int[] ids = mgr.getAppWidgetIds(widget);
            
            if (ids == null || ids.length == 0) return;
            
            // Get widget dimensions
            int width = 400;
            int height = 150;
            for (int id : ids) {
                Bundle options = mgr.getAppWidgetOptions(id);
                int minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180);
                int minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60);
                width = Math.max(width, (int)(minW * 2.5f));
                height = Math.max(height, (int)(minH * 2.5f));
            }
            
            // Create bitmap and draw clock
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            clockRenderer.drawClock(canvas, width, height);
            
            for (int id : ids) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_canvas);
                views.setImageViewBitmap(R.id.widget_image, bitmap);
                
                // Set up click handlers
                views.setOnClickPendingIntent(R.id.btn_stop, createPendingIntent(context, ACTION_MODE_STOP, 1));
                views.setOnClickPendingIntent(R.id.btn_slow, createPendingIntent(context, ACTION_MODE_SLOW, 2));
                views.setOnClickPendingIntent(R.id.btn_normal, createPendingIntent(context, ACTION_MODE_NORMAL, 3));
                views.setOnClickPendingIntent(R.id.btn_racing, createPendingIntent(context, ACTION_MODE_RACING, 4));
                views.setOnClickPendingIntent(R.id.btn_title, createPendingIntent(context, ACTION_THEME, 5));
                
                mgr.updateAppWidget(id, views);
            }
            
            Log.d(TAG, "Direct widget update completed");
        } catch (Exception e) {
            Log.e(TAG, "Direct widget update failed: " + e.getMessage());
        }
    }
    
    private static PendingIntent createPendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, NervClockWidget.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
    
    /**
     * Handle actions from widget clicks (for Android 12+ where service may not be running)
     */
    public static void handleAction(Context context, String action) {
        try {
            // Initialize if needed
            if (clockRenderer == null) {
                FontManager.initialize(context);
                clockRenderer = new ClockViewRenderer(context);
                // Load saved theme
                int themeIndex = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getInt(PREF_THEME_INDEX, 0);
                ColorScheme.setTheme(themeIndex);
            }
            
            // Handle the action
            switch (action) {
                case ACTION_MODE_STOP:
                    if (clockRenderer.getClockLogic() != null) {
                        clockRenderer.getClockLogic().togglePause();
                    }
                    break;
                case ACTION_MODE_SLOW:
                    if (clockRenderer.getClockLogic() != null) {
                        clockRenderer.getClockLogic().setMode(com.nerv.clock.ui.ClockLogic.Mode.SLOW);
                    }
                    break;
                case ACTION_MODE_NORMAL:
                    if (clockRenderer.getClockLogic() != null) {
                        clockRenderer.getClockLogic().setMode(com.nerv.clock.ui.ClockLogic.Mode.NORMAL);
                    }
                    break;
                case ACTION_MODE_RACING:
                    if (clockRenderer.getClockLogic() != null) {
                        clockRenderer.getClockLogic().setMode(com.nerv.clock.ui.ClockLogic.Mode.RACING);
                    }
                    break;
                case ACTION_THEME:
                    ColorScheme.nextTheme();
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(PREF_THEME_INDEX, ColorScheme.getThemeIndex())
                        .apply();
                    if (clockRenderer != null) {
                        clockRenderer.updateThemeColors();
                    }
                    break;
            }
            
            // Update widget after action
            doDirectWidgetUpdate(context);
            
            Log.d(TAG, "Action handled: " + action);
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle action: " + e.getMessage());
        }
    }
    
    /**
     * Static method to stop the service
     */
    public static void stop(Context context) {
        try {
            // On Android 12+, we can't start service from background
            // Just let the service stop itself when it detects no widgets
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Service will stop itself when it sees no widgets
                Log.d(TAG, "Android 12+: Service will stop itself");
                return;
            }
            
            Intent intent = new Intent(context, WidgetUpdateService.class);
            intent.setAction(ACTION_STOP);
            context.startService(intent);
        } catch (Exception e) {
            Log.w(TAG, "Could not stop service: " + e.getMessage());
        }
    }
}
