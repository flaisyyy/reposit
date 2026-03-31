package com.google.android.exoplayer2.upstream;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

public final class Loader implements LoaderErrorThrower {
    public static final int DONT_RETRY = 2;
    public static final int DONT_RETRY_FATAL = 3;
    public static final int RETRY = 0;
    public static final int RETRY_RESET_ERROR_COUNT = 1;
    /* access modifiers changed from: private */
    public LoadTask<? extends Loadable> currentTask;
    /* access modifiers changed from: private */
    public final ExecutorService downloadExecutorService;
    /* access modifiers changed from: private */
    public IOException fatalError;

    public interface Callback<T extends Loadable> {
        void onLoadCanceled(T t, long j, long j2, boolean z);

        void onLoadCompleted(T t, long j, long j2);

        int onLoadError(T t, long j, long j2, IOException iOException);
    }

    public interface Loadable {
        void cancelLoad();

        boolean isLoadCanceled();

        void load() throws IOException, InterruptedException;
    }

    public interface ReleaseCallback {
        void onLoaderReleased();
    }

    public static final class UnexpectedLoaderException extends IOException {
        public UnexpectedLoaderException(Throwable cause) {
            super("Unexpected " + cause.getClass().getSimpleName() + ": " + cause.getMessage(), cause);
        }
    }

    public Loader(String threadName) {
        this.downloadExecutorService = Util.newSingleThreadExecutor(threadName);
    }

    public <T extends Loadable> long startLoading(T loadable, Callback<T> callback, int defaultMinRetryCount) {
        Looper looper = Looper.myLooper();
        Assertions.checkState(looper != null);
        long startTimeMs = SystemClock.elapsedRealtime();
        new LoadTask(looper, loadable, callback, defaultMinRetryCount, startTimeMs).start(0);
        return startTimeMs;
    }

    public boolean isLoading() {
        return this.currentTask != null;
    }

    public void cancelLoading() {
        this.currentTask.cancel(false);
    }

    public void release() {
        release((ReleaseCallback) null);
    }

    public boolean release(ReleaseCallback callback) {
        boolean callbackInvoked = false;
        if (this.currentTask != null) {
            this.currentTask.cancel(true);
            if (callback != null) {
                this.downloadExecutorService.execute(new ReleaseTask(callback));
            }
        } else if (callback != null) {
            callback.onLoaderReleased();
            callbackInvoked = true;
        }
        this.downloadExecutorService.shutdown();
        return callbackInvoked;
    }

    public void maybeThrowError() throws IOException {
        maybeThrowError(Integer.MIN_VALUE);
    }

    public void maybeThrowError(int minRetryCount) throws IOException {
        if (this.fatalError != null) {
            throw this.fatalError;
        } else if (this.currentTask != null) {
            LoadTask<? extends Loadable> loadTask = this.currentTask;
            if (minRetryCount == Integer.MIN_VALUE) {
                minRetryCount = this.currentTask.defaultMinRetryCount;
            }
            loadTask.maybeThrowError(minRetryCount);
        }
    }

    @SuppressLint({"HandlerLeak"})
    private final class LoadTask<T extends Loadable> extends Handler implements Runnable {
        private static final int MSG_CANCEL = 1;
        private static final int MSG_END_OF_SOURCE = 2;
        private static final int MSG_FATAL_ERROR = 4;
        private static final int MSG_IO_EXCEPTION = 3;
        private static final int MSG_START = 0;
        private static final String TAG = "LoadTask";
        private final Callback<T> callback;
        private IOException currentError;
        public final int defaultMinRetryCount;
        private int errorCount;
        private volatile Thread executorThread;
        private final T loadable;
        private volatile boolean released;
        private final long startTimeMs;

        public LoadTask(Looper looper, T loadable2, Callback<T> callback2, int defaultMinRetryCount2, long startTimeMs2) {
            super(looper);
            this.loadable = loadable2;
            this.callback = callback2;
            this.defaultMinRetryCount = defaultMinRetryCount2;
            this.startTimeMs = startTimeMs2;
        }

        public void maybeThrowError(int minRetryCount) throws IOException {
            if (this.currentError != null && this.errorCount > minRetryCount) {
                throw this.currentError;
            }
        }

        public void start(long delayMillis) {
            Assertions.checkState(Loader.this.currentTask == null);
            LoadTask unused = Loader.this.currentTask = this;
            if (delayMillis > 0) {
                sendEmptyMessageDelayed(0, delayMillis);
            } else {
                execute();
            }
        }

        public void cancel(boolean released2) {
            this.released = released2;
            this.currentError = null;
            if (hasMessages(0)) {
                removeMessages(0);
                if (!released2) {
                    sendEmptyMessage(1);
                }
            } else {
                this.loadable.cancelLoad();
                if (this.executorThread != null) {
                    this.executorThread.interrupt();
                }
            }
            if (released2) {
                finish();
                long nowMs = SystemClock.elapsedRealtime();
                this.callback.onLoadCanceled(this.loadable, nowMs, nowMs - this.startTimeMs, true);
            }
        }

        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
                r5 = this;
                r4 = 2
                r3 = 3
                java.lang.Thread r1 = java.lang.Thread.currentThread()     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                r5.executorThread = r1     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                T r1 = r5.loadable     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                boolean r1 = r1.isLoadCanceled()     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                if (r1 != 0) goto L_0x0038
                java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                r1.<init>()     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                java.lang.String r2 = "load:"
                java.lang.StringBuilder r1 = r1.append(r2)     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                T r2 = r5.loadable     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                java.lang.Class r2 = r2.getClass()     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                java.lang.String r2 = r2.getSimpleName()     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                java.lang.StringBuilder r1 = r1.append(r2)     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                java.lang.String r1 = r1.toString()     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                com.google.android.exoplayer2.util.TraceUtil.beginSection(r1)     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                T r1 = r5.loadable     // Catch:{ all -> 0x0041 }
                r1.load()     // Catch:{ all -> 0x0041 }
                com.google.android.exoplayer2.util.TraceUtil.endSection()     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
            L_0x0038:
                boolean r1 = r5.released     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                if (r1 != 0) goto L_0x0040
                r1 = 2
                r5.sendEmptyMessage(r1)     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
            L_0x0040:
                return
            L_0x0041:
                r1 = move-exception
                com.google.android.exoplayer2.util.TraceUtil.endSection()     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
                throw r1     // Catch:{ IOException -> 0x0046, InterruptedException -> 0x0053, Exception -> 0x0065, OutOfMemoryError -> 0x007e, Error -> 0x0097 }
            L_0x0046:
                r0 = move-exception
                boolean r1 = r5.released
                if (r1 != 0) goto L_0x0040
                android.os.Message r1 = r5.obtainMessage(r3, r0)
                r1.sendToTarget()
                goto L_0x0040
            L_0x0053:
                r0 = move-exception
                T r1 = r5.loadable
                boolean r1 = r1.isLoadCanceled()
                com.google.android.exoplayer2.util.Assertions.checkState(r1)
                boolean r1 = r5.released
                if (r1 != 0) goto L_0x0040
                r5.sendEmptyMessage(r4)
                goto L_0x0040
            L_0x0065:
                r0 = move-exception
                java.lang.String r1 = "LoadTask"
                java.lang.String r2 = "Unexpected exception loading stream"
                android.util.Log.e(r1, r2, r0)
                boolean r1 = r5.released
                if (r1 != 0) goto L_0x0040
                com.google.android.exoplayer2.upstream.Loader$UnexpectedLoaderException r1 = new com.google.android.exoplayer2.upstream.Loader$UnexpectedLoaderException
                r1.<init>(r0)
                android.os.Message r1 = r5.obtainMessage(r3, r1)
                r1.sendToTarget()
                goto L_0x0040
            L_0x007e:
                r0 = move-exception
                java.lang.String r1 = "LoadTask"
                java.lang.String r2 = "OutOfMemory error loading stream"
                android.util.Log.e(r1, r2, r0)
                boolean r1 = r5.released
                if (r1 != 0) goto L_0x0040
                com.google.android.exoplayer2.upstream.Loader$UnexpectedLoaderException r1 = new com.google.android.exoplayer2.upstream.Loader$UnexpectedLoaderException
                r1.<init>(r0)
                android.os.Message r1 = r5.obtainMessage(r3, r1)
                r1.sendToTarget()
                goto L_0x0040
            L_0x0097:
                r0 = move-exception
                java.lang.String r1 = "LoadTask"
                java.lang.String r2 = "Unexpected error loading stream"
                android.util.Log.e(r1, r2, r0)
                boolean r1 = r5.released
                if (r1 != 0) goto L_0x00ab
                r1 = 4
                android.os.Message r1 = r5.obtainMessage(r1, r0)
                r1.sendToTarget()
            L_0x00ab:
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.upstream.Loader.LoadTask.run():void");
        }

        public void handleMessage(Message msg) {
            int i;
            if (!this.released) {
                if (msg.what == 0) {
                    execute();
                } else if (msg.what == 4) {
                    throw ((Error) msg.obj);
                } else {
                    finish();
                    long nowMs = SystemClock.elapsedRealtime();
                    long durationMs = nowMs - this.startTimeMs;
                    if (this.loadable.isLoadCanceled()) {
                        this.callback.onLoadCanceled(this.loadable, nowMs, durationMs, false);
                        return;
                    }
                    switch (msg.what) {
                        case 1:
                            this.callback.onLoadCanceled(this.loadable, nowMs, durationMs, false);
                            return;
                        case 2:
                            try {
                                this.callback.onLoadCompleted(this.loadable, nowMs, durationMs);
                                return;
                            } catch (RuntimeException e) {
                                Log.e(TAG, "Unexpected exception handling load completed", e);
                                IOException unused = Loader.this.fatalError = new UnexpectedLoaderException(e);
                                return;
                            }
                        case 3:
                            this.currentError = (IOException) msg.obj;
                            int retryAction = this.callback.onLoadError(this.loadable, nowMs, durationMs, this.currentError);
                            if (retryAction == 3) {
                                IOException unused2 = Loader.this.fatalError = this.currentError;
                                return;
                            } else if (retryAction != 2) {
                                if (retryAction == 1) {
                                    i = 1;
                                } else {
                                    i = this.errorCount + 1;
                                }
                                this.errorCount = i;
                                start(getRetryDelayMillis());
                                return;
                            } else {
                                return;
                            }
                        default:
                            return;
                    }
                }
            }
        }

        private void execute() {
            this.currentError = null;
            Loader.this.downloadExecutorService.execute(Loader.this.currentTask);
        }

        private void finish() {
            LoadTask unused = Loader.this.currentTask = null;
        }

        private long getRetryDelayMillis() {
            return (long) Math.min((this.errorCount - 1) * 1000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        }
    }

    private static final class ReleaseTask extends Handler implements Runnable {
        private final ReleaseCallback callback;

        public ReleaseTask(ReleaseCallback callback2) {
            this.callback = callback2;
        }

        public void run() {
            if (getLooper().getThread().isAlive()) {
                sendEmptyMessage(0);
            }
        }

        public void handleMessage(Message msg) {
            this.callback.onLoaderReleased();
        }
    }
}
