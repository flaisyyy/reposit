package com.google.android.exoplayer2.source;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;

public final class ExtractorMediaSource implements MediaSource, ExtractorMediaPeriod.Listener {
    public static final int DEFAULT_LOADING_CHECK_INTERVAL_BYTES = 1048576;
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT_LIVE = 6;
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT_ON_DEMAND = 3;
    public static final int MIN_RETRY_COUNT_DEFAULT_FOR_MEDIA = -1;
    private final int continueLoadingCheckIntervalBytes;
    private final String customCacheKey;
    private final DataSource.Factory dataSourceFactory;
    private final MediaSourceEventListener.EventDispatcher eventDispatcher;
    private final ExtractorsFactory extractorsFactory;
    private final int minLoadableRetryCount;
    private MediaSource.Listener sourceListener;
    private long timelineDurationUs;
    private boolean timelineIsSeekable;
    private final Uri uri;

    @Deprecated
    public interface EventListener {
        void onLoadError(IOException iOException);
    }

    public static final class Factory implements AdsMediaSource.MediaSourceFactory {
        private int continueLoadingCheckIntervalBytes = 1048576;
        @Nullable
        private String customCacheKey;
        private final DataSource.Factory dataSourceFactory;
        @Nullable
        private ExtractorsFactory extractorsFactory;
        private boolean isCreateCalled;
        private int minLoadableRetryCount = -1;

        public Factory(DataSource.Factory dataSourceFactory2) {
            this.dataSourceFactory = dataSourceFactory2;
        }

        public Factory setExtractorsFactory(ExtractorsFactory extractorsFactory2) {
            Assertions.checkState(!this.isCreateCalled);
            this.extractorsFactory = extractorsFactory2;
            return this;
        }

        public Factory setCustomCacheKey(String customCacheKey2) {
            Assertions.checkState(!this.isCreateCalled);
            this.customCacheKey = customCacheKey2;
            return this;
        }

        public Factory setMinLoadableRetryCount(int minLoadableRetryCount2) {
            Assertions.checkState(!this.isCreateCalled);
            this.minLoadableRetryCount = minLoadableRetryCount2;
            return this;
        }

        public Factory setContinueLoadingCheckIntervalBytes(int continueLoadingCheckIntervalBytes2) {
            Assertions.checkState(!this.isCreateCalled);
            this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes2;
            return this;
        }

        public ExtractorMediaSource createMediaSource(Uri uri) {
            return createMediaSource(uri, (Handler) null, (MediaSourceEventListener) null);
        }

        public ExtractorMediaSource createMediaSource(Uri uri, @Nullable Handler eventHandler, @Nullable MediaSourceEventListener eventListener) {
            this.isCreateCalled = true;
            if (this.extractorsFactory == null) {
                this.extractorsFactory = new DefaultExtractorsFactory();
            }
            return new ExtractorMediaSource(uri, this.dataSourceFactory, this.extractorsFactory, this.minLoadableRetryCount, eventHandler, eventListener, this.customCacheKey, this.continueLoadingCheckIntervalBytes);
        }

        public int[] getSupportedTypes() {
            return new int[]{3};
        }
    }

    @Deprecated
    public ExtractorMediaSource(Uri uri2, DataSource.Factory dataSourceFactory2, ExtractorsFactory extractorsFactory2, Handler eventHandler, EventListener eventListener) {
        this(uri2, dataSourceFactory2, extractorsFactory2, eventHandler, eventListener, (String) null);
    }

    @Deprecated
    public ExtractorMediaSource(Uri uri2, DataSource.Factory dataSourceFactory2, ExtractorsFactory extractorsFactory2, Handler eventHandler, EventListener eventListener, String customCacheKey2) {
        this(uri2, dataSourceFactory2, extractorsFactory2, -1, eventHandler, eventListener, customCacheKey2, 1048576);
    }

    /* JADX INFO: this call moved to the top of the method (can break code semantics) */
    @Deprecated
    public ExtractorMediaSource(Uri uri2, DataSource.Factory dataSourceFactory2, ExtractorsFactory extractorsFactory2, int minLoadableRetryCount2, Handler eventHandler, EventListener eventListener, String customCacheKey2, int continueLoadingCheckIntervalBytes2) {
        this(uri2, dataSourceFactory2, extractorsFactory2, minLoadableRetryCount2, eventHandler, (MediaSourceEventListener) eventListener == null ? null : new EventListenerWrapper(eventListener), customCacheKey2, continueLoadingCheckIntervalBytes2);
    }

    private ExtractorMediaSource(Uri uri2, DataSource.Factory dataSourceFactory2, ExtractorsFactory extractorsFactory2, int minLoadableRetryCount2, @Nullable Handler eventHandler, @Nullable MediaSourceEventListener eventListener, @Nullable String customCacheKey2, int continueLoadingCheckIntervalBytes2) {
        this.uri = uri2;
        this.dataSourceFactory = dataSourceFactory2;
        this.extractorsFactory = extractorsFactory2;
        this.minLoadableRetryCount = minLoadableRetryCount2;
        this.eventDispatcher = new MediaSourceEventListener.EventDispatcher(eventHandler, eventListener);
        this.customCacheKey = customCacheKey2;
        this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes2;
    }

    public void prepareSource(ExoPlayer player, boolean isTopLevelSource, MediaSource.Listener listener) {
        this.sourceListener = listener;
        notifySourceInfoRefreshed(C.TIME_UNSET, false);
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator) {
        Assertions.checkArgument(id.periodIndex == 0);
        return new ExtractorMediaPeriod(this.uri, this.dataSourceFactory.createDataSource(), this.extractorsFactory.createExtractors(), this.minLoadableRetryCount, this.eventDispatcher, this, allocator, this.customCacheKey, this.continueLoadingCheckIntervalBytes);
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((ExtractorMediaPeriod) mediaPeriod).release();
    }

    public void releaseSource() {
        this.sourceListener = null;
    }

    public void onSourceInfoRefreshed(long durationUs, boolean isSeekable) {
        if (durationUs == C.TIME_UNSET) {
            durationUs = this.timelineDurationUs;
        }
        if (this.timelineDurationUs != durationUs || this.timelineIsSeekable != isSeekable) {
            notifySourceInfoRefreshed(durationUs, isSeekable);
        }
    }

    private void notifySourceInfoRefreshed(long durationUs, boolean isSeekable) {
        this.timelineDurationUs = durationUs;
        this.timelineIsSeekable = isSeekable;
        this.sourceListener.onSourceInfoRefreshed(this, new SinglePeriodTimeline(this.timelineDurationUs, this.timelineIsSeekable), (Object) null);
    }

    private static final class EventListenerWrapper implements MediaSourceEventListener {
        private final EventListener eventListener;

        public EventListenerWrapper(EventListener eventListener2) {
            this.eventListener = (EventListener) Assertions.checkNotNull(eventListener2);
        }

        public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {
        }

        public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
        }

        public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
        }

        public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
            this.eventListener.onLoadError(error);
        }

        public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
        }

        public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {
        }
    }
}
