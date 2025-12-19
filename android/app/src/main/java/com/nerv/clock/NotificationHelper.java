package com.nerv.clock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

/**
 * Helper class for managing NERV Clock notifications
 */
public class NotificationHelper {
    
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "nerv_clock_timer";
    private static final String CHANNEL_NAME = "Timer Alerts";
    private static final int NOTIFICATION_ID = 1001;
    
    private Context context;
    private NotificationManager notificationManager;
    
    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Get default alarm sound
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("NERV Clock timer completion alerts");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
            
            // Set sound
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
            channel.setSound(alarmSound, audioAttributes);
            
            channel.enableLights(true);
            channel.setLightColor(0xFFFF6A00); // NERV Orange
            
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }
    
    /**
     * Show timer completed notification with sound and vibration
     */
    public void showTimerCompleteNotification(int durationMinutes) {
        Log.d(TAG, "Showing timer complete notification for " + durationMinutes + " min timer");
        
        // Vibrate
        vibrate();
        
        // Build notification
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
        // Intent to open app when notification is tapped
        Intent intent = new Intent(context, NervClockWidget.class);
        intent.setAction("com.nerv.clock.ACTION_SLOW");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        
        // Build notification using native API
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
            builder.setSound(alarmSound);
        }
        
        builder.setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("NERV CHRONOMETER")
            .setContentText("Timer " + durationMinutes + "m completed - TIME DEPLETED")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(new long[]{0, 500, 200, 500, 200, 500});
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setPriority(Notification.PRIORITY_HIGH);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_ALARM);
        }
        
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "Notification posted");
    }
    
    /**
     * Vibrate the device
     */
    private void vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    Vibrator vibrator = vibratorManager.getDefaultVibrator();
                    vibrator.vibrate(VibrationEffect.createWaveform(
                        new long[]{0, 500, 200, 500, 200, 500}, -1));
                }
            } else {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(
                            new long[]{0, 500, 200, 500, 200, 500}, -1));
                    } else {
                        vibrator.vibrate(new long[]{0, 500, 200, 500, 200, 500}, -1);
                    }
                }
            }
            Log.d(TAG, "Vibration triggered");
        } catch (Exception e) {
            Log.e(TAG, "Vibration error: " + e.getMessage());
        }
    }
    
    /**
     * Cancel any active notifications
     */
    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
