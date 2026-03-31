package com.google.android.exoplayer2.source;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;

public final class SingleSampleMediaSource implements MediaSource {
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT = 3;
    private final DataSource.Factory dataSourceFactory;
    private final DataSpec dataSpec;
    private final long durationUs;
    private final MediaSourceEventListener.EventDispatcher eventDispatcher;
    private final Format format;
    private final int minLoadableRetryCount;
    private final Timeline timeline;
    private final boolean treatLoadErrorsAsEndOfStream;

    @Deprecated
    public interface EventListener {
        void onLoadError(int i, IOException iOException);
    }

    public static final class Factory {
        private final DataSource.Factory dataSourceFactory;
        private boolean isCreateCalled;
        private int minLoadableRetryCount = 3;
        private boolean treatLoadErrorsAsEndOfStream;

        public Factory(DataSource.Factory dataSourceFactory2) {
            this.dataSourceFactory = (DataSource.Factory) Assertions.checkNotNull(dataSourceFactory2);
        }

        public Factory setMinLoadableRetryCount(int minLoadableRetryCount2) {
            Assertions.checkState(!this.isCreateCalled);
            this.minLoadableRetryCount = minLoadableRetryCount2;
            return this;
        }

        public Factory setTreatLoadErrorsAsEndOfStream(boolean treatLoadErrorsAsEndOfStream2) {
            Assertions.checkState(!this.isCreateCalled);
            this.treatLoadErrorsAsEndOfStream = treatLoadErrorsAsEndOfStream2;
            return this;
        }

        public SingleSampleMediaSource createMediaSource(Uri uri, Format format, long durationUs) {
            return createMediaSource(uri, format, durationUs, (Handler) null, (MediaSourceEventListener) null);
        }

        public SingleSampleMediaSource createMediaSource(Uri uri, Format format, long durationUs, @Nullable Handler eventHandler, @Nullable MediaSourceEventListener eventListener) {
            this.isCreateCalled = true;
            return new SingleSampleMediaSource(uri, this.dataSourceFactory, format, durationUs, this.minLoadableRetryCount, eventHandler, eventListener, this.treatLoadErrorsAsEndOfStream);
        }
    }

    @Deprecated
    public SingleSampleMediaSource(Uri uri, DataSource.Factory dataSourceFactory2, Format format2, long durationUs2) {
        this(uri, dataSourceFactory2, format2, durationUs2, 3);
    }

    @Deprecated
    public SingleSampleMediaSource(Uri uri, DataSource.Factory dataSourceFactory2, Format format2, long durationUs2, int minLoadableRetryCount2) {
        this(uri, dataSourceFactory2, format2, durationUs2, minLoadableRetryCount2, (Handler) null, (MediaSourceEventListener) null, false);
    }

    /* JADX INFO: this call moved to the top of the method (can break code semantics) */
    @Deprecated
    public SingleSampleMediaSource(Uri uri, DataSource.Factory dataSourceFactory2, Format format2, long durationUs2, int minLoadableRetryCount2, Handler eventHandler, EventListener eventListener, int eventSourceId, boolean treatLoadErrorsAsEndOfStream2) {
        this(uri, dataSourceFactory2, format2, durationUs2, minLoadableRetryCount2, eventHandler, eventListener == null ? null : new EventListenerWrapper(eventListener, eventSourceId), treatLoadErrorsAsEndOfStream2);
    }

    private SingleSampleMediaSource(Uri uri, DataSource.Factory dataSourceFactory2, Format format2, long durationUs2, int minLoadableRetryCount2, Handler eventHandler, MediaSourceEventListener eventListener, boolean treatLoadErrorsAsEndOfStream2) {
        this.dataSourceFactory = dataSourceFactory2;
        this.format = format2;
        this.durationUs = durationUs2;
        this.minLoadableRetryCount = minLoadableRetryCount2;
        this.treatLoadErrorsAsEndOfStream = treatLoadErrorsAsEndOfStream2;
        this.eventDispatcher = new MediaSourceEventListener.EventDispatcher(eventHandler, eventListener);
        this.dataSpec = new DataSpec(uri);
        this.timeline = new SinglePeriodTimeline(durationUs2, true);
    }

    public void prepareSource(ExoPlayer player, boolean isTopLevelSource, MediaSource.Listener listener) {
        listener.onSourceInfoRefreshed(this, this.timeline, (Object) null);
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator) {
        Assertions.checkArgument(id.periodIndex == 0);
        return new SingleSampleMediaPeriod(this.dataSpec, this.dataSourceFactory, this.format, this.durationUs, this.minLoadableRetryCount, this.eventDispatcher, this.treatLoadErrorsAsEndOfStream);
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((SingleSampleMediaPeriod) mediaPeriod).release();
    }

    public void releaseSource() {
    }

    private static final class EventListenerWrapper implements MediaSourceEventListener {
        private final EventListener eventListener;
        private final int eventSourceId;

        public EventListenerWrapper(EventListener eventListener2, int eventSourceId2) {
            this.eventListener = (EventListener) Assertions.checkNotNull(eventListener2);
            this.eventSourceId = eventSourceId2;
        }

        public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {
        }

        public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
        }

        public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
        }

        public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
            this.eventListener.onLoadError(this.eventSourceId, error);
        }

        public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
        }

        public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {
        }
    }
}
