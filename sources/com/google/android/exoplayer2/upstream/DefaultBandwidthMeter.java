package com.google.android.exoplayer2.upstream;

import android.os.Handler;
import android.support.v4.media.session.PlaybackStateCompat;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.SlidingPercentile;

public final class DefaultBandwidthMeter implements BandwidthMeter, TransferListener<Object> {
    private static final int BYTES_TRANSFERRED_FOR_ESTIMATE = 524288;
    public static final int DEFAULT_MAX_WEIGHT = 2000;
    private static final int ELAPSED_MILLIS_FOR_ESTIMATE = 2000;
    private long bitrateEstimate;
    private final Clock clock;
    private final Handler eventHandler;
    /* access modifiers changed from: private */
    public final BandwidthMeter.EventListener eventListener;
    private long sampleBytesTransferred;
    private long sampleStartTimeMs;
    private final SlidingPercentile slidingPercentile;
    private int streamCount;
    private long totalBytesTransferred;
    private long totalElapsedTimeMs;

    public DefaultBandwidthMeter() {
        this((Handler) null, (BandwidthMeter.EventListener) null);
    }

    public DefaultBandwidthMeter(Handler eventHandler2, BandwidthMeter.EventListener eventListener2) {
        this(eventHandler2, eventListener2, 2000);
    }

    public DefaultBandwidthMeter(Handler eventHandler2, BandwidthMeter.EventListener eventListener2, int maxWeight) {
        this(eventHandler2, eventListener2, maxWeight, Clock.DEFAULT);
    }

    public DefaultBandwidthMeter(Handler eventHandler2, BandwidthMeter.EventListener eventListener2, int maxWeight, Clock clock2) {
        this.eventHandler = eventHandler2;
        this.eventListener = eventListener2;
        this.slidingPercentile = new SlidingPercentile(maxWeight);
        this.clock = clock2;
        this.bitrateEstimate = -1;
    }

    public synchronized long getBitrateEstimate() {
        return this.bitrateEstimate;
    }

    public synchronized void onTransferStart(Object source, DataSpec dataSpec) {
        if (this.streamCount == 0) {
            this.sampleStartTimeMs = this.clock.elapsedRealtime();
        }
        this.streamCount++;
    }

    public synchronized void onBytesTransferred(Object source, int bytes) {
        this.sampleBytesTransferred += (long) bytes;
    }

    public synchronized void onTransferEnd(Object source) {
        Assertions.checkState(this.streamCount > 0);
        long nowMs = this.clock.elapsedRealtime();
        int sampleElapsedTimeMs = (int) (nowMs - this.sampleStartTimeMs);
        this.totalElapsedTimeMs += (long) sampleElapsedTimeMs;
        this.totalBytesTransferred += this.sampleBytesTransferred;
        if (sampleElapsedTimeMs > 0) {
            this.slidingPercentile.addSample((int) Math.sqrt((double) this.sampleBytesTransferred), (float) ((this.sampleBytesTransferred * 8000) / ((long) sampleElapsedTimeMs)));
            if (this.totalElapsedTimeMs >= 2000 || this.totalBytesTransferred >= PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED) {
                float bitrateEstimateFloat = this.slidingPercentile.getPercentile(0.5f);
                this.bitrateEstimate = Float.isNaN(bitrateEstimateFloat) ? -1 : (long) bitrateEstimateFloat;
            }
        }
        notifyBandwidthSample(sampleElapsedTimeMs, this.sampleBytesTransferred, this.bitrateEstimate);
        int i = this.streamCount - 1;
        this.streamCount = i;
        if (i > 0) {
            this.sampleStartTimeMs = nowMs;
        }
        this.sampleBytesTransferred = 0;
    }

    private void notifyBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        if (this.eventHandler != null && this.eventListener != null) {
            final int i = elapsedMs;
            final long j = bytes;
            final long j2 = bitrate;
            this.eventHandler.post(new Runnable() {
                public void run() {
                    DefaultBandwidthMeter.this.eventListener.onBandwidthSample(i, j, j2);
                }
            });
        }
    }
}
