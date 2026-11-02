package com.reed.fcmguard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

/**
 * Schedules the "Wake FCM now" action every 10 minutes using AlarmManager, so it
 * keeps firing in Doze / deep sleep. Each alarm re-arms the next one.
 */
public final class WakeScheduler {
    public static final long WAKE_INTERVAL_MS = 10L * 60L * 1000L;
    private static final int REQUEST_CODE = 1010;

    private static final String KEY_ALARM_LAST = "auto_wake_alarm_last";
    private static final String KEY_ALARM_COUNT = "auto_wake_alarm_count";
    private static final String KEY_SERVICE_LAST = "auto_wake_service_last";

    private WakeScheduler() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(SettingsGuard.PREFS, Context.MODE_PRIVATE);
    }

    /** Called every time the 10-minute alarm actually fires. */
    public static void recordAlarm(Context context) {
        SharedPreferences p = prefs(context);
        p.edit()
                .putLong(KEY_ALARM_LAST, System.currentTimeMillis())
                .putInt(KEY_ALARM_COUNT, p.getInt(KEY_ALARM_COUNT, 0) + 1)
                .apply();
    }

    /** Called when GuardService itself sent the wake heartbeat. */
    public static void recordService(Context context) {
        prefs(context).edit().putLong(KEY_SERVICE_LAST, System.currentTimeMillis()).apply();
    }

    public static long lastAlarm(Context context) { return prefs(context).getLong(KEY_ALARM_LAST, 0L); }
    public static long lastService(Context context) { return prefs(context).getLong(KEY_SERVICE_LAST, 0L); }
    public static int alarmCount(Context context) { return prefs(context).getInt(KEY_ALARM_COUNT, 0); }

    private static PendingIntent pending(Context context) {
        Intent intent = new Intent(context, WakeAlarmReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
    }

    /** Arms (or re-arms) the next wake alarm, 10 minutes from now. */
    public static void schedule(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            long triggerAt = SystemClock.elapsedRealtime() + WAKE_INTERVAL_MS;
            PendingIntent pi = pending(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            }
        } catch (Throwable ignored) {}
    }

    public static void cancel(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pending(context));
        } catch (Throwable ignored) {}
    }
}
