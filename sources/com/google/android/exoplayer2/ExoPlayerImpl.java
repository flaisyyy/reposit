package com.google.android.exoplayer2;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

final class ExoPlayerImpl implements ExoPlayer {
    private static final String TAG = "ExoPlayerImpl";
    private final TrackSelectionArray emptyTrackSelections;
    private final Handler eventHandler;
    private final ExoPlayerImplInternal internalPlayer;
    private boolean isLoading;
    private final CopyOnWriteArraySet<Player.EventListener> listeners;
    private int maskingPeriodIndex;
    private int maskingWindowIndex;
    private long maskingWindowPositionMs;
    private int pendingPrepareAcks;
    private int pendingSeekAcks;
    private final Timeline.Period period;
    private boolean playWhenReady;
    private PlaybackInfo playbackInfo;
    private PlaybackParameters playbackParameters;
    private int playbackState;
    private final Renderer[] renderers;
    private int repeatMode;
    private boolean shuffleModeEnabled;
    private TrackGroupArray trackGroups;
    private TrackSelectionArray trackSelections;
    private final TrackSelector trackSelector;
    private boolean tracksSelected;
    private final Timeline.Window window;

    @SuppressLint({"HandlerLeak"})
    public ExoPlayerImpl(Renderer[] renderers2, TrackSelector trackSelector2, LoadControl loadControl) {
        boolean z;
        Log.i(TAG, "Init " + Integer.toHexString(System.identityHashCode(this)) + " [" + ExoPlayerLibraryInfo.VERSION_SLASHY + "] [" + Util.DEVICE_DEBUG_INFO + "]");
        if (renderers2.length > 0) {
            z = true;
        } else {
            z = false;
        }
        Assertions.checkState(z);
        this.renderers = (Renderer[]) Assertions.checkNotNull(renderers2);
        this.trackSelector = (TrackSelector) Assertions.checkNotNull(trackSelector2);
        this.playWhenReady = false;
        this.repeatMode = 0;
        this.shuffleModeEnabled = false;
        this.playbackState = 1;
        this.listeners = new CopyOnWriteArraySet<>();
        this.emptyTrackSelections = new TrackSelectionArray(new TrackSelection[renderers2.length]);
        this.window = new Timeline.Window();
        this.period = new Timeline.Period();
        this.trackGroups = TrackGroupArray.EMPTY;
        this.trackSelections = this.emptyTrackSelections;
        this.playbackParameters = PlaybackParameters.DEFAULT;
        this.eventHandler = new Handler(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                ExoPlayerImpl.this.handleEvent(msg);
            }
        };
        this.playbackInfo = new PlaybackInfo(Timeline.EMPTY, (Object) null, 0, 0);
        this.internalPlayer = new ExoPlayerImplInternal(renderers2, trackSelector2, loadControl, this.playWhenReady, this.repeatMode, this.shuffleModeEnabled, this.eventHandler, this);
    }

    public Looper getPlaybackLooper() {
        return this.internalPlayer.getPlaybackLooper();
    }

    public void addListener(Player.EventListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Player.EventListener listener) {
        this.listeners.remove(listener);
    }

    public int getPlaybackState() {
        return this.playbackState;
    }

    public void prepare(MediaSource mediaSource) {
        prepare(mediaSource, true, true);
    }

    public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
        if (!resetPosition) {
            this.maskingWindowIndex = getCurrentWindowIndex();
            this.maskingPeriodIndex = getCurrentPeriodIndex();
            this.maskingWindowPositionMs = getCurrentPosition();
        } else {
            this.maskingWindowIndex = 0;
            this.maskingPeriodIndex = 0;
            this.maskingWindowPositionMs = 0;
        }
        if (resetState) {
            if (!this.playbackInfo.timeline.isEmpty() || this.playbackInfo.manifest != null) {
                this.playbackInfo = this.playbackInfo.copyWithTimeline(Timeline.EMPTY, (Object) null);
                Iterator<Player.EventListener> it = this.listeners.iterator();
                while (it.hasNext()) {
                    it.next().onTimelineChanged(this.playbackInfo.timeline, this.playbackInfo.manifest);
                }
            }
            if (this.tracksSelected) {
                this.tracksSelected = false;
                this.trackGroups = TrackGroupArray.EMPTY;
                this.trackSelections = this.emptyTrackSelections;
                this.trackSelector.onSelectionActivated((Object) null);
                Iterator<Player.EventListener> it2 = this.listeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onTracksChanged(this.trackGroups, this.trackSelections);
                }
            }
        }
        this.pendingPrepareAcks++;
        this.internalPlayer.prepare(mediaSource, resetPosition);
    }

    public void setPlayWhenReady(boolean playWhenReady2) {
        if (this.playWhenReady != playWhenReady2) {
            this.playWhenReady = playWhenReady2;
            this.internalPlayer.setPlayWhenReady(playWhenReady2);
            Iterator<Player.EventListener> it = this.listeners.iterator();
            while (it.hasNext()) {
                it.next().onPlayerStateChanged(playWhenReady2, this.playbackState);
            }
        }
    }

    public boolean getPlayWhenReady() {
        return this.playWhenReady;
    }

    public void setRepeatMode(int repeatMode2) {
        if (this.repeatMode != repeatMode2) {
            this.repeatMode = repeatMode2;
            this.internalPlayer.setRepeatMode(repeatMode2);
            Iterator<Player.EventListener> it = this.listeners.iterator();
            while (it.hasNext()) {
                it.next().onRepeatModeChanged(repeatMode2);
            }
        }
    }

    public int getRepeatMode() {
        return this.repeatMode;
    }

    public void setShuffleModeEnabled(boolean shuffleModeEnabled2) {
        if (this.shuffleModeEnabled != shuffleModeEnabled2) {
            this.shuffleModeEnabled = shuffleModeEnabled2;
            this.internalPlayer.setShuffleModeEnabled(shuffleModeEnabled2);
            Iterator<Player.EventListener> it = this.listeners.iterator();
            while (it.hasNext()) {
                it.next().onShuffleModeEnabledChanged(shuffleModeEnabled2);
            }
        }
    }

    public boolean getShuffleModeEnabled() {
        return this.shuffleModeEnabled;
    }

    public boolean isLoading() {
        return this.isLoading;
    }

    public void seekToDefaultPosition() {
        seekToDefaultPosition(getCurrentWindowIndex());
    }

    public void seekToDefaultPosition(int windowIndex) {
        seekTo(windowIndex, C.TIME_UNSET);
    }

    public void seekTo(long positionMs) {
        seekTo(getCurrentWindowIndex(), positionMs);
    }

    public void seekTo(int windowIndex, long positionMs) {
        long windowPositionUs;
        long j;
        Timeline timeline = this.playbackInfo.timeline;
        if (windowIndex < 0 || (!timeline.isEmpty() && windowIndex >= timeline.getWindowCount())) {
            throw new IllegalSeekPositionException(timeline, windowIndex, positionMs);
        } else if (isPlayingAd()) {
            Log.w(TAG, "seekTo ignored because an ad is playing");
            if (this.pendingSeekAcks == 0) {
                Iterator<Player.EventListener> it = this.listeners.iterator();
                while (it.hasNext()) {
                    it.next().onSeekProcessed();
                }
            }
        } else {
            this.pendingSeekAcks++;
            this.maskingWindowIndex = windowIndex;
            if (timeline.isEmpty()) {
                if (positionMs == C.TIME_UNSET) {
                    j = 0;
                } else {
                    j = positionMs;
                }
                this.maskingWindowPositionMs = j;
                this.maskingPeriodIndex = 0;
            } else {
                timeline.getWindow(windowIndex, this.window);
                if (positionMs == C.TIME_UNSET) {
                    windowPositionUs = this.window.getDefaultPositionUs();
                } else {
                    windowPositionUs = C.msToUs(positionMs);
                }
                int periodIndex = this.window.firstPeriodIndex;
                long periodPositionUs = this.window.getPositionInFirstPeriodUs() + windowPositionUs;
                long periodDurationUs = timeline.getPeriod(periodIndex, this.period).getDurationUs();
                while (periodDurationUs != C.TIME_UNSET && periodPositionUs >= periodDurationUs && periodIndex < this.window.lastPeriodIndex) {
                    periodPositionUs -= periodDurationUs;
                    periodIndex++;
                    periodDurationUs = timeline.getPeriod(periodIndex, this.period).getDurationUs();
                }
                this.maskingWindowPositionMs = C.usToMs(windowPositionUs);
                this.maskingPeriodIndex = periodIndex;
            }
            this.internalPlayer.seekTo(timeline, windowIndex, C.msToUs(positionMs));
            Iterator<Player.EventListener> it2 = this.listeners.iterator();
            while (it2.hasNext()) {
                it2.next().onPositionDiscontinuity(1);
            }
        }
    }

    public void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters2) {
        if (playbackParameters2 == null) {
            playbackParameters2 = PlaybackParameters.DEFAULT;
        }
        this.internalPlayer.setPlaybackParameters(playbackParameters2);
    }

    public PlaybackParameters getPlaybackParameters() {
        return this.playbackParameters;
    }

    public void stop() {
        this.internalPlayer.stop();
    }

    public void release() {
        Log.i(TAG, "Release " + Integer.toHexString(System.identityHashCode(this)) + " [" + ExoPlayerLibraryInfo.VERSION_SLASHY + "] [" + Util.DEVICE_DEBUG_INFO + "] [" + ExoPlayerLibraryInfo.registeredModules() + "]");
        this.internalPlayer.release();
        this.eventHandler.removeCallbacksAndMessages((Object) null);
    }

    public void sendMessages(ExoPlayer.ExoPlayerMessage... messages) {
        this.internalPlayer.sendMessages(messages);
    }

    public void blockingSendMessages(ExoPlayer.ExoPlayerMessage... messages) {
        this.internalPlayer.blockingSendMessages(messages);
    }

    public int getCurrentPeriodIndex() {
        if (shouldMaskPosition()) {
            return this.maskingPeriodIndex;
        }
        return this.playbackInfo.periodId.periodIndex;
    }

    public int getCurrentWindowIndex() {
        if (shouldMaskPosition()) {
            return this.maskingWindowIndex;
        }
        return this.playbackInfo.timeline.getPeriod(this.playbackInfo.periodId.periodIndex, this.period).windowIndex;
    }

    public int getNextWindowIndex() {
        Timeline timeline = this.playbackInfo.timeline;
        if (timeline.isEmpty()) {
            return -1;
        }
        return timeline.getNextWindowIndex(getCurrentWindowIndex(), this.repeatMode, this.shuffleModeEnabled);
    }

    public int getPreviousWindowIndex() {
        Timeline timeline = this.playbackInfo.timeline;
        if (timeline.isEmpty()) {
            return -1;
        }
        return timeline.getPreviousWindowIndex(getCurrentWindowIndex(), this.repeatMode, this.shuffleModeEnabled);
    }

    public long getDuration() {
        Timeline timeline = this.playbackInfo.timeline;
        if (timeline.isEmpty()) {
            return C.TIME_UNSET;
        }
        if (!isPlayingAd()) {
            return timeline.getWindow(getCurrentWindowIndex(), this.window).getDurationMs();
        }
        MediaSource.MediaPeriodId periodId = this.playbackInfo.periodId;
        timeline.getPeriod(periodId.periodIndex, this.period);
        return C.usToMs(this.period.getAdDurationUs(periodId.adGroupIndex, periodId.adIndexInAdGroup));
    }

    public long getCurrentPosition() {
        if (shouldMaskPosition()) {
            return this.maskingWindowPositionMs;
        }
        return playbackInfoPositionUsToWindowPositionMs(this.playbackInfo.positionUs);
    }

    public long getBufferedPosition() {
        if (shouldMaskPosition()) {
            return this.maskingWindowPositionMs;
        }
        return playbackInfoPositionUsToWindowPositionMs(this.playbackInfo.bufferedPositionUs);
    }

    public int getBufferedPercentage() {
        long position = getBufferedPosition();
        long duration = getDuration();
        if (position == C.TIME_UNSET || duration == C.TIME_UNSET) {
            return 0;
        }
        if (duration != 0) {
            return Util.constrainValue((int) ((100 * position) / duration), 0, 100);
        }
        return 100;
    }

    public boolean isCurrentWindowDynamic() {
        Timeline timeline = this.playbackInfo.timeline;
        return !timeline.isEmpty() && timeline.getWindow(getCurrentWindowIndex(), this.window).isDynamic;
    }

    public boolean isCurrentWindowSeekable() {
        Timeline timeline = this.playbackInfo.timeline;
        return !timeline.isEmpty() && timeline.getWindow(getCurrentWindowIndex(), this.window).isSeekable;
    }

    public boolean isPlayingAd() {
        return !shouldMaskPosition() && this.playbackInfo.periodId.isAd();
    }

    public int getCurrentAdGroupIndex() {
        if (isPlayingAd()) {
            return this.playbackInfo.periodId.adGroupIndex;
        }
        return -1;
    }

    public int getCurrentAdIndexInAdGroup() {
        if (isPlayingAd()) {
            return this.playbackInfo.periodId.adIndexInAdGroup;
        }
        return -1;
    }

    public long getContentPosition() {
        if (!isPlayingAd()) {
            return getCurrentPosition();
        }
        this.playbackInfo.timeline.getPeriod(this.playbackInfo.periodId.periodIndex, this.period);
        return this.period.getPositionInWindowMs() + C.usToMs(this.playbackInfo.contentPositionUs);
    }

    public int getRendererCount() {
        return this.renderers.length;
    }

    public int getRendererType(int index) {
        return this.renderers[index].getTrackType();
    }

    public TrackGroupArray getCurrentTrackGroups() {
        return this.trackGroups;
    }

    public TrackSelectionArray getCurrentTrackSelections() {
        return this.trackSelections;
    }

    public Timeline getCurrentTimeline() {
        return this.playbackInfo.timeline;
    }

    public Object getCurrentManifest() {
        return this.playbackInfo.manifest;
    }

    /* access modifiers changed from: package-private */
    public void handleEvent(Message msg) {
        switch (msg.what) {
            case 0:
                this.playbackState = msg.arg1;
                Iterator<Player.EventListener> it = this.listeners.iterator();
                while (it.hasNext()) {
                    it.next().onPlayerStateChanged(this.playWhenReady, this.playbackState);
                }
                return;
            case 1:
                this.isLoading = msg.arg1 != 0;
                Iterator<Player.EventListener> it2 = this.listeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onLoadingChanged(this.isLoading);
                }
                return;
            case 2:
                if (this.pendingPrepareAcks == 0) {
                    TrackSelectorResult trackSelectorResult = (TrackSelectorResult) msg.obj;
                    this.tracksSelected = true;
                    this.trackGroups = trackSelectorResult.groups;
                    this.trackSelections = trackSelectorResult.selections;
                    this.trackSelector.onSelectionActivated(trackSelectorResult.info);
                    Iterator<Player.EventListener> it3 = this.listeners.iterator();
                    while (it3.hasNext()) {
                        it3.next().onTracksChanged(this.trackGroups, this.trackSelections);
                    }
                    return;
                }
                return;
            case 3:
                handlePlaybackInfo((PlaybackInfo) msg.obj, 0, 1, msg.arg1 != 0, 2);
                return;
            case 4:
                handlePlaybackInfo((PlaybackInfo) msg.obj, 0, 0, true, msg.arg1);
                return;
            case 5:
                handlePlaybackInfo((PlaybackInfo) msg.obj, msg.arg1, msg.arg2, false, 3);
                return;
            case 6:
                PlaybackParameters playbackParameters2 = (PlaybackParameters) msg.obj;
                if (!this.playbackParameters.equals(playbackParameters2)) {
                    this.playbackParameters = playbackParameters2;
                    Iterator<Player.EventListener> it4 = this.listeners.iterator();
                    while (it4.hasNext()) {
                        it4.next().onPlaybackParametersChanged(playbackParameters2);
                    }
                    return;
                }
                return;
            case 7:
                ExoPlaybackException exception = (ExoPlaybackException) msg.obj;
                Iterator<Player.EventListener> it5 = this.listeners.iterator();
                while (it5.hasNext()) {
                    it5.next().onPlayerError(exception);
                }
                return;
            default:
                throw new IllegalStateException();
        }
    }

    private void handlePlaybackInfo(PlaybackInfo playbackInfo2, int prepareAcks, int seekAcks, boolean positionDiscontinuity, int positionDiscontinuityReason) {
        boolean timelineOrManifestChanged;
        Assertions.checkNotNull(playbackInfo2.timeline);
        this.pendingPrepareAcks -= prepareAcks;
        this.pendingSeekAcks -= seekAcks;
        if (this.pendingPrepareAcks == 0 && this.pendingSeekAcks == 0) {
            if (this.playbackInfo.timeline == playbackInfo2.timeline && this.playbackInfo.manifest == playbackInfo2.manifest) {
                timelineOrManifestChanged = false;
            } else {
                timelineOrManifestChanged = true;
            }
            this.playbackInfo = playbackInfo2;
            if (playbackInfo2.timeline.isEmpty()) {
                this.maskingPeriodIndex = 0;
                this.maskingWindowIndex = 0;
                this.maskingWindowPositionMs = 0;
            }
            if (timelineOrManifestChanged) {
                Iterator<Player.EventListener> it = this.listeners.iterator();
                while (it.hasNext()) {
                    it.next().onTimelineChanged(playbackInfo2.timeline, playbackInfo2.manifest);
                }
            }
            if (positionDiscontinuity) {
                Iterator<Player.EventListener> it2 = this.listeners.iterator();
                while (it2.hasNext()) {
                    it2.next().onPositionDiscontinuity(positionDiscontinuityReason);
                }
            }
        }
        if (this.pendingSeekAcks == 0 && seekAcks > 0) {
            Iterator<Player.EventListener> it3 = this.listeners.iterator();
            while (it3.hasNext()) {
                it3.next().onSeekProcessed();
            }
        }
    }

    private long playbackInfoPositionUsToWindowPositionMs(long positionUs) {
        long positionMs = C.usToMs(positionUs);
        if (this.playbackInfo.periodId.isAd()) {
            return positionMs;
        }
        this.playbackInfo.timeline.getPeriod(this.playbackInfo.periodId.periodIndex, this.period);
        return positionMs + this.period.getPositionInWindowMs();
    }

    private boolean shouldMaskPosition() {
        return this.playbackInfo.timeline.isEmpty() || this.pendingSeekAcks > 0 || this.pendingPrepareAcks > 0;
    }
}
