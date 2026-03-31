package com.google.android.exoplayer2;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaPeriodInfoSequence;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ClippingMediaPeriod;
import com.google.android.exoplayer2.source.EmptySampleStream;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MediaClock;
import com.google.android.exoplayer2.util.StandaloneMediaClock;
import com.google.android.exoplayer2.util.TraceUtil;
import java.io.IOException;

final class ExoPlayerImplInternal implements Handler.Callback, MediaPeriod.Callback, TrackSelector.InvalidationListener, MediaSource.Listener {
    private static final int IDLE_INTERVAL_MS = 1000;
    private static final int MAXIMUM_BUFFER_AHEAD_PERIODS = 100;
    private static final int MSG_CUSTOM = 11;
    private static final int MSG_DO_SOME_WORK = 2;
    public static final int MSG_ERROR = 7;
    public static final int MSG_LOADING_CHANGED = 1;
    private static final int MSG_PERIOD_PREPARED = 8;
    public static final int MSG_PLAYBACK_PARAMETERS_CHANGED = 6;
    public static final int MSG_POSITION_DISCONTINUITY = 4;
    private static final int MSG_PREPARE = 0;
    private static final int MSG_REFRESH_SOURCE_INFO = 7;
    private static final int MSG_RELEASE = 6;
    public static final int MSG_SEEK_ACK = 3;
    private static final int MSG_SEEK_TO = 3;
    private static final int MSG_SET_PLAYBACK_PARAMETERS = 4;
    private static final int MSG_SET_PLAY_WHEN_READY = 1;
    private static final int MSG_SET_REPEAT_MODE = 12;
    private static final int MSG_SET_SHUFFLE_ENABLED = 13;
    private static final int MSG_SOURCE_CONTINUE_LOADING_REQUESTED = 9;
    public static final int MSG_SOURCE_INFO_REFRESHED = 5;
    public static final int MSG_STATE_CHANGED = 0;
    private static final int MSG_STOP = 5;
    public static final int MSG_TRACKS_CHANGED = 2;
    private static final int MSG_TRACK_SELECTION_INVALIDATED = 10;
    private static final int PREPARING_SOURCE_INTERVAL_MS = 10;
    private static final int RENDERER_TIMESTAMP_OFFSET_US = 60000000;
    private static final int RENDERING_INTERVAL_MS = 10;
    private static final String TAG = "ExoPlayerImplInternal";
    private int customMessagesProcessed;
    private int customMessagesSent;
    private long elapsedRealtimeUs;
    private Renderer[] enabledRenderers;
    private final Handler eventHandler;
    private final Handler handler;
    private final HandlerThread internalPlaybackThread;
    private boolean isLoading;
    private final LoadControl loadControl;
    private MediaPeriodHolder loadingPeriodHolder;
    private final MediaPeriodInfoSequence mediaPeriodInfoSequence;
    private MediaSource mediaSource;
    private int pendingInitialSeekCount;
    private int pendingPrepareCount;
    private SeekPosition pendingSeekPosition;
    private final Timeline.Period period;
    private boolean playWhenReady;
    private PlaybackInfo playbackInfo;
    private PlaybackParameters playbackParameters;
    private final ExoPlayer player;
    private MediaPeriodHolder playingPeriodHolder;
    private MediaPeriodHolder readingPeriodHolder;
    private boolean rebuffering;
    private boolean released;
    private final RendererCapabilities[] rendererCapabilities;
    private MediaClock rendererMediaClock;
    private Renderer rendererMediaClockSource;
    private long rendererPositionUs;
    private final Renderer[] renderers;
    private int repeatMode;
    private boolean shuffleModeEnabled;
    private final StandaloneMediaClock standaloneMediaClock;
    private int state = 1;
    private final TrackSelector trackSelector;
    private final Timeline.Window window;

    public ExoPlayerImplInternal(Renderer[] renderers2, TrackSelector trackSelector2, LoadControl loadControl2, boolean playWhenReady2, int repeatMode2, boolean shuffleModeEnabled2, Handler eventHandler2, ExoPlayer player2) {
        this.renderers = renderers2;
        this.trackSelector = trackSelector2;
        this.loadControl = loadControl2;
        this.playWhenReady = playWhenReady2;
        this.repeatMode = repeatMode2;
        this.shuffleModeEnabled = shuffleModeEnabled2;
        this.eventHandler = eventHandler2;
        this.player = player2;
        this.playbackInfo = new PlaybackInfo((Timeline) null, (Object) null, 0, C.TIME_UNSET);
        this.rendererCapabilities = new RendererCapabilities[renderers2.length];
        for (int i = 0; i < renderers2.length; i++) {
            renderers2[i].setIndex(i);
            this.rendererCapabilities[i] = renderers2[i].getCapabilities();
        }
        this.standaloneMediaClock = new StandaloneMediaClock();
        this.enabledRenderers = new Renderer[0];
        this.window = new Timeline.Window();
        this.period = new Timeline.Period();
        this.mediaPeriodInfoSequence = new MediaPeriodInfoSequence();
        trackSelector2.init(this);
        this.playbackParameters = PlaybackParameters.DEFAULT;
        this.internalPlaybackThread = new HandlerThread("ExoPlayerImplInternal:Handler", -16);
        this.internalPlaybackThread.start();
        this.handler = new Handler(this.internalPlaybackThread.getLooper(), this);
    }

    public void prepare(MediaSource mediaSource2, boolean resetPosition) {
        int i;
        Handler handler2 = this.handler;
        if (resetPosition) {
            i = 1;
        } else {
            i = 0;
        }
        handler2.obtainMessage(0, i, 0, mediaSource2).sendToTarget();
    }

    public void setPlayWhenReady(boolean playWhenReady2) {
        int i;
        Handler handler2 = this.handler;
        if (playWhenReady2) {
            i = 1;
        } else {
            i = 0;
        }
        handler2.obtainMessage(1, i, 0).sendToTarget();
    }

    public void setRepeatMode(int repeatMode2) {
        this.handler.obtainMessage(12, repeatMode2, 0).sendToTarget();
    }

    public void setShuffleModeEnabled(boolean shuffleModeEnabled2) {
        int i;
        Handler handler2 = this.handler;
        if (shuffleModeEnabled2) {
            i = 1;
        } else {
            i = 0;
        }
        handler2.obtainMessage(13, i, 0).sendToTarget();
    }

    public void seekTo(Timeline timeline, int windowIndex, long positionUs) {
        this.handler.obtainMessage(3, new SeekPosition(timeline, windowIndex, positionUs)).sendToTarget();
    }

    public void setPlaybackParameters(PlaybackParameters playbackParameters2) {
        this.handler.obtainMessage(4, playbackParameters2).sendToTarget();
    }

    public void stop() {
        this.handler.sendEmptyMessage(5);
    }

    public void sendMessages(ExoPlayer.ExoPlayerMessage... messages) {
        if (this.released) {
            Log.w(TAG, "Ignoring messages sent after release.");
            return;
        }
        this.customMessagesSent++;
        this.handler.obtainMessage(11, messages).sendToTarget();
    }

    public synchronized void blockingSendMessages(ExoPlayer.ExoPlayerMessage... messages) {
        if (this.released) {
            Log.w(TAG, "Ignoring messages sent after release.");
        } else {
            int messageNumber = this.customMessagesSent;
            this.customMessagesSent = messageNumber + 1;
            this.handler.obtainMessage(11, messages).sendToTarget();
            boolean wasInterrupted = false;
            while (this.customMessagesProcessed <= messageNumber) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void release() {
        if (!this.released) {
            this.handler.sendEmptyMessage(6);
            boolean wasInterrupted = false;
            while (!this.released) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public Looper getPlaybackLooper() {
        return this.internalPlaybackThread.getLooper();
    }

    public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, Object manifest) {
        this.handler.obtainMessage(7, new MediaSourceRefreshInfo(source, timeline, manifest)).sendToTarget();
    }

    public void onPrepared(MediaPeriod source) {
        this.handler.obtainMessage(8, source).sendToTarget();
    }

    public void onContinueLoadingRequested(MediaPeriod source) {
        this.handler.obtainMessage(9, source).sendToTarget();
    }

    public void onTrackSelectionsInvalidated() {
        this.handler.sendEmptyMessage(10);
    }

    public boolean handleMessage(Message msg) {
        boolean z = false;
        try {
            switch (msg.what) {
                case 0:
                    MediaSource mediaSource2 = (MediaSource) msg.obj;
                    if (msg.arg1 != 0) {
                        z = true;
                    }
                    prepareInternal(mediaSource2, z);
                    return true;
                case 1:
                    if (msg.arg1 != 0) {
                        z = true;
                    }
                    setPlayWhenReadyInternal(z);
                    return true;
                case 2:
                    doSomeWork();
                    return true;
                case 3:
                    seekToInternal((SeekPosition) msg.obj);
                    return true;
                case 4:
                    setPlaybackParametersInternal((PlaybackParameters) msg.obj);
                    return true;
                case 5:
                    stopInternal();
                    return true;
                case 6:
                    releaseInternal();
                    return true;
                case 7:
                    handleSourceInfoRefreshed((MediaSourceRefreshInfo) msg.obj);
                    return true;
                case 8:
                    handlePeriodPrepared((MediaPeriod) msg.obj);
                    return true;
                case 9:
                    handleContinueLoadingRequested((MediaPeriod) msg.obj);
                    return true;
                case 10:
                    reselectTracksInternal();
                    return true;
                case 11:
                    sendMessagesInternal((ExoPlayer.ExoPlayerMessage[]) msg.obj);
                    return true;
                case 12:
                    setRepeatModeInternal(msg.arg1);
                    return true;
                case 13:
                    if (msg.arg1 != 0) {
                        z = true;
                    }
                    setShuffleModeEnabledInternal(z);
                    return true;
                default:
                    return false;
            }
        } catch (ExoPlaybackException e) {
            Log.e(TAG, "Renderer error.", e);
            this.eventHandler.obtainMessage(7, e).sendToTarget();
            stopInternal();
            return true;
        } catch (IOException e2) {
            Log.e(TAG, "Source error.", e2);
            this.eventHandler.obtainMessage(7, ExoPlaybackException.createForSource(e2)).sendToTarget();
            stopInternal();
            return true;
        } catch (RuntimeException e3) {
            Log.e(TAG, "Internal runtime error.", e3);
            this.eventHandler.obtainMessage(7, ExoPlaybackException.createForUnexpected(e3)).sendToTarget();
            stopInternal();
            return true;
        }
    }

    private void setState(int state2) {
        if (this.state != state2) {
            this.state = state2;
            this.eventHandler.obtainMessage(0, state2, 0).sendToTarget();
        }
    }

    private void setIsLoading(boolean isLoading2) {
        int i;
        if (this.isLoading != isLoading2) {
            this.isLoading = isLoading2;
            Handler handler2 = this.eventHandler;
            if (isLoading2) {
                i = 1;
            } else {
                i = 0;
            }
            handler2.obtainMessage(1, i, 0).sendToTarget();
        }
    }

    private void prepareInternal(MediaSource mediaSource2, boolean resetPosition) {
        this.pendingPrepareCount++;
        resetInternal(true);
        this.loadControl.onPrepared();
        if (resetPosition) {
            this.playbackInfo = new PlaybackInfo((Timeline) null, (Object) null, 0, C.TIME_UNSET);
        } else {
            this.playbackInfo = new PlaybackInfo((Timeline) null, (Object) null, this.playbackInfo.periodId, this.playbackInfo.positionUs, this.playbackInfo.contentPositionUs);
        }
        this.mediaSource = mediaSource2;
        mediaSource2.prepareSource(this.player, true, this);
        setState(2);
        this.handler.sendEmptyMessage(2);
    }

    private void setPlayWhenReadyInternal(boolean playWhenReady2) throws ExoPlaybackException {
        this.rebuffering = false;
        this.playWhenReady = playWhenReady2;
        if (!playWhenReady2) {
            stopRenderers();
            updatePlaybackPositions();
        } else if (this.state == 3) {
            startRenderers();
            this.handler.sendEmptyMessage(2);
        } else if (this.state == 2) {
            this.handler.sendEmptyMessage(2);
        }
    }

    private void setRepeatModeInternal(int repeatMode2) throws ExoPlaybackException {
        this.repeatMode = repeatMode2;
        this.mediaPeriodInfoSequence.setRepeatMode(repeatMode2);
        validateExistingPeriodHolders();
    }

    private void setShuffleModeEnabledInternal(boolean shuffleModeEnabled2) throws ExoPlaybackException {
        this.shuffleModeEnabled = shuffleModeEnabled2;
        this.mediaPeriodInfoSequence.setShuffleModeEnabled(shuffleModeEnabled2);
        validateExistingPeriodHolders();
    }

    private void validateExistingPeriodHolders() throws ExoPlaybackException {
        int readingPeriodHolderIndex;
        boolean seenLoadingPeriodHolder;
        boolean seenReadingPeriodHolder = true;
        MediaPeriodHolder lastValidPeriodHolder = this.playingPeriodHolder != null ? this.playingPeriodHolder : this.loadingPeriodHolder;
        if (lastValidPeriodHolder != null) {
            while (true) {
                int nextPeriodIndex = this.playbackInfo.timeline.getNextPeriodIndex(lastValidPeriodHolder.info.id.periodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
                while (lastValidPeriodHolder.next != null && !lastValidPeriodHolder.info.isLastInTimelinePeriod) {
                    lastValidPeriodHolder = lastValidPeriodHolder.next;
                }
                if (nextPeriodIndex == -1 || lastValidPeriodHolder.next == null || lastValidPeriodHolder.next.info.id.periodIndex != nextPeriodIndex) {
                    int loadingPeriodHolderIndex = this.loadingPeriodHolder.index;
                } else {
                    lastValidPeriodHolder = lastValidPeriodHolder.next;
                }
            }
            int loadingPeriodHolderIndex2 = this.loadingPeriodHolder.index;
            if (this.readingPeriodHolder != null) {
                readingPeriodHolderIndex = this.readingPeriodHolder.index;
            } else {
                readingPeriodHolderIndex = -1;
            }
            if (lastValidPeriodHolder.next != null) {
                releasePeriodHoldersFrom(lastValidPeriodHolder.next);
                lastValidPeriodHolder.next = null;
            }
            lastValidPeriodHolder.info = this.mediaPeriodInfoSequence.getUpdatedMediaPeriodInfo(lastValidPeriodHolder.info);
            if (loadingPeriodHolderIndex2 <= lastValidPeriodHolder.index) {
                seenLoadingPeriodHolder = true;
            } else {
                seenLoadingPeriodHolder = false;
            }
            if (!seenLoadingPeriodHolder) {
                this.loadingPeriodHolder = lastValidPeriodHolder;
            }
            if (readingPeriodHolderIndex == -1 || readingPeriodHolderIndex > lastValidPeriodHolder.index) {
                seenReadingPeriodHolder = false;
            }
            if (!seenReadingPeriodHolder && this.playingPeriodHolder != null) {
                MediaSource.MediaPeriodId periodId = this.playingPeriodHolder.info.id;
                long newPositionUs = seekToPeriodPosition(periodId, this.playbackInfo.positionUs);
                if (newPositionUs != this.playbackInfo.positionUs) {
                    this.playbackInfo = this.playbackInfo.fromNewPosition(periodId, newPositionUs, this.playbackInfo.contentPositionUs);
                    this.eventHandler.obtainMessage(4, 3, 0, this.playbackInfo).sendToTarget();
                }
            }
        }
    }

    private void startRenderers() throws ExoPlaybackException {
        this.rebuffering = false;
        this.standaloneMediaClock.start();
        for (Renderer renderer : this.enabledRenderers) {
            renderer.start();
        }
    }

    private void stopRenderers() throws ExoPlaybackException {
        this.standaloneMediaClock.stop();
        for (Renderer renderer : this.enabledRenderers) {
            ensureStopped(renderer);
        }
    }

    private void updatePlaybackPositions() throws ExoPlaybackException {
        long bufferedPositionUs;
        if (this.playingPeriodHolder != null) {
            long periodPositionUs = this.playingPeriodHolder.mediaPeriod.readDiscontinuity();
            if (periodPositionUs != C.TIME_UNSET) {
                resetRendererPosition(periodPositionUs);
                this.playbackInfo = this.playbackInfo.fromNewPosition(this.playbackInfo.periodId, periodPositionUs, this.playbackInfo.contentPositionUs);
                this.eventHandler.obtainMessage(4, 3, 0, this.playbackInfo).sendToTarget();
            } else {
                if (this.rendererMediaClockSource == null || this.rendererMediaClockSource.isEnded() || (!this.rendererMediaClockSource.isReady() && rendererWaitingForNextStream(this.rendererMediaClockSource))) {
                    this.rendererPositionUs = this.standaloneMediaClock.getPositionUs();
                } else {
                    this.rendererPositionUs = this.rendererMediaClock.getPositionUs();
                    this.standaloneMediaClock.setPositionUs(this.rendererPositionUs);
                }
                periodPositionUs = this.playingPeriodHolder.toPeriodTime(this.rendererPositionUs);
            }
            this.playbackInfo.positionUs = periodPositionUs;
            this.elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
            if (this.enabledRenderers.length == 0) {
                bufferedPositionUs = Long.MIN_VALUE;
            } else {
                bufferedPositionUs = this.playingPeriodHolder.mediaPeriod.getBufferedPositionUs();
            }
            PlaybackInfo playbackInfo2 = this.playbackInfo;
            if (bufferedPositionUs == Long.MIN_VALUE) {
                bufferedPositionUs = this.playingPeriodHolder.info.durationUs;
            }
            playbackInfo2.bufferedPositionUs = bufferedPositionUs;
        }
    }

    private void doSomeWork() throws ExoPlaybackException, IOException {
        boolean isStillReady;
        boolean isNewlyReady;
        long operationStartTimeMs = SystemClock.elapsedRealtime();
        updatePeriods();
        if (this.playingPeriodHolder == null) {
            maybeThrowPeriodPrepareError();
            scheduleNextWork(operationStartTimeMs, 10);
            return;
        }
        TraceUtil.beginSection("doSomeWork");
        updatePlaybackPositions();
        this.playingPeriodHolder.mediaPeriod.discardBuffer(this.playbackInfo.positionUs);
        boolean allRenderersEnded = true;
        boolean allRenderersReadyOrEnded = true;
        for (Renderer renderer : this.enabledRenderers) {
            renderer.render(this.rendererPositionUs, this.elapsedRealtimeUs);
            allRenderersEnded = allRenderersEnded && renderer.isEnded();
            boolean rendererReadyOrEnded = renderer.isReady() || renderer.isEnded() || rendererWaitingForNextStream(renderer);
            if (!rendererReadyOrEnded) {
                renderer.maybeThrowStreamError();
            }
            if (!allRenderersReadyOrEnded || !rendererReadyOrEnded) {
                allRenderersReadyOrEnded = false;
            } else {
                allRenderersReadyOrEnded = true;
            }
        }
        if (!allRenderersReadyOrEnded) {
            maybeThrowPeriodPrepareError();
        }
        if (this.rendererMediaClock != null) {
            PlaybackParameters playbackParameters2 = this.rendererMediaClock.getPlaybackParameters();
            if (!playbackParameters2.equals(this.playbackParameters)) {
                this.playbackParameters = playbackParameters2;
                this.standaloneMediaClock.setPlaybackParameters(playbackParameters2);
                this.eventHandler.obtainMessage(6, playbackParameters2).sendToTarget();
            }
        }
        long playingPeriodDurationUs = this.playingPeriodHolder.info.durationUs;
        if (allRenderersEnded && ((playingPeriodDurationUs == C.TIME_UNSET || playingPeriodDurationUs <= this.playbackInfo.positionUs) && this.playingPeriodHolder.info.isFinal)) {
            setState(4);
            stopRenderers();
        } else if (this.state == 2) {
            if (this.enabledRenderers.length > 0) {
                if (allRenderersReadyOrEnded) {
                    if (this.loadingPeriodHolder.haveSufficientBuffer(this.rebuffering, this.rendererPositionUs)) {
                        isNewlyReady = true;
                    }
                }
                isNewlyReady = false;
            } else {
                isNewlyReady = isTimelineReady(playingPeriodDurationUs);
            }
            if (isNewlyReady) {
                setState(3);
                if (this.playWhenReady) {
                    startRenderers();
                }
            }
        } else if (this.state == 3) {
            if (this.enabledRenderers.length > 0) {
                isStillReady = allRenderersReadyOrEnded;
            } else {
                isStillReady = isTimelineReady(playingPeriodDurationUs);
            }
            if (!isStillReady) {
                this.rebuffering = this.playWhenReady;
                setState(2);
                stopRenderers();
            }
        }
        if (this.state == 2) {
            for (Renderer renderer2 : this.enabledRenderers) {
                renderer2.maybeThrowStreamError();
            }
        }
        if ((this.playWhenReady && this.state == 3) || this.state == 2) {
            scheduleNextWork(operationStartTimeMs, 10);
        } else if (this.enabledRenderers.length == 0 || this.state == 4) {
            this.handler.removeMessages(2);
        } else {
            scheduleNextWork(operationStartTimeMs, 1000);
        }
        TraceUtil.endSection();
    }

    private void scheduleNextWork(long thisOperationStartTimeMs, long intervalMs) {
        this.handler.removeMessages(2);
        long nextOperationDelayMs = (thisOperationStartTimeMs + intervalMs) - SystemClock.elapsedRealtime();
        if (nextOperationDelayMs <= 0) {
            this.handler.sendEmptyMessage(2);
        } else {
            this.handler.sendEmptyMessageDelayed(2, nextOperationDelayMs);
        }
    }

    private void seekToInternal(SeekPosition seekPosition) throws ExoPlaybackException {
        int firstPeriodIndex;
        Timeline timeline = this.playbackInfo.timeline;
        if (timeline == null) {
            this.pendingInitialSeekCount++;
            this.pendingSeekPosition = seekPosition;
            return;
        }
        Pair<Integer, Long> periodPosition = resolveSeekPosition(seekPosition);
        if (periodPosition == null) {
            if (timeline.isEmpty()) {
                firstPeriodIndex = 0;
            } else {
                firstPeriodIndex = timeline.getWindow(timeline.getFirstWindowIndex(this.shuffleModeEnabled), this.window).firstPeriodIndex;
            }
            this.playbackInfo = this.playbackInfo.fromNewPosition(firstPeriodIndex, (long) C.TIME_UNSET, (long) C.TIME_UNSET);
            setState(4);
            this.eventHandler.obtainMessage(3, 1, 0, this.playbackInfo.fromNewPosition(firstPeriodIndex, 0, (long) C.TIME_UNSET)).sendToTarget();
            resetInternal(false);
            return;
        }
        boolean seekPositionAdjusted = seekPosition.windowPositionUs == C.TIME_UNSET;
        int periodIndex = ((Integer) periodPosition.first).intValue();
        long periodPositionUs = ((Long) periodPosition.second).longValue();
        long contentPositionUs = periodPositionUs;
        MediaSource.MediaPeriodId periodId = this.mediaPeriodInfoSequence.resolvePeriodPositionForAds(periodIndex, periodPositionUs);
        if (periodId.isAd()) {
            seekPositionAdjusted = true;
            periodPositionUs = 0;
        }
        try {
            if (!periodId.equals(this.playbackInfo.periodId) || periodPositionUs / 1000 != this.playbackInfo.positionUs / 1000) {
                newPeriodPositionUs = seekToPeriodPosition(periodId, periodPositionUs);
                seekPositionAdjusted |= periodPositionUs != newPeriodPositionUs;
                if (!seekPositionAdjusted) {
                    int i = 0;
                }
                return;
            }
            this.playbackInfo = this.playbackInfo.fromNewPosition(periodId, periodPositionUs, contentPositionUs);
            this.eventHandler.obtainMessage(3, seekPositionAdjusted ? 1 : 0, 0, this.playbackInfo).sendToTarget();
        } finally {
            Throwable th = newPeriodPositionUs;
            this.playbackInfo = this.playbackInfo.fromNewPosition(periodId, periodPositionUs, contentPositionUs);
            this.eventHandler.obtainMessage(3, seekPositionAdjusted ? 1 : 0, 0, this.playbackInfo).sendToTarget();
        }
    }

    private long seekToPeriodPosition(MediaSource.MediaPeriodId periodId, long periodPositionUs) throws ExoPlaybackException {
        stopRenderers();
        this.rebuffering = false;
        setState(2);
        MediaPeriodHolder newPlayingPeriodHolder = null;
        if (this.playingPeriodHolder != null) {
            for (MediaPeriodHolder periodHolder = this.playingPeriodHolder; periodHolder != null; periodHolder = periodHolder.next) {
                if (newPlayingPeriodHolder != null || !shouldKeepPeriodHolder(periodId, periodPositionUs, periodHolder)) {
                    periodHolder.release();
                } else {
                    newPlayingPeriodHolder = periodHolder;
                }
            }
        } else if (this.loadingPeriodHolder != null) {
            this.loadingPeriodHolder.release();
        }
        if (!(this.playingPeriodHolder == newPlayingPeriodHolder && this.playingPeriodHolder == this.readingPeriodHolder)) {
            for (Renderer renderer : this.enabledRenderers) {
                disableRenderer(renderer);
            }
            this.enabledRenderers = new Renderer[0];
            this.playingPeriodHolder = null;
        }
        if (newPlayingPeriodHolder != null) {
            newPlayingPeriodHolder.next = null;
            this.loadingPeriodHolder = newPlayingPeriodHolder;
            this.readingPeriodHolder = newPlayingPeriodHolder;
            setPlayingPeriodHolder(newPlayingPeriodHolder);
            if (this.playingPeriodHolder.hasEnabledTracks) {
                periodPositionUs = this.playingPeriodHolder.mediaPeriod.seekToUs(periodPositionUs);
            }
            resetRendererPosition(periodPositionUs);
            maybeContinueLoading();
        } else {
            this.loadingPeriodHolder = null;
            this.readingPeriodHolder = null;
            this.playingPeriodHolder = null;
            resetRendererPosition(periodPositionUs);
        }
        this.handler.sendEmptyMessage(2);
        return periodPositionUs;
    }

    private boolean shouldKeepPeriodHolder(MediaSource.MediaPeriodId seekPeriodId, long positionUs, MediaPeriodHolder holder) {
        if (seekPeriodId.equals(holder.info.id) && holder.prepared) {
            this.playbackInfo.timeline.getPeriod(holder.info.id.periodIndex, this.period);
            int nextAdGroupIndex = this.period.getAdGroupIndexAfterPositionUs(positionUs);
            if (nextAdGroupIndex == -1 || this.period.getAdGroupTimeUs(nextAdGroupIndex) == holder.info.endPositionUs) {
                return true;
            }
        }
        return false;
    }

    private void resetRendererPosition(long periodPositionUs) throws ExoPlaybackException {
        long rendererTime;
        if (this.playingPeriodHolder == null) {
            rendererTime = 60000000 + periodPositionUs;
        } else {
            rendererTime = this.playingPeriodHolder.toRendererTime(periodPositionUs);
        }
        this.rendererPositionUs = rendererTime;
        this.standaloneMediaClock.setPositionUs(this.rendererPositionUs);
        for (Renderer renderer : this.enabledRenderers) {
            renderer.resetPosition(this.rendererPositionUs);
        }
    }

    private void setPlaybackParametersInternal(PlaybackParameters playbackParameters2) {
        if (this.rendererMediaClock != null) {
            playbackParameters2 = this.rendererMediaClock.setPlaybackParameters(playbackParameters2);
        }
        this.standaloneMediaClock.setPlaybackParameters(playbackParameters2);
        this.playbackParameters = playbackParameters2;
        this.eventHandler.obtainMessage(6, playbackParameters2).sendToTarget();
    }

    private void stopInternal() {
        resetInternal(true);
        this.loadControl.onStopped();
        setState(1);
    }

    private void releaseInternal() {
        resetInternal(true);
        this.loadControl.onReleased();
        setState(1);
        this.internalPlaybackThread.quit();
        synchronized (this) {
            this.released = true;
            notifyAll();
        }
    }

    private void resetInternal(boolean releaseMediaSource) {
        this.handler.removeMessages(2);
        this.rebuffering = false;
        this.standaloneMediaClock.stop();
        this.rendererPositionUs = 60000000;
        for (Renderer renderer : this.enabledRenderers) {
            try {
                disableRenderer(renderer);
            } catch (ExoPlaybackException | RuntimeException e) {
                Log.e(TAG, "Stop failed.", e);
            }
        }
        this.enabledRenderers = new Renderer[0];
        releasePeriodHoldersFrom(this.playingPeriodHolder != null ? this.playingPeriodHolder : this.loadingPeriodHolder);
        this.loadingPeriodHolder = null;
        this.readingPeriodHolder = null;
        this.playingPeriodHolder = null;
        setIsLoading(false);
        if (releaseMediaSource) {
            if (this.mediaSource != null) {
                this.mediaSource.releaseSource();
                this.mediaSource = null;
            }
            this.mediaPeriodInfoSequence.setTimeline((Timeline) null);
            this.playbackInfo = this.playbackInfo.copyWithTimeline((Timeline) null, (Object) null);
        }
    }

    private void sendMessagesInternal(ExoPlayer.ExoPlayerMessage[] messages) throws ExoPlaybackException {
        try {
            for (ExoPlayer.ExoPlayerMessage message : messages) {
                message.target.handleMessage(message.messageType, message.message);
            }
            if (this.state == 3 || this.state == 2) {
                this.handler.sendEmptyMessage(2);
            }
            synchronized (this) {
                this.customMessagesProcessed++;
                notifyAll();
            }
        } catch (Throwable th) {
            synchronized (this) {
                this.customMessagesProcessed++;
                notifyAll();
                throw th;
            }
        }
    }

    private void ensureStopped(Renderer renderer) throws ExoPlaybackException {
        if (renderer.getState() == 2) {
            renderer.stop();
        }
    }

    private void disableRenderer(Renderer renderer) throws ExoPlaybackException {
        if (renderer == this.rendererMediaClockSource) {
            this.rendererMediaClock = null;
            this.rendererMediaClockSource = null;
        }
        ensureStopped(renderer);
        renderer.disable();
    }

    private void reselectTracksInternal() throws ExoPlaybackException {
        if (this.playingPeriodHolder != null) {
            MediaPeriodHolder periodHolder = this.playingPeriodHolder;
            boolean selectionsChangedForReadPeriod = true;
            while (periodHolder != null && periodHolder.prepared) {
                if (periodHolder.selectTracks()) {
                    if (selectionsChangedForReadPeriod) {
                        boolean recreateStreams = this.readingPeriodHolder != this.playingPeriodHolder;
                        releasePeriodHoldersFrom(this.playingPeriodHolder.next);
                        this.playingPeriodHolder.next = null;
                        this.loadingPeriodHolder = this.playingPeriodHolder;
                        this.readingPeriodHolder = this.playingPeriodHolder;
                        boolean[] streamResetFlags = new boolean[this.renderers.length];
                        long periodPositionUs = this.playingPeriodHolder.updatePeriodTrackSelection(this.playbackInfo.positionUs, recreateStreams, streamResetFlags);
                        if (!(this.state == 4 || periodPositionUs == this.playbackInfo.positionUs)) {
                            this.playbackInfo = this.playbackInfo.fromNewPosition(this.playbackInfo.periodId, periodPositionUs, this.playbackInfo.contentPositionUs);
                            this.eventHandler.obtainMessage(4, 3, 0, this.playbackInfo).sendToTarget();
                            resetRendererPosition(periodPositionUs);
                        }
                        int enabledRendererCount = 0;
                        boolean[] rendererWasEnabledFlags = new boolean[this.renderers.length];
                        for (int i = 0; i < this.renderers.length; i++) {
                            Renderer renderer = this.renderers[i];
                            rendererWasEnabledFlags[i] = renderer.getState() != 0;
                            SampleStream sampleStream = this.playingPeriodHolder.sampleStreams[i];
                            if (sampleStream != null) {
                                enabledRendererCount++;
                            }
                            if (rendererWasEnabledFlags[i]) {
                                if (sampleStream != renderer.getStream()) {
                                    disableRenderer(renderer);
                                } else if (streamResetFlags[i]) {
                                    renderer.resetPosition(this.rendererPositionUs);
                                }
                            }
                        }
                        this.eventHandler.obtainMessage(2, periodHolder.trackSelectorResult).sendToTarget();
                        enableRenderers(rendererWasEnabledFlags, enabledRendererCount);
                    } else {
                        this.loadingPeriodHolder = periodHolder;
                        for (MediaPeriodHolder periodHolder2 = this.loadingPeriodHolder.next; periodHolder2 != null; periodHolder2 = periodHolder2.next) {
                            periodHolder2.release();
                        }
                        this.loadingPeriodHolder.next = null;
                        if (this.loadingPeriodHolder.prepared) {
                            this.loadingPeriodHolder.updatePeriodTrackSelection(Math.max(this.loadingPeriodHolder.info.startPositionUs, this.loadingPeriodHolder.toPeriodTime(this.rendererPositionUs)), false);
                        }
                    }
                    if (this.state != 4) {
                        maybeContinueLoading();
                        updatePlaybackPositions();
                        this.handler.sendEmptyMessage(2);
                        return;
                    }
                    return;
                }
                if (periodHolder == this.readingPeriodHolder) {
                    selectionsChangedForReadPeriod = false;
                }
                periodHolder = periodHolder.next;
            }
        }
    }

    private boolean isTimelineReady(long playingPeriodDurationUs) {
        return playingPeriodDurationUs == C.TIME_UNSET || this.playbackInfo.positionUs < playingPeriodDurationUs || (this.playingPeriodHolder.next != null && (this.playingPeriodHolder.next.prepared || this.playingPeriodHolder.next.info.id.isAd()));
    }

    private void maybeThrowPeriodPrepareError() throws IOException {
        if (this.loadingPeriodHolder != null && !this.loadingPeriodHolder.prepared) {
            if (this.readingPeriodHolder == null || this.readingPeriodHolder.next == this.loadingPeriodHolder) {
                Renderer[] rendererArr = this.enabledRenderers;
                int length = rendererArr.length;
                int i = 0;
                while (i < length) {
                    if (rendererArr[i].hasReadStreamToEnd()) {
                        i++;
                    } else {
                        return;
                    }
                }
                this.loadingPeriodHolder.mediaPeriod.maybeThrowPrepareError();
            }
        }
    }

    private void handleSourceInfoRefreshed(MediaSourceRefreshInfo sourceRefreshInfo) throws ExoPlaybackException {
        Object playingPeriodUid;
        MediaPeriodHolder previousPeriodHolder;
        MediaPeriodHolder periodHolder;
        long j;
        long j2;
        if (sourceRefreshInfo.source == this.mediaSource) {
            Timeline oldTimeline = this.playbackInfo.timeline;
            Timeline timeline = sourceRefreshInfo.timeline;
            Object manifest = sourceRefreshInfo.manifest;
            this.mediaPeriodInfoSequence.setTimeline(timeline);
            this.playbackInfo = this.playbackInfo.copyWithTimeline(timeline, manifest);
            if (oldTimeline == null) {
                int processedPrepareAcks = this.pendingPrepareCount;
                this.pendingPrepareCount = 0;
                if (this.pendingInitialSeekCount > 0) {
                    Pair<Integer, Long> periodPosition = resolveSeekPosition(this.pendingSeekPosition);
                    int processedInitialSeekCount = this.pendingInitialSeekCount;
                    this.pendingInitialSeekCount = 0;
                    this.pendingSeekPosition = null;
                    if (periodPosition == null) {
                        handleSourceInfoRefreshEndedPlayback(processedPrepareAcks, processedInitialSeekCount);
                        return;
                    }
                    int periodIndex = ((Integer) periodPosition.first).intValue();
                    long positionUs = ((Long) periodPosition.second).longValue();
                    MediaSource.MediaPeriodId periodId = this.mediaPeriodInfoSequence.resolvePeriodPositionForAds(periodIndex, positionUs);
                    PlaybackInfo playbackInfo2 = this.playbackInfo;
                    if (periodId.isAd()) {
                        j2 = 0;
                    } else {
                        j2 = positionUs;
                    }
                    this.playbackInfo = playbackInfo2.fromNewPosition(periodId, j2, positionUs);
                    notifySourceInfoRefresh(processedPrepareAcks, processedInitialSeekCount);
                } else if (this.playbackInfo.startPositionUs != C.TIME_UNSET) {
                    notifySourceInfoRefresh(processedPrepareAcks, 0);
                } else if (timeline.isEmpty()) {
                    handleSourceInfoRefreshEndedPlayback(processedPrepareAcks, 0);
                } else {
                    Pair<Integer, Long> defaultPosition = getPeriodPosition(timeline, timeline.getFirstWindowIndex(this.shuffleModeEnabled), C.TIME_UNSET);
                    int periodIndex2 = ((Integer) defaultPosition.first).intValue();
                    long startPositionUs = ((Long) defaultPosition.second).longValue();
                    MediaSource.MediaPeriodId periodId2 = this.mediaPeriodInfoSequence.resolvePeriodPositionForAds(periodIndex2, startPositionUs);
                    PlaybackInfo playbackInfo3 = this.playbackInfo;
                    if (periodId2.isAd()) {
                        j = 0;
                    } else {
                        j = startPositionUs;
                    }
                    this.playbackInfo = playbackInfo3.fromNewPosition(periodId2, j, startPositionUs);
                    notifySourceInfoRefresh(processedPrepareAcks, 0);
                }
            } else {
                int playingPeriodIndex = this.playbackInfo.periodId.periodIndex;
                MediaPeriodHolder periodHolder2 = this.playingPeriodHolder != null ? this.playingPeriodHolder : this.loadingPeriodHolder;
                if (periodHolder2 != null || playingPeriodIndex < oldTimeline.getPeriodCount()) {
                    if (periodHolder2 == null) {
                        playingPeriodUid = oldTimeline.getPeriod(playingPeriodIndex, this.period, true).uid;
                    } else {
                        playingPeriodUid = periodHolder2.uid;
                    }
                    int periodIndex3 = timeline.getIndexOfPeriod(playingPeriodUid);
                    if (periodIndex3 == -1) {
                        int newPeriodIndex = resolveSubsequentPeriod(playingPeriodIndex, oldTimeline, timeline);
                        if (newPeriodIndex == -1) {
                            handleSourceInfoRefreshEndedPlayback();
                            return;
                        }
                        Pair<Integer, Long> defaultPosition2 = getPeriodPosition(timeline, timeline.getPeriod(newPeriodIndex, this.period).windowIndex, C.TIME_UNSET);
                        int newPeriodIndex2 = ((Integer) defaultPosition2.first).intValue();
                        long newPositionUs = ((Long) defaultPosition2.second).longValue();
                        timeline.getPeriod(newPeriodIndex2, this.period, true);
                        if (periodHolder2 != null) {
                            Object newPeriodUid = this.period.uid;
                            periodHolder2.info = periodHolder2.info.copyWithPeriodIndex(-1);
                            while (periodHolder2.next != null) {
                                periodHolder2 = periodHolder2.next;
                                if (periodHolder2.uid.equals(newPeriodUid)) {
                                    periodHolder2.info = this.mediaPeriodInfoSequence.getUpdatedMediaPeriodInfo(periodHolder2.info, newPeriodIndex2);
                                } else {
                                    periodHolder2.info = periodHolder2.info.copyWithPeriodIndex(-1);
                                }
                            }
                        }
                        MediaSource.MediaPeriodId periodId3 = new MediaSource.MediaPeriodId(newPeriodIndex2);
                        this.playbackInfo = this.playbackInfo.fromNewPosition(periodId3, seekToPeriodPosition(periodId3, newPositionUs), (long) C.TIME_UNSET);
                        notifySourceInfoRefresh();
                        return;
                    }
                    if (periodIndex3 != playingPeriodIndex) {
                        this.playbackInfo = this.playbackInfo.copyWithPeriodIndex(periodIndex3);
                    }
                    if (this.playbackInfo.periodId.isAd()) {
                        MediaSource.MediaPeriodId periodId4 = this.mediaPeriodInfoSequence.resolvePeriodPositionForAds(periodIndex3, this.playbackInfo.contentPositionUs);
                        if (!periodId4.isAd() || periodId4.adIndexInAdGroup != this.playbackInfo.periodId.adIndexInAdGroup) {
                            this.playbackInfo = this.playbackInfo.fromNewPosition(periodId4, seekToPeriodPosition(periodId4, this.playbackInfo.contentPositionUs), periodId4.isAd() ? this.playbackInfo.contentPositionUs : C.TIME_UNSET);
                            notifySourceInfoRefresh();
                            return;
                        }
                    }
                    if (periodHolder2 == null) {
                        notifySourceInfoRefresh();
                        return;
                    }
                    MediaPeriodHolder periodHolder3 = updatePeriodInfo(periodHolder2, periodIndex3);
                    while (true) {
                        if (periodHolder3.next == null) {
                            break;
                        }
                        previousPeriodHolder = periodHolder3;
                        periodHolder = periodHolder3.next;
                        periodIndex3 = timeline.getNextPeriodIndex(periodIndex3, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
                        if (periodIndex3 == -1) {
                            break;
                        }
                        if (!periodHolder.uid.equals(timeline.getPeriod(periodIndex3, this.period, true).uid)) {
                            break;
                        }
                        periodHolder3 = updatePeriodInfo(periodHolder, periodIndex3);
                    }
                    if (!(this.readingPeriodHolder != null && this.readingPeriodHolder.index < periodHolder.index)) {
                        this.playbackInfo = this.playbackInfo.fromNewPosition(this.playingPeriodHolder.info.id, seekToPeriodPosition(this.playingPeriodHolder.info.id, this.playbackInfo.positionUs), this.playbackInfo.contentPositionUs);
                    } else {
                        this.loadingPeriodHolder = previousPeriodHolder;
                        this.loadingPeriodHolder.next = null;
                        releasePeriodHoldersFrom(periodHolder);
                    }
                    notifySourceInfoRefresh();
                    return;
                }
                notifySourceInfoRefresh();
            }
        }
    }

    private MediaPeriodHolder updatePeriodInfo(MediaPeriodHolder periodHolder, int periodIndex) {
        while (true) {
            periodHolder.info = this.mediaPeriodInfoSequence.getUpdatedMediaPeriodInfo(periodHolder.info, periodIndex);
            if (periodHolder.info.isLastInTimelinePeriod || periodHolder.next == null) {
                return periodHolder;
            }
            periodHolder = periodHolder.next;
        }
        return periodHolder;
    }

    private void handleSourceInfoRefreshEndedPlayback() {
        handleSourceInfoRefreshEndedPlayback(0, 0);
    }

    private void handleSourceInfoRefreshEndedPlayback(int prepareAcks, int seekAcks) {
        Timeline timeline = this.playbackInfo.timeline;
        int firstPeriodIndex = timeline.isEmpty() ? 0 : timeline.getWindow(timeline.getFirstWindowIndex(this.shuffleModeEnabled), this.window).firstPeriodIndex;
        this.playbackInfo = this.playbackInfo.fromNewPosition(firstPeriodIndex, (long) C.TIME_UNSET, (long) C.TIME_UNSET);
        setState(4);
        notifySourceInfoRefresh(prepareAcks, seekAcks, this.playbackInfo.fromNewPosition(firstPeriodIndex, 0, (long) C.TIME_UNSET));
        resetInternal(false);
    }

    private void notifySourceInfoRefresh() {
        notifySourceInfoRefresh(0, 0);
    }

    private void notifySourceInfoRefresh(int prepareAcks, int seekAcks) {
        notifySourceInfoRefresh(prepareAcks, seekAcks, this.playbackInfo);
    }

    private void notifySourceInfoRefresh(int prepareAcks, int seekAcks, PlaybackInfo playbackInfo2) {
        this.eventHandler.obtainMessage(5, prepareAcks, seekAcks, playbackInfo2).sendToTarget();
    }

    private int resolveSubsequentPeriod(int oldPeriodIndex, Timeline oldTimeline, Timeline newTimeline) {
        int newPeriodIndex = -1;
        int maxIterations = oldTimeline.getPeriodCount();
        for (int i = 0; i < maxIterations && newPeriodIndex == -1; i++) {
            oldPeriodIndex = oldTimeline.getNextPeriodIndex(oldPeriodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
            if (oldPeriodIndex == -1) {
                break;
            }
            newPeriodIndex = newTimeline.getIndexOfPeriod(oldTimeline.getPeriod(oldPeriodIndex, this.period, true).uid);
        }
        return newPeriodIndex;
    }

    private Pair<Integer, Long> resolveSeekPosition(SeekPosition seekPosition) {
        Timeline timeline = this.playbackInfo.timeline;
        Timeline seekTimeline = seekPosition.timeline;
        if (seekTimeline.isEmpty()) {
            seekTimeline = timeline;
        }
        try {
            Pair<Integer, Long> periodPosition = seekTimeline.getPeriodPosition(this.window, this.period, seekPosition.windowIndex, seekPosition.windowPositionUs);
            if (timeline == seekTimeline) {
                return periodPosition;
            }
            int periodIndex = timeline.getIndexOfPeriod(seekTimeline.getPeriod(((Integer) periodPosition.first).intValue(), this.period, true).uid);
            if (periodIndex != -1) {
                return Pair.create(Integer.valueOf(periodIndex), periodPosition.second);
            }
            int periodIndex2 = resolveSubsequentPeriod(((Integer) periodPosition.first).intValue(), seekTimeline, timeline);
            if (periodIndex2 != -1) {
                return getPeriodPosition(timeline, timeline.getPeriod(periodIndex2, this.period).windowIndex, C.TIME_UNSET);
            }
            return null;
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalSeekPositionException(timeline, seekPosition.windowIndex, seekPosition.windowPositionUs);
        }
    }

    private Pair<Integer, Long> getPeriodPosition(Timeline timeline, int windowIndex, long windowPositionUs) {
        return timeline.getPeriodPosition(this.window, this.period, windowIndex, windowPositionUs);
    }

    private void updatePeriods() throws ExoPlaybackException, IOException {
        if (this.playbackInfo.timeline == null) {
            this.mediaSource.maybeThrowSourceInfoRefreshError();
            return;
        }
        maybeUpdateLoadingPeriod();
        if (this.loadingPeriodHolder == null || this.loadingPeriodHolder.isFullyBuffered()) {
            setIsLoading(false);
        } else if (this.loadingPeriodHolder != null && !this.isLoading) {
            maybeContinueLoading();
        }
        if (this.playingPeriodHolder != null) {
            while (this.playWhenReady && this.playingPeriodHolder != this.readingPeriodHolder && this.rendererPositionUs >= this.playingPeriodHolder.next.rendererPositionOffsetUs) {
                this.playingPeriodHolder.release();
                setPlayingPeriodHolder(this.playingPeriodHolder.next);
                this.playbackInfo = this.playbackInfo.fromNewPosition(this.playingPeriodHolder.info.id, this.playingPeriodHolder.info.startPositionUs, this.playingPeriodHolder.info.contentPositionUs);
                updatePlaybackPositions();
                this.eventHandler.obtainMessage(4, 0, 0, this.playbackInfo).sendToTarget();
            }
            if (this.readingPeriodHolder.info.isFinal) {
                for (int i = 0; i < this.renderers.length; i++) {
                    Renderer renderer = this.renderers[i];
                    SampleStream sampleStream = this.readingPeriodHolder.sampleStreams[i];
                    if (sampleStream != null && renderer.getStream() == sampleStream && renderer.hasReadStreamToEnd()) {
                        renderer.setCurrentStreamFinal();
                    }
                }
            } else if (this.readingPeriodHolder.next != null && this.readingPeriodHolder.next.prepared) {
                int i2 = 0;
                while (i2 < this.renderers.length) {
                    Renderer renderer2 = this.renderers[i2];
                    SampleStream sampleStream2 = this.readingPeriodHolder.sampleStreams[i2];
                    if (renderer2.getStream() != sampleStream2) {
                        return;
                    }
                    if (sampleStream2 == null || renderer2.hasReadStreamToEnd()) {
                        i2++;
                    } else {
                        return;
                    }
                }
                TrackSelectorResult oldTrackSelectorResult = this.readingPeriodHolder.trackSelectorResult;
                this.readingPeriodHolder = this.readingPeriodHolder.next;
                TrackSelectorResult newTrackSelectorResult = this.readingPeriodHolder.trackSelectorResult;
                boolean initialDiscontinuity = this.readingPeriodHolder.mediaPeriod.readDiscontinuity() != C.TIME_UNSET;
                for (int i3 = 0; i3 < this.renderers.length; i3++) {
                    Renderer renderer3 = this.renderers[i3];
                    if (oldTrackSelectorResult.renderersEnabled[i3]) {
                        if (initialDiscontinuity) {
                            renderer3.setCurrentStreamFinal();
                        } else if (!renderer3.isCurrentStreamFinal()) {
                            TrackSelection newSelection = newTrackSelectorResult.selections.get(i3);
                            boolean newRendererEnabled = newTrackSelectorResult.renderersEnabled[i3];
                            boolean isNoSampleRenderer = this.rendererCapabilities[i3].getTrackType() == 5;
                            RendererConfiguration oldConfig = oldTrackSelectorResult.rendererConfigurations[i3];
                            RendererConfiguration newConfig = newTrackSelectorResult.rendererConfigurations[i3];
                            if (!newRendererEnabled || !newConfig.equals(oldConfig) || isNoSampleRenderer) {
                                renderer3.setCurrentStreamFinal();
                            } else {
                                renderer3.replaceStream(getFormats(newSelection), this.readingPeriodHolder.sampleStreams[i3], this.readingPeriodHolder.getRendererOffset());
                            }
                        }
                    }
                }
            }
        }
    }

    private void maybeUpdateLoadingPeriod() throws IOException {
        MediaPeriodInfoSequence.MediaPeriodInfo info;
        long rendererPositionOffsetUs;
        if (this.loadingPeriodHolder == null) {
            info = this.mediaPeriodInfoSequence.getFirstMediaPeriodInfo(this.playbackInfo);
        } else if (!this.loadingPeriodHolder.info.isFinal && this.loadingPeriodHolder.isFullyBuffered() && this.loadingPeriodHolder.info.durationUs != C.TIME_UNSET) {
            if (this.playingPeriodHolder == null || this.loadingPeriodHolder.index - this.playingPeriodHolder.index != 100) {
                info = this.mediaPeriodInfoSequence.getNextMediaPeriodInfo(this.loadingPeriodHolder.info, this.loadingPeriodHolder.getRendererOffset(), this.rendererPositionUs);
            } else {
                return;
            }
        } else {
            return;
        }
        if (info == null) {
            this.mediaSource.maybeThrowSourceInfoRefreshError();
            return;
        }
        if (this.loadingPeriodHolder == null) {
            rendererPositionOffsetUs = 60000000;
        } else {
            rendererPositionOffsetUs = this.loadingPeriodHolder.getRendererOffset() + this.loadingPeriodHolder.info.durationUs;
        }
        MediaPeriodHolder newPeriodHolder = new MediaPeriodHolder(this.renderers, this.rendererCapabilities, rendererPositionOffsetUs, this.trackSelector, this.loadControl, this.mediaSource, this.playbackInfo.timeline.getPeriod(info.id.periodIndex, this.period, true).uid, this.loadingPeriodHolder == null ? 0 : this.loadingPeriodHolder.index + 1, info);
        if (this.loadingPeriodHolder != null) {
            this.loadingPeriodHolder.next = newPeriodHolder;
        }
        this.loadingPeriodHolder = newPeriodHolder;
        this.loadingPeriodHolder.mediaPeriod.prepare(this, info.startPositionUs);
        setIsLoading(true);
    }

    private void handlePeriodPrepared(MediaPeriod period2) throws ExoPlaybackException {
        if (this.loadingPeriodHolder != null && this.loadingPeriodHolder.mediaPeriod == period2) {
            this.loadingPeriodHolder.handlePrepared();
            if (this.playingPeriodHolder == null) {
                this.readingPeriodHolder = this.loadingPeriodHolder;
                resetRendererPosition(this.readingPeriodHolder.info.startPositionUs);
                setPlayingPeriodHolder(this.readingPeriodHolder);
            }
            maybeContinueLoading();
        }
    }

    private void handleContinueLoadingRequested(MediaPeriod period2) {
        if (this.loadingPeriodHolder != null && this.loadingPeriodHolder.mediaPeriod == period2) {
            maybeContinueLoading();
        }
    }

    private void maybeContinueLoading() {
        boolean continueLoading = this.loadingPeriodHolder.shouldContinueLoading(this.rendererPositionUs);
        setIsLoading(continueLoading);
        if (continueLoading) {
            this.loadingPeriodHolder.continueLoading(this.rendererPositionUs);
        }
    }

    private void releasePeriodHoldersFrom(MediaPeriodHolder periodHolder) {
        while (periodHolder != null) {
            periodHolder.release();
            periodHolder = periodHolder.next;
        }
    }

    private void setPlayingPeriodHolder(MediaPeriodHolder periodHolder) throws ExoPlaybackException {
        if (this.playingPeriodHolder != periodHolder) {
            int enabledRendererCount = 0;
            boolean[] rendererWasEnabledFlags = new boolean[this.renderers.length];
            for (int i = 0; i < this.renderers.length; i++) {
                Renderer renderer = this.renderers[i];
                rendererWasEnabledFlags[i] = renderer.getState() != 0;
                if (periodHolder.trackSelectorResult.renderersEnabled[i]) {
                    enabledRendererCount++;
                }
                if (rendererWasEnabledFlags[i] && (!periodHolder.trackSelectorResult.renderersEnabled[i] || (renderer.isCurrentStreamFinal() && renderer.getStream() == this.playingPeriodHolder.sampleStreams[i]))) {
                    disableRenderer(renderer);
                }
            }
            this.playingPeriodHolder = periodHolder;
            this.eventHandler.obtainMessage(2, periodHolder.trackSelectorResult).sendToTarget();
            enableRenderers(rendererWasEnabledFlags, enabledRendererCount);
        }
    }

    private void enableRenderers(boolean[] rendererWasEnabledFlags, int totalEnabledRendererCount) throws ExoPlaybackException {
        this.enabledRenderers = new Renderer[totalEnabledRendererCount];
        int enabledRendererCount = 0;
        for (int i = 0; i < this.renderers.length; i++) {
            if (this.playingPeriodHolder.trackSelectorResult.renderersEnabled[i]) {
                enableRenderer(i, rendererWasEnabledFlags[i], enabledRendererCount);
                enabledRendererCount++;
            }
        }
    }

    private void enableRenderer(int rendererIndex, boolean wasRendererEnabled, int enabledRendererIndex) throws ExoPlaybackException {
        boolean playing;
        boolean joining = true;
        Renderer renderer = this.renderers[rendererIndex];
        this.enabledRenderers[enabledRendererIndex] = renderer;
        if (renderer.getState() == 0) {
            RendererConfiguration rendererConfiguration = this.playingPeriodHolder.trackSelectorResult.rendererConfigurations[rendererIndex];
            Format[] formats = getFormats(this.playingPeriodHolder.trackSelectorResult.selections.get(rendererIndex));
            if (!this.playWhenReady || this.state != 3) {
                playing = false;
            } else {
                playing = true;
            }
            if (wasRendererEnabled || !playing) {
                joining = false;
            }
            renderer.enable(rendererConfiguration, formats, this.playingPeriodHolder.sampleStreams[rendererIndex], this.rendererPositionUs, joining, this.playingPeriodHolder.getRendererOffset());
            MediaClock mediaClock = renderer.getMediaClock();
            if (mediaClock != null) {
                if (this.rendererMediaClock != null) {
                    throw ExoPlaybackException.createForUnexpected(new IllegalStateException("Multiple renderer media clocks enabled."));
                }
                this.rendererMediaClock = mediaClock;
                this.rendererMediaClockSource = renderer;
                this.rendererMediaClock.setPlaybackParameters(this.playbackParameters);
            }
            if (playing) {
                renderer.start();
            }
        }
    }

    private boolean rendererWaitingForNextStream(Renderer renderer) {
        return this.readingPeriodHolder.next != null && this.readingPeriodHolder.next.prepared && renderer.hasReadStreamToEnd();
    }

    @NonNull
    private static Format[] getFormats(TrackSelection newSelection) {
        int length = newSelection != null ? newSelection.length() : 0;
        Format[] formats = new Format[length];
        for (int i = 0; i < length; i++) {
            formats[i] = newSelection.getFormat(i);
        }
        return formats;
    }

    private static final class MediaPeriodHolder {
        public boolean hasEnabledTracks;
        public final int index;
        public MediaPeriodInfoSequence.MediaPeriodInfo info;
        private final LoadControl loadControl;
        public final boolean[] mayRetainStreamFlags;
        public final MediaPeriod mediaPeriod;
        private final MediaSource mediaSource;
        public MediaPeriodHolder next;
        private TrackSelectorResult periodTrackSelectorResult;
        public boolean prepared;
        private final RendererCapabilities[] rendererCapabilities;
        public final long rendererPositionOffsetUs;
        private final Renderer[] renderers;
        public final SampleStream[] sampleStreams;
        private final TrackSelector trackSelector;
        public TrackSelectorResult trackSelectorResult;
        public final Object uid;

        public MediaPeriodHolder(Renderer[] renderers2, RendererCapabilities[] rendererCapabilities2, long rendererPositionOffsetUs2, TrackSelector trackSelector2, LoadControl loadControl2, MediaSource mediaSource2, Object periodUid, int index2, MediaPeriodInfoSequence.MediaPeriodInfo info2) {
            this.renderers = renderers2;
            this.rendererCapabilities = rendererCapabilities2;
            this.rendererPositionOffsetUs = rendererPositionOffsetUs2;
            this.trackSelector = trackSelector2;
            this.loadControl = loadControl2;
            this.mediaSource = mediaSource2;
            this.uid = Assertions.checkNotNull(periodUid);
            this.index = index2;
            this.info = info2;
            this.sampleStreams = new SampleStream[renderers2.length];
            this.mayRetainStreamFlags = new boolean[renderers2.length];
            MediaPeriod mediaPeriod2 = mediaSource2.createPeriod(info2.id, loadControl2.getAllocator());
            if (info2.endPositionUs != Long.MIN_VALUE) {
                ClippingMediaPeriod clippingMediaPeriod = new ClippingMediaPeriod(mediaPeriod2, true);
                clippingMediaPeriod.setClipping(0, info2.endPositionUs);
                mediaPeriod2 = clippingMediaPeriod;
            }
            this.mediaPeriod = mediaPeriod2;
        }

        public long toRendererTime(long periodTimeUs) {
            return getRendererOffset() + periodTimeUs;
        }

        public long toPeriodTime(long rendererTimeUs) {
            return rendererTimeUs - getRendererOffset();
        }

        public long getRendererOffset() {
            return this.index == 0 ? this.rendererPositionOffsetUs : this.rendererPositionOffsetUs - this.info.startPositionUs;
        }

        public boolean isFullyBuffered() {
            return this.prepared && (!this.hasEnabledTracks || this.mediaPeriod.getBufferedPositionUs() == Long.MIN_VALUE);
        }

        public boolean haveSufficientBuffer(boolean rebuffering, long rendererPositionUs) {
            long bufferedPositionUs;
            if (!this.prepared) {
                bufferedPositionUs = this.info.startPositionUs;
            } else {
                bufferedPositionUs = this.mediaPeriod.getBufferedPositionUs();
            }
            if (bufferedPositionUs == Long.MIN_VALUE) {
                if (this.info.isFinal) {
                    return true;
                }
                bufferedPositionUs = this.info.durationUs;
            }
            return this.loadControl.shouldStartPlayback(bufferedPositionUs - toPeriodTime(rendererPositionUs), rebuffering);
        }

        public void handlePrepared() throws ExoPlaybackException {
            this.prepared = true;
            selectTracks();
            this.info = this.info.copyWithStartPositionUs(updatePeriodTrackSelection(this.info.startPositionUs, false));
        }

        public boolean shouldContinueLoading(long rendererPositionUs) {
            long nextLoadPositionUs = !this.prepared ? 0 : this.mediaPeriod.getNextLoadPositionUs();
            if (nextLoadPositionUs == Long.MIN_VALUE) {
                return false;
            }
            return this.loadControl.shouldContinueLoading(nextLoadPositionUs - toPeriodTime(rendererPositionUs));
        }

        public void continueLoading(long rendererPositionUs) {
            this.mediaPeriod.continueLoading(toPeriodTime(rendererPositionUs));
        }

        public boolean selectTracks() throws ExoPlaybackException {
            TrackSelectorResult selectorResult = this.trackSelector.selectTracks(this.rendererCapabilities, this.mediaPeriod.getTrackGroups());
            if (selectorResult.isEquivalent(this.periodTrackSelectorResult)) {
                return false;
            }
            this.trackSelectorResult = selectorResult;
            return true;
        }

        public long updatePeriodTrackSelection(long positionUs, boolean forceRecreateStreams) {
            return updatePeriodTrackSelection(positionUs, forceRecreateStreams, new boolean[this.renderers.length]);
        }

        public long updatePeriodTrackSelection(long positionUs, boolean forceRecreateStreams, boolean[] streamResetFlags) {
            boolean z;
            TrackSelectionArray trackSelections = this.trackSelectorResult.selections;
            for (int i = 0; i < trackSelections.length; i++) {
                boolean[] zArr = this.mayRetainStreamFlags;
                if (forceRecreateStreams || !this.trackSelectorResult.isEquivalent(this.periodTrackSelectorResult, i)) {
                    z = false;
                } else {
                    z = true;
                }
                zArr[i] = z;
            }
            disassociateNoSampleRenderersWithEmptySampleStream(this.sampleStreams);
            updatePeriodTrackSelectorResult(this.trackSelectorResult);
            long positionUs2 = this.mediaPeriod.selectTracks(trackSelections.getAll(), this.mayRetainStreamFlags, this.sampleStreams, streamResetFlags, positionUs);
            associateNoSampleRenderersWithEmptySampleStream(this.sampleStreams);
            this.hasEnabledTracks = false;
            for (int i2 = 0; i2 < this.sampleStreams.length; i2++) {
                if (this.sampleStreams[i2] != null) {
                    Assertions.checkState(this.trackSelectorResult.renderersEnabled[i2]);
                    if (this.rendererCapabilities[i2].getTrackType() != 5) {
                        this.hasEnabledTracks = true;
                    }
                } else {
                    Assertions.checkState(trackSelections.get(i2) == null);
                }
            }
            this.loadControl.onTracksSelected(this.renderers, this.trackSelectorResult.groups, trackSelections);
            return positionUs2;
        }

        public void release() {
            updatePeriodTrackSelectorResult((TrackSelectorResult) null);
            try {
                if (this.info.endPositionUs != Long.MIN_VALUE) {
                    this.mediaSource.releasePeriod(((ClippingMediaPeriod) this.mediaPeriod).mediaPeriod);
                } else {
                    this.mediaSource.releasePeriod(this.mediaPeriod);
                }
            } catch (RuntimeException e) {
                Log.e(ExoPlayerImplInternal.TAG, "Period release failed.", e);
            }
        }

        private void updatePeriodTrackSelectorResult(TrackSelectorResult trackSelectorResult2) {
            if (this.periodTrackSelectorResult != null) {
                disableTrackSelectionsInResult(this.periodTrackSelectorResult);
            }
            this.periodTrackSelectorResult = trackSelectorResult2;
            if (this.periodTrackSelectorResult != null) {
                enableTrackSelectionsInResult(this.periodTrackSelectorResult);
            }
        }

        private void enableTrackSelectionsInResult(TrackSelectorResult trackSelectorResult2) {
            for (int i = 0; i < trackSelectorResult2.renderersEnabled.length; i++) {
                boolean rendererEnabled = trackSelectorResult2.renderersEnabled[i];
                TrackSelection trackSelection = trackSelectorResult2.selections.get(i);
                if (rendererEnabled && trackSelection != null) {
                    trackSelection.enable();
                }
            }
        }

        private void disableTrackSelectionsInResult(TrackSelectorResult trackSelectorResult2) {
            for (int i = 0; i < trackSelectorResult2.renderersEnabled.length; i++) {
                boolean rendererEnabled = trackSelectorResult2.renderersEnabled[i];
                TrackSelection trackSelection = trackSelectorResult2.selections.get(i);
                if (rendererEnabled && trackSelection != null) {
                    trackSelection.disable();
                }
            }
        }

        private void disassociateNoSampleRenderersWithEmptySampleStream(SampleStream[] sampleStreams2) {
            for (int i = 0; i < this.rendererCapabilities.length; i++) {
                if (this.rendererCapabilities[i].getTrackType() == 5) {
                    sampleStreams2[i] = null;
                }
            }
        }

        private void associateNoSampleRenderersWithEmptySampleStream(SampleStream[] sampleStreams2) {
            for (int i = 0; i < this.rendererCapabilities.length; i++) {
                if (this.rendererCapabilities[i].getTrackType() == 5 && this.trackSelectorResult.renderersEnabled[i]) {
                    sampleStreams2[i] = new EmptySampleStream();
                }
            }
        }
    }

    private static final class SeekPosition {
        public final Timeline timeline;
        public final int windowIndex;
        public final long windowPositionUs;

        public SeekPosition(Timeline timeline2, int windowIndex2, long windowPositionUs2) {
            this.timeline = timeline2;
            this.windowIndex = windowIndex2;
            this.windowPositionUs = windowPositionUs2;
        }
    }

    private static final class MediaSourceRefreshInfo {
        public final Object manifest;
        public final MediaSource source;
        public final Timeline timeline;

        public MediaSourceRefreshInfo(MediaSource source2, Timeline timeline2, Object manifest2) {
            this.source = source2;
            this.timeline = timeline2;
            this.manifest = manifest2;
        }
    }
}
