package com.nerv.clock.ui;

import android.animation.ValueAnimator;
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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.HashMap;
import java.util.Map;

/**
 * Main NERV Clock custom View
 * Renders the digital clock interface with all visual effects
 */
public class ClockView extends View implements ClockLogic.OnClockUpdateListener {
    
    private static final String TAG = "ClockView";
    
    // Clock logic
    private ClockLogic clockLogic;
    
    // Paints
    private Paint digitPaint;
    private Paint colonPaint;
    private Paint segmentOffPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private Paint smallTextPaint;
    private Paint borderPaint;
    private Paint buttonPaint;
    private Paint buttonActivePaint;
    private Paint scanLinesPaint;
    
    // Brushes/shaders
    private LinearGradient bgGradient;
    private LinearGradient buttonGradient;
    private LinearGradient activeButtonGradient;
    
    // Animation state
    private Map<String, Float> digitGhostAlpha = new HashMap<>();
    private Map<String, String> lastDigitValues = new HashMap<>();
    private float colonBlinkAlpha = 1f;
    private float criticalPulseAlpha = 1f;
    
    // UI Layout
    private float topMargin;
    private float bottomMargin;
    private float sideMargin;
    private float topBarHeight;
    private float controlBarHeight;
    private float digitSize;
    private float smallDigitSize;
    private float colonSize;
    private float smallColonSize;
    
    // Button dimensions
    private float buttonHeight;
    private RectF[] buttonRects = new RectF[4];
    private static final String[] BUTTON_MODES = {"STOP", "SLOW", "NORMAL", "RACING"};
    
    // Warning state
    private ClockLogic.WarningState currentWarningState = ClockLogic.WarningState.NORMAL;
    
    // Listener for button clicks
    public interface OnModeChangeListener {
        void onModeChanged(ClockLogic.Mode mode);
        void onPauseToggled();
    }
    
    private OnModeChangeListener modeChangeListener;
    
    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        FontManager.initialize(context);
        initializePaints();
        clockLogic = new ClockLogic();
        clockLogic.setUpdateListener(this);
        setupAnimations();
        setOnTouchListener(new ButtonTouchListener());
    }
    
    public ClockView(Context context) {
        super(context);
        FontManager.initialize(context);
        initializePaints();
        clockLogic = new ClockLogic();
        clockLogic.setUpdateListener(this);
        setupAnimations();
        setOnTouchListener(new ButtonTouchListener());
    }
    
    private void initializePaints() {
        // Digit paint (7-segment style)
        digitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        digitPaint.setColor(ColorScheme.NERV_ORANGE);
        digitPaint.setTypeface(FontManager.getDSEG7());
        digitPaint.setTextAlign(Paint.Align.CENTER);
        
        // Colon paint (blinking)
        colonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colonPaint.setColor(ColorScheme.NERV_ORANGE);
        colonPaint.setTypeface(FontManager.getDSEG7());
        colonPaint.setTextAlign(Paint.Align.CENTER);
        
        // Segment OFF paint (dim segments)
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
        
        // Button paints
        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setColor(ColorScheme.BUTTON_BACKGROUND_TOP);
        buttonPaint.setStyle(Paint.Style.FILL);
        
        buttonActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonActivePaint.setColor(ColorScheme.ACTIVE_BUTTON_BG_TOP);
        buttonActivePaint.setStyle(Paint.Style.FILL);
        
        // Scan lines paint
        scanLinesPaint = new Paint();
        scanLinesPaint.setColor(Color.argb(25, 0, 0, 0));
        scanLinesPaint.setStrokeWidth(2);
    }
    
    private void setupAnimations() {
        // Colon blinking animation
        ValueAnimator colonBlink = ValueAnimator.ofFloat(1f, 0.3f, 1f);
        colonBlink.setDuration(1000);
        colonBlink.setRepeatCount(ValueAnimator.INFINITE);
        colonBlink.addUpdateListener(animation -> {
            colonBlinkAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        colonBlink.start();
        
        // Critical pulse animation
        ValueAnimator criticalPulse = ValueAnimator.ofFloat(1f, 0.7f, 1f);
        criticalPulse.setDuration(500);
        criticalPulse.setRepeatCount(ValueAnimator.INFINITE);
        criticalPulse.addUpdateListener(animation -> {
            criticalPulseAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        criticalPulse.start();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Calculate dimensions based on view size
        topMargin = h * 0.01f;
        bottomMargin = h * 0.01f;
        sideMargin = w * 0.02f;
        topBarHeight = h * 0.15f;
        controlBarHeight = h * 0.12f;
        buttonHeight = controlBarHeight * 0.8f;
        
        // Calculate digit size based on available space
        float availableHeight = h - topBarHeight - controlBarHeight - (topMargin + bottomMargin) * 2;
        digitSize = availableHeight * 0.8f; // 80% of available height
        smallDigitSize = digitSize * 0.7f;
        colonSize = digitSize * 0.9f;
        smallColonSize = digitSize * 0.65f;
        
        // Update digit sizes
        digitPaint.setTextSize(digitSize);
        colonPaint.setTextSize(colonSize);
        segmentOffPaint.setTextSize(digitSize);
        smallTextPaint.setTextSize(smallDigitSize);
        
        // Calculate button rectangles
        float buttonGap = w * 0.01f;
        float buttonWidth = (w - sideMargin * 2 - buttonGap * 3) / 4;
        float controlBarTop = h - controlBarHeight - bottomMargin;
        
        for (int i = 0; i < 4; i++) {
            float left = sideMargin + (buttonWidth + buttonGap) * i;
            buttonRects[i] = new RectF(left, controlBarTop, left + buttonWidth, controlBarTop + buttonHeight);
        }
        
        // Background gradient
        bgGradient = new LinearGradient(0, 0, 0, h, ColorScheme.BG_TOP, ColorScheme.BG_BOTTOM, Shader.TileMode.CLAMP);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw background
        if (bgGradient != null) {
            Paint bgPaint = new Paint();
            bgPaint.setShader(bgGradient);
            canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        } else {
            canvas.drawColor(ColorScheme.NERV_DARK);
        }
        
        // Draw hexagon pattern background
        drawHexagonPattern(canvas);
        
        // Draw scan lines overlay
        drawScanLines(canvas);
        
        // Draw border
        canvas.drawRect(2, 2, getWidth() - 2, getHeight() - 2, borderPaint);
        
        // Draw top bar
        drawTopBar(canvas);
        
        // Draw main clock display
        drawClockDisplay(canvas);
        
        // Draw control bar and buttons
        drawControlBar(canvas);
        
        // Draw corner accents
        drawCorners(canvas);
    }
    
    private void drawHexagonPattern(Canvas canvas) {
        // Simplified hexagon pattern - would be more complex for exact replica
        Paint hexPaint = new Paint();
        hexPaint.setColor(Color.argb(38, 136, 0, 0)); // ~15% red at 25%
        hexPaint.setStyle(Paint.Style.STROKE);
        hexPaint.setStrokeWidth(1);
        
        // Draw a simple grid of hexagons
        float spacing = 28;
        for (float x = 0; x < getWidth(); x += spacing) {
            for (float y = 0; y < getHeight(); y += spacing * 0.866f) {
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
    
    private void drawScanLines(Canvas canvas) {
        for (int y = 0; y < getHeight(); y += 4) {
            canvas.drawLine(0, y, getWidth(), y, scanLinesPaint);
        }
    }
    
    private void drawTopBar(Canvas canvas) {
        float topBarBottom = topMargin + topBarHeight;
        
        // Draw border line below top bar
        Paint linePaint = new Paint();
        linePaint.setColor(Color.argb(39, 255, 106, 0)); // NERV_ORANGE_DIM
        linePaint.setStrokeWidth(1);
        canvas.drawLine(sideMargin, topBarBottom, getWidth() - sideMargin, topBarBottom, linePaint);
        
        // Draw Japanese text "作戦終了まで"
        float jpTextSize = topBarHeight * 0.4f;
        textPaint.setTextSize(jpTextSize);
        canvas.drawText("作戦終了まで", sideMargin + getWidth() * 0.1f, topMargin + jpTextSize * 1.5f, textPaint);
        
        // Draw "NERV CHRONOMETER SYSTEM"
        float labelSize = topBarHeight * 0.3f;
        labelPaint.setTextSize(labelSize);
        canvas.drawText("NERV CHRONOMETER SYSTEM", getWidth() / 2, topMargin + labelSize * 1.5f, labelPaint);
        
        // Draw mode indicator
        String modeText = clockLogic.getCurrentMode().name();
        if (clockLogic.getCurrentMode() == ClockLogic.Mode.SLOW) {
            long mins = clockLogic.getPomodoroDuration() / 60000;
            modeText = "SLOW " + mins + "m";
        }
        if (clockLogic.isPaused()) {
            modeText += " [PAUSED]";
        }
        
        Paint modePaint = new Paint(textPaint);
        modePaint.setColor(ClockLogic.Mode.NORMAL == clockLogic.getCurrentMode() ? ColorScheme.NERV_GREEN : 
                          ColorScheme.getModeColor(clockLogic.getCurrentMode().name().toLowerCase()));
        modePaint.setTextSize(labelSize);
        canvas.drawText(modeText, getWidth() - sideMargin - getWidth() * 0.1f, topMargin + labelSize * 1.5f, modePaint);
        
        // Draw warning box
        drawWarningBox(canvas);
    }
    
    private void drawWarningBox(Canvas canvas) {
        float boxHeight = topBarHeight * 0.6f;
        float boxWidth = getWidth() * 0.12f;
        float boxRight = getWidth() - sideMargin - getWidth() * 0.01f;
        float boxLeft = boxRight - boxWidth;
        float boxTop = topMargin + (topBarHeight - boxHeight) / 2;
        float boxBottom = boxTop + boxHeight;
        
        // Draw striped part
        float stripeWidth = boxWidth * 0.15f;
        Paint stripePaint = new Paint();
        stripePaint.setColor(ColorScheme.NERV_RED);
        stripePaint.setStyle(Paint.Style.FILL);
        
        for (float x = boxLeft - stripeWidth; x < boxLeft; x += 3) {
            canvas.drawRect(x, boxTop, x + 3, boxBottom, stripePaint);
        }
        
        // Draw content box
        Paint contentPaint = new Paint();
        contentPaint.setColor(ColorScheme.NERV_DARK);
        contentPaint.setStyle(Paint.Style.FILL);
        RectF contentRect = new RectF(boxLeft - stripeWidth, boxTop, boxLeft, boxBottom);
        canvas.drawRect(contentRect, contentPaint);
        
        // Draw border
        Paint borderPaint2 = new Paint();
        borderPaint2.setColor(ColorScheme.NERV_ORANGE);
        borderPaint2.setStyle(Paint.Style.STROKE);
        borderPaint2.setStrokeWidth(1);
        canvas.drawRect(contentRect, borderPaint2);
        
        // Draw kanji "内部"
        float kanjiSize = boxHeight * 0.5f;
        Paint kanjiPaint = new Paint(labelPaint);
        kanjiPaint.setTextSize(kanjiSize);
        kanjiPaint.setColor(ColorScheme.NERV_ORANGE);
        canvas.drawText("内部", (boxLeft - stripeWidth + boxLeft) / 2, boxTop + kanjiSize * 1.2f, kanjiPaint);
        
        // Draw "INTERNAL"
        float internalSize = boxHeight * 0.3f;
        Paint internalPaint = new Paint(smallTextPaint);
        internalPaint.setTextSize(internalSize);
        internalPaint.setColor(ColorScheme.NERV_ORANGE);
        canvas.drawText("INTERNAL", (boxLeft - stripeWidth + boxLeft) / 2, boxTop + kanjiSize + internalSize * 0.8f, internalPaint);
    }
    
    private void drawClockDisplay(Canvas canvas) {
        float displayTop = topMargin + topBarHeight;
        float displayBottom = getHeight() - controlBarHeight - bottomMargin;
        float displayHeight = displayBottom - displayTop;
        float displayCenterY = displayTop + displayHeight / 2;
        
        // Get current time values
        String hour = clockLogic.getHourString();
        String minute = clockLogic.getMinuteString();
        String second = clockLogic.getSecondString();
        String centisecond = clockLogic.getCentisecondString();
        
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
                drawDepletedMessage(canvas, displayTop, displayBottom);
                return;
        }
        
        digitPaint.setColor(digitColor);
        colonPaint.setColor(digitColor);
        
        // Calculate digit spacing
        Paint tempPaint = new Paint(digitPaint);
        Rect bounds = new Rect();
        tempPaint.getTextBounds("0", 0, 1, bounds);
        float digitWidth = bounds.width();
        
        float totalWidth = digitWidth * 2 * 3 + (float) digitWidth * 0.35f * 2 + (float) digitWidth * 0.3f + (float) digitWidth * 0.2f;
        float startX = (getWidth() - totalWidth) / 2;
        
        // Draw hours
        drawDigit(canvas, hour.charAt(0) + "", startX, displayCenterY, digitPaint, "hour1");
        drawDigit(canvas, hour.charAt(1) + "", startX + digitWidth * 1.2f, displayCenterY, digitPaint, "hour2");
        
        // Draw colon
        float colonAlpha = currentWarningState == ClockLogic.WarningState.CRITICAL ? criticalPulseAlpha : colonBlinkAlpha;
        colonPaint.setAlpha((int) (255 * colonAlpha));
        canvas.drawText(":", startX + digitWidth * 2.5f, displayCenterY, colonPaint);
        colonPaint.setAlpha(255);
        
        // Draw minutes
        drawDigit(canvas, minute.charAt(0) + "", startX + digitWidth * 3.2f, displayCenterY, digitPaint, "min1");
        drawDigit(canvas, minute.charAt(1) + "", startX + digitWidth * 4.4f, displayCenterY, digitPaint, "min2");
        
        // Draw colon
        colonPaint.setAlpha((int) (255 * colonAlpha));
        canvas.drawText(":", startX + digitWidth * 5.7f, displayCenterY, colonPaint);
        colonPaint.setAlpha(255);
        
        // Draw seconds
        drawDigit(canvas, second.charAt(0) + "", startX + digitWidth * 6.4f, displayCenterY, digitPaint, "sec1");
        drawDigit(canvas, second.charAt(1) + "", startX + digitWidth * 7.6f, displayCenterY, digitPaint, "sec2");
        
        // Draw small dot separator
        float dotSize = colonSize * 0.4f;
        colonPaint.setTextSize(dotSize);
        canvas.drawText(".", startX + digitWidth * 8.7f, displayCenterY + dotSize * 0.3f, colonPaint);
        
        // Draw centiseconds (small)
        Paint smallDigitPaint = new Paint(digitPaint);
        smallDigitPaint.setTextSize(smallDigitSize);
        drawDigit(canvas, centisecond.charAt(0) + "", startX + digitWidth * 9.2f, displayCenterY, smallDigitPaint, "ms1");
        drawDigit(canvas, centisecond.charAt(1) + "", startX + digitWidth * 10.2f, displayCenterY, smallDigitPaint, "ms2");
    }
    
    private void drawDigit(Canvas canvas, String digit, float x, float y, Paint paint, String id) {
        // Draw the background "8" (off segment)
        segmentOffPaint.setTextSize(paint.getTextSize());
        canvas.drawText("8", x, y, segmentOffPaint);
        
        // Draw the actual digit
        canvas.drawText(digit, x, y, paint);
    }
    
    private void drawDepletedMessage(Canvas canvas, float top, float bottom) {
        float height = bottom - top;
        float centerY = top + height / 2;
        
        // Draw left stripe
        float stripeWidth = getWidth() * 0.05f;
        Paint stripePaint = new Paint();
        stripePaint.setColor(ColorScheme.CRITICAL_RED);
        stripePaint.setStyle(Paint.Style.FILL);
        
        for (float x = sideMargin; x < sideMargin + stripeWidth; x += 3) {
            canvas.drawRect(x, top, x + 3, bottom, stripePaint);
        }
        
        // Draw content box
        float contentLeft = sideMargin + stripeWidth + getWidth() * 0.01f;
        float contentRight = getWidth() - sideMargin - stripeWidth - getWidth() * 0.01f;
        RectF contentRect = new RectF(contentLeft, top + height * 0.2f, contentRight, bottom - height * 0.2f);
        
        Paint contentPaint = new Paint();
        contentPaint.setColor(Color.argb(25, 255, 0, 0));
        contentPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(contentRect, contentPaint);
        
        Paint contentBorderPaint = new Paint();
        contentBorderPaint.setColor(ColorScheme.CRITICAL_RED);
        contentBorderPaint.setStyle(Paint.Style.STROKE);
        contentBorderPaint.setStrokeWidth(1);
        canvas.drawRect(contentRect, contentBorderPaint);
        
        // Draw Japanese text "電力枯渇"
        float jpSize = height * 0.35f;
        Paint jpPaint = new Paint(labelPaint);
        jpPaint.setTextSize(jpSize);
        jpPaint.setColor(ColorScheme.CRITICAL_RED);
        canvas.drawText("電力枯渇", (contentLeft + contentRight) / 2, centerY - jpSize * 0.3f, jpPaint);
        
        // Draw "DEPLETED"
        float enSize = height * 0.2f;
        Paint enPaint = new Paint(smallTextPaint);
        enPaint.setTextSize(enSize);
        enPaint.setColor(ColorScheme.CRITICAL_RED);
        canvas.drawText("DEPLETED", (contentLeft + contentRight) / 2, centerY + jpSize * 0.4f, enPaint);
    }
    
    private void drawControlBar(Canvas canvas) {
        float controlBarTop = getHeight() - controlBarHeight - bottomMargin;
        
        // Draw separator line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.argb(39, 255, 106, 0));
        linePaint.setStrokeWidth(1);
        canvas.drawLine(sideMargin, controlBarTop, getWidth() - sideMargin, controlBarTop, linePaint);
        
        // Draw buttons
        for (int i = 0; i < 4; i++) {
            drawButton(canvas, i);
        }
        
        // Draw status light
        drawStatusLight(canvas);
    }
    
    private void drawButton(Canvas canvas, int index) {
        RectF rect = buttonRects[index];
        if (rect == null) return;
        
        String mode = BUTTON_MODES[index];
        boolean isActive = false;
        boolean isDisabled = false;
        
        switch (index) {
            case 0: // STOP
                isActive = clockLogic.isPaused();
                isDisabled = clockLogic.getCurrentMode() == ClockLogic.Mode.NORMAL;
                break;
            case 1: // SLOW
                isActive = clockLogic.getCurrentMode() == ClockLogic.Mode.SLOW;
                break;
            case 2: // NORMAL
                isActive = clockLogic.getCurrentMode() == ClockLogic.Mode.NORMAL;
                break;
            case 3: // RACING
                isActive = clockLogic.getCurrentMode() == ClockLogic.Mode.RACING;
                break;
        }
        
        // Draw button background
        Paint bgPaint = new Paint();
        if (isActive) {
            bgPaint.setColor(ColorScheme.ACTIVE_BUTTON_BG_TOP);
        } else {
            bgPaint.setColor(ColorScheme.BUTTON_BACKGROUND_TOP);
        }
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rect, bgPaint);
        
        // Draw border
        Paint borderPaint2 = new Paint();
        if (isActive) {
            borderPaint2.setColor(ColorScheme.getModeColor(mode.toLowerCase()));
        } else {
            borderPaint2.setColor(ColorScheme.BUTTON_BORDER);
        }
        borderPaint2.setStyle(Paint.Style.STROKE);
        borderPaint2.setStrokeWidth(2);
        canvas.drawRect(rect, borderPaint2);
        
        // Draw button text
        Paint textPaint2 = new Paint(textPaint);
        if (index == 0 && isActive) {
            textPaint2.setTextSize(rect.height() * 0.4f);
            textPaint2.setColor(ColorScheme.NERV_GREEN);
        } else if (isActive) {
            textPaint2.setTextSize(rect.height() * 0.4f);
            textPaint2.setColor(ColorScheme.getModeColor(mode.toLowerCase()));
        } else {
            textPaint2.setTextSize(rect.height() * 0.4f);
            textPaint2.setColor(ColorScheme.NERV_GREEN);
        }
        
        if (isDisabled) {
            textPaint2.setAlpha(77); // ~30% opacity
        }
        
        String buttonText = mode;
        if (index == 0) {
            buttonText = clockLogic.isPaused() ? "PLAY" : "STOP";
        }
        
        canvas.drawText(buttonText, rect.centerX(), rect.centerY() + rect.height() * 0.15f, textPaint2);
    }
    
    private void drawStatusLight(Canvas canvas) {
        float lightSize = buttonHeight * 0.6f;
        float lightRight = getWidth() - sideMargin - getWidth() * 0.01f;
        float lightX = lightRight - lightSize / 2;
        float lightTop = getHeight() - controlBarHeight - bottomMargin + (controlBarHeight - lightSize) / 2;
        
        RectF lightRect = new RectF(lightX - lightSize / 2, lightTop, lightX + lightSize / 2, lightTop + lightSize);
        
        Paint lightPaint = new Paint();
        lightPaint.setColor(ColorScheme.STATUS_LIGHT_BRIGHT);
        lightPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(lightRect, lightPaint);
        
        Paint borderLight = new Paint();
        borderLight.setColor(ColorScheme.STATUS_LIGHT_DARK);
        borderLight.setStyle(Paint.Style.STROKE);
        borderLight.setStrokeWidth(2);
        canvas.drawRect(lightRect, borderLight);
        
        // Glow effect
        Paint glowPaint = new Paint();
        glowPaint.setColor(Color.argb(100, 255, 51, 51));
        glowPaint.setStyle(Paint.Style.FILL);
        RectF glowRect = new RectF(lightRect.left - 4, lightRect.top - 4, lightRect.right + 4, lightRect.bottom + 4);
        canvas.drawRect(glowRect, glowPaint);
    }
    
    private void drawCorners(Canvas canvas) {
        float cornerSize = getWidth() * 0.02f;
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
        trPath.moveTo(getWidth() - sideMargin - cornerSize, sideMargin);
        trPath.lineTo(getWidth() - sideMargin, sideMargin);
        trPath.lineTo(getWidth() - sideMargin, sideMargin + cornerSize);
        canvas.drawPath(trPath, cornerPaint);
        
        // Bottom-left
        Path blPath = new Path();
        blPath.moveTo(sideMargin, getHeight() - sideMargin - cornerSize);
        blPath.lineTo(sideMargin, getHeight() - sideMargin);
        blPath.lineTo(sideMargin + cornerSize, getHeight() - sideMargin);
        canvas.drawPath(blPath, cornerPaint);
        
        // Bottom-right
        Path brPath = new Path();
        brPath.moveTo(getWidth() - sideMargin - cornerSize, getHeight() - sideMargin);
        brPath.lineTo(getWidth() - sideMargin, getHeight() - sideMargin);
        brPath.lineTo(getWidth() - sideMargin, getHeight() - sideMargin - cornerSize);
        canvas.drawPath(brPath, cornerPaint);
    }
    
    @Override
    public void onTimeUpdate(int h, int m, int s, int cs) {
        invalidate();
    }
    
    @Override
    public void onModeChanged(ClockLogic.Mode mode) {
        invalidate();
    }
    
    @Override
    public void onWarningStateChanged(ClockLogic.WarningState state) {
        currentWarningState = state;
        invalidate();
    }
    
    @Override
    public void onDepletedStateChanged(boolean isDepleted) {
        invalidate();
    }
    
    public void startUpdates() {
        if (getHandler() == null) return;
        getHandler().post(updateRunnable);
    }
    
    public void stopUpdates() {
        if (getHandler() != null) {
            getHandler().removeCallbacks(updateRunnable);
        }
    }
    
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            clockLogic.update();
            postDelayed(this, 40); // Update every 40ms
        }
    };
    
    public void setModeChangeListener(OnModeChangeListener listener) {
        this.modeChangeListener = listener;
    }
    
    private class ButtonTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN) {
                return false;
            }
            
            float x = event.getX();
            float y = event.getY();
            
            for (int i = 0; i < 4; i++) {
                RectF rect = buttonRects[i];
                if (rect != null && rect.contains(x, y)) {
                    handleButtonClick(i);
                    return true;
                }
            }
            
            return false;
        }
    }
    
    private void handleButtonClick(int index) {
        switch (index) {
            case 0: // STOP
                if (clockLogic.getCurrentMode() != ClockLogic.Mode.NORMAL) {
                    clockLogic.togglePause();
                    if (modeChangeListener != null) {
                        modeChangeListener.onPauseToggled();
                    }
                }
                break;
            case 1: // SLOW
                clockLogic.setMode(ClockLogic.Mode.SLOW);
                if (modeChangeListener != null) {
                    modeChangeListener.onModeChanged(ClockLogic.Mode.SLOW);
                }
                break;
            case 2: // NORMAL
                clockLogic.setMode(ClockLogic.Mode.NORMAL);
                if (modeChangeListener != null) {
                    modeChangeListener.onModeChanged(ClockLogic.Mode.NORMAL);
                }
                break;
            case 3: // RACING
                clockLogic.setMode(ClockLogic.Mode.RACING);
                if (modeChangeListener != null) {
                    modeChangeListener.onModeChanged(ClockLogic.Mode.RACING);
                }
                break;
        }
    }
    
    public ClockLogic getClockLogic() {
        return clockLogic;
    }
    
    private android.os.Handler getHandler() {
        return new android.os.Handler(android.os.Looper.getMainLooper());
    }
}
