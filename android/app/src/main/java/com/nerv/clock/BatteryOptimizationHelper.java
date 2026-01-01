package com.nerv.clock;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Helper class to request battery optimization exclusion on various OEM devices.
 * Xiaomi, Huawei, OPPO, Vivo and others have aggressive battery optimization
 * that kills background processes and widgets.
 */
public class BatteryOptimizationHelper {
    
    private static final String TAG = "BatteryOptHelper";
    
    /**
     * Check if the app is exempt from battery optimizations
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }
    
    /**
     * Request to ignore battery optimizations (shows system dialog)
     */
    public static void requestIgnoreBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error requesting battery optimization: " + e.getMessage());
                // Fallback to battery settings
                openBatterySettings(context);
            }
        }
    }
    
    /**
     * Open battery optimization settings
     */
    public static void openBatterySettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening battery settings: " + e.getMessage());
        }
    }
    
    /**
     * Open Xiaomi/MIUI specific autostart settings
     */
    public static boolean openXiaomiAutostart(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Xiaomi autostart: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Open Xiaomi/MIUI battery saver settings
     */
    public static boolean openXiaomiBatterySaver(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            ));
            intent.putExtra("package_name", context.getPackageName());
            intent.putExtra("package_label", "NERV Clock");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Xiaomi battery saver: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Open Huawei protected apps settings
     */
    public static boolean openHuaweiProtectedApps(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
            
            // Try alternative Huawei component
            intent.setComponent(new ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            ));
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Huawei protected apps: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Open OPPO autostart settings
     */
    public static boolean openOppoAutostart(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
            
            // Try alternative OPPO component
            intent.setComponent(new ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity"
            ));
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening OPPO autostart: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Open Vivo background app settings
     */
    public static boolean openVivoAutostart(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Vivo autostart: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Open Samsung battery optimization settings
     */
    public static boolean openSamsungBatterySettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.battery.ui.BatteryActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Samsung battery settings: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Open OnePlus autostart settings
     */
    public static boolean openOnePlusAutostart(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening OnePlus autostart: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Detect device manufacturer and open appropriate settings
     */
    public static void openManufacturerBatterySettings(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Log.d(TAG, "Manufacturer: " + manufacturer);
        
        boolean opened = false;
        
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            opened = openXiaomiAutostart(context) || openXiaomiBatterySaver(context);
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            opened = openHuaweiProtectedApps(context);
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            opened = openOppoAutostart(context);
        } else if (manufacturer.contains("vivo")) {
            opened = openVivoAutostart(context);
        } else if (manufacturer.contains("samsung")) {
            opened = openSamsungBatterySettings(context);
        } else if (manufacturer.contains("oneplus")) {
            opened = openOnePlusAutostart(context);
        }
        
        // If manufacturer-specific settings failed, open standard battery settings
        if (!opened) {
            openBatterySettings(context);
        }
    }
    
    /**
     * Check if device is from a manufacturer known to have aggressive battery optimization
     */
    public static boolean isAggressiveOEM() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("xiaomi") ||
               manufacturer.contains("redmi") ||
               manufacturer.contains("huawei") ||
               manufacturer.contains("honor") ||
               manufacturer.contains("oppo") ||
               manufacturer.contains("realme") ||
               manufacturer.contains("vivo") ||
               manufacturer.contains("oneplus") ||
               manufacturer.contains("meizu") ||
               manufacturer.contains("asus") ||
               manufacturer.contains("lenovo");
    }
    
    /**
     * Get manufacturer name for display
     */
    public static String getManufacturerName() {
        return Build.MANUFACTURER;
    }
    
    /**
     * Check if an intent can be resolved
     */
    private static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        return pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null;
    }
}
