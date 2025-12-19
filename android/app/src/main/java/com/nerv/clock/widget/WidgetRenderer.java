package com.nerv.clock.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.RemoteViews;

import com.nerv.clock.R;
import com.nerv.clock.util.BitmapFactory;

/**
 * Handles rendering and updating widget views.
 */
public class WidgetRenderer {
    
    private static final String TAG = "WidgetRenderer";
    
    private final Context context;
    private final Class<?> widgetClass;
    
    public WidgetRenderer(Context context, Class<?> widgetClass) {
        this.context = context.getApplicationContext();
        this.widgetClass = widgetClass;
    }
    
    /**
     * Update all widget instances with the given bitmap.
     */
    public void updateWithBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = getWidgetIds(mgr);
            
            for (int id : ids) {
                RemoteViews views = createRemoteViews(bitmap, id);
                mgr.updateAppWidget(id, views);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating widgets: " + e.getMessage());
        }
    }
    
    /**
     * Update a specific widget with placeholder.
     */
    public void showPlaceholder(AppWidgetManager mgr, int widgetId, int width, int height) {
        try {
            Bitmap placeholder = BitmapFactory.createPlaceholderBitmap(width, height);
            if (placeholder != null) {
                RemoteViews views = createRemoteViews(placeholder, widgetId);
                mgr.updateAppWidget(widgetId, views);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing placeholder: " + e.getMessage());
        }
    }
    
    /**
     * Update all widgets with native time display.
     */
    public void showNativeTime(int width, int height) {
        try {
            Bitmap timeBitmap = BitmapFactory.createTimeBitmap(width, height);
            updateWithBitmap(timeBitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error showing native time: " + e.getMessage());
        }
    }
    
    /**
     * Update all widgets with error display.
     */
    public void showError(int width, int height) {
        try {
            Bitmap errorBitmap = BitmapFactory.createErrorBitmap(width, height);
            updateWithBitmap(errorBitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error showing error bitmap: " + e.getMessage());
        }
    }
    
    /**
     * Get all widget IDs for this provider.
     */
    public int[] getWidgetIds(AppWidgetManager mgr) {
        return mgr.getAppWidgetIds(new ComponentName(context, widgetClass));
    }
    
    private RemoteViews createRemoteViews(Bitmap bitmap, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_webview);
        views.setImageViewBitmap(R.id.widget_image, bitmap);
        
        // Set click handlers
        views.setOnClickPendingIntent(R.id.btn_stop, 
            createPendingIntent(WidgetConfig.ACTION_STOP, widgetId));
        views.setOnClickPendingIntent(R.id.btn_slow, 
            createPendingIntent(WidgetConfig.ACTION_SLOW, widgetId));
        views.setOnClickPendingIntent(R.id.btn_normal, 
            createPendingIntent(WidgetConfig.ACTION_NORMAL, widgetId));
        views.setOnClickPendingIntent(R.id.btn_racing, 
            createPendingIntent(WidgetConfig.ACTION_RACING, widgetId));
        
        return views;
    }
    
    private PendingIntent createPendingIntent(String action, int widgetId) {
        Intent intent = new Intent(context, widgetClass);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getBroadcast(
            context, 
            widgetId + action.hashCode(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
