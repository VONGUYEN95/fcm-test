package com.reed.fcmguard;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Invisible, very short-lived screen started by the 10-minute alarm. While it is
 * on screen the app counts as "foreground", which is the same situation as pressing
 * "Wake FCM now" by hand. It sends the heartbeat, again after one second, then closes.
 */
public class WakeActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WakeScheduler.recordActivity(this);
        FcmReconnect.kick(this);
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                FcmReconnect.kick(WakeActivity.this);
            }
        }, 1000L);
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                finish();
            }
        }, 2500L);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
