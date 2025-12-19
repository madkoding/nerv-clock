package com.nerv.clock.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.nerv.clock.widget.WidgetConfig;

/**
 * Factory class for creating various bitmaps used by the widget.
 */
public final class BitmapFactory {
    
    private static final String TAG = "BitmapFactory";
    
    private BitmapFactory() {} // Prevent instantiation
    
    /**
     * Creates a simple time display bitmap (fallback when WebView unavailable).
     */
    public static Bitmap createTimeBitmap(int width, int height) {
        try {
            width = Math.max(width, WidgetConfig.BASE_WIDTH_DP);
            height = Math.max(height, WidgetConfig.BASE_HEIGHT_DP);
            
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(Color.parseColor(WidgetConfig.COLOR_BACKGROUND));
            
            // No initializing message - direct rendering
            
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Creates a loading placeholder bitmap with NERV/Evangelion style.
     */
    public static Bitmap createPlaceholderBitmap(int width, int height) {
        try {
            width = Math.max(width, WidgetConfig.BASE_WIDTH_DP);
            height = Math.max(height, WidgetConfig.BASE_HEIGHT_DP);
            
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(Color.WHITE);
            
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            // Draw warning stripes at top
            drawWarningStripes(canvas, width, height, WidgetConfig.COLOR_YELLOW, WidgetConfig.COLOR_DARK);
            
            // Draw orange border
            paint.setColor(Color.parseColor(WidgetConfig.COLOR_ORANGE));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            canvas.drawRect(2, 2, width - 2, height - 2, paint);
            
            // Draw text
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            paint.setTextSize(height / 6f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("NERV CLOCK", width / 2f, height / 2f - height / 8f, paint);
            
            paint.setColor(Color.RED);
            paint.setTextSize(height / 8f);
            canvas.drawText("LOADING...", width / 2f, height / 2f + height / 6f, paint);
            
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Creates an error bitmap when widget fails to load.
     */
    public static Bitmap createErrorBitmap(int width, int height) {
        try {
            width = Math.max(width, WidgetConfig.BASE_WIDTH_DP);
            height = Math.max(height, WidgetConfig.BASE_HEIGHT_DP);
            
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(Color.parseColor(WidgetConfig.COLOR_BACKGROUND));
            
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            // Draw red warning stripes at top
            drawWarningStripes(canvas, width, height, WidgetConfig.COLOR_RED, WidgetConfig.COLOR_DARK);
            
            // Draw red border
            paint.setColor(Color.parseColor(WidgetConfig.COLOR_RED));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            canvas.drawRect(2, 2, width - 2, height - 2, paint);
            
            // Draw Japanese text "読込失敗" (Load Failed)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor(WidgetConfig.COLOR_RED));
            paint.setTextSize(height / 5f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("読込失敗", width / 2f, height / 2f - height / 12f, paint);
            
            // Draw English text
            paint.setTextSize(height / 8f);
            paint.setColor(Color.parseColor("#FF6666"));
            canvas.drawText("LOAD FAILED - RETRYING...", width / 2f, height / 2f + height / 5f, paint);
            
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Draws diagonal warning stripes at the top of the canvas.
     */
    private static void drawWarningStripes(Canvas canvas, int width, int height, 
            String color1, String color2) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        
        int stripeHeight = height / 8;
        int stripeWidth = stripeHeight;
        
        for (int x = 0; x < width + stripeHeight; x += stripeWidth * 2) {
            // First color stripe
            paint.setColor(Color.parseColor(color1));
            Path path = new Path();
            path.moveTo(x, 0);
            path.lineTo(x + stripeWidth, 0);
            path.lineTo(x, stripeHeight);
            path.lineTo(x - stripeWidth, stripeHeight);
            path.close();
            canvas.drawPath(path, paint);
            
            // Second color stripe
            paint.setColor(Color.parseColor(color2));
            path = new Path();
            path.moveTo(x + stripeWidth, 0);
            path.lineTo(x + stripeWidth * 2, 0);
            path.lineTo(x + stripeWidth, stripeHeight);
            path.lineTo(x, stripeHeight);
            path.close();
            canvas.drawPath(path, paint);
        }
    }
}
