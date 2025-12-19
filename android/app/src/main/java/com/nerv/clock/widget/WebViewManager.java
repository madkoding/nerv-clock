package com.nerv.clock.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Manages WebView lifecycle for rendering the clock HTML.
 */
public class WebViewManager {
    
    private static final String TAG = "WebViewManager";
    
    public interface Callback {
        void onPageLoaded();
        void onError(String message);
    }
    
    private final Context context;
    private final Handler handler;
    private WebView webView;
    private boolean pageLoaded = false;
    private boolean isCreating = false;
    private long creationTime = 0;
    private Callback callback;
    
    // Reusable bitmap for rendering
    private Bitmap renderBitmap;
    private Canvas renderCanvas;
    private int lastWidth = 0;
    private int lastHeight = 0;
    
    public WebViewManager(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    public boolean isReady() {
        return webView != null && pageLoaded && !isCreating;
    }
    
    public boolean isCreating() {
        return isCreating;
    }
    
    public long getAge() {
        return System.currentTimeMillis() - creationTime;
    }
    
    /**
     * Create and initialize the WebView with the given dimensions.
     */
    public void create(final int width, final int height) {
        if (isCreating) {
            Log.d(TAG, "Creation already in progress");
            return;
        }
        
        destroy();
        isCreating = true;
        creationTime = System.currentTimeMillis();
        
        handler.post(() -> {
            try {
                Log.d(TAG, "Creating WebView " + width + "x" + height);
                
                webView = new WebView(context);
                webView.setLayerType(View.LAYER_TYPE_NONE, null);
                webView.setBackgroundColor(Color.parseColor(WidgetConfig.COLOR_BACKGROUND));
                
                configureWebSettings(webView.getSettings());
                configureWebView(webView, width, height);
                
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        Log.d(TAG, "Page loaded: " + url);
                        isCreating = false;
                        pageLoaded = true;
                        if (callback != null) callback.onPageLoaded();
                    }
                    
                    @Override
                    public void onReceivedError(WebView view, int errorCode, 
                            String description, String failingUrl) {
                        Log.e(TAG, "WebView error: " + errorCode + " - " + description);
                        isCreating = false;
                        if (callback != null) callback.onError(description);
                    }
                });
                
                // Load the widget HTML
                loadWidgetHtml();
                
                // Schedule timeout
                scheduleLoadTimeout();
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating WebView: " + e.getMessage());
                isCreating = false;
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    private void configureWebSettings(WebSettings settings) {
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
        settings.setDatabaseEnabled(false);
        settings.setGeolocationEnabled(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    }
    
    private void configureWebView(WebView wv, int width, int height) {
        wv.setLongClickable(false);
        wv.setHapticFeedbackEnabled(false);
        wv.setInitialScale(100);
        
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        wv.measure(widthSpec, heightSpec);
        wv.layout(0, 0, width, height);
    }
    
    private void loadWidgetHtml() {
        webView.clearCache(true);
        String url = "file:///android_asset/widget.html?t=" + System.currentTimeMillis();
        Log.d(TAG, "Loading URL: " + url);
        
        try {
            webView.loadUrl(url);
            webView.onResume();
            webView.resumeTimers();
        } catch (Exception e) {
            Log.e(TAG, "loadUrl failed: " + e.getMessage());
            if (callback != null) callback.onError(e.getMessage());
        }
    }
    
    private void scheduleLoadTimeout() {
        handler.postDelayed(() -> {
            if (!pageLoaded && isCreating) {
                Log.w(TAG, "Page load timeout");
                isCreating = false;
                if (callback != null) callback.onError("Page load timeout");
            }
        }, WidgetConfig.PAGE_LOAD_TIMEOUT_MS);
    }
    
    /**
     * Render the WebView content to a bitmap.
     */
    public Bitmap render(int width, int height) {
        if (webView == null || !pageLoaded) {
            return null;
        }
        
        try {
            // Reuse bitmap if dimensions match
            if (renderBitmap == null || lastWidth != width || lastHeight != height) {
                if (renderBitmap != null) {
                    renderBitmap.recycle();
                }
                renderBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                renderCanvas = new Canvas(renderBitmap);
                lastWidth = width;
                lastHeight = height;
            }
            
            // Clear with background color
            renderCanvas.drawColor(Color.parseColor(WidgetConfig.COLOR_BACKGROUND));
            
            // Ensure WebView is visible and has layout
            webView.setDrawingCacheEnabled(true);
            webView.buildDrawingCache(true);
            
            // Draw WebView onto canvas
            webView.draw(renderCanvas);
            
            return renderBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Render error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Execute JavaScript in the WebView.
     */
    public void executeJS(String script) {
        if (webView != null && pageLoaded) {
            handler.post(() -> {
                if (webView != null) {
                    webView.evaluateJavascript(script, null);
                }
            });
        }
    }
    
    /**
     * Destroy and cleanup the WebView.
     */
    public void destroy() {
        pageLoaded = false;
        isCreating = false;
        
        if (webView != null) {
            final WebView wv = webView;
            webView = null;
            
            handler.post(() -> {
                try {
                    wv.stopLoading();
                    wv.pauseTimers();
                    wv.onPause();
                    wv.loadUrl("about:blank");
                    wv.removeAllViews();
                    wv.destroy();
                } catch (Exception e) {
                    Log.e(TAG, "Error destroying WebView: " + e.getMessage());
                }
            });
        }
        
        if (renderBitmap != null) {
            renderBitmap.recycle();
            renderBitmap = null;
            renderCanvas = null;
        }
    }
}
