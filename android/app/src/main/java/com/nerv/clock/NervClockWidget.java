package com.nerv.clock;

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
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.RemoteViews;
import android.util.Log;
import android.util.DisplayMetrics;

public class NervClockWidget extends AppWidgetProvider {

    private static final String TAG = "NervClockWidget";
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static WebView webView;
    private static boolean isUpdating = false;
    private static boolean pageLoaded = false;
    private static final int UPDATE_INTERVAL = 50;
    private static Context appContext;
    private static long lastAppUpdateTime = 0;
    
    // Actions for buttons
    public static final String ACTION_STOP = "com.nerv.clock.ACTION_STOP";
    public static final String ACTION_SLOW = "com.nerv.clock.ACTION_SLOW";
    public static final String ACTION_NORMAL = "com.nerv.clock.ACTION_NORMAL";
    public static final String ACTION_RACING = "com.nerv.clock.ACTION_RACING";
    
    // Base render size in dp
    private static final int BASE_WIDTH_DP = 400;
    private static final int BASE_HEIGHT_DP = 120;
    private static int renderWidth = 400;
    private static int renderHeight = 120;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called with " + appWidgetIds.length + " widgets");
        
        appContext = context.getApplicationContext();
        
        // Check if app was reinstalled by comparing update times
        long currentUpdateTime = getAppUpdateTime(context);
        boolean appReinstalled = (currentUpdateTime != lastAppUpdateTime);
        lastAppUpdateTime = currentUpdateTime;
        
        if (appReinstalled) {
            Log.d(TAG, "App reinstall detected, forcing WebView recreation");
            stopUpdates();
        }
        
        // Calculate render size based on screen density
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        renderWidth = (int)(BASE_WIDTH_DP * dm.density);
        renderHeight = (int)(BASE_HEIGHT_DP * dm.density);
        
        // Setup click handlers for each widget
        for (int appWidgetId : appWidgetIds) {
            setupClickHandlers(context, appWidgetManager, appWidgetId);
        }
        
        startWebViewUpdates(appContext);
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

    private void setupClickHandlers(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_webview);
        
        // STOP button
        views.setOnClickPendingIntent(R.id.btn_stop, 
            getPendingIntent(context, ACTION_STOP, appWidgetId));
        
        // SLOW button
        views.setOnClickPendingIntent(R.id.btn_slow, 
            getPendingIntent(context, ACTION_SLOW, appWidgetId));
        
        // NORMAL button
        views.setOnClickPendingIntent(R.id.btn_normal, 
            getPendingIntent(context, ACTION_NORMAL, appWidgetId));
        
        // RACING button
        views.setOnClickPendingIntent(R.id.btn_racing, 
            getPendingIntent(context, ACTION_RACING, appWidgetId));
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
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
            handler.post(new Runnable() {
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
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        stopUpdates();
    }

    private static void startWebViewUpdates(final Context context) {
        if (isUpdating && webView != null && pageLoaded) {
            Log.d(TAG, "Already updating, skipping");
            return;
        }
        
        // Always create fresh WebView
        stopUpdates();
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Creating WebView " + renderWidth + "x" + renderHeight);
                    webView = new WebView(context);
                    
                    webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    webView.setBackgroundColor(Color.parseColor("#0d0900"));
                    
                    WebSettings settings = webView.getSettings();
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true);
                    settings.setAllowFileAccess(true);
                    settings.setUseWideViewPort(true);
                    settings.setLoadWithOverviewMode(true);
                    settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                    
                    webView.setInitialScale(100);
                    
                    int widthSpec = View.MeasureSpec.makeMeasureSpec(renderWidth, View.MeasureSpec.EXACTLY);
                    int heightSpec = View.MeasureSpec.makeMeasureSpec(renderHeight, View.MeasureSpec.EXACTLY);
                    webView.measure(widthSpec, heightSpec);
                    webView.layout(0, 0, renderWidth, renderHeight);
                    
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            Log.d(TAG, "Page loaded!");
                            pageLoaded = true;
                            isUpdating = true;
                            scheduleUpdate(context);
                        }
                        
                        @Override
                        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                            Log.e(TAG, "WebView error: " + description);
                            // Try to reload after error
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startWebViewUpdates(context);
                                }
                            }, 1000);
                        }
                    });
                    
                    // Clear cache and load fresh
                    webView.clearCache(true);
                    webView.loadUrl("file:///android_asset/widget.html?t=" + System.currentTimeMillis());
                } catch (Exception e) {
                    Log.e(TAG, "Error creating WebView: " + e.getMessage());
                }
            }
        });
    }

    private static void scheduleUpdate(final Context context) {
        handler.postDelayed(new Runnable() {
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
        if (context == null) return;
        
        if (!pageLoaded || webView == null) {
            Log.w(TAG, "WebView not ready, attempting restart");
            stopUpdates();
            startWebViewUpdates(context.getApplicationContext());
            return;
        }
        
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                new android.content.ComponentName(context, NervClockWidget.class));
            
            if (ids.length == 0) return;

            Bitmap bmp = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            webView.draw(canvas);
            
            // Check if bitmap is empty/transparent (WebView failed to render)
            if (isBitmapEmpty(bmp)) {
                Log.w(TAG, "Empty bitmap detected, recreating WebView");
                stopUpdates();
                startWebViewUpdates(context.getApplicationContext());
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
            // Try to recover
            stopUpdates();
            startWebViewUpdates(context.getApplicationContext());
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
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
