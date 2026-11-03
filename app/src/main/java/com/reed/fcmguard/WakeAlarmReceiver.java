package com.reed.fcmguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

/**
 * Fires every 10 minutes. Re-arms first so the chain never breaks, then sends the
 * same heartbeat as the "Wake FCM now" button, both directly and via GuardService
 * (a foreground service is less likely to be restricted than a plain receiver).
 */
public class WakeAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!SettingsGuard.isProtectionEnabled(context)) return;

        WakeScheduler.schedule(context);
        WakeScheduler.recordAlarm(context);

        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                // Auto-released after 20s; keeps the CPU awake for the service hand-off.
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "fcmguard:wake").acquire(20000L);
            }
        } catch (Throwable ignored) {}

        // Foreground-like path: an invisible activity (same situation as pressing the button).
        try {
            Intent screen = new Intent(context, WakeActivity.class);
            screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivity(screen);
        } catch (Throwable ignored) {}

        FcmReconnect.kick(context);

        try {
            Intent svc = new Intent(context, GuardService.class).setAction(GuardService.ACTION_WAKE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    SettingsGuard.usePersistentNotification(context)) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        } catch (Throwable ignored) {}
    }
}
