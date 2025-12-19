package com.nerv.clock;

import android.app.Activity;
import android.os.Bundle;
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
    
    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE |
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }
}
