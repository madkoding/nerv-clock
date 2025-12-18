package com.nerv.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.RemoteViews;
import android.util.Log;
import android.util.DisplayMetrics;

public class NervClockWidget extends AppWidgetProvider {

    private static final String TAG = "NervClockWidget";
    private static Handler handler = null;
    private static WebView webView;
    private static boolean isUpdating = false;
    private static boolean pageLoaded = false;
    private static boolean isCreatingWebView = false;
    
    // Reusable bitmap and canvas for better performance
    private static Bitmap reusableBitmap = null;
    private static Canvas reusableCanvas = null;
    private static int lastRenderWidth = 0;
    private static int lastRenderHeight = 0;
    private static final int UPDATE_INTERVAL = 33; // ~30 fps
    private static final int PAGE_LOAD_TIMEOUT = 10000; // 10 seconds timeout for page load
    private static final int WEBVIEW_INIT_DELAY = 100; // Reduced delay before creating WebView
    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_RESET_DELAY = 30000; // 30 seconds before retrying after max failures
    private static int retryCount = 0;
    private static boolean showingError = false;
    private static Context appContext;
    private static long lastAppUpdateTime = 0;
    private static long lastWakeCheck = 0;
    private static long webViewCreationTime = 0;
    private static final long WAKE_CHECK_INTERVAL = 30000; // 30 seconds
    private static final long ALARM_INTERVAL = 60000; // 1 minute
    
    // Actions for buttons
    public static final String ACTION_STOP = "com.nerv.clock.ACTION_STOP";
    public static final String ACTION_SLOW = "com.nerv.clock.ACTION_SLOW";
    public static final String ACTION_NORMAL = "com.nerv.clock.ACTION_NORMAL";
    public static final String ACTION_RACING = "com.nerv.clock.ACTION_RACING";
    public static final String ACTION_WAKE_UPDATE = "com.nerv.clock.ACTION_WAKE_UPDATE";
    
    // Base render size in dp
    private static final int BASE_WIDTH_DP = 400;
    private static final int BASE_HEIGHT_DP = 120;
    private static int renderWidth = 400;
    private static int renderHeight = 120;
    
    /**
     * Get or create the handler on the main looper.
     * This ensures we always have a valid handler even after process restart.
     */
    private static synchronized Handler getHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called with " + appWidgetIds.length + " widgets");
        
        appContext = context.getApplicationContext();
        
        // Calculate render size based on screen density FIRST
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        renderWidth = Math.max((int)(BASE_WIDTH_DP * dm.density), BASE_WIDTH_DP);
        renderHeight = Math.max((int)(BASE_HEIGHT_DP * dm.density), BASE_HEIGHT_DP);
        
        Log.d(TAG, "Render size: " + renderWidth + "x" + renderHeight + " (density: " + dm.density + ")");
        
        // IMMEDIATELY set initial view with placeholder for each widget - this must happen first
        for (int appWidgetId : appWidgetIds) {
            setupInitialView(context, appWidgetManager, appWidgetId);
        }
        
        // Check if app was reinstalled by comparing update times
        long currentUpdateTime = getAppUpdateTime(context);
        boolean appReinstalled = (currentUpdateTime != lastAppUpdateTime);
        lastAppUpdateTime = currentUpdateTime;
        
        if (appReinstalled) {
            Log.d(TAG, "App reinstall detected, forcing WebView recreation");
            stopUpdates();
            retryCount = 0; // Reset retry count on reinstall
        }
        
        // Schedule periodic alarm to keep widget alive
        scheduleAlarm(context);
        
        // Start WebView with a small delay to ensure everything is initialized
        final Context ctx = appContext;
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startWebViewUpdates(ctx);
            }
        }, WEBVIEW_INIT_DELAY);
    }
    
    /**
     * Setup initial view with placeholder and click handlers.
     * This ensures the widget is immediately visible and responsive.
     */
    private void setupInitialView(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            Log.d(TAG, "Setting up initial view for widget " + appWidgetId);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_webview);
            
            // Create a simple placeholder bitmap with NERV colors - keep it fast
            Bitmap placeholder = createPlaceholderBitmap();
            if (placeholder != null) {
                views.setImageViewBitmap(R.id.widget_image, placeholder);
            }
            
            // Set click handlers
            views.setOnClickPendingIntent(R.id.btn_stop, 
                getPendingIntent(context, ACTION_STOP, appWidgetId));
            views.setOnClickPendingIntent(R.id.btn_slow, 
                getPendingIntent(context, ACTION_SLOW, appWidgetId));
            views.setOnClickPendingIntent(R.id.btn_normal, 
                getPendingIntent(context, ACTION_NORMAL, appWidgetId));
            views.setOnClickPendingIntent(R.id.btn_racing, 
                getPendingIntent(context, ACTION_RACING, appWidgetId));
            
            // Update widget immediately - this is critical
            appWidgetManager.updateAppWidget(appWidgetId, views);
            Log.d(TAG, "Initial view set successfully for widget " + appWidgetId);
        } catch (Exception e) {
            Log.e(TAG, "Error setting initial view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create a placeholder bitmap with NERV/Evangelion style while loading.
     * Optimized for speed to avoid "widget could not be added" error.
     */
    private Bitmap createPlaceholderBitmap() {
        try {
            int width = Math.max(renderWidth, BASE_WIDTH_DP);
            int height = Math.max(renderHeight, BASE_HEIGHT_DP);
            
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            
            // Fill with NERV dark background
            canvas.drawColor(Color.parseColor("#0d0900"));
            
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setAntiAlias(true);
            
            // Draw warning stripes bar at top
            int stripeHeight = height / 8;
            int stripeWidth = stripeHeight;
            paint.setStyle(android.graphics.Paint.Style.FILL);
            
            for (int x = 0; x < width + stripeHeight; x += stripeWidth * 2) {
                // Yellow stripe
                paint.setColor(Color.parseColor("#FFB800"));
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(x, 0);
                path.lineTo(x + stripeWidth, 0);
                path.lineTo(x, stripeHeight);
                path.lineTo(x - stripeWidth, stripeHeight);
                path.close();
                canvas.drawPath(path, paint);
                
                // Black stripe
                paint.setColor(Color.parseColor("#1a1a1a"));
                path = new android.graphics.Path();
                path.moveTo(x + stripeWidth, 0);
                path.lineTo(x + stripeWidth * 2, 0);
                path.lineTo(x + stripeWidth, stripeHeight);
                path.lineTo(x, stripeHeight);
                path.close();
                canvas.drawPath(path, paint);
            }
            
            // Draw orange border
            paint.setColor(Color.parseColor("#FF6A00"));
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            canvas.drawRect(2, 2, width - 2, height - 2, paint);
            
            // Draw Japanese text "初期化中" (Initializing)
            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setColor(Color.parseColor("#FF6A00"));
            paint.setTextSize(height / 5);
            paint.setTextAlign(android.graphics.Paint.Align.CENTER);
            canvas.drawText("初期化中", width / 2, height / 2 - height / 12, paint);
            
            // Draw English text "INITIALIZING..."
            paint.setTextSize(height / 7);
            paint.setColor(Color.parseColor("#FFCC00"));
            canvas.drawText("INITIALIZING...", width / 2, height / 2 + height / 5, paint);
            
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "Error creating placeholder: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Schedule a periodic alarm to wake up the widget and check if it needs to be restarted.
     * This ensures the widget stays alive even after long periods of inactivity.
     * Updated for Android 12+ compatibility.
     */
    private static void scheduleAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, NervClockWidget.class);
            intent.setAction(ACTION_WAKE_UPDATE);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Cancel any existing alarm
            alarmManager.cancel(pendingIntent);
            
            // For Android 12+ (API 31+), use setAndAllowWhileIdle for better battery optimization
            // For older versions, use setRepeating
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                // On Android 12+, setRepeating behaves like setInexactRepeating
                // Use setAndAllowWhileIdle for more reliable updates
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + ALARM_INTERVAL,
                    pendingIntent
                );
                Log.d(TAG, "Alarm scheduled with setAndAllowWhileIdle (Android 12+)");
            } else {
                // Schedule repeating alarm for older versions
                alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + ALARM_INTERVAL,
                    ALARM_INTERVAL,
                    pendingIntent
                );
                Log.d(TAG, "Alarm scheduled with setRepeating (Android 11 and below)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm: " + e.getMessage());
        }
    }
    
    /**
     * Cancel the periodic alarm when widget is disabled.
     */
    private static void cancelAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, NervClockWidget.class);
            intent.setAction(ACTION_WAKE_UPDATE);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarm cancelled");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling alarm: " + e.getMessage());
        }
    }
    
    private static long getAppUpdateTime(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            super.onReceive(context, intent);
            return;
        }
        
        Log.d(TAG, "onReceive: " + action);
        
        // Handle package replacement (app reinstall)
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.d(TAG, "Package replaced, forcing widget refresh");
            stopUpdates();
            appContext = context.getApplicationContext();
            
            // Force update all widgets
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                new android.content.ComponentName(context, NervClockWidget.class));
            if (ids.length > 0) {
                onUpdate(context, mgr, ids);
            }
            return;
        }
        
        // Handle wake update action (from alarm or screen on)
        if (ACTION_WAKE_UPDATE.equals(action)) {
            Log.d(TAG, "Wake update received, checking widget state");
            handleWakeUpdate(context);
            return;
        }
        
        super.onReceive(context, intent);
        
        switch (action) {
            case ACTION_STOP:
                executeJS("nervClock.setMode('stop')");
                break;
            case ACTION_SLOW:
                executeJS("nervClock.setMode('slow')");
                break;
            case ACTION_NORMAL:
                executeJS("nervClock.setMode('normal')");
                break;
            case ACTION_RACING:
                executeJS("nervClock.setMode('racing')");
                break;
        }
    }
    
    /**
     * Handle wake update - check if widget is running and restart if needed.
     */
    private void handleWakeUpdate(Context context) {
        long now = System.currentTimeMillis();
        
        // Avoid checking too frequently
        if (now - lastWakeCheck < WAKE_CHECK_INTERVAL) {
            Log.d(TAG, "Wake check skipped, too soon since last check");
            // Still reschedule alarm for Android 12+
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                scheduleAlarm(context);
            }
            return;
        }
        lastWakeCheck = now;
        
        appContext = context.getApplicationContext();
        
        // Re-schedule alarm for Android 12+ (since setAndAllowWhileIdle is not repeating)
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            scheduleAlarm(context);
        }
        
        // Check if WebView is in a valid state
        if (!isUpdating || webView == null || !pageLoaded) {
            Log.d(TAG, "Widget not running, restarting...");
            
            // Get all widget ids and trigger update
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                new android.content.ComponentName(context, NervClockWidget.class));
            
            if (ids.length > 0) {
                onUpdate(context, mgr, ids);
            }
        } else {
            Log.d(TAG, "Widget is running, no restart needed");
        }
    }
    
    private PendingIntent getPendingIntent(Context context, String action, int appWidgetId) {
        Intent intent = new Intent(context, NervClockWidget.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId + action.hashCode(), intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    
    private static void executeJS(final String script) {
        if (webView != null && pageLoaded) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (webView != null) {
                        webView.evaluateJavascript(script, null);
                        Log.d(TAG, "Executed JS: " + script);
                    }
                }
            });
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");
        scheduleAlarm(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widget disabled");
        cancelAlarm(context);
        stopUpdates();
    }

    private static void startWebViewUpdates(final Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot start WebView");
            return;
        }
        
        if (isUpdating && webView != null && pageLoaded) {
            Log.d(TAG, "Already updating, skipping");
            return;
        }
        
        if (isCreatingWebView) {
            Log.d(TAG, "WebView creation already in progress, skipping");
            return;
        }
        
        // Always create fresh WebView
        stopUpdates();
        isCreatingWebView = true;
        webViewCreationTime = System.currentTimeMillis();
        
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Creating WebView " + renderWidth + "x" + renderHeight);
                    
                    // Ensure we have valid dimensions
                    if (renderWidth <= 0 || renderHeight <= 0) {
                        Log.e(TAG, "Invalid render dimensions, using defaults");
                        renderWidth = BASE_WIDTH_DP;
                        renderHeight = BASE_HEIGHT_DP;
                    }
                    
                    webView = new WebView(context.getApplicationContext());
                    
                    webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    webView.setBackgroundColor(Color.parseColor("#0d0900"));
                    
                    WebSettings settings = webView.getSettings();
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true);
                    settings.setAllowFileAccess(true);
                    settings.setAllowFileAccessFromFileURLs(true);
                    settings.setAllowUniversalAccessFromFileURLs(true);
                    settings.setUseWideViewPort(true);
                    settings.setLoadWithOverviewMode(true);
                    settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                    settings.setBlockNetworkLoads(true);
                    settings.setBlockNetworkImage(true);
                    settings.setLoadsImagesAutomatically(true);
                    
                    webView.setInitialScale(100);
                    
                    int widthSpec = View.MeasureSpec.makeMeasureSpec(renderWidth, View.MeasureSpec.EXACTLY);
                    int heightSpec = View.MeasureSpec.makeMeasureSpec(renderHeight, View.MeasureSpec.EXACTLY);
                    webView.measure(widthSpec, heightSpec);
                    webView.layout(0, 0, renderWidth, renderHeight);
                    
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                            Log.d(TAG, "Page started loading: " + url);
                        }
                        
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            Log.d(TAG, "Page loaded!");
                            isCreatingWebView = false;
                            pageLoaded = true;
                            isUpdating = true;
                            retryCount = 0; // Reset retry count on success
                            showingError = false;
                            scheduleUpdate(context);
                        }
                        
                        @Override
                        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                            Log.e(TAG, "WebView error: " + errorCode + " - " + description + " URL: " + failingUrl);
                            isCreatingWebView = false;
                            retryWithBackoff(context);
                        }
                    });
                    
                    // Set timeout for page load
                    schedulePageLoadTimeout(context);
                    
                    // Clear cache and load fresh
                    webView.clearCache(true);
                    String url = "file:///android_asset/widget.html?t=" + System.currentTimeMillis();
                    Log.d(TAG, "Loading URL: " + url);
                    webView.loadUrl(url);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error creating WebView: " + e.getMessage());
                    e.printStackTrace();
                    isCreatingWebView = false;
                    retryWithBackoff(context);
                }
            }
        });
    }
    
    /**
     * Schedule a timeout for page load. If page doesn't load in time, restart.
     */
    private static void schedulePageLoadTimeout(final Context context) {
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!pageLoaded && isCreatingWebView) {
                    Log.w(TAG, "Page load timeout, retrying...");
                    isCreatingWebView = false;
                    retryWithBackoff(context);
                }
            }
        }, PAGE_LOAD_TIMEOUT);
    }
    
    /**
     * Retry WebView creation with exponential backoff.
     */
    private static void retryWithBackoff(final Context context) {
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.e(TAG, "Max retries reached, showing error and scheduling retry in " + RETRY_RESET_DELAY + "ms");
            
            // Show error bitmap
            showErrorBitmap(context);
            
            // Schedule another attempt after delay
            retryCount = 0;
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Retrying after max failures...");
                    showingError = false;
                    startWebViewUpdates(context);
                }
            }, RETRY_RESET_DELAY);
            return;
        }
        
        retryCount++;
        int delay = 1000 * retryCount; // 1s, 2s, 3s
        Log.d(TAG, "Retrying in " + delay + "ms (attempt " + retryCount + "/" + MAX_RETRY_COUNT + ")");
        
        stopUpdates();
        
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startWebViewUpdates(context);
            }
        }, delay);
    }
    
    /**
     * Show an error bitmap when widget fails to load.
     */
    private static void showErrorBitmap(Context context) {
        if (context == null) context = appContext;
        if (context == null) return;
        
        showingError = true;
        
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                new android.content.ComponentName(context, NervClockWidget.class));
            
            if (ids.length == 0) return;
            
            Bitmap errorBmp = createErrorBitmap();
            if (errorBmp == null) return;
            
            for (int id : ids) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_webview);
                views.setImageViewBitmap(R.id.widget_image, errorBmp);
                mgr.updateAppWidget(id, views);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing error bitmap: " + e.getMessage());
        }
    }
    
    /**
     * Create an error bitmap with Evangelion style.
     */
    private static Bitmap createErrorBitmap() {
        try {
            int width = Math.max(renderWidth, BASE_WIDTH_DP);
            int height = Math.max(renderHeight, BASE_HEIGHT_DP);
            
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            
            // Fill with dark background
            canvas.drawColor(Color.parseColor("#0d0900"));
            
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setAntiAlias(true);
            
            // Draw warning stripes bar at top (red for error)
            int stripeHeight = height / 8;
            int stripeWidth = stripeHeight;
            paint.setStyle(android.graphics.Paint.Style.FILL);
            
            for (int x = 0; x < width + stripeHeight; x += stripeWidth * 2) {
                // Red stripe
                paint.setColor(Color.parseColor("#CC0000"));
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(x, 0);
                path.lineTo(x + stripeWidth, 0);
                path.lineTo(x, stripeHeight);
                path.lineTo(x - stripeWidth, stripeHeight);
                path.close();
                canvas.drawPath(path, paint);
                
                // Black stripe
                paint.setColor(Color.parseColor("#1a1a1a"));
                path = new android.graphics.Path();
                path.moveTo(x + stripeWidth, 0);
                path.lineTo(x + stripeWidth * 2, 0);
                path.lineTo(x + stripeWidth, stripeHeight);
                path.lineTo(x, stripeHeight);
                path.close();
                canvas.drawPath(path, paint);
            }
            
            // Draw red border
            paint.setColor(Color.parseColor("#CC0000"));
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            canvas.drawRect(2, 2, width - 2, height - 2, paint);
            
            // Draw Japanese text "読込失敗" (Load Failed)
            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setColor(Color.parseColor("#CC0000"));
            paint.setTextSize(height / 5);
            paint.setTextAlign(android.graphics.Paint.Align.CENTER);
            canvas.drawText("読込失敗", width / 2, height / 2 - height / 12, paint);
            
            // Draw English text "LOAD FAILED - RETRYING..."
            paint.setTextSize(height / 8);
            paint.setColor(Color.parseColor("#FF6666"));
            canvas.drawText("LOAD FAILED - RETRYING...", width / 2, height / 2 + height / 5, paint);
            
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "Error creating error bitmap: " + e.getMessage());
            return null;
        }
    }

    private static void scheduleUpdate(final Context context) {
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isUpdating || webView == null) return;
                updateWidget(context);
                scheduleUpdate(context);
            }
        }, UPDATE_INTERVAL);
    }

    private static void updateWidget(Context context) {
        if (context == null) context = appContext;
        if (context == null) {
            Log.e(TAG, "No context available for widget update");
            return;
        }
        
        if (!pageLoaded || webView == null) {
            Log.w(TAG, "WebView not ready, attempting restart");
            retryWithBackoff(context.getApplicationContext());
            return;
        }
        
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                new android.content.ComponentName(context, NervClockWidget.class));
            
            if (ids.length == 0) return;

            // Reuse bitmap if dimensions haven't changed
            if (reusableBitmap == null || lastRenderWidth != renderWidth || lastRenderHeight != renderHeight) {
                if (reusableBitmap != null) {
                    reusableBitmap.recycle();
                }
                reusableBitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
                reusableCanvas = new Canvas(reusableBitmap);
                lastRenderWidth = renderWidth;
                lastRenderHeight = renderHeight;
                Log.d(TAG, "Created new reusable bitmap: " + renderWidth + "x" + renderHeight);
            }
            
            // Clear and redraw
            reusableBitmap.eraseColor(Color.TRANSPARENT);
            webView.draw(reusableCanvas);
            Bitmap bmp = reusableBitmap;
            
            // Check if bitmap is empty/transparent (WebView failed to render)
            if (isBitmapEmpty(bmp)) {
                long timeSinceCreation = System.currentTimeMillis() - webViewCreationTime;
                // Give WebView some time to render before considering it failed
                if (timeSinceCreation > 3000) { // 3 seconds grace period
                    Log.w(TAG, "Empty bitmap detected after " + timeSinceCreation + "ms, recreating WebView");
                    retryWithBackoff(context.getApplicationContext());
                }
                return;
            }

            for (int id : ids) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_webview);
                views.setImageViewBitmap(R.id.widget_image, bmp);
                
                // Re-setup click handlers
                views.setOnClickPendingIntent(R.id.btn_stop, 
                    getPendingIntentStatic(context, ACTION_STOP, id));
                views.setOnClickPendingIntent(R.id.btn_slow, 
                    getPendingIntentStatic(context, ACTION_SLOW, id));
                views.setOnClickPendingIntent(R.id.btn_normal, 
                    getPendingIntentStatic(context, ACTION_NORMAL, id));
                views.setOnClickPendingIntent(R.id.btn_racing, 
                    getPendingIntentStatic(context, ACTION_RACING, id));
                
                mgr.updateAppWidget(id, views);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating widget: " + e.getMessage());
            e.printStackTrace();
            // Try to recover
            retryWithBackoff(context.getApplicationContext());
        }
    }
    
    private static boolean isBitmapEmpty(Bitmap bmp) {
        // Sample a few pixels to check if the bitmap has content
        int centerX = bmp.getWidth() / 2;
        int centerY = bmp.getHeight() / 2;
        int pixel = bmp.getPixel(centerX, centerY);
        // If center pixel is fully transparent, consider it empty
        return Color.alpha(pixel) == 0;
    }
    
    private static PendingIntent getPendingIntentStatic(Context context, String action, int appWidgetId) {
        Intent intent = new Intent(context, NervClockWidget.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId + action.hashCode(), intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void stopUpdates() {
        isUpdating = false;
        pageLoaded = false;
        isCreatingWebView = false;
        
        // Remove all callbacks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        // Recycle reusable bitmap
        if (reusableBitmap != null) {
            reusableBitmap.recycle();
            reusableBitmap = null;
            reusableCanvas = null;
            lastRenderWidth = 0;
            lastRenderHeight = 0;
        }
        
        // Destroy WebView on main thread
        if (webView != null) {
            final WebView wv = webView;
            webView = null;
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        wv.stopLoading();
                        wv.destroy();
                    } catch (Exception e) {
                        Log.e(TAG, "Error destroying WebView: " + e.getMessage());
                    }
                }
            });
        }
    }
}
