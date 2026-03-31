package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.Allocator;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;

public final class MergingMediaSource implements MediaSource {
    private static final int PERIOD_COUNT_UNSET = -1;
    private MediaSource.Listener listener;
    private final MediaSource[] mediaSources;
    private IllegalMergeException mergeError;
    private final ArrayList<MediaSource> pendingTimelineSources;
    private int periodCount = -1;
    private Object primaryManifest;
    private Timeline primaryTimeline;
    private final Timeline.Window window = new Timeline.Window();

    public static final class IllegalMergeException extends IOException {
        public static final int REASON_PERIOD_COUNT_MISMATCH = 1;
        public static final int REASON_WINDOWS_ARE_DYNAMIC = 0;
        public final int reason;

        @Retention(RetentionPolicy.SOURCE)
        public @interface Reason {
        }

        public IllegalMergeException(int reason2) {
            this.reason = reason2;
        }
    }

    public MergingMediaSource(MediaSource... mediaSources2) {
        this.mediaSources = mediaSources2;
        this.pendingTimelineSources = new ArrayList<>(Arrays.asList(mediaSources2));
    }

    public void prepareSource(ExoPlayer player, boolean isTopLevelSource, MediaSource.Listener listener2) {
        this.listener = listener2;
        for (int i = 0; i < this.mediaSources.length; i++) {
            final int sourceIndex = i;
            this.mediaSources[sourceIndex].prepareSource(player, false, new MediaSource.Listener() {
                public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, Object manifest) {
                    MergingMediaSource.this.handleSourceInfoRefreshed(sourceIndex, timeline, manifest);
                }
            });
        }
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
        if (this.mergeError != null) {
            throw this.mergeError;
        }
        for (MediaSource mediaSource : this.mediaSources) {
            mediaSource.maybeThrowSourceInfoRefreshError();
        }
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator) {
        MediaPeriod[] periods = new MediaPeriod[this.mediaSources.length];
        for (int i = 0; i < periods.length; i++) {
            periods[i] = this.mediaSources[i].createPeriod(id, allocator);
        }
        return new MergingMediaPeriod(periods);
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        MergingMediaPeriod mergingPeriod = (MergingMediaPeriod) mediaPeriod;
        for (int i = 0; i < this.mediaSources.length; i++) {
            this.mediaSources[i].releasePeriod(mergingPeriod.periods[i]);
        }
    }

    public void releaseSource() {
        for (MediaSource mediaSource : this.mediaSources) {
            mediaSource.releaseSource();
        }
    }

    /* access modifiers changed from: private */
    public void handleSourceInfoRefreshed(int sourceIndex, Timeline timeline, Object manifest) {
        if (this.mergeError == null) {
            this.mergeError = checkTimelineMerges(timeline);
        }
        if (this.mergeError == null) {
            this.pendingTimelineSources.remove(this.mediaSources[sourceIndex]);
            if (sourceIndex == 0) {
                this.primaryTimeline = timeline;
                this.primaryManifest = manifest;
            }
            if (this.pendingTimelineSources.isEmpty()) {
                this.listener.onSourceInfoRefreshed(this, this.primaryTimeline, this.primaryManifest);
            }
        }
    }

    private IllegalMergeException checkTimelineMerges(Timeline timeline) {
        int windowCount = timeline.getWindowCount();
        for (int i = 0; i < windowCount; i++) {
            if (timeline.getWindow(i, this.window, false).isDynamic) {
                return new IllegalMergeException(0);
            }
        }
        if (this.periodCount == -1) {
            this.periodCount = timeline.getPeriodCount();
        } else if (timeline.getPeriodCount() != this.periodCount) {
            return new IllegalMergeException(1);
        }
        return null;
    }
}
