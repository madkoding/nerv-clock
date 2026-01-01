package com.nerv.clock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import com.nerv.clock.ui.ClockView;

/**
 * Main NERV Clock Activity
 * Displays the digital clock interface in full-screen mode
 */
public class NervClockActivity extends Activity {
    
    private static final String PREFS_NAME = "NervClockPrefs";
    private static final String PREF_BATTERY_DIALOG_SHOWN = "battery_dialog_shown";
    
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
        
        // Check battery optimization on aggressive OEMs (Xiaomi, Huawei, etc.)
        checkBatteryOptimization();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (clockView != null) {
            // Start updates - this will also reset lastUpdateTime to prevent jumps
            clockView.startUpdates();
            hideSystemUI();
        }
        
        // Ensure widget service is running
        WidgetUpdateService.start(this);
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
    
    /**
     * Check if device needs battery optimization exclusion and show dialog if needed
     */
    private void checkBatteryOptimization() {
        // Only check on Android M+ and aggressive OEMs
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        
        // Check if we already showed this dialog
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean dialogShown = prefs.getBoolean(PREF_BATTERY_DIALOG_SHOWN, false);
        
        // Show dialog if:
        // 1. It's an aggressive OEM (Xiaomi, Huawei, etc.)
        // 2. We haven't shown the dialog before
        // 3. Battery optimization is not already disabled
        if (BatteryOptimizationHelper.isAggressiveOEM() && 
            !dialogShown && 
            !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            
            showBatteryOptimizationDialog();
        }
    }
    
    private void showBatteryOptimizationDialog() {
        String manufacturer = BatteryOptimizationHelper.getManufacturerName();
        
        new AlertDialog.Builder(this)
            .setTitle("Widget Configuration")
            .setMessage("Para que el widget funcione correctamente en " + manufacturer + 
                       ", necesitas excluir NERV Clock de la optimización de batería.\n\n" +
                       "Esto permite que el reloj se actualice continuamente cuando el launcher está visible.\n\n" +
                       "¿Deseas abrir la configuración ahora?")
            .setPositiveButton("Configurar", (dialog, which) -> {
                // Mark as shown
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_BATTERY_DIALOG_SHOWN, true)
                    .apply();
                
                // Open manufacturer-specific settings
                BatteryOptimizationHelper.openManufacturerBatterySettings(this);
            })
            .setNegativeButton("Más tarde", (dialog, which) -> {
                dialog.dismiss();
            })
            .setNeutralButton("No mostrar", (dialog, which) -> {
                // Mark as shown so it doesn't appear again
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_BATTERY_DIALOG_SHOWN, true)
                    .apply();
            })
            .setCancelable(true)
            .show();
    }
}
