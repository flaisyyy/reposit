package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ShuffleOrder;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;

public final class LoopingMediaSource implements MediaSource {
    /* access modifiers changed from: private */
    public int childPeriodCount;
    private final MediaSource childSource;
    /* access modifiers changed from: private */
    public final int loopCount;

    public LoopingMediaSource(MediaSource childSource2) {
        this(childSource2, Integer.MAX_VALUE);
    }

    public LoopingMediaSource(MediaSource childSource2, int loopCount2) {
        Assertions.checkArgument(loopCount2 > 0);
        this.childSource = childSource2;
        this.loopCount = loopCount2;
    }

    public void prepareSource(ExoPlayer player, boolean isTopLevelSource, final MediaSource.Listener listener) {
        this.childSource.prepareSource(player, false, new MediaSource.Listener() {
            public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, Object manifest) {
                int unused = LoopingMediaSource.this.childPeriodCount = timeline.getPeriodCount();
                listener.onSourceInfoRefreshed(LoopingMediaSource.this, LoopingMediaSource.this.loopCount != Integer.MAX_VALUE ? new LoopingTimeline(timeline, LoopingMediaSource.this.loopCount) : new InfinitelyLoopingTimeline(timeline), manifest);
            }
        });
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
        this.childSource.maybeThrowSourceInfoRefreshError();
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator) {
        if (this.loopCount != Integer.MAX_VALUE) {
            return this.childSource.createPeriod(id.copyWithPeriodIndex(id.periodIndex % this.childPeriodCount), allocator);
        }
        return this.childSource.createPeriod(id, allocator);
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        this.childSource.releasePeriod(mediaPeriod);
    }

    public void releaseSource() {
        this.childSource.releaseSource();
    }

    private static final class LoopingTimeline extends AbstractConcatenatedTimeline {
        private final int childPeriodCount;
        private final Timeline childTimeline;
        private final int childWindowCount;
        private final int loopCount;

        public LoopingTimeline(Timeline childTimeline2, int loopCount2) {
            super(new ShuffleOrder.UnshuffledShuffleOrder(loopCount2));
            this.childTimeline = childTimeline2;
            this.childPeriodCount = childTimeline2.getPeriodCount();
            this.childWindowCount = childTimeline2.getWindowCount();
            this.loopCount = loopCount2;
            if (this.childPeriodCount > 0) {
                Assertions.checkState(loopCount2 <= Integer.MAX_VALUE / this.childPeriodCount, "LoopingMediaSource contains too many periods");
            }
        }

        public int getWindowCount() {
            return this.childWindowCount * this.loopCount;
        }

        public int getPeriodCount() {
            return this.childPeriodCount * this.loopCount;
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByPeriodIndex(int periodIndex) {
            return periodIndex / this.childPeriodCount;
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByWindowIndex(int windowIndex) {
            return windowIndex / this.childWindowCount;
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByChildUid(Object childUid) {
            if (!(childUid instanceof Integer)) {
                return -1;
            }
            return ((Integer) childUid).intValue();
        }

        /* access modifiers changed from: protected */
        public Timeline getTimelineByChildIndex(int childIndex) {
            return this.childTimeline;
        }

        /* access modifiers changed from: protected */
        public int getFirstPeriodIndexByChildIndex(int childIndex) {
            return this.childPeriodCount * childIndex;
        }

        /* access modifiers changed from: protected */
        public int getFirstWindowIndexByChildIndex(int childIndex) {
            return this.childWindowCount * childIndex;
        }

        /* access modifiers changed from: protected */
        public Object getChildUidByChildIndex(int childIndex) {
            return Integer.valueOf(childIndex);
        }
    }

    private static final class InfinitelyLoopingTimeline extends ForwardingTimeline {
        public InfinitelyLoopingTimeline(Timeline timeline) {
            super(timeline);
        }

        public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            int childNextWindowIndex = this.timeline.getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
            return childNextWindowIndex == -1 ? getFirstWindowIndex(shuffleModeEnabled) : childNextWindowIndex;
        }

        public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            int childPreviousWindowIndex = this.timeline.getPreviousWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
            return childPreviousWindowIndex == -1 ? getLastWindowIndex(shuffleModeEnabled) : childPreviousWindowIndex;
        }
    }
}
