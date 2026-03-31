package com.google.android.exoplayer2.util;

import android.os.Handler;

final class SystemClock implements Clock {
    SystemClock() {
    }

    public long elapsedRealtime() {
        return android.os.SystemClock.elapsedRealtime();
    }

    public void sleep(long sleepTimeMs) {
        android.os.SystemClock.sleep(sleepTimeMs);
    }

    public void postDelayed(Handler handler, Runnable runnable, long delayMs) {
        handler.postDelayed(runnable, delayMs);
    }
}
