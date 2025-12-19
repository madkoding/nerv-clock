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

/**
 * Renders NERV Clock UI to Canvas
 * ALL SIZES are relative to WIDTH (not height) for consistent scaling
 */
public class ClockViewRenderer {
    
    private static final String TAG = "ClockViewRenderer";
    private static final long FADE_DURATION_MS = 200;  // Fade transition duration
    
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
    
    // Current state
    private ClockLogic.WarningState currentWarningState = ClockLogic.WarningState.NORMAL;
    private boolean isCharging = false;
    
    // Animation state
    private long lastBlinkTime = 0;
    private boolean colonVisible = true;
    
    // Fade transition state - store previous values and change timestamps
    private String prevHour = "";
    private String prevMinute = "";
    private String prevSecond = "";
    private String prevCentisecond = "";
    private String fadingHour = "";
    private String fadingMinute = "";
    private String fadingSecond = "";
    private String fadingCentisecond = "";
    private long hourChangeTime = 0;
    private long minuteChangeTime = 0;
    private long secondChangeTime = 0;
    private long centisecondChangeTime = 0;
    
    public ClockViewRenderer(Context context) {
        this.context = context;
        this.clockLogic = new ClockLogic();
        initializePaints();
    }
    
    public void setCharging(boolean charging) {
        this.isCharging = charging;
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
    }
    
    /**
     * Draw the complete clock to canvas
     * All sizes relative to WIDTH for consistent scaling
     */
    public void drawClock(Canvas canvas, int width, int height) {
        // Update clock state
        clockLogic.update();
        updateWarningState();
        
        // BASE UNIT = width (all sizes relative to this)
        float baseUnit = width;
        
        // Layout constants (relative to width)
        float sideMargin = baseUnit * 0.02f;
        float topBarHeight = baseUnit * 0.06f;
        float controlBarHeight = baseUnit * 0.05f;
        float topMargin = baseUnit * 0.01f;
        float bottomMargin = baseUnit * 0.01f;
        
        // Draw background
        drawBackground(canvas, width, height);
        
        // Draw hexagon pattern
        drawHexagonPattern(canvas, width, height);
        
        // Draw border
        canvas.drawRect(2, 2, width - 2, height - 2, borderPaint);
        
        // Draw top bar
        drawTopBar(canvas, width, baseUnit, sideMargin, topMargin, topBarHeight);
        
        // Draw clock display (main area)
        float clockTop = topMargin + topBarHeight + baseUnit * 0.02f;
        float clockBottom = height - controlBarHeight - bottomMargin - baseUnit * 0.02f;
        drawClockDisplay(canvas, width, height, baseUnit, sideMargin, clockTop, clockBottom);
        
        // Draw control bar
        drawControlBar(canvas, width, height, baseUnit, sideMargin, controlBarHeight, bottomMargin);
        
        // Draw corners
        drawCorners(canvas, width, height, baseUnit, sideMargin);
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
        LinearGradient gradient = new LinearGradient(0, 0, 0, height,
            ColorScheme.BG_TOP, ColorScheme.BG_BOTTOM, Shader.TileMode.CLAMP);
        Paint bgPaint = new Paint();
        bgPaint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, bgPaint);
    }
    
    private void drawHexagonPattern(Canvas canvas, int width, int height) {
        // Hexagon pattern relative to widget size - LARGER hexagons
        float baseSpacing = width * 0.08f;  // Bigger spacing
        float hexSize = baseSpacing * 0.5f;
        
        Paint hexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexPaint.setColor(Color.argb(50, 180, 0, 0));  // Red with ~20% opacity
        hexPaint.setStyle(Paint.Style.STROKE);
        hexPaint.setStrokeWidth(2f);
        
        // Offset for honeycomb pattern
        float rowHeight = baseSpacing * 0.866f;
        int row = 0;
        
        for (float y = -hexSize; y < height + hexSize; y += rowHeight) {
            float xOffset = (row % 2 == 0) ? 0 : baseSpacing * 0.5f;
            for (float x = -hexSize + xOffset; x < width + hexSize; x += baseSpacing) {
                drawHexagon(canvas, x, y, hexSize, hexPaint);
            }
            row++;
        }
    }
    
    private void drawHexagon(Canvas canvas, float cx, float cy, float size, Paint paint) {
        Path path = new Path();
        for (int i = 0; i < 6; i++) {
            float angle = (float) (Math.PI / 3 * i - Math.PI / 6);
            float x = cx + size * (float) Math.cos(angle);
            float y = cy + size * (float) Math.sin(angle);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        canvas.drawPath(path, paint);
    }
    
    private void drawTopBar(Canvas canvas, int width, float baseUnit, float sideMargin, float topMargin, float topBarHeight) {
        float topBarBottom = topMargin + topBarHeight;
        
        // Draw separator line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.argb(39, 255, 106, 0));
        linePaint.setStrokeWidth(1);
        canvas.drawLine(sideMargin, topBarBottom, width - sideMargin, topBarBottom, linePaint);
        
        // Text sizes relative to WIDTH
        float jpTextSize = baseUnit * 0.035f;
        float labelSize = baseUnit * 0.028f;
        
        // Calculate vertical center of top bar
        float topBarCenterY = topMargin + topBarHeight / 2;
        
        // Draw Japanese text (left aligned, vertically centered)
        Paint jpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        jpPaint.setTypeface(FontManager.getNimbusSansRegular());
        jpPaint.setTextSize(jpTextSize);
        jpPaint.setColor(ColorScheme.NERV_ORANGE);
        jpPaint.setTextAlign(Paint.Align.LEFT);
        Paint.FontMetrics jpFm = jpPaint.getFontMetrics();
        float jpTextY = topBarCenterY + (jpFm.descent - jpFm.ascent) / 2 - jpFm.descent;
        canvas.drawText("作戦終了まで", sideMargin + baseUnit * 0.02f, jpTextY, jpPaint);
        
        // Draw label "NERV CHRONOMETER" (centered)
        Paint chronoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chronoPaint.setTypeface(FontManager.getNimbusSansBold());
        chronoPaint.setTextSize(labelSize);
        chronoPaint.setColor(ColorScheme.NERV_ORANGE);
        chronoPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics chronoFm = chronoPaint.getFontMetrics();
        float chronoTextY = topBarCenterY + (chronoFm.descent - chronoFm.ascent) / 2 - chronoFm.descent;
        canvas.drawText("NERV CHRONOMETER", width / 2, chronoTextY, chronoPaint);
        
        // Draw mode (right aligned, vertically centered)
        String modeText = clockLogic.getCurrentMode().name();
        if (clockLogic.getCurrentMode() == ClockLogic.Mode.SLOW) {
            long mins = clockLogic.getPomodoroDuration() / 60000;
            modeText = "SLOW " + mins + "m";
        }
        
        Paint modePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        modePaint.setTypeface(FontManager.getNimbusSansBold());
        modePaint.setTextSize(labelSize);
        modePaint.setTextAlign(Paint.Align.RIGHT);
        modePaint.setColor(ClockLogic.Mode.NORMAL == clockLogic.getCurrentMode() ? 
            ColorScheme.NERV_GREEN : ColorScheme.getModeColor(clockLogic.getCurrentMode().name().toLowerCase()));
        Paint.FontMetrics modeFm = modePaint.getFontMetrics();
        float modeTextY = topBarCenterY + (modeFm.descent - modeFm.ascent) / 2 - modeFm.descent;
        canvas.drawText(modeText, width - sideMargin - baseUnit * 0.12f, modeTextY, modePaint);
        
        // Draw warning box
        drawWarningBox(canvas, width, baseUnit, sideMargin, topMargin, topBarHeight);
    }
    
    private void drawWarningBox(Canvas canvas, int width, float baseUnit, float sideMargin, float topMargin, float topBarHeight) {
        float boxHeight = topBarHeight * 0.85f;
        float boxWidth = baseUnit * 0.1f;
        float boxRight = width - sideMargin - baseUnit * 0.01f;
        float boxLeft = boxRight - boxWidth;
        float boxTop = topMargin + (topBarHeight - boxHeight) / 2;
        float boxBottom = boxTop + boxHeight;
        
        // Color depends on charging state
        int stripeColor = isCharging ? ColorScheme.NERV_GREEN : ColorScheme.NERV_RED;
        int textColor = isCharging ? ColorScheme.NERV_GREEN : ColorScheme.NERV_ORANGE;
        String kanjiText = isCharging ? "外部" : "内部";
        String labelText = isCharging ? "EXTERNAL" : "INTERNAL";
        
        // Draw striped part (left edge)
        float stripeWidth = boxWidth * 0.15f;
        Paint stripePaint = new Paint();
        stripePaint.setColor(stripeColor);
        stripePaint.setStyle(Paint.Style.FILL);
        
        for (float x = boxLeft; x < boxLeft + stripeWidth; x += 3) {
            canvas.drawRect(x, boxTop, x + 2, boxBottom, stripePaint);
        }
        
        // Draw content box background
        Paint contentPaint = new Paint();
        contentPaint.setColor(ColorScheme.NERV_DARK);
        contentPaint.setStyle(Paint.Style.FILL);
        RectF contentRect = new RectF(boxLeft + stripeWidth, boxTop, boxRight, boxBottom);
        canvas.drawRect(contentRect, contentPaint);
        
        // Draw border
        Paint borderPaint2 = new Paint();
        borderPaint2.setColor(textColor);
        borderPaint2.setStyle(Paint.Style.STROKE);
        borderPaint2.setStrokeWidth(1);
        canvas.drawRect(contentRect, borderPaint2);
        
        // Draw kanji and label vertically centered in box
        float contentCenterX = boxLeft + stripeWidth + (boxWidth - stripeWidth) / 2;
        float kanjiSize = baseUnit * 0.022f;      // Slightly larger
        float internalSize = baseUnit * 0.014f;   // Larger for better visibility
        
        // Use font metrics for accurate vertical centering
        Paint kanjiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kanjiPaint.setTypeface(FontManager.getNimbusSansBold());
        kanjiPaint.setTextSize(kanjiSize);
        kanjiPaint.setColor(textColor);
        kanjiPaint.setTextAlign(Paint.Align.CENTER);
        
        Paint internalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        internalPaint.setTypeface(FontManager.getNimbusSansRegular());
        internalPaint.setTextSize(internalSize);
        internalPaint.setColor(textColor);
        internalPaint.setTextAlign(Paint.Align.CENTER);
        
        // Calculate total height of both texts with gap
        Paint.FontMetrics kanjiFm = kanjiPaint.getFontMetrics();
        Paint.FontMetrics internalFm = internalPaint.getFontMetrics();
        float kanjiHeight = kanjiFm.descent - kanjiFm.ascent;
        float internalHeight = internalFm.descent - internalFm.ascent;
        float gap = baseUnit * 0.002f;  // Small gap between texts
        float totalHeight = kanjiHeight + gap + internalHeight;
        
        // Center both texts vertically in the box (with slight offset down)
        float boxCenterY = boxTop + boxHeight / 2;
        float textOffset = baseUnit * 0.005f;  // Offset down a few pixels
        float startY = boxCenterY - totalHeight / 2 + textOffset;
        
        // Draw kanji
        float kanjiY = startY + kanjiHeight - kanjiFm.descent;
        canvas.drawText(kanjiText, contentCenterX, kanjiY, kanjiPaint);
        
        // Draw label text
        float internalY = startY + kanjiHeight + gap + internalHeight - internalFm.descent;
        canvas.drawText(labelText, contentCenterX, internalY, internalPaint);
    }
    
    private void drawClockDisplay(Canvas canvas, int width, int height, float baseUnit, float sideMargin, float clockTop, float clockBottom) {
        String hour = clockLogic.getHourString();
        String minute = clockLogic.getMinuteString();
        String second = clockLogic.getSecondString();
        String centisecond = clockLogic.getCentisecondString();
        
        long currentTime = System.currentTimeMillis();
        
        // Track changes and store previous values for fade out
        if (!hour.equals(prevHour)) {
            fadingHour = prevHour;
            hourChangeTime = currentTime;
            prevHour = hour;
        }
        if (!minute.equals(prevMinute)) {
            fadingMinute = prevMinute;
            minuteChangeTime = currentTime;
            prevMinute = minute;
        }
        if (!second.equals(prevSecond)) {
            fadingSecond = prevSecond;
            secondChangeTime = currentTime;
            prevSecond = second;
        }
        if (!centisecond.equals(prevCentisecond)) {
            fadingCentisecond = prevCentisecond;
            centisecondChangeTime = currentTime;
            prevCentisecond = centisecond;
        }
        
        // Set digit color based on warning state
        int digitColor = ColorScheme.NERV_ORANGE;
        switch (currentWarningState) {
            case WARNING:
                digitColor = ColorScheme.WARNING_YELLOW;
                break;
            case CRITICAL:
                digitColor = ColorScheme.CRITICAL_RED;
                break;
            case DEPLETED:
                drawDepletedMessage(canvas, width, clockTop, clockBottom, baseUnit, sideMargin);
                return;
        }
        
        // SIZES RELATIVE TO WIDTH (not height!)
        float digitSize = baseUnit * 0.132f;      // Main digits (10% larger)
        float smallDigitSize = digitSize * 0.5f;  // Centiseconds
        
        // Update blink state
        updateColonBlink();
        
        // Build time strings
        String mainTime = hour + ":" + minute + ":" + second;
        String mainTimeBg = "88:88:88";  // Background for 7-segment effect
        
        // Create paint for main time
        Paint mainPaint = new Paint(digitPaint);
        mainPaint.setTextSize(digitSize);
        mainPaint.setTextAlign(Paint.Align.CENTER);
        
        // Create paint for centiseconds
        Paint smallPaint = new Paint(digitPaint);
        smallPaint.setTextSize(smallDigitSize);
        smallPaint.setTextAlign(Paint.Align.LEFT);
        
        // Calculate dimensions
        Rect bounds = new Rect();
        mainPaint.getTextBounds(mainTime, 0, mainTime.length(), bounds);
        float mainWidth = mainPaint.measureText(mainTime);
        float mainHeight = bounds.height();
        
        smallPaint.getTextBounds(centisecond, 0, centisecond.length(), bounds);
        float smallWidth = smallPaint.measureText("." + centisecond);
        
        // Total width and center position
        float centerX = width / 2f;
        float mainX = centerX - smallWidth / 2;  // Shift left to account for centiseconds
        float centerY = clockTop + (clockBottom - clockTop) / 2 + mainHeight / 3;
        
        // Draw background "88:88:88" for 7-segment effect (dimmed)
        mainPaint.setColor(Color.argb(25, 255, 106, 0));  // Very dim orange
        canvas.drawText(mainTimeBg, mainX, centerY, mainPaint);
        
        // Calculate fade out alpha for previous second (fading out)
        float secondElapsed = currentTime - secondChangeTime;
        if (secondElapsed < FADE_DURATION_MS && fadingSecond.length() > 0) {
            float fadeOutAlpha = 1f - (secondElapsed / (float) FADE_DURATION_MS);
            String fadingTime = colonVisible ? 
                hour + ":" + minute + ":" + fadingSecond : 
                hour + " " + minute + " " + fadingSecond;
            mainPaint.setColor(Color.argb((int)(255 * fadeOutAlpha), 
                Color.red(digitColor), Color.green(digitColor), Color.blue(digitColor)));
            canvas.drawText(fadingTime, mainX, centerY, mainPaint);
        }
        
        // Draw current time (always full opacity) with glow effect
        String displayTime = colonVisible ? mainTime : hour + " " + minute + " " + second;
        mainPaint.setColor(digitColor);
        // Add subtle outer glow
        mainPaint.setShadowLayer(digitSize * 0.08f, 0, 0, digitColor);
        canvas.drawText(displayTime, mainX, centerY, mainPaint);
        mainPaint.setShadowLayer(0, 0, 0, 0);  // Clear shadow for next draws
        
        // Draw centiseconds with dot - SEPARATE dot and digits to avoid shifting
        float dotWidth = smallPaint.measureText(".");
        float smallX = mainX + mainWidth / 2 + dotWidth * 0.2f;
        float smallY = centerY;  // Align baseline with main digits
        
        // Background for dot
        smallPaint.setColor(Color.argb(25, 255, 106, 0));
        canvas.drawText(".", smallX, smallY, smallPaint);
        
        // Draw actual dot (only if visible)
        if (colonVisible) {
            smallPaint.setColor(digitColor);
            canvas.drawText(".", smallX, smallY, smallPaint);
        }
        
        // Position for centiseconds (after dot, fixed position)
        float centiX = smallX + dotWidth;
        
        // Background for centiseconds
        smallPaint.setColor(Color.argb(25, 255, 106, 0));
        canvas.drawText("88", centiX, smallY, smallPaint);
        
        // Draw fading out previous centiseconds
        float centiElapsed = currentTime - centisecondChangeTime;
        if (centiElapsed < FADE_DURATION_MS && fadingCentisecond.length() > 0) {
            float fadeOutAlpha = 1f - (centiElapsed / (float) FADE_DURATION_MS);
            smallPaint.setColor(Color.argb((int)(255 * fadeOutAlpha), 
                Color.red(digitColor), Color.green(digitColor), Color.blue(digitColor)));
            canvas.drawText(fadingCentisecond, centiX, smallY, smallPaint);
        }
        
        // Actual centiseconds (always full opacity) with glow effect
        smallPaint.setColor(digitColor);
        smallPaint.setShadowLayer(smallDigitSize * 0.08f, 0, 0, digitColor);
        canvas.drawText(centisecond, centiX, smallY, smallPaint);
        smallPaint.setShadowLayer(0, 0, 0, 0);  // Clear shadow
    }
    
    private void updateColonBlink() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBlinkTime > 500) {
            colonVisible = !colonVisible;
            lastBlinkTime = currentTime;
        }
    }
    
    private void drawDigit(Canvas canvas, String digit, float x, float y, Paint paint) {
        // Draw background "8"
        segmentOffPaint.setTextSize(paint.getTextSize());
        canvas.drawText("8", x, y, segmentOffPaint);
        
        // Draw actual digit
        canvas.drawText(digit, x, y, paint);
    }
    
    private void drawSmallDigit(Canvas canvas, String digit, float x, float y, Paint paint, float size) {
        // Draw background "8"
        segmentOffPaint.setTextSize(size);
        canvas.drawText("8", x, y, segmentOffPaint);
        
        // Draw actual digit
        canvas.drawText(digit, x, y, paint);
    }
    
    private void drawDepletedMessage(Canvas canvas, int width, float top, float bottom, float baseUnit, float sideMargin) {
        float height = bottom - top;
        float centerY = top + height / 2;
        
        float contentLeft = sideMargin + baseUnit * 0.1f;
        float contentRight = width - sideMargin - baseUnit * 0.1f;
        RectF contentRect = new RectF(contentLeft, top + height * 0.2f, contentRight, bottom - height * 0.2f);
        
        Paint contentPaint = new Paint();
        contentPaint.setColor(Color.argb(25, 255, 0, 0));
        canvas.drawRect(contentRect, contentPaint);
        
        Paint contentBorder = new Paint();
        contentBorder.setColor(ColorScheme.CRITICAL_RED);
        contentBorder.setStyle(Paint.Style.STROKE);
        contentBorder.setStrokeWidth(1);
        canvas.drawRect(contentRect, contentBorder);
        
        // Draw JP text (relative to width)
        float jpSize = baseUnit * 0.06f;
        Paint jpPaint = new Paint(labelPaint);
        jpPaint.setTextSize(jpSize);
        jpPaint.setColor(ColorScheme.CRITICAL_RED);
        canvas.drawText("電力枯渇", (contentLeft + contentRight) / 2, centerY - jpSize * 0.2f, jpPaint);
        
        // Draw EN text (relative to width)
        float enSize = baseUnit * 0.035f;
        Paint enPaint = new Paint(smallTextPaint);
        enPaint.setTextSize(enSize);
        enPaint.setColor(ColorScheme.CRITICAL_RED);
        canvas.drawText("DEPLETED", (contentLeft + contentRight) / 2, centerY + jpSize * 0.5f, enPaint);
    }
    
    private void drawControlBar(Canvas canvas, int width, int height, float baseUnit, float sideMargin, float controlBarHeight, float bottomMargin) {
        float controlBarTop = height - controlBarHeight - bottomMargin;
        
        // Draw separator
        Paint linePaint = new Paint();
        linePaint.setColor(Color.argb(39, 255, 106, 0));
        linePaint.setStrokeWidth(1);
        canvas.drawLine(sideMargin, controlBarTop, width - sideMargin, controlBarTop, linePaint);
        
        // Draw 4 buttons
        String[] buttonModes = {"STOP", "SLOW", "NORMAL", "RACING"};
        int[] buttonColors = {
            ColorScheme.NERV_GREEN,
            ColorScheme.WARNING_YELLOW,
            ColorScheme.NERV_GREEN,
            ColorScheme.NERV_RED
        };
        
        float buttonGap = baseUnit * 0.01f;
        float availableWidth = width - sideMargin * 2 - buttonGap * 3;
        float buttonWidth = availableWidth / 4;
        float buttonHeight = controlBarHeight * 0.85f;
        float buttonTop = controlBarTop + (controlBarHeight - buttonHeight) / 2;
        
        for (int i = 0; i < 4; i++) {
            float buttonLeft = sideMargin + (buttonWidth + buttonGap) * i;
            RectF buttonRect = new RectF(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight);
            
            // Draw button background
            Paint bgPaint = new Paint();
            bgPaint.setColor(ColorScheme.BUTTON_BACKGROUND_TOP);
            bgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(buttonRect, bgPaint);
            
            // Draw button border
            Paint borderPaint2 = new Paint();
            borderPaint2.setColor(ColorScheme.BUTTON_BORDER);
            borderPaint2.setStyle(Paint.Style.STROKE);
            borderPaint2.setStrokeWidth(1);
            canvas.drawRect(buttonRect, borderPaint2);
            
            // Draw button text with proper centering
            Paint btnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            btnTextPaint.setTypeface(FontManager.getNimbusSansBold());
            btnTextPaint.setTextSize(baseUnit * 0.02f);
            btnTextPaint.setColor(buttonColors[i]);
            btnTextPaint.setTextAlign(Paint.Align.CENTER);
            
            // Calculate proper vertical center using font metrics
            Paint.FontMetrics fm = btnTextPaint.getFontMetrics();
            float textHeight = fm.descent - fm.ascent;
            float textY = buttonRect.centerY() + textHeight / 2 - fm.descent;
            
            canvas.drawText(buttonModes[i], buttonRect.centerX(), textY, btnTextPaint);
        }
        
        // Draw status light
        drawStatusLight(canvas, width, height, baseUnit, sideMargin, controlBarHeight, bottomMargin);
    }
    
    private void drawStatusLight(Canvas canvas, int width, int height, float baseUnit, float sideMargin, float controlBarHeight, float bottomMargin) {
        float lightSize = baseUnit * 0.025f;
        float lightRight = width - sideMargin - baseUnit * 0.01f;
        float lightX = lightRight - lightSize / 2;
        float lightTop = height - controlBarHeight - bottomMargin + (controlBarHeight - lightSize) / 2;
        
        RectF lightRect = new RectF(lightX - lightSize / 2, lightTop, lightX + lightSize / 2, lightTop + lightSize);
        
        Paint lightPaint = new Paint();
        lightPaint.setColor(ColorScheme.STATUS_LIGHT_BRIGHT);
        canvas.drawRect(lightRect, lightPaint);
        
        Paint lightBorder = new Paint();
        lightBorder.setColor(ColorScheme.STATUS_LIGHT_DARK);
        lightBorder.setStyle(Paint.Style.STROKE);
        lightBorder.setStrokeWidth(1);
        canvas.drawRect(lightRect, lightBorder);
    }
    
    private void drawCorners(Canvas canvas, int width, int height, float baseUnit, float sideMargin) {
        float cornerSize = baseUnit * 0.02f;  // Half the original size (was 0.04f)
        float cornerMargin = sideMargin * 0.3f;  // Corners closer to edge
        Paint cornerPaint = new Paint();
        cornerPaint.setColor(ColorScheme.NERV_ORANGE);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(2);
        
        // Top-left
        Path tlPath = new Path();
        tlPath.moveTo(cornerMargin, cornerMargin + cornerSize);
        tlPath.lineTo(cornerMargin, cornerMargin);
        tlPath.lineTo(cornerMargin + cornerSize, cornerMargin);
        canvas.drawPath(tlPath, cornerPaint);
        
        // Top-right
        Path trPath = new Path();
        trPath.moveTo(width - cornerMargin - cornerSize, cornerMargin);
        trPath.lineTo(width - cornerMargin, cornerMargin);
        trPath.lineTo(width - cornerMargin, cornerMargin + cornerSize);
        canvas.drawPath(trPath, cornerPaint);
        
        // Bottom-left
        Path blPath = new Path();
        blPath.moveTo(cornerMargin, height - cornerMargin - cornerSize);
        blPath.lineTo(cornerMargin, height - cornerMargin);
        blPath.lineTo(cornerMargin + cornerSize, height - cornerMargin);
        canvas.drawPath(blPath, cornerPaint);
        
        // Bottom-right
        Path brPath = new Path();
        brPath.moveTo(width - cornerMargin - cornerSize, height - cornerMargin);
        brPath.lineTo(width - cornerMargin, height - cornerMargin);
        brPath.lineTo(width - cornerMargin, height - cornerMargin - cornerSize);
        canvas.drawPath(brPath, cornerPaint);
    }
    
    public ClockLogic getClockLogic() {
        return clockLogic;
    }
}
