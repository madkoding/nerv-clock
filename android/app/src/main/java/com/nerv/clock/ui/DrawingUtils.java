package com.nerv.clock.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Drawing utilities for NERV Clock UI
 */
public class DrawingUtils {
    
    /**
     * Draw a glow effect around text
     */
    public static void drawTextWithGlow(Canvas canvas, String text, float x, float y, 
                                        Paint paint, int glowColor, float glowRadius) {
        int originalColor = paint.getColor();
        
        // Draw multiple layers of glow
        paint.setColor(glowColor);
        paint.setAlpha(30);
        canvas.drawText(text, x, y, paint);
        
        paint.setAlpha(50);
        canvas.drawText(text, x, y, paint);
        
        // Draw main text
        paint.setColor(originalColor);
        paint.setAlpha(255);
        canvas.drawText(text, x, y, paint);
    }
    
    /**
     * Draw button with gradient and border
     */
    public static void drawButton(Canvas canvas, RectF rect, String text, Paint bgPaint, 
                                 Paint borderPaint, Paint textPaint, boolean isActive) {
        // Draw background
        canvas.drawRect(rect, bgPaint);
        
        // Draw border
        canvas.drawRect(rect, borderPaint);
        
        // Draw text
        float centerY = rect.centerY() + (textPaint.descent() + textPaint.ascent()) / 2;
        canvas.drawText(text, rect.centerX(), centerY, textPaint);
    }
    
    /**
     * Draw a rectangular border (corner accent style)
     */
    public static void drawCornerAccent(Canvas canvas, float x, float y, float size, Paint paint) {
        float offset = size / 2;
        
        // Horizontal line
        canvas.drawLine(x, y, x + size, y, paint);
        
        // Vertical line
        canvas.drawLine(x, y, x, y + size, paint);
    }
    
    /**
     * Draw diagonal stripes pattern (for warning boxes)
     */
    public static void drawDiagonalStripes(Canvas canvas, RectF rect, Paint paint, float stripeWidth) {
        float spacing = 6;
        float startX = rect.left - rect.height();
        float endX = rect.right;
        
        for (float x = startX; x < endX; x += spacing) {
            float y1 = rect.top;
            float x1 = x;
            float y2 = rect.bottom;
            float x2 = x + rect.height();
            
            if (x1 < rect.right && x2 > rect.left) {
                canvas.drawLine(
                    Math.max(x1, rect.left), 
                    Math.max(y1, rect.top), 
                    Math.min(x2, rect.right), 
                    Math.min(y2, rect.bottom), 
                    paint
                );
            }
        }
    }
    
    /**
     * Draw text with shadow effect
     */
    public static void drawTextWithShadow(Canvas canvas, String text, float x, float y,
                                         Paint textPaint, Paint shadowPaint) {
        // Draw shadow
        canvas.drawText(text, x + 2, y + 2, shadowPaint);
        // Draw text
        canvas.drawText(text, x, y, textPaint);
    }
}
