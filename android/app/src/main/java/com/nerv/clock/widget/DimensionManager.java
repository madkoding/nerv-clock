package com.nerv.clock.widget;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Manages widget render dimensions based on orientation and widget size.
 */
public class DimensionManager {
    
    private static final String TAG = "DimensionManager";
    
    private int renderWidth = WidgetConfig.BASE_WIDTH_DP;
    private int renderHeight = WidgetConfig.BASE_HEIGHT_DP;
    
    public int getRenderWidth() {
        return renderWidth;
    }
    
    public int getRenderHeight() {
        return renderHeight;
    }
    
    /**
     * Update render dimensions based on widget options.
     * Android reports min/max as ranges for different orientations:
     * - Portrait: uses minWidth × maxHeight
     * - Landscape: uses maxWidth × minHeight
     */
    public void update(Context context, int minWidthDp, int minHeightDp, 
            int maxWidthDp, int maxHeightDp) {
        
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        
        // Detect orientation based on screen dimensions
        boolean isLandscape = dm.widthPixels > dm.heightPixels;
        
        int widgetWidthDp, widgetHeightDp;
        
        if (isLandscape) {
            widgetWidthDp = maxWidthDp > 0 ? maxWidthDp : minWidthDp;
            widgetHeightDp = minHeightDp > 0 ? minHeightDp : maxHeightDp;
        } else {
            widgetWidthDp = minWidthDp > 0 ? minWidthDp : maxWidthDp;
            widgetHeightDp = maxHeightDp > 0 ? maxHeightDp : minHeightDp;
        }
        
        if (widgetWidthDp > 0 && widgetHeightDp > 0) {
            renderWidth = (int)(widgetWidthDp * dm.density);
            renderHeight = (int)(widgetHeightDp * dm.density);
            
            // Ensure minimum dimensions
            renderWidth = Math.max(renderWidth, (int)(WidgetConfig.MIN_WIDTH_DP * dm.density));
            renderHeight = Math.max(renderHeight, (int)(WidgetConfig.MIN_HEIGHT_DP * dm.density));
            
            Log.d(TAG, "Dimensions: " + renderWidth + "x" + renderHeight + 
                       " (widget dp: " + widgetWidthDp + "x" + widgetHeightDp + 
                       ", " + (isLandscape ? "landscape" : "portrait") + ")");
        } else {
            setDefaults(dm);
        }
    }
    
    /**
     * Set default dimensions based on screen density.
     */
    public void setDefaults(DisplayMetrics dm) {
        renderWidth = Math.max((int)(WidgetConfig.BASE_WIDTH_DP * dm.density), WidgetConfig.BASE_WIDTH_DP);
        renderHeight = Math.max((int)(WidgetConfig.BASE_HEIGHT_DP * dm.density), WidgetConfig.BASE_HEIGHT_DP);
        Log.d(TAG, "Using defaults: " + renderWidth + "x" + renderHeight);
    }
}
