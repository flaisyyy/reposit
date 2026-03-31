package com.google.android.exoplayer2.decoder;

import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.OutputBuffer;
import com.google.android.exoplayer2.util.Assertions;
import java.lang.Exception;
import java.util.LinkedList;

public abstract class SimpleDecoder<I extends DecoderInputBuffer, O extends OutputBuffer, E extends Exception> implements Decoder<I, O, E> {
    private int availableInputBufferCount;
    private final I[] availableInputBuffers;
    private int availableOutputBufferCount;
    private final O[] availableOutputBuffers;
    private final Thread decodeThread;
    private I dequeuedInputBuffer;
    private E exception;
    private boolean flushed;
    private final Object lock = new Object();
    private final LinkedList<I> queuedInputBuffers = new LinkedList<>();
    private final LinkedList<O> queuedOutputBuffers = new LinkedList<>();
    private boolean released;
    private int skippedOutputBufferCount;

    /* access modifiers changed from: protected */
    public abstract I createInputBuffer();

    /* access modifiers changed from: protected */
    public abstract O createOutputBuffer();

    /* access modifiers changed from: protected */
    public abstract E decode(I i, O o, boolean z);

    protected SimpleDecoder(I[] inputBuffers, O[] outputBuffers) {
        this.availableInputBuffers = inputBuffers;
        this.availableInputBufferCount = inputBuffers.length;
        for (int i = 0; i < this.availableInputBufferCount; i++) {
            this.availableInputBuffers[i] = createInputBuffer();
        }
        this.availableOutputBuffers = outputBuffers;
        this.availableOutputBufferCount = outputBuffers.length;
        for (int i2 = 0; i2 < this.availableOutputBufferCount; i2++) {
            this.availableOutputBuffers[i2] = createOutputBuffer();
        }
        this.decodeThread = new Thread() {
            public void run() {
                SimpleDecoder.this.run();
            }
        };
        this.decodeThread.start();
    }

    /* access modifiers changed from: protected */
    public final void setInitialInputBufferSize(int size) {
        boolean z;
        if (this.availableInputBufferCount == this.availableInputBuffers.length) {
            z = true;
        } else {
            z = false;
        }
        Assertions.checkState(z);
        for (I inputBuffer : this.availableInputBuffers) {
            inputBuffer.ensureSpaceForWrite(size);
        }
    }

    public final I dequeueInputBuffer() throws Exception {
        I i;
        I i2;
        synchronized (this.lock) {
            maybeThrowException();
            Assertions.checkState(this.dequeuedInputBuffer == null);
            if (this.availableInputBufferCount == 0) {
                i = null;
            } else {
                I[] iArr = this.availableInputBuffers;
                int i3 = this.availableInputBufferCount - 1;
                this.availableInputBufferCount = i3;
                i = iArr[i3];
            }
            this.dequeuedInputBuffer = i;
            i2 = this.dequeuedInputBuffer;
        }
        return i2;
    }

    public final void queueInputBuffer(I inputBuffer) throws Exception {
        synchronized (this.lock) {
            maybeThrowException();
            Assertions.checkArgument(inputBuffer == this.dequeuedInputBuffer);
            this.queuedInputBuffers.addLast(inputBuffer);
            maybeNotifyDecodeLoop();
            this.dequeuedInputBuffer = null;
        }
    }

    public final O dequeueOutputBuffer() throws Exception {
        O o;
        synchronized (this.lock) {
            maybeThrowException();
            if (this.queuedOutputBuffers.isEmpty()) {
                o = null;
            } else {
                o = (OutputBuffer) this.queuedOutputBuffers.removeFirst();
            }
        }
        return o;
    }

    /* access modifiers changed from: protected */
    public void releaseOutputBuffer(O outputBuffer) {
        synchronized (this.lock) {
            releaseOutputBufferInternal(outputBuffer);
            maybeNotifyDecodeLoop();
        }
    }

    public final void flush() {
        synchronized (this.lock) {
            this.flushed = true;
            this.skippedOutputBufferCount = 0;
            if (this.dequeuedInputBuffer != null) {
                releaseInputBufferInternal(this.dequeuedInputBuffer);
                this.dequeuedInputBuffer = null;
            }
            while (!this.queuedInputBuffers.isEmpty()) {
                releaseInputBufferInternal((DecoderInputBuffer) this.queuedInputBuffers.removeFirst());
            }
            while (!this.queuedOutputBuffers.isEmpty()) {
                releaseOutputBufferInternal((OutputBuffer) this.queuedOutputBuffers.removeFirst());
            }
        }
    }

    public void release() {
        synchronized (this.lock) {
            this.released = true;
            this.lock.notify();
        }
        try {
            this.decodeThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void maybeThrowException() throws Exception {
        if (this.exception != null) {
            throw this.exception;
        }
    }

    private void maybeNotifyDecodeLoop() {
        if (canDecodeBuffer()) {
            this.lock.notify();
        }
    }

    /* access modifiers changed from: private */
    public void run() {
        do {
            try {
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        } while (decode());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0039, code lost:
        if (r0.isEndOfStream() == false) goto L_0x004f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x003b, code lost:
        r1.addFlag(4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x003f, code lost:
        r4 = r7.lock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0041, code lost:
        monitor-enter(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0044, code lost:
        if (r7.flushed == false) goto L_0x006c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0046, code lost:
        releaseOutputBufferInternal(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0049, code lost:
        releaseInputBufferInternal(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x004c, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x004d, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0053, code lost:
        if (r0.isDecodeOnly() == false) goto L_0x005a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0055, code lost:
        r1.addFlag(Integer.MIN_VALUE);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x005a, code lost:
        r7.exception = decode(r0, r1, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0062, code lost:
        if (r7.exception == null) goto L_0x003f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0064, code lost:
        r4 = r7.lock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0066, code lost:
        monitor-enter(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:?, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x0070, code lost:
        if (r1.isDecodeOnly() == false) goto L_0x007f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x0072, code lost:
        r7.skippedOutputBufferCount++;
        releaseOutputBufferInternal(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:?, code lost:
        r1.skippedOutputBufferCount = r7.skippedOutputBufferCount;
        r7.skippedOutputBufferCount = 0;
        r7.queuedOutputBuffers.addLast(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:?, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean decode() throws java.lang.InterruptedException {
        /*
            r7 = this;
            r3 = 0
            java.lang.Object r4 = r7.lock
            monitor-enter(r4)
        L_0x0004:
            boolean r5 = r7.released     // Catch:{ all -> 0x0014 }
            if (r5 != 0) goto L_0x0017
            boolean r5 = r7.canDecodeBuffer()     // Catch:{ all -> 0x0014 }
            if (r5 != 0) goto L_0x0017
            java.lang.Object r5 = r7.lock     // Catch:{ all -> 0x0014 }
            r5.wait()     // Catch:{ all -> 0x0014 }
            goto L_0x0004
        L_0x0014:
            r3 = move-exception
            monitor-exit(r4)     // Catch:{ all -> 0x0014 }
            throw r3
        L_0x0017:
            boolean r5 = r7.released     // Catch:{ all -> 0x0014 }
            if (r5 == 0) goto L_0x001d
            monitor-exit(r4)     // Catch:{ all -> 0x0014 }
        L_0x001c:
            return r3
        L_0x001d:
            java.util.LinkedList<I> r5 = r7.queuedInputBuffers     // Catch:{ all -> 0x0014 }
            java.lang.Object r0 = r5.removeFirst()     // Catch:{ all -> 0x0014 }
            com.google.android.exoplayer2.decoder.DecoderInputBuffer r0 = (com.google.android.exoplayer2.decoder.DecoderInputBuffer) r0     // Catch:{ all -> 0x0014 }
            O[] r5 = r7.availableOutputBuffers     // Catch:{ all -> 0x0014 }
            int r6 = r7.availableOutputBufferCount     // Catch:{ all -> 0x0014 }
            int r6 = r6 + -1
            r7.availableOutputBufferCount = r6     // Catch:{ all -> 0x0014 }
            r1 = r5[r6]     // Catch:{ all -> 0x0014 }
            boolean r2 = r7.flushed     // Catch:{ all -> 0x0014 }
            r5 = 0
            r7.flushed = r5     // Catch:{ all -> 0x0014 }
            monitor-exit(r4)     // Catch:{ all -> 0x0014 }
            boolean r4 = r0.isEndOfStream()
            if (r4 == 0) goto L_0x004f
            r3 = 4
            r1.addFlag(r3)
        L_0x003f:
            java.lang.Object r4 = r7.lock
            monitor-enter(r4)
            boolean r3 = r7.flushed     // Catch:{ all -> 0x007c }
            if (r3 == 0) goto L_0x006c
            r7.releaseOutputBufferInternal(r1)     // Catch:{ all -> 0x007c }
        L_0x0049:
            r7.releaseInputBufferInternal(r0)     // Catch:{ all -> 0x007c }
            monitor-exit(r4)     // Catch:{ all -> 0x007c }
            r3 = 1
            goto L_0x001c
        L_0x004f:
            boolean r4 = r0.isDecodeOnly()
            if (r4 == 0) goto L_0x005a
            r4 = -2147483648(0xffffffff80000000, float:-0.0)
            r1.addFlag(r4)
        L_0x005a:
            java.lang.Exception r4 = r7.decode(r0, r1, r2)
            r7.exception = r4
            E r4 = r7.exception
            if (r4 == 0) goto L_0x003f
            java.lang.Object r4 = r7.lock
            monitor-enter(r4)
            monitor-exit(r4)     // Catch:{ all -> 0x0069 }
            goto L_0x001c
        L_0x0069:
            r3 = move-exception
            monitor-exit(r4)     // Catch:{ all -> 0x0069 }
            throw r3
        L_0x006c:
            boolean r3 = r1.isDecodeOnly()     // Catch:{ all -> 0x007c }
            if (r3 == 0) goto L_0x007f
            int r3 = r7.skippedOutputBufferCount     // Catch:{ all -> 0x007c }
            int r3 = r3 + 1
            r7.skippedOutputBufferCount = r3     // Catch:{ all -> 0x007c }
            r7.releaseOutputBufferInternal(r1)     // Catch:{ all -> 0x007c }
            goto L_0x0049
        L_0x007c:
            r3 = move-exception
            monitor-exit(r4)     // Catch:{ all -> 0x007c }
            throw r3
        L_0x007f:
            int r3 = r7.skippedOutputBufferCount     // Catch:{ all -> 0x007c }
            r1.skippedOutputBufferCount = r3     // Catch:{ all -> 0x007c }
            r3 = 0
            r7.skippedOutputBufferCount = r3     // Catch:{ all -> 0x007c }
            java.util.LinkedList<O> r3 = r7.queuedOutputBuffers     // Catch:{ all -> 0x007c }
            r3.addLast(r1)     // Catch:{ all -> 0x007c }
            goto L_0x0049
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.decoder.SimpleDecoder.decode():boolean");
    }

    private boolean canDecodeBuffer() {
        return !this.queuedInputBuffers.isEmpty() && this.availableOutputBufferCount > 0;
    }

    private void releaseInputBufferInternal(I inputBuffer) {
        inputBuffer.clear();
        I[] iArr = this.availableInputBuffers;
        int i = this.availableInputBufferCount;
        this.availableInputBufferCount = i + 1;
        iArr[i] = inputBuffer;
    }

    private void releaseOutputBufferInternal(O outputBuffer) {
        outputBuffer.clear();
        O[] oArr = this.availableOutputBuffers;
        int i = this.availableOutputBufferCount;
        this.availableOutputBufferCount = i + 1;
        oArr[i] = outputBuffer;
    }
}
