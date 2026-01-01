package com.nerv.clock;

import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects if the home launcher is currently in the foreground.
 * This is used to determine when to run fast widget updates.
 * 
 * On Xiaomi and other devices with aggressive battery optimization,
 * we need to know when the launcher is visible to keep the widget updating.
 */
public class LauncherDetector {
    
    private static final String TAG = "LauncherDetector";
    private static final long CHECK_INTERVAL_MS = 500; // Check every 500ms
    
    private final Context context;
    private final LauncherStateListener listener;
    private final Handler handler;
    private final Set<String> launcherPackages;
    private UsageStatsManager usageStatsManager;
    private boolean isRunning = false;
    private boolean lastKnownState = true;
    
    public interface LauncherStateListener {
        void onLauncherStateChanged(boolean isInForeground);
    }
    
    public LauncherDetector(Context context, LauncherStateListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
        this.launcherPackages = new HashSet<>();
        
        // Get UsageStatsManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        }
        
        // Discover all launcher packages
        discoverLauncherPackages();
    }
    
    /**
     * Discovers all installed launcher/home apps on the device
     */
    private void discoverLauncherPackages() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> launchers = pm.queryIntentActivities(intent, 0);
        
        for (ResolveInfo info : launchers) {
            launcherPackages.add(info.activityInfo.packageName);
            Log.d(TAG, "Found launcher: " + info.activityInfo.packageName);
        }
        
        // Also add common launcher packages that might not be detected
        launcherPackages.add("com.miui.home"); // Xiaomi MIUI
        launcherPackages.add("com.huawei.android.launcher"); // Huawei
        launcherPackages.add("com.oppo.launcher"); // OPPO
        launcherPackages.add("com.vivo.launcher"); // Vivo
        launcherPackages.add("com.sec.android.app.launcher"); // Samsung
        launcherPackages.add("com.google.android.apps.nexuslauncher"); // Pixel
        launcherPackages.add("com.android.launcher3"); // AOSP
        launcherPackages.add("com.teslacoilsw.launcher"); // Nova
        launcherPackages.add("com.microsoft.launcher"); // Microsoft
        launcherPackages.add("com.actionlauncher.playstore"); // Action Launcher
        launcherPackages.add("com.lawnchair"); // Lawnchair
        launcherPackages.add("ch.deletescape.lawnchair.plah"); // Lawnchair alt
        launcherPackages.add("com.nothing.launcher"); // Nothing Phone
        launcherPackages.add("com.oneplus.launcher"); // OnePlus
        
        Log.d(TAG, "Total launcher packages: " + launcherPackages.size());
    }
    
    /**
     * Start monitoring launcher state
     */
    public void start() {
        if (isRunning) return;
        isRunning = true;
        Log.d(TAG, "Starting launcher detection");
        scheduleCheck();
    }
    
    /**
     * Stop monitoring launcher state
     */
    public void stop() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Stopped launcher detection");
    }
    
    private void scheduleCheck() {
        if (!isRunning) return;
        handler.postDelayed(this::checkLauncherState, CHECK_INTERVAL_MS);
    }
    
    private void checkLauncherState() {
        if (!isRunning) return;
        
        boolean isLauncherForeground = isLauncherInForeground();
        
        // Only notify if state changed
        if (isLauncherForeground != lastKnownState) {
            lastKnownState = isLauncherForeground;
            if (listener != null) {
                listener.onLauncherStateChanged(isLauncherForeground);
            }
        }
        
        scheduleCheck();
    }
    
    /**
     * Check if launcher is currently in the foreground using multiple methods
     */
    private boolean isLauncherInForeground() {
        // Try UsageStats first (most reliable on Android 5.1+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && hasUsageStatsPermission()) {
            String foregroundApp = getForegroundAppFromUsageStats();
            if (foregroundApp != null) {
                return launcherPackages.contains(foregroundApp);
            }
        }
        
        // Fallback: Use ActivityManager (less reliable on newer Android versions)
        return isLauncherInForegroundLegacy();
    }
    
    /**
     * Get foreground app using UsageStatsManager (Android 5.1+)
     */
    private String getForegroundAppFromUsageStats() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || usageStatsManager == null) {
            return null;
        }
        
        try {
            long endTime = System.currentTimeMillis();
            long beginTime = endTime - 1000; // Last second
            
            UsageEvents usageEvents = usageStatsManager.queryEvents(beginTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();
            String lastForegroundApp = null;
            
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForegroundApp = event.getPackageName();
                }
            }
            
            return lastForegroundApp;
        } catch (Exception e) {
            Log.e(TAG, "Error getting foreground app: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Legacy method using ActivityManager (for older devices or no permission)
     */
    @SuppressWarnings("deprecation")
    private boolean isLauncherInForegroundLegacy() {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return true;
            
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty()) {
                String topPackage = tasks.get(0).topActivity.getPackageName();
                return launcherPackages.contains(topPackage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking launcher state: " + e.getMessage());
        }
        
        // Default to true if we can't determine
        return true;
    }
    
    /**
     * Check if we have UsageStats permission
     */
    public boolean hasUsageStatsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        
        try {
            long endTime = System.currentTimeMillis();
            long beginTime = endTime - 1000;
            UsageEvents events = usageStatsManager.queryEvents(beginTime, endTime);
            return events != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Open system settings to grant UsageStats permission
     */
    public void requestUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    /**
     * Get current known launcher state
     */
    public boolean isLauncherVisible() {
        return lastKnownState;
    }
}
