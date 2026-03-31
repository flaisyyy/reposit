package com.google.android.exoplayer2.source.ads;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ViewGroup;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.DeferredMediaPeriod;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AdsMediaSource implements MediaSource {
    private static final String TAG = "AdsMediaSource";
    private long[][] adDurationsUs;
    private MediaSource[][] adGroupMediaSources;
    private final MediaSourceFactory adMediaSourceFactory;
    private AdPlaybackState adPlaybackState;
    /* access modifiers changed from: private */
    public final ViewGroup adUiViewGroup;
    /* access modifiers changed from: private */
    public final AdsLoader adsLoader;
    /* access modifiers changed from: private */
    public final ComponentListener componentListener;
    private Object contentManifest;
    private final MediaSource contentMediaSource;
    private Timeline contentTimeline;
    private final Map<MediaSource, List<DeferredMediaPeriod>> deferredMediaPeriodByAdMediaSource;
    /* access modifiers changed from: private */
    @Nullable
    public final Handler eventHandler;
    /* access modifiers changed from: private */
    @Nullable
    public final EventListener eventListener;
    private MediaSource.Listener listener;
    private final Handler mainHandler;
    private final Timeline.Period period;
    private ExoPlayer player;
    /* access modifiers changed from: private */
    public Handler playerHandler;
    /* access modifiers changed from: private */
    public volatile boolean released;

    public interface EventListener extends MediaSourceEventListener {
        void onAdClicked();

        void onAdLoadError(IOException iOException);

        void onAdTapped();
    }

    public interface MediaSourceFactory {
        MediaSource createMediaSource(Uri uri, @Nullable Handler handler, @Nullable MediaSourceEventListener mediaSourceEventListener);

        int[] getSupportedTypes();
    }

    public AdsMediaSource(MediaSource contentMediaSource2, DataSource.Factory dataSourceFactory, AdsLoader adsLoader2, ViewGroup adUiViewGroup2) {
        this(contentMediaSource2, dataSourceFactory, adsLoader2, adUiViewGroup2, (Handler) null, (EventListener) null);
    }

    public AdsMediaSource(MediaSource contentMediaSource2, DataSource.Factory dataSourceFactory, AdsLoader adsLoader2, ViewGroup adUiViewGroup2, @Nullable Handler eventHandler2, @Nullable EventListener eventListener2) {
        this(contentMediaSource2, (MediaSourceFactory) new ExtractorMediaSource.Factory(dataSourceFactory), adsLoader2, adUiViewGroup2, eventHandler2, eventListener2);
    }

    public AdsMediaSource(MediaSource contentMediaSource2, MediaSourceFactory adMediaSourceFactory2, AdsLoader adsLoader2, ViewGroup adUiViewGroup2, @Nullable Handler eventHandler2, @Nullable EventListener eventListener2) {
        this.contentMediaSource = contentMediaSource2;
        this.adMediaSourceFactory = adMediaSourceFactory2;
        this.adsLoader = adsLoader2;
        this.adUiViewGroup = adUiViewGroup2;
        this.eventHandler = eventHandler2;
        this.eventListener = eventListener2;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.componentListener = new ComponentListener();
        this.deferredMediaPeriodByAdMediaSource = new HashMap();
        this.period = new Timeline.Period();
        this.adGroupMediaSources = new MediaSource[0][];
        this.adDurationsUs = new long[0][];
        adsLoader2.setSupportedContentTypes(adMediaSourceFactory2.getSupportedTypes());
    }

    public void prepareSource(final ExoPlayer player2, boolean isTopLevelSource, MediaSource.Listener listener2) {
        Assertions.checkArgument(isTopLevelSource);
        this.listener = listener2;
        this.player = player2;
        this.playerHandler = new Handler();
        this.contentMediaSource.prepareSource(player2, false, new MediaSource.Listener() {
            public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, Object manifest) {
                AdsMediaSource.this.onContentSourceInfoRefreshed(timeline, manifest);
            }
        });
        this.mainHandler.post(new Runnable() {
            public void run() {
                AdsMediaSource.this.adsLoader.attachPlayer(player2, AdsMediaSource.this.componentListener, AdsMediaSource.this.adUiViewGroup);
            }
        });
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
        this.contentMediaSource.maybeThrowSourceInfoRefreshError();
        for (MediaSource[] mediaSources : this.adGroupMediaSources) {
            for (MediaSource mediaSource : r5[r4]) {
                if (mediaSource != null) {
                    mediaSource.maybeThrowSourceInfoRefreshError();
                }
            }
        }
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator) {
        if (this.adPlaybackState.adGroupCount <= 0 || !id.isAd()) {
            DeferredMediaPeriod mediaPeriod = new DeferredMediaPeriod(this.contentMediaSource, id, allocator);
            mediaPeriod.createPeriod();
            return mediaPeriod;
        }
        final int adGroupIndex = id.adGroupIndex;
        final int adIndexInAdGroup = id.adIndexInAdGroup;
        if (this.adGroupMediaSources[adGroupIndex].length <= adIndexInAdGroup) {
            final MediaSource adMediaSource = this.adMediaSourceFactory.createMediaSource(this.adPlaybackState.adUris[id.adGroupIndex][id.adIndexInAdGroup], this.eventHandler, this.eventListener);
            int oldAdCount = this.adGroupMediaSources[id.adGroupIndex].length;
            if (adIndexInAdGroup >= oldAdCount) {
                int adCount = adIndexInAdGroup + 1;
                this.adGroupMediaSources[adGroupIndex] = (MediaSource[]) Arrays.copyOf(this.adGroupMediaSources[adGroupIndex], adCount);
                this.adDurationsUs[adGroupIndex] = Arrays.copyOf(this.adDurationsUs[adGroupIndex], adCount);
                Arrays.fill(this.adDurationsUs[adGroupIndex], oldAdCount, adCount, C.TIME_UNSET);
            }
            this.adGroupMediaSources[adGroupIndex][adIndexInAdGroup] = adMediaSource;
            this.deferredMediaPeriodByAdMediaSource.put(adMediaSource, new ArrayList());
            adMediaSource.prepareSource(this.player, false, new MediaSource.Listener() {
                public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, @Nullable Object manifest) {
                    AdsMediaSource.this.onAdSourceInfoRefreshed(adMediaSource, adGroupIndex, adIndexInAdGroup, timeline);
                }
            });
        }
        MediaSource mediaSource = this.adGroupMediaSources[adGroupIndex][adIndexInAdGroup];
        DeferredMediaPeriod deferredMediaPeriod = new DeferredMediaPeriod(mediaSource, new MediaSource.MediaPeriodId(0), allocator);
        List<DeferredMediaPeriod> mediaPeriods = this.deferredMediaPeriodByAdMediaSource.get(mediaSource);
        if (mediaPeriods == null) {
            deferredMediaPeriod.createPeriod();
            return deferredMediaPeriod;
        }
        mediaPeriods.add(deferredMediaPeriod);
        return deferredMediaPeriod;
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((DeferredMediaPeriod) mediaPeriod).releasePeriod();
    }

    public void releaseSource() {
        this.released = true;
        this.contentMediaSource.releaseSource();
        for (MediaSource[] mediaSources : this.adGroupMediaSources) {
            for (MediaSource mediaSource : r5[r4]) {
                if (mediaSource != null) {
                    mediaSource.releaseSource();
                }
            }
        }
        this.mainHandler.post(new Runnable() {
            public void run() {
                AdsMediaSource.this.adsLoader.detachPlayer();
            }
        });
    }

    /* access modifiers changed from: private */
    public void onAdPlaybackState(AdPlaybackState adPlaybackState2) {
        if (this.adPlaybackState == null) {
            this.adGroupMediaSources = new MediaSource[adPlaybackState2.adGroupCount][];
            Arrays.fill(this.adGroupMediaSources, new MediaSource[0]);
            this.adDurationsUs = new long[adPlaybackState2.adGroupCount][];
            Arrays.fill(this.adDurationsUs, new long[0]);
        }
        this.adPlaybackState = adPlaybackState2;
        maybeUpdateSourceInfo();
    }

    /* access modifiers changed from: private */
    public void onLoadError(final IOException error) {
        Log.w(TAG, "Ad load error", error);
        if (this.eventHandler != null && this.eventListener != null) {
            this.eventHandler.post(new Runnable() {
                public void run() {
                    if (!AdsMediaSource.this.released) {
                        AdsMediaSource.this.eventListener.onAdLoadError(error);
                    }
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void onContentSourceInfoRefreshed(Timeline timeline, Object manifest) {
        this.contentTimeline = timeline;
        this.contentManifest = manifest;
        maybeUpdateSourceInfo();
    }

    /* access modifiers changed from: private */
    public void onAdSourceInfoRefreshed(MediaSource mediaSource, int adGroupIndex, int adIndexInAdGroup, Timeline timeline) {
        boolean z = true;
        if (timeline.getPeriodCount() != 1) {
            z = false;
        }
        Assertions.checkArgument(z);
        this.adDurationsUs[adGroupIndex][adIndexInAdGroup] = timeline.getPeriod(0, this.period).getDurationUs();
        if (this.deferredMediaPeriodByAdMediaSource.containsKey(mediaSource)) {
            List<DeferredMediaPeriod> mediaPeriods = this.deferredMediaPeriodByAdMediaSource.get(mediaSource);
            for (int i = 0; i < mediaPeriods.size(); i++) {
                mediaPeriods.get(i).createPeriod();
            }
            this.deferredMediaPeriodByAdMediaSource.remove(mediaSource);
        }
        maybeUpdateSourceInfo();
    }

    private void maybeUpdateSourceInfo() {
        if (this.adPlaybackState != null && this.contentTimeline != null) {
            this.listener.onSourceInfoRefreshed(this, this.adPlaybackState.adGroupCount == 0 ? this.contentTimeline : new SinglePeriodAdTimeline(this.contentTimeline, this.adPlaybackState.adGroupTimesUs, this.adPlaybackState.adCounts, this.adPlaybackState.adsLoadedCounts, this.adPlaybackState.adsPlayedCounts, this.adDurationsUs, this.adPlaybackState.adResumePositionUs, this.adPlaybackState.contentDurationUs), this.contentManifest);
        }
    }

    private final class ComponentListener implements AdsLoader.EventListener {
        private ComponentListener() {
        }

        public void onAdPlaybackState(final AdPlaybackState adPlaybackState) {
            if (!AdsMediaSource.this.released) {
                AdsMediaSource.this.playerHandler.post(new Runnable() {
                    public void run() {
                        if (!AdsMediaSource.this.released) {
                            AdsMediaSource.this.onAdPlaybackState(adPlaybackState);
                        }
                    }
                });
            }
        }

        public void onAdClicked() {
            if (AdsMediaSource.this.eventHandler != null && AdsMediaSource.this.eventListener != null) {
                AdsMediaSource.this.eventHandler.post(new Runnable() {
                    public void run() {
                        if (!AdsMediaSource.this.released) {
                            AdsMediaSource.this.eventListener.onAdClicked();
                        }
                    }
                });
            }
        }

        public void onAdTapped() {
            if (AdsMediaSource.this.eventHandler != null && AdsMediaSource.this.eventListener != null) {
                AdsMediaSource.this.eventHandler.post(new Runnable() {
                    public void run() {
                        if (!AdsMediaSource.this.released) {
                            AdsMediaSource.this.eventListener.onAdTapped();
                        }
                    }
                });
            }
        }

        public void onLoadError(final IOException error) {
            if (!AdsMediaSource.this.released) {
                AdsMediaSource.this.playerHandler.post(new Runnable() {
                    public void run() {
                        if (!AdsMediaSource.this.released) {
                            AdsMediaSource.this.onLoadError(error);
                        }
                    }
                });
            }
        }
    }
}
