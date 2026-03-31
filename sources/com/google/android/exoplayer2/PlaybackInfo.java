package com.google.android.exoplayer2;

import com.google.android.exoplayer2.source.MediaSource;

final class PlaybackInfo {
    public volatile long bufferedPositionUs;
    public final long contentPositionUs;
    public final Object manifest;
    public final MediaSource.MediaPeriodId periodId;
    public volatile long positionUs;
    public final long startPositionUs;
    public final Timeline timeline;

    public PlaybackInfo(Timeline timeline2, Object manifest2, int periodIndex, long startPositionUs2) {
        this(timeline2, manifest2, new MediaSource.MediaPeriodId(periodIndex), startPositionUs2, C.TIME_UNSET);
    }

    public PlaybackInfo(Timeline timeline2, Object manifest2, MediaSource.MediaPeriodId periodId2, long startPositionUs2, long contentPositionUs2) {
        this.timeline = timeline2;
        this.manifest = manifest2;
        this.periodId = periodId2;
        this.startPositionUs = startPositionUs2;
        this.contentPositionUs = contentPositionUs2;
        this.positionUs = startPositionUs2;
        this.bufferedPositionUs = startPositionUs2;
    }

    public PlaybackInfo fromNewPosition(int periodIndex, long startPositionUs2, long contentPositionUs2) {
        return fromNewPosition(new MediaSource.MediaPeriodId(periodIndex), startPositionUs2, contentPositionUs2);
    }

    public PlaybackInfo fromNewPosition(MediaSource.MediaPeriodId periodId2, long startPositionUs2, long contentPositionUs2) {
        return new PlaybackInfo(this.timeline, this.manifest, periodId2, startPositionUs2, contentPositionUs2);
    }

    public PlaybackInfo copyWithPeriodIndex(int periodIndex) {
        PlaybackInfo playbackInfo = new PlaybackInfo(this.timeline, this.manifest, this.periodId.copyWithPeriodIndex(periodIndex), this.startPositionUs, this.contentPositionUs);
        copyMutablePositions(this, playbackInfo);
        return playbackInfo;
    }

    public PlaybackInfo copyWithTimeline(Timeline timeline2, Object manifest2) {
        PlaybackInfo playbackInfo = new PlaybackInfo(timeline2, manifest2, this.periodId, this.startPositionUs, this.contentPositionUs);
        copyMutablePositions(this, playbackInfo);
        return playbackInfo;
    }

    private static void copyMutablePositions(PlaybackInfo from, PlaybackInfo to) {
        to.positionUs = from.positionUs;
        to.bufferedPositionUs = from.bufferedPositionUs;
    }
}
