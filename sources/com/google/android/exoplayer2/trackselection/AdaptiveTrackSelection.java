package com.google.android.exoplayer2.trackselection;

import android.os.SystemClock;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.chunk.MediaChunk;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import java.util.List;

public class AdaptiveTrackSelection extends BaseTrackSelection {
    public static final float DEFAULT_BANDWIDTH_FRACTION = 0.75f;
    public static final float DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE = 0.75f;
    public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
    public static final int DEFAULT_MAX_INITIAL_BITRATE = 800000;
    public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
    public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
    private final float bandwidthFraction;
    private final BandwidthMeter bandwidthMeter;
    private final float bufferedFractionToLiveEdgeForQualityIncrease;
    private final long maxDurationForQualityDecreaseUs;
    private final int maxInitialBitrate;
    private final long minDurationForQualityIncreaseUs;
    private final long minDurationToRetainAfterDiscardUs;
    private int reason;
    private int selectedIndex;

    public static final class Factory implements TrackSelection.Factory {
        private final float bandwidthFraction;
        private final BandwidthMeter bandwidthMeter;
        private final float bufferedFractionToLiveEdgeForQualityIncrease;
        private final int maxDurationForQualityDecreaseMs;
        private final int maxInitialBitrate;
        private final int minDurationForQualityIncreaseMs;
        private final int minDurationToRetainAfterDiscardMs;

        public Factory(BandwidthMeter bandwidthMeter2) {
            this(bandwidthMeter2, AdaptiveTrackSelection.DEFAULT_MAX_INITIAL_BITRATE, 10000, 25000, 25000, 0.75f, 0.75f);
        }

        public Factory(BandwidthMeter bandwidthMeter2, int maxInitialBitrate2, int minDurationForQualityIncreaseMs2, int maxDurationForQualityDecreaseMs2, int minDurationToRetainAfterDiscardMs2, float bandwidthFraction2) {
            this(bandwidthMeter2, maxInitialBitrate2, minDurationForQualityIncreaseMs2, maxDurationForQualityDecreaseMs2, minDurationToRetainAfterDiscardMs2, bandwidthFraction2, 0.75f);
        }

        public Factory(BandwidthMeter bandwidthMeter2, int maxInitialBitrate2, int minDurationForQualityIncreaseMs2, int maxDurationForQualityDecreaseMs2, int minDurationToRetainAfterDiscardMs2, float bandwidthFraction2, float bufferedFractionToLiveEdgeForQualityIncrease2) {
            this.bandwidthMeter = bandwidthMeter2;
            this.maxInitialBitrate = maxInitialBitrate2;
            this.minDurationForQualityIncreaseMs = minDurationForQualityIncreaseMs2;
            this.maxDurationForQualityDecreaseMs = maxDurationForQualityDecreaseMs2;
            this.minDurationToRetainAfterDiscardMs = minDurationToRetainAfterDiscardMs2;
            this.bandwidthFraction = bandwidthFraction2;
            this.bufferedFractionToLiveEdgeForQualityIncrease = bufferedFractionToLiveEdgeForQualityIncrease2;
        }

        public AdaptiveTrackSelection createTrackSelection(TrackGroup group, int... tracks) {
            return new AdaptiveTrackSelection(group, tracks, this.bandwidthMeter, this.maxInitialBitrate, (long) this.minDurationForQualityIncreaseMs, (long) this.maxDurationForQualityDecreaseMs, (long) this.minDurationToRetainAfterDiscardMs, this.bandwidthFraction, this.bufferedFractionToLiveEdgeForQualityIncrease);
        }
    }

    public AdaptiveTrackSelection(TrackGroup group, int[] tracks, BandwidthMeter bandwidthMeter2) {
        this(group, tracks, bandwidthMeter2, DEFAULT_MAX_INITIAL_BITRATE, 10000, 25000, 25000, 0.75f, 0.75f);
    }

    public AdaptiveTrackSelection(TrackGroup group, int[] tracks, BandwidthMeter bandwidthMeter2, int maxInitialBitrate2, long minDurationForQualityIncreaseMs, long maxDurationForQualityDecreaseMs, long minDurationToRetainAfterDiscardMs, float bandwidthFraction2, float bufferedFractionToLiveEdgeForQualityIncrease2) {
        super(group, tracks);
        this.bandwidthMeter = bandwidthMeter2;
        this.maxInitialBitrate = maxInitialBitrate2;
        this.minDurationForQualityIncreaseUs = 1000 * minDurationForQualityIncreaseMs;
        this.maxDurationForQualityDecreaseUs = 1000 * maxDurationForQualityDecreaseMs;
        this.minDurationToRetainAfterDiscardUs = 1000 * minDurationToRetainAfterDiscardMs;
        this.bandwidthFraction = bandwidthFraction2;
        this.bufferedFractionToLiveEdgeForQualityIncrease = bufferedFractionToLiveEdgeForQualityIncrease2;
        this.selectedIndex = determineIdealSelectedIndex(Long.MIN_VALUE);
        this.reason = 1;
    }

    public void updateSelectedTrack(long playbackPositionUs, long bufferedDurationUs, long availableDurationUs) {
        long nowMs = SystemClock.elapsedRealtime();
        int currentSelectedIndex = this.selectedIndex;
        this.selectedIndex = determineIdealSelectedIndex(nowMs);
        if (this.selectedIndex != currentSelectedIndex) {
            if (!isBlacklisted(currentSelectedIndex, nowMs)) {
                Format currentFormat = getFormat(currentSelectedIndex);
                Format selectedFormat = getFormat(this.selectedIndex);
                if (selectedFormat.bitrate > currentFormat.bitrate && bufferedDurationUs < minDurationForQualityIncreaseUs(availableDurationUs)) {
                    this.selectedIndex = currentSelectedIndex;
                } else if (selectedFormat.bitrate < currentFormat.bitrate && bufferedDurationUs >= this.maxDurationForQualityDecreaseUs) {
                    this.selectedIndex = currentSelectedIndex;
                }
            }
            if (this.selectedIndex != currentSelectedIndex) {
                this.reason = 3;
            }
        }
    }

    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    public int getSelectionReason() {
        return this.reason;
    }

    public Object getSelectionData() {
        return null;
    }

    public int evaluateQueueSize(long playbackPositionUs, List<? extends MediaChunk> queue) {
        if (queue.isEmpty()) {
            return 0;
        }
        int queueSize = queue.size();
        if (((MediaChunk) queue.get(queueSize - 1)).endTimeUs - playbackPositionUs < this.minDurationToRetainAfterDiscardUs) {
            return queueSize;
        }
        Format idealFormat = getFormat(determineIdealSelectedIndex(SystemClock.elapsedRealtime()));
        for (int i = 0; i < queueSize; i++) {
            MediaChunk chunk = (MediaChunk) queue.get(i);
            Format format = chunk.trackFormat;
            if (chunk.startTimeUs - playbackPositionUs >= this.minDurationToRetainAfterDiscardUs && format.bitrate < idealFormat.bitrate && format.height != -1 && format.height < 720 && format.width != -1 && format.width < 1280 && format.height < idealFormat.height) {
                return i;
            }
        }
        return queueSize;
    }

    private int determineIdealSelectedIndex(long nowMs) {
        long bitrateEstimate = this.bandwidthMeter.getBitrateEstimate();
        long effectiveBitrate = bitrateEstimate == -1 ? (long) this.maxInitialBitrate : (long) (((float) bitrateEstimate) * this.bandwidthFraction);
        int lowestBitrateNonBlacklistedIndex = 0;
        for (int i = 0; i < this.length; i++) {
            if (nowMs == Long.MIN_VALUE || !isBlacklisted(i, nowMs)) {
                if (((long) getFormat(i).bitrate) <= effectiveBitrate) {
                    return i;
                }
                lowestBitrateNonBlacklistedIndex = i;
            }
        }
        return lowestBitrateNonBlacklistedIndex;
    }

    private long minDurationForQualityIncreaseUs(long availableDurationUs) {
        return (availableDurationUs > C.TIME_UNSET ? 1 : (availableDurationUs == C.TIME_UNSET ? 0 : -1)) != 0 && (availableDurationUs > this.minDurationForQualityIncreaseUs ? 1 : (availableDurationUs == this.minDurationForQualityIncreaseUs ? 0 : -1)) <= 0 ? (long) (((float) availableDurationUs) * this.bufferedFractionToLiveEdgeForQualityIncrease) : this.minDurationForQualityIncreaseUs;
    }
}
