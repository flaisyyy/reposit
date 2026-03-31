package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.util.ArrayList;

public final class ClippingMediaSource implements MediaSource, MediaSource.Listener {
    private final boolean enableInitialDiscontinuity;
    private final long endUs;
    private final ArrayList<ClippingMediaPeriod> mediaPeriods;
    private final MediaSource mediaSource;
    private MediaSource.Listener sourceListener;
    private final long startUs;

    public ClippingMediaSource(MediaSource mediaSource2, long startPositionUs, long endPositionUs) {
        this(mediaSource2, startPositionUs, endPositionUs, true);
    }

    public ClippingMediaSource(MediaSource mediaSource2, long startPositionUs, long endPositionUs, boolean enableInitialDiscontinuity2) {
        Assertions.checkArgument(startPositionUs >= 0);
        this.mediaSource = (MediaSource) Assertions.checkNotNull(mediaSource2);
        this.startUs = startPositionUs;
        this.endUs = endPositionUs;
        this.enableInitialDiscontinuity = enableInitialDiscontinuity2;
        this.mediaPeriods = new ArrayList<>();
    }

    public void prepareSource(ExoPlayer player, boolean isTopLevelSource, MediaSource.Listener listener) {
        this.sourceListener = listener;
        this.mediaSource.prepareSource(player, false, this);
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
        this.mediaSource.maybeThrowSourceInfoRefreshError();
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator) {
        ClippingMediaPeriod mediaPeriod = new ClippingMediaPeriod(this.mediaSource.createPeriod(id, allocator), this.enableInitialDiscontinuity);
        this.mediaPeriods.add(mediaPeriod);
        mediaPeriod.setClipping(this.startUs, this.endUs);
        return mediaPeriod;
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        Assertions.checkState(this.mediaPeriods.remove(mediaPeriod));
        this.mediaSource.releasePeriod(((ClippingMediaPeriod) mediaPeriod).mediaPeriod);
    }

    public void releaseSource() {
        this.mediaSource.releaseSource();
    }

    public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, Object manifest) {
        this.sourceListener.onSourceInfoRefreshed(this, new ClippingTimeline(timeline, this.startUs, this.endUs), manifest);
        int count = this.mediaPeriods.size();
        for (int i = 0; i < count; i++) {
            this.mediaPeriods.get(i).setClipping(this.startUs, this.endUs);
        }
    }

    private static final class ClippingTimeline extends ForwardingTimeline {
        private final long endUs;
        private final long startUs;

        public ClippingTimeline(Timeline timeline, long startUs2, long endUs2) {
            super(timeline);
            long resolvedEndUs;
            Assertions.checkArgument(timeline.getWindowCount() == 1);
            Assertions.checkArgument(timeline.getPeriodCount() == 1);
            Timeline.Window window = timeline.getWindow(0, new Timeline.Window(), false);
            Assertions.checkArgument(!window.isDynamic);
            if (endUs2 == Long.MIN_VALUE) {
                resolvedEndUs = window.durationUs;
            } else {
                resolvedEndUs = endUs2;
            }
            if (window.durationUs != C.TIME_UNSET) {
                resolvedEndUs = resolvedEndUs > window.durationUs ? window.durationUs : resolvedEndUs;
                Assertions.checkArgument(startUs2 == 0 || window.isSeekable);
                Assertions.checkArgument(startUs2 <= resolvedEndUs);
            }
            Assertions.checkArgument(timeline.getPeriod(0, new Timeline.Period()).getPositionInWindowUs() == 0);
            this.startUs = startUs2;
            this.endUs = resolvedEndUs;
        }

        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, boolean setIds, long defaultPositionProjectionUs) {
            long j;
            long min;
            Timeline.Window window2 = this.timeline.getWindow(0, window, setIds, defaultPositionProjectionUs);
            if (this.endUs != C.TIME_UNSET) {
                j = this.endUs - this.startUs;
            } else {
                j = -9223372036854775807L;
            }
            window2.durationUs = j;
            if (window2.defaultPositionUs != C.TIME_UNSET) {
                window2.defaultPositionUs = Math.max(window2.defaultPositionUs, this.startUs);
                if (this.endUs == C.TIME_UNSET) {
                    min = window2.defaultPositionUs;
                } else {
                    min = Math.min(window2.defaultPositionUs, this.endUs);
                }
                window2.defaultPositionUs = min;
                window2.defaultPositionUs -= this.startUs;
            }
            long startMs = C.usToMs(this.startUs);
            if (window2.presentationStartTimeMs != C.TIME_UNSET) {
                window2.presentationStartTimeMs += startMs;
            }
            if (window2.windowStartTimeMs != C.TIME_UNSET) {
                window2.windowStartTimeMs += startMs;
            }
            return window2;
        }

        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
            long j = C.TIME_UNSET;
            Timeline.Period period2 = this.timeline.getPeriod(0, period, setIds);
            if (this.endUs != C.TIME_UNSET) {
                j = this.endUs - this.startUs;
            }
            period2.durationUs = j;
            return period2;
        }
    }
}
