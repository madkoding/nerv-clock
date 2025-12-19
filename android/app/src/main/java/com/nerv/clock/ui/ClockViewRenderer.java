package com.nerv.clock.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Renders NERV Clock UI to Canvas
 * Used for both View and Widget rendering
 */
public class ClockViewRenderer {
    
    private static final String TAG = "ClockViewRenderer";
    
    private Context context;
    private ClockLogic clockLogic;
    
    // Paints
    private Paint digitPaint;
    private Paint colonPaint;
    private Paint segmentOffPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private Paint smallTextPaint;
    private Paint borderPaint;
    private Paint scanLinesPaint;
    
    // Layout dimensions
    private float topMargin;
    private float bottomMargin;
    private float sideMargin;
    private float topBarHeight;
    private float controlBarHeight;
    private float digitSize;
    private float smallDigitSize;
    private float colonSize;
    
    // Current state
    private ClockLogic.WarningState currentWarningState = ClockLogic.WarningState.NORMAL;
    
    // Animation state (for widget)
    private long lastBlinkTime = 0;
    private boolean colonVisible = true;
    
    public ClockViewRenderer(Context context) {
        FontManager.initialize(context);
        this.context = context;
        this.clockLogic = new ClockLogic();
        initializePaints();
    }
    
    private void initializePaints() {
        // Digit paint (7-segment style)
        digitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        digitPaint.setColor(ColorScheme.NERV_ORANGE);
        digitPaint.setTypeface(FontManager.getDSEG7());
        digitPaint.setTextAlign(Paint.Align.CENTER);
        
        // Colon paint
        colonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colonPaint.setColor(ColorScheme.NERV_ORANGE);
        colonPaint.setTypeface(FontManager.getDSEG7());
        colonPaint.setTextAlign(Paint.Align.CENTER);
        
        // Segment OFF paint
        segmentOffPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        segmentOffPaint.setColor(ColorScheme.SEGMENT_OFF);
        segmentOffPaint.setTypeface(FontManager.getDSEG7());
        segmentOffPaint.setTextAlign(Paint.Align.CENTER);
        
        // Regular text paint
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ColorScheme.NERV_ORANGE);
        textPaint.setTypeface(FontManager.getNimbusSansRegular());
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        // Label paint (bold)
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(ColorScheme.NERV_ORANGE);
        labelPaint.setTypeface(FontManager.getNimbusSansBold());
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setStyle(Paint.Style.FILL);
        
        // Small text paint
        smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallTextPaint.setColor(ColorScheme.NERV_ORANGE);
        smallTextPaint.setTypeface(FontManager.getNimbusSansRegular());
        smallTextPaint.setTextAlign(Paint.Align.CENTER);
        
        // Border paint
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(ColorScheme.BORDER_COLOR);
        borderPaint.setStrokeWidth(2);
        borderPaint.setStyle(Paint.Style.STROKE);
        
        // Scan lines paint
        scanLinesPaint = new Paint();
        scanLinesPaint.setColor(Color.argb(25, 0, 0, 0));
        scanLinesPaint.setStrokeWidth(2);
    }
    
    /**
     * Draw the complete clock to canvas
     */
    public void drawClock(Canvas canvas, int width, int height) {
        // Update layout dimensions
        updateDimensions(width, height);
        
        // Update clock state
        clockLogic.update();
        updateWarningState();
        
        // Draw background
        drawBackground(canvas, width, height);
        
        // Draw hexagon pattern
        drawHexagonPattern(canvas, width, height);
        
        // Draw scan lines
        drawScanLines(canvas, width, height);
        
        // Draw border
        canvas.drawRect(2, 2, width - 2, height - 2, borderPaint);
        
        // Draw top bar
        drawTopBar(canvas, width, height);
        
        // Draw clock display
        drawClockDisplay(canvas, width, height);
        
        // Draw control bar
        drawControlBar(canvas, width, height);
        
        // Draw corners
        drawCorners(canvas, width, height);
    }
    
    private void updateDimensions(int width, int height) {
        topMargin = height * UIConfig.MARGIN_TOP;
        bottomMargin = height * UIConfig.MARGIN_BOTTOM;
        sideMargin = width * UIConfig.MARGIN_SIDES;
        topBarHeight = height * UIConfig.TOP_BAR_HEIGHT;
        controlBarHeight = height * UIConfig.CONTROL_BAR_HEIGHT;
        
        float availableHeight = height - topBarHeight - controlBarHeight - (topMargin + bottomMargin) * 2;
        digitSize = availableHeight * UIConfig.DIGIT_SIZE_RATIO;
        smallDigitSize = digitSize * UIConfig.DIGIT_SMALL_RATIO;
        colonSize = digitSize * 0.9f;
        
        digitPaint.setTextSize(digitSize);
        colonPaint.setTextSize(colonSize);
        segmentOffPaint.setTextSize(digitSize);
    }
    
    private void updateWarningState() {
        if (clockLogic.getCurrentMode() == ClockLogic.Mode.SLOW) {
            long remainingMinutes = clockLogic.getPomodoroRemaining() / 60000;
            if (clockLogic.getPomodoroRemaining() <= 0) {
                currentWarningState = ClockLogic.WarningState.DEPLETED;
            } else if (remainingMinutes <= 1) {
                currentWarningState = ClockLogic.WarningState.CRITICAL;
            } else if (remainingMinutes <= 4) {
                currentWarningState = ClockLogic.WarningState.WARNING;
            } else {
                currentWarningState = ClockLogic.WarningState.NORMAL;
            }
        } else {
            currentWarningState = ClockLogic.WarningState.NORMAL;
        }
    }
    
    private void drawBackground(Canvas canvas, int width, int height) {
        // Gradient background
        LinearGradient gradient = new LinearGradient(0, 0, 0, height,
            ColorScheme.BG_TOP, ColorScheme.BG_BOTTOM, Shader.TileMode.CLAMP);
        Paint bgPaint = new Paint();
        bgPaint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, bgPaint);
    }
    
    private void drawHexagonPattern(Canvas canvas, int width, int height) {
        Paint hexPaint = new Paint();
        hexPaint.setColor(Color.argb(38, 136, 0, 0));
        hexPaint.setStyle(Paint.Style.STROKE);
        hexPaint.setStrokeWidth(1);
        
        float spacing = 28;
        for (float x = 0; x < width; x += spacing) {
            for (float y = 0; y < height; y += spacing * 0.866f) {
                drawHexagon(canvas, x, y, 14, hexPaint);
            }
        }
    }
    
    private void drawHexagon(Canvas canvas, float cx, float cy, float radius, Paint paint) {
        Path path = new Path();
        for (int i = 0; i < 6; i++) {
            float angle = (float) (i * Math.PI / 3);
            float x = cx + radius * (float) Math.cos(angle);
            float y = cy + radius * (float) Math.sin(angle);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        canvas.drawPath(path, paint);
    }
    
    private void drawScanLines(Canvas canvas, int width, int height) {
        for (int y = 0; y < height; y += 4) {
            canvas.drawLine(0, y, width, y, scanLinesPaint);
        }
    }
    
    private void drawTopBar(Canvas canvas, int width, int height) {
        float topBarBottom = topMargin + topBarHeight;
        
        // Draw border line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.argb(39, 255, 106, 0));
        linePaint.setStrokeWidth(1);
        canvas.drawLine(sideMargin, topBarBottom, width - sideMargin, topBarBottom, linePaint);
        
        // Draw Japanese text
        float jpTextSize = topBarHeight * 0.4f;
        textPaint.setTextSize(jpTextSize);
        canvas.drawText("作戦終了まで", sideMargin + width * 0.1f, topMargin + jpTextSize * 1.5f, textPaint);
        
        // Draw label
        float labelSize = topBarHeight * 0.3f;
        labelPaint.setTextSize(labelSize);
        canvas.drawText("NERV CHRONOMETER SYSTEM", width / 2, topMargin + labelSize * 1.5f, labelPaint);
        
        // Draw mode
        String modeText = clockLogic.getCurrentMode().name();
        if (clockLogic.getCurrentMode() == ClockLogic.Mode.SLOW) {
            long mins = clockLogic.getPomodoroDuration() / 60000;
            modeText = "SLOW " + mins + "m";
        }
        
        Paint modePaint = new Paint(textPaint);
        modePaint.setColor(ClockLogic.Mode.NORMAL == clockLogic.getCurrentMode() ? 
            ColorScheme.NERV_GREEN : ColorScheme.getModeColor(clockLogic.getCurrentMode().name().toLowerCase()));
        modePaint.setTextSize(labelSize);
        canvas.drawText(modeText, width - sideMargin - width * 0.1f, topMargin + labelSize * 1.5f, modePaint);
    }
    
    private void drawClockDisplay(Canvas canvas, int width, int height) {
        float displayTop = topMargin + topBarHeight;
        float displayBottom = height - controlBarHeight - bottomMargin;
        float displayCenterY = displayTop + (displayBottom - displayTop) / 2;
        
        String hour = clockLogic.getHourString();
        String minute = clockLogic.getMinuteString();
        String second = clockLogic.getSecondString();
        String centisecond = clockLogic.getCentisecondString();
        
        // Set digit color
        int digitColor = ColorScheme.NERV_ORANGE;
        switch (currentWarningState) {
            case WARNING:
                digitColor = ColorScheme.WARNING_YELLOW;
                break;
            case CRITICAL:
                digitColor = ColorScheme.CRITICAL_RED;
                break;
            case DEPLETED:
                drawDepletedMessage(canvas, displayTop, displayBottom, width);
                return;
        }
        
        digitPaint.setColor(digitColor);
        colonPaint.setColor(digitColor);
        
        // Calculate spacing
        Paint tempPaint = new Paint(digitPaint);
        Rect bounds = new Rect();
        tempPaint.getTextBounds("0", 0, 1, bounds);
        float digitWidth = bounds.width();
        
        // Total width: 6 digits (HH:MM:SS) + 2 colons + dot + 2 small digits
        float colonWidth = digitWidth * 0.35f;
        float dotWidth = digitWidth * 0.15f;
        float totalWidth = digitWidth * 6 + colonWidth * 2 + dotWidth + digitWidth * 0.5f;
        float startX = (width - totalWidth) / 2;
        
        // Draw hours
        drawDigit(canvas, hour.charAt(0) + "", startX, displayCenterY, digitPaint, "hour1");
        drawDigit(canvas, hour.charAt(1) + "", startX + digitWidth * 1.1f, displayCenterY, digitPaint, "hour2");
        
        // Draw colon (with blink animation)
        updateColonBlink();
        if (colonVisible) {
            canvas.drawText(":", startX + digitWidth * 2.15f, displayCenterY, colonPaint);
        }
        
        // Draw minutes
        drawDigit(canvas, minute.charAt(0) + "", startX + digitWidth * 2.6f, displayCenterY, digitPaint, "min1");
        drawDigit(canvas, minute.charAt(1) + "", startX + digitWidth * 3.7f, displayCenterY, digitPaint, "min2");
        
        // Draw second colon
        if (colonVisible) {
            canvas.drawText(":", startX + digitWidth * 4.75f, displayCenterY, colonPaint);
        }
        
        // Draw seconds
        drawDigit(canvas, second.charAt(0) + "", startX + digitWidth * 5.2f, displayCenterY, digitPaint, "sec1");
        drawDigit(canvas, second.charAt(1) + "", startX + digitWidth * 6.3f, displayCenterY, digitPaint, "sec2");
        
        // Draw small dot
        float dotSize = colonSize * 0.4f;
        colonPaint.setTextSize(dotSize);
        canvas.drawText(".", startX + digitWidth * 7.35f, displayCenterY + dotSize * 0.2f, colonPaint);
        
        // Draw centiseconds
        Paint smallDigitPaint = new Paint(digitPaint);
        smallDigitPaint.setTextSize(smallDigitSize);
        drawDigit(canvas, centisecond.charAt(0) + "", startX + digitWidth * 7.75f, displayCenterY, smallDigitPaint, "ms1");
        drawDigit(canvas, centisecond.charAt(1) + "", startX + digitWidth * 8.55f, displayCenterY, smallDigitPaint, "ms2");
    }
    
    private void updateColonBlink() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBlinkTime > 500) {
            colonVisible = !colonVisible;
            lastBlinkTime = currentTime;
        }
    }
    
    private void drawDigit(Canvas canvas, String digit, float x, float y, Paint paint, String id) {
        // Draw background "8"
        segmentOffPaint.setTextSize(paint.getTextSize());
        canvas.drawText("8", x, y, segmentOffPaint);
        
        // Draw actual digit
        canvas.drawText(digit, x, y, paint);
    }
    
    private void drawDepletedMessage(Canvas canvas, float top, float bottom, int width) {
        float height = bottom - top;
        float centerY = top + height / 2;
        
        // Draw content
        float contentLeft = sideMargin + width * 0.1f;
        float contentRight = width - sideMargin - width * 0.1f;
        RectF contentRect = new RectF(contentLeft, top + height * 0.2f, contentRight, bottom - height * 0.2f);
        
        Paint contentPaint = new Paint();
        contentPaint.setColor(Color.argb(25, 255, 0, 0));
        canvas.drawRect(contentRect, contentPaint);
        
        Paint contentBorder = new Paint();
        contentBorder.setColor(ColorScheme.CRITICAL_RED);
        contentBorder.setStyle(Paint.Style.STROKE);
        contentBorder.setStrokeWidth(1);
        canvas.drawRect(contentRect, contentBorder);
        
        // Draw JP text
        float jpSize = height * 0.35f;
        Paint jpPaint = new Paint(labelPaint);
        jpPaint.setTextSize(jpSize);
        jpPaint.setColor(ColorScheme.CRITICAL_RED);
        canvas.drawText("電力枯渇", (contentLeft + contentRight) / 2, centerY - jpSize * 0.3f, jpPaint);
        
        // Draw EN text
        float enSize = height * 0.2f;
        Paint enPaint = new Paint(smallTextPaint);
        enPaint.setTextSize(enSize);
        enPaint.setColor(ColorScheme.CRITICAL_RED);
        canvas.drawText("DEPLETED", (contentLeft + contentRight) / 2, centerY + jpSize * 0.4f, enPaint);
    }
    
    private void drawControlBar(Canvas canvas, int width, int height) {
        float controlBarTop = height - controlBarHeight - bottomMargin;
        
        // Draw separator
        Paint linePaint = new Paint();
        linePaint.setColor(Color.argb(39, 255, 106, 0));
        linePaint.setStrokeWidth(1);
        canvas.drawLine(sideMargin, controlBarTop, width - sideMargin, controlBarTop, linePaint);
        
        // Draw status light
        drawStatusLight(canvas, width, height);
    }
    
    private void drawStatusLight(Canvas canvas, int width, int height) {
        float lightSize = (controlBarHeight * 0.6f);
        float lightRight = width - sideMargin - width * 0.01f;
        float lightX = lightRight - lightSize / 2;
        float lightTop = height - controlBarHeight - bottomMargin + (controlBarHeight - lightSize) / 2;
        
        RectF lightRect = new RectF(lightX - lightSize / 2, lightTop, lightX + lightSize / 2, lightTop + lightSize);
        
        Paint lightPaint = new Paint();
        lightPaint.setColor(ColorScheme.STATUS_LIGHT_BRIGHT);
        canvas.drawRect(lightRect, lightPaint);
        
        Paint lightBorder = new Paint();
        lightBorder.setColor(ColorScheme.STATUS_LIGHT_DARK);
        lightBorder.setStyle(Paint.Style.STROKE);
        lightBorder.setStrokeWidth(2);
        canvas.drawRect(lightRect, lightBorder);
    }
    
    private void drawCorners(Canvas canvas, int width, int height) {
        float cornerSize = width * UIConfig.CORNER_SIZE;
        Paint cornerPaint = new Paint();
        cornerPaint.setColor(ColorScheme.NERV_ORANGE);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(2);
        
        // Top-left
        Path tlPath = new Path();
        tlPath.moveTo(sideMargin, sideMargin + cornerSize);
        tlPath.lineTo(sideMargin, sideMargin);
        tlPath.lineTo(sideMargin + cornerSize, sideMargin);
        canvas.drawPath(tlPath, cornerPaint);
        
        // Top-right
        Path trPath = new Path();
        trPath.moveTo(width - sideMargin - cornerSize, sideMargin);
        trPath.lineTo(width - sideMargin, sideMargin);
        trPath.lineTo(width - sideMargin, sideMargin + cornerSize);
        canvas.drawPath(trPath, cornerPaint);
        
        // Bottom-left
        Path blPath = new Path();
        blPath.moveTo(sideMargin, height - sideMargin - cornerSize);
        blPath.lineTo(sideMargin, height - sideMargin);
        blPath.lineTo(sideMargin + cornerSize, height - sideMargin);
        canvas.drawPath(blPath, cornerPaint);
        
        // Bottom-right
        Path brPath = new Path();
        brPath.moveTo(width - sideMargin - cornerSize, height - sideMargin);
        brPath.lineTo(width - sideMargin, height - sideMargin);
        brPath.lineTo(width - sideMargin, height - sideMargin - cornerSize);
        canvas.drawPath(brPath, cornerPaint);
    }
    
    public ClockLogic getClockLogic() {
        return clockLogic;
    }
}
