package com.nerv.clock;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nerv.clock.widget.AlarmScheduler;
import com.nerv.clock.widget.CanvasClockRenderer;
import com.nerv.clock.widget.DimensionManager;
import com.nerv.clock.widget.WebViewManager;
import com.nerv.clock.widget.WidgetConfig;
import com.nerv.clock.widget.WidgetRenderer;

/**
 * NERV Clock Widget Provider.
 * Renders an animated HTML/CSS clock using WebView and displays it as a widget.
 */
public class NervClockWidget extends AppWidgetProvider implements WebViewManager.Callback {
    
    private static final String TAG = "NervClockWidget";
    
    // Static instances (shared across widget updates)
    private static Handler handler;
    private static WebViewManager webViewManager;
    private static CanvasClockRenderer canvasRenderer;
    private static WidgetRenderer renderer;
    private static DimensionManager dimensions;
    private static Context appContext;
    
    // State tracking
    private static boolean isUpdating = false;
    private static int retryCount = 0;
    private static int consecutiveFailures = 0;
    private static long lastUpdateTime = 0;
    
    private static synchronized Handler getHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "========== NERV CLOCK " + WidgetConfig.BUILD_VERSION + " ==========");
        Log.d(TAG, "onUpdate called with " + appWidgetIds.length + " widgets");
        
        appContext = context.getApplicationContext();
        initComponents(appContext);
        
        // Calculate dimensions from first widget
        if (appWidgetIds.length > 0) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetIds[0]);
            dimensions.update(context,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
        }
        
        // Show placeholder immediately
        for (int id : appWidgetIds) {
            renderer.showPlaceholder(appWidgetManager, id, 
                dimensions.getRenderWidth(), dimensions.getRenderHeight());
        }
        
        // Show native time as fallback
        renderer.showNativeTime(dimensions.getRenderWidth(), dimensions.getRenderHeight());
        
        // Schedule alarm for periodic updates
        AlarmScheduler.scheduleAlarm(context, NervClockWidget.class);
        
        // Start WebView
        startWebView();
    }
    
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        
        Log.d(TAG, "Widget options changed for " + appWidgetId);
        
        appContext = context.getApplicationContext();
        initComponents(appContext);
        
        dimensions.update(context,
            newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
            newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
            newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
            newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
        
        // Restart WebView with new dimensions
        stopWebView();
        startWebView();
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        
        // Always let parent handle standard widget actions first
        super.onReceive(context, intent);
        
        if (action == null) {
            return;
        }
        
        appContext = context.getApplicationContext();
        initComponents(appContext);
        
        // Handle custom actions
        switch (action) {
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                handlePackageReplaced(context);
                break;
                
            case WidgetConfig.ACTION_WAKE_UPDATE:
                handleWakeUpdate(context);
                break;
                
            case WidgetConfig.ACTION_STOP:
                executeJS("nervClock.setMode('stop')");
                break;
                
            case WidgetConfig.ACTION_SLOW:
                executeJS("nervClock.setMode('slow')");
                break;
                
            case WidgetConfig.ACTION_NORMAL:
                executeJS("nervClock.setMode('normal')");
                break;
                
            case WidgetConfig.ACTION_RACING:
                executeJS("nervClock.setMode('racing')");
                break;
        }
    }
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");
        AlarmScheduler.scheduleAlarm(context, NervClockWidget.class);
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widget disabled");
        AlarmScheduler.cancelAlarm(context, NervClockWidget.class);
        stopWebView();
    }
    
    // ==================== WebViewManager.Callback ====================
    
    @Override
    public void onPageLoaded() {
        Log.d(TAG, "WebView page loaded");
        isUpdating = true;
        retryCount = 0;
        consecutiveFailures = 0;
        scheduleUpdate();
    }
    
    @Override
    public void onError(String message) {
        Log.e(TAG, "WebView error: " + message);
        retryWithBackoff();
    }
    
    // ==================== Private Methods ====================
    
    private void initComponents(Context context) {
        if (dimensions == null) {
            dimensions = new DimensionManager();
        }
        if (renderer == null) {
            renderer = new WidgetRenderer(context, NervClockWidget.class);
        }
        if (canvasRenderer == null) {
            canvasRenderer = new CanvasClockRenderer(context);
        }
        if (webViewManager == null) {
            webViewManager = new WebViewManager(context);
            webViewManager.setCallback(this);
        }
    }
    
    private void startWebView() {
        if (webViewManager == null || appContext == null) return;
        
        if (webViewManager.isReady() && webViewManager.getAge() < 60000) {
            Log.d(TAG, "WebView already ready, skipping creation");
            return;
        }
        
        if (webViewManager.isCreating()) {
            Log.d(TAG, "WebView creation in progress");
            return;
        }
        
        webViewManager.create(dimensions.getRenderWidth(), dimensions.getRenderHeight());
    }
    
    private void stopWebView() {
        isUpdating = false;
        if (webViewManager != null) {
            webViewManager.destroy();
        }
    }
    
    private void handlePackageReplaced(Context context) {
        Log.d(TAG, "Package replaced, refreshing widget");
        stopWebView();
        retryCount = 0;
        consecutiveFailures = 0;
        
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = renderer.getWidgetIds(mgr);
        if (ids.length > 0) {
            onUpdate(context, mgr, ids);
        }
    }
    
    private void handleWakeUpdate(Context context) {
        Log.d(TAG, "Wake update received");
        
        // Re-schedule alarm for next interval
        AlarmScheduler.scheduleAlarm(context, NervClockWidget.class);
        
        // Force widget update
        if (webViewManager != null && webViewManager.isReady()) {
            updateWidget();
        } else {
            Log.d(TAG, "WebView not ready, restarting...");
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = renderer.getWidgetIds(mgr);
            if (ids.length > 0) {
                onUpdate(context, mgr, ids);
            }
        }
    }
    
    private void executeJS(String script) {
        if (webViewManager != null) {
            webViewManager.executeJS(script);
            Log.d(TAG, "Executed JS: " + script);
        }
    }
    
    private void scheduleUpdate() {
        getHandler().postDelayed(() -> {
            if (!isUpdating || webViewManager == null) return;
            
            // Detect freeze
            long now = System.currentTimeMillis();
            if (lastUpdateTime > 0 && 
                (now - lastUpdateTime) > WidgetConfig.FREEZE_DETECTION_THRESHOLD_MS) {
                Log.d(TAG, "Freeze detected, refreshing clock");
                executeJS("if(typeof updateClock === 'function') updateClock();");
            }
            lastUpdateTime = now;
            
            updateWidget();
            scheduleUpdate();
        }, WidgetConfig.UPDATE_INTERVAL_MS);
    }
    
    private void updateWidget() {
        try {
            // Use Canvas renderer instead of WebView
            if (canvasRenderer == null) {
                retryWithBackoff();
                return;
            }
            
            Bitmap bitmap = canvasRenderer.renderClockBitmap(
                dimensions.getRenderWidth(), 
                dimensions.getRenderHeight());
            
            if (bitmap != null && renderer != null) {
                renderer.updateWithBitmap(bitmap);
                consecutiveFailures = 0; // Reset on success
            } else {
                retryWithBackoff();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateWidget: " + e.getMessage());
            retryWithBackoff();
        }
    }
    
    private void retryWithBackoff() {
        consecutiveFailures++;
        Log.d(TAG, "Consecutive failures: " + consecutiveFailures);
        
        // Check threshold
        if (consecutiveFailures >= WidgetConfig.CONSECUTIVE_FAILURES_THRESHOLD) {
            Log.e(TAG, "Max consecutive failures reached, showing native time and resetting");
            if (renderer != null) {
                renderer.showNativeTime(dimensions.getRenderWidth(), dimensions.getRenderHeight());
            }
            // Reset counters and retry after delay
            consecutiveFailures = 0;
            retryCount = 0;
            stopWebView();
            getHandler().postDelayed(this::startWebView, WidgetConfig.RETRY_RESET_DELAY_MS);
            return;
        }
        
        if (retryCount >= WidgetConfig.MAX_RETRY_COUNT) {
            Log.e(TAG, "Max retries reached, showing error");
            if (renderer != null) {
                renderer.showError(dimensions.getRenderWidth(), dimensions.getRenderHeight());
            }
            
            retryCount = 0;
            getHandler().postDelayed(this::startWebView, WidgetConfig.RETRY_RESET_DELAY_MS);
            return;
        }
        
        retryCount++;
        int delay = 1000 * retryCount;
        Log.d(TAG, "Retrying in " + delay + "ms (attempt " + retryCount + ")");
        
        stopWebView();
        getHandler().postDelayed(this::startWebView, delay);
    }
}
