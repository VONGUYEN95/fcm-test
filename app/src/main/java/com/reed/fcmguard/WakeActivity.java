package com.reed.fcmguard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

/**
 * Short-lived screen started by the 10-minute alarm.
 *
 * - Screen on (you are using the phone): invisible, sends the heartbeat twice, closes.
 * - Screen off: first opens Google's own FCM diagnostics screen on top of this task so
 *   Google Play services is brought to the foreground (not frozen), then sends the
 *   heartbeat, and finally removes the whole task again.
 */
public class WakeActivity extends Activity {
    private static final String[] GMS_DIAGNOSTICS = {
            "com.google.android.gms.gcm.GcmDiagnostics",
            "com.google.android.gms.gtalkservice.diagnostics.GTalkServiceDiagnostics"
    };

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WakeScheduler.recordActivity(this);

        boolean interactive = true;
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) interactive = pm.isInteractive();
        } catch (Throwable ignored) {}

        if (interactive) {
            FcmReconnect.kick(this);
            postKick(1000L);
            postClose(2500L);
        } else {
            openGmsDiagnostics();
            postKick(1500L);
            postKick(3500L);
            postClose(6000L);
        }
    }

    private void openGmsDiagnostics() {
        for (String className : GMS_DIAGNOSTICS) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.google.android.gms", className);
                startActivity(intent);
                return;
            } catch (Throwable ignored) {}
        }
    }

    private void postKick(long delayMs) {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                FcmReconnect.kick(WakeActivity.this);
            }
        }, delayMs);
    }

    private void postClose(long delayMs) {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    finishAndRemoveTask();
                } catch (Throwable t) {
                    finish();
                }
            }
        }, delayMs);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
