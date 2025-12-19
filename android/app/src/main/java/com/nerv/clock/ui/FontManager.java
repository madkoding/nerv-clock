package com.nerv.clock.ui;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Font manager for NERV Chronometer
 * Loads custom fonts from assets
 */
public class FontManager {
    
    private static Typeface dseg7Font;
    private static Typeface nimbusSansRegular;
    private static Typeface nimbusSansBold;
    
    private static boolean isInitialized = false;
    
    public static void initialize(Context context) {
        if (isInitialized) return;
        
        try {
            dseg7Font = Typeface.createFromAsset(context.getAssets(), "fonts/dseg7.ttf");
        } catch (Exception e) {
            dseg7Font = Typeface.MONOSPACE;
        }
        
        try {
            nimbusSansRegular = Typeface.createFromAsset(context.getAssets(), "fonts/NimbusSans-Regular.otf");
        } catch (Exception e) {
            nimbusSansRegular = Typeface.SANS_SERIF;
        }
        
        try {
            nimbusSansBold = Typeface.createFromAsset(context.getAssets(), "fonts/NimbusSans-Bold.otf");
        } catch (Exception e) {
            nimbusSansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        }
        
        isInitialized = true;
    }
    
    public static Typeface getDSEG7() {
        return dseg7Font != null ? dseg7Font : Typeface.MONOSPACE;
    }
    
    public static Typeface getNimbusSansRegular() {
        return nimbusSansRegular != null ? nimbusSansRegular : Typeface.SANS_SERIF;
    }
    
    public static Typeface getNimbusSansBold() {
        return nimbusSansBold != null ? nimbusSansBold : Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    }
}
