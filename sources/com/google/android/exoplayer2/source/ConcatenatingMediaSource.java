package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ShuffleOrder;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ConcatenatingMediaSource implements MediaSource {
    private final boolean[] duplicateFlags;
    private final boolean isAtomic;
    private MediaSource.Listener listener;
    private final Object[] manifests;
    private final MediaSource[] mediaSources;
    private final ShuffleOrder shuffleOrder;
    private final Map<MediaPeriod, Integer> sourceIndexByMediaPeriod;
    private ConcatenatedTimeline timeline;
    private final Timeline[] timelines;

    public ConcatenatingMediaSource(MediaSource... mediaSources2) {
        this(false, mediaSources2);
    }

    public ConcatenatingMediaSource(boolean isAtomic2, MediaSource... mediaSources2) {
        this(isAtomic2, new ShuffleOrder.DefaultShuffleOrder(mediaSources2.length), mediaSources2);
    }

    public ConcatenatingMediaSource(boolean isAtomic2, ShuffleOrder shuffleOrder2, MediaSource... mediaSources2) {
        boolean z = false;
        for (MediaSource mediaSource : mediaSources2) {
            Assertions.checkNotNull(mediaSource);
        }
        Assertions.checkArgument(shuffleOrder2.getLength() == mediaSources2.length ? true : z);
        this.mediaSources = mediaSources2;
        this.isAtomic = isAtomic2;
        this.shuffleOrder = shuffleOrder2;
        this.timelines = new Timeline[mediaSources2.length];
        this.manifests = new Object[mediaSources2.length];
        this.sourceIndexByMediaPeriod = new HashMap();
        this.duplicateFlags = buildDuplicateFlags(mediaSources2);
    }

    public void prepareSource(ExoPlayer player, boolean isTopLevelSource, MediaSource.Listener listener2) {
        this.listener = listener2;
        if (this.mediaSources.length == 0) {
            listener2.onSourceInfoRefreshed(this, Timeline.EMPTY, (Object) null);
            return;
        }
        for (int i = 0; i < this.mediaSources.length; i++) {
            if (!this.duplicateFlags[i]) {
                final int index = i;
                this.mediaSources[i].prepareSource(player, false, new MediaSource.Listener() {
                    public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, Object manifest) {
                        ConcatenatingMediaSource.this.handleSourceInfoRefreshed(index, timeline, manifest);
                    }
                });
            }
        }
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
        for (int i = 0; i < this.mediaSources.length; i++) {
            if (!this.duplicateFlags[i]) {
                this.mediaSources[i].maybeThrowSourceInfoRefreshError();
            }
        }
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator) {
        int sourceIndex = this.timeline.getChildIndexByPeriodIndex(id.periodIndex);
        MediaPeriod mediaPeriod = this.mediaSources[sourceIndex].createPeriod(id.copyWithPeriodIndex(id.periodIndex - this.timeline.getFirstPeriodIndexByChildIndex(sourceIndex)), allocator);
        this.sourceIndexByMediaPeriod.put(mediaPeriod, Integer.valueOf(sourceIndex));
        return mediaPeriod;
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        int sourceIndex = this.sourceIndexByMediaPeriod.get(mediaPeriod).intValue();
        this.sourceIndexByMediaPeriod.remove(mediaPeriod);
        this.mediaSources[sourceIndex].releasePeriod(mediaPeriod);
    }

    public void releaseSource() {
        for (int i = 0; i < this.mediaSources.length; i++) {
            if (!this.duplicateFlags[i]) {
                this.mediaSources[i].releaseSource();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleSourceInfoRefreshed(int sourceFirstIndex, Timeline sourceTimeline, Object sourceManifest) {
        this.timelines[sourceFirstIndex] = sourceTimeline;
        this.manifests[sourceFirstIndex] = sourceManifest;
        for (int i = sourceFirstIndex + 1; i < this.mediaSources.length; i++) {
            if (this.mediaSources[i] == this.mediaSources[sourceFirstIndex]) {
                this.timelines[i] = sourceTimeline;
                this.manifests[i] = sourceManifest;
            }
        }
        Timeline[] timelineArr = this.timelines;
        int length = timelineArr.length;
        int i2 = 0;
        while (i2 < length) {
            if (timelineArr[i2] != null) {
                i2++;
            } else {
                return;
            }
        }
        this.timeline = new ConcatenatedTimeline((Timeline[]) this.timelines.clone(), this.isAtomic, this.shuffleOrder);
        this.listener.onSourceInfoRefreshed(this, this.timeline, this.manifests.clone());
    }

    private static boolean[] buildDuplicateFlags(MediaSource[] mediaSources2) {
        boolean[] duplicateFlags2 = new boolean[mediaSources2.length];
        IdentityHashMap<MediaSource, Void> sources = new IdentityHashMap<>(mediaSources2.length);
        for (int i = 0; i < mediaSources2.length; i++) {
            MediaSource source = mediaSources2[i];
            if (!sources.containsKey(source)) {
                sources.put(source, (Object) null);
            } else {
                duplicateFlags2[i] = true;
            }
        }
        return duplicateFlags2;
    }

    private static final class ConcatenatedTimeline extends AbstractConcatenatedTimeline {
        private final boolean isAtomic;
        private final int[] sourcePeriodOffsets;
        private final int[] sourceWindowOffsets;
        private final Timeline[] timelines;

        public ConcatenatedTimeline(Timeline[] timelines2, boolean isAtomic2, ShuffleOrder shuffleOrder) {
            super(shuffleOrder);
            int[] sourcePeriodOffsets2 = new int[timelines2.length];
            int[] sourceWindowOffsets2 = new int[timelines2.length];
            long periodCount = 0;
            int windowCount = 0;
            for (int i = 0; i < timelines2.length; i++) {
                Timeline timeline = timelines2[i];
                periodCount += (long) timeline.getPeriodCount();
                Assertions.checkState(periodCount <= 2147483647L, "ConcatenatingMediaSource children contain too many periods");
                sourcePeriodOffsets2[i] = (int) periodCount;
                windowCount += timeline.getWindowCount();
                sourceWindowOffsets2[i] = windowCount;
            }
            this.timelines = timelines2;
            this.sourcePeriodOffsets = sourcePeriodOffsets2;
            this.sourceWindowOffsets = sourceWindowOffsets2;
            this.isAtomic = isAtomic2;
        }

        public int getWindowCount() {
            return this.sourceWindowOffsets[this.sourceWindowOffsets.length - 1];
        }

        public int getPeriodCount() {
            return this.sourcePeriodOffsets[this.sourcePeriodOffsets.length - 1];
        }

        public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            boolean z = true;
            if (this.isAtomic && repeatMode == 1) {
                repeatMode = 2;
            }
            if (this.isAtomic || !shuffleModeEnabled) {
                z = false;
            }
            return super.getNextWindowIndex(windowIndex, repeatMode, z);
        }

        public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
            boolean z = true;
            if (this.isAtomic && repeatMode == 1) {
                repeatMode = 2;
            }
            if (this.isAtomic || !shuffleModeEnabled) {
                z = false;
            }
            return super.getPreviousWindowIndex(windowIndex, repeatMode, z);
        }

        public int getLastWindowIndex(boolean shuffleModeEnabled) {
            return super.getLastWindowIndex(!this.isAtomic && shuffleModeEnabled);
        }

        public int getFirstWindowIndex(boolean shuffleModeEnabled) {
            return super.getFirstWindowIndex(!this.isAtomic && shuffleModeEnabled);
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByPeriodIndex(int periodIndex) {
            return Util.binarySearchFloor(this.sourcePeriodOffsets, periodIndex + 1, false, false) + 1;
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByWindowIndex(int windowIndex) {
            return Util.binarySearchFloor(this.sourceWindowOffsets, windowIndex + 1, false, false) + 1;
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
            return this.timelines[childIndex];
        }

        /* access modifiers changed from: protected */
        public int getFirstPeriodIndexByChildIndex(int childIndex) {
            if (childIndex == 0) {
                return 0;
            }
            return this.sourcePeriodOffsets[childIndex - 1];
        }

        /* access modifiers changed from: protected */
        public int getFirstWindowIndexByChildIndex(int childIndex) {
            if (childIndex == 0) {
                return 0;
            }
            return this.sourceWindowOffsets[childIndex - 1];
        }

        /* access modifiers changed from: protected */
        public Object getChildUidByChildIndex(int childIndex) {
            return Integer.valueOf(childIndex);
        }
    }
}
