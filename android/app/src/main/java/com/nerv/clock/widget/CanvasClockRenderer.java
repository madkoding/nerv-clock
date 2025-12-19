package com.nerv.clock.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.nerv.clock.ui.ClockLogic;
import com.nerv.clock.ui.ClockViewRenderer;

/**
 * Canvas-based widget renderer
 * Generates Bitmaps using ClockView drawing logic without WebView
 */
public class CanvasClockRenderer {
    
    private static final String TAG = "CanvasClockRenderer";
    
    private final Context context;
    private ClockViewRenderer clockRenderer;
    
    public CanvasClockRenderer(Context context) {
        this.context = context.getApplicationContext();
        // Initialize fonts before creating renderer
        com.nerv.clock.ui.FontManager.initialize(context);
        this.clockRenderer = new ClockViewRenderer(context);
    }
    
    /**
     * Render clock to bitmap
     */
    public Bitmap renderClockBitmap(int width, int height) {
        try {
            if (width <= 0 || height <= 0) {
                return null;
            }
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            // Draw clock using ClockViewRenderer
            clockRenderer.drawClock(canvas, width, height);
            
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
