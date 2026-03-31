package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.Allocator;
import java.io.IOException;

public final class DeferredMediaPeriod implements MediaPeriod, MediaPeriod.Callback {
    private final Allocator allocator;
    private MediaPeriod.Callback callback;
    private final MediaSource.MediaPeriodId id;
    private MediaPeriod mediaPeriod;
    public final MediaSource mediaSource;
    private long preparePositionUs;

    public DeferredMediaPeriod(MediaSource mediaSource2, MediaSource.MediaPeriodId id2, Allocator allocator2) {
        this.id = id2;
        this.allocator = allocator2;
        this.mediaSource = mediaSource2;
    }

    public void createPeriod() {
        this.mediaPeriod = this.mediaSource.createPeriod(this.id, this.allocator);
        if (this.callback != null) {
            this.mediaPeriod.prepare(this, this.preparePositionUs);
        }
    }

    public void releasePeriod() {
        if (this.mediaPeriod != null) {
            this.mediaSource.releasePeriod(this.mediaPeriod);
        }
    }

    public void prepare(MediaPeriod.Callback callback2, long preparePositionUs2) {
        this.callback = callback2;
        this.preparePositionUs = preparePositionUs2;
        if (this.mediaPeriod != null) {
            this.mediaPeriod.prepare(this, preparePositionUs2);
        }
    }

    public void maybeThrowPrepareError() throws IOException {
        if (this.mediaPeriod != null) {
            this.mediaPeriod.maybeThrowPrepareError();
        } else {
            this.mediaSource.maybeThrowSourceInfoRefreshError();
        }
    }

    public TrackGroupArray getTrackGroups() {
        return this.mediaPeriod.getTrackGroups();
    }

    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        return this.mediaPeriod.selectTracks(selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs);
    }

    public void discardBuffer(long positionUs) {
        this.mediaPeriod.discardBuffer(positionUs);
    }

    public long readDiscontinuity() {
        return this.mediaPeriod.readDiscontinuity();
    }

    public long getBufferedPositionUs() {
        return this.mediaPeriod.getBufferedPositionUs();
    }

    public long seekToUs(long positionUs) {
        return this.mediaPeriod.seekToUs(positionUs);
    }

    public long getNextLoadPositionUs() {
        return this.mediaPeriod.getNextLoadPositionUs();
    }

    public boolean continueLoading(long positionUs) {
        return this.mediaPeriod != null && this.mediaPeriod.continueLoading(positionUs);
    }

    public void onContinueLoadingRequested(MediaPeriod source) {
        this.callback.onContinueLoadingRequested(this);
    }

    public void onPrepared(MediaPeriod mediaPeriod2) {
        this.callback.onPrepared(this);
    }
}
