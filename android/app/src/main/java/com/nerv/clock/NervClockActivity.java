package com.nerv.clock;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import com.nerv.clock.ui.ClockView;

/**
 * Main NERV Clock Activity
 * Displays the digital clock interface in full-screen mode
 */
public class NervClockActivity extends Activity {
    
    private ClockView clockView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Keep screen on while this activity is visible
        // This prevents the timer from stopping due to screen off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Hide status bar and navigation bar for immersive experience
        hideSystemUI();
        
        // Create and set ClockView as the main content
        clockView = new ClockView(this);
        setContentView(clockView);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (clockView != null) {
            // Start updates - this will also reset lastUpdateTime to prevent jumps
            clockView.startUpdates();
            hideSystemUI();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (clockView != null) {
            clockView.stopUpdates();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear keep screen on flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
}
