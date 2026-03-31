package com.google.android.exoplayer2;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.PlaybackParams;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

@TargetApi(16)
public class SimpleExoPlayer implements ExoPlayer {
    private static final String TAG = "SimpleExoPlayer";
    private AudioAttributes audioAttributes;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<AudioRendererEventListener> audioDebugListeners = new CopyOnWriteArraySet<>();
    /* access modifiers changed from: private */
    public DecoderCounters audioDecoderCounters;
    /* access modifiers changed from: private */
    public Format audioFormat;
    private final int audioRendererCount;
    /* access modifiers changed from: private */
    public int audioSessionId;
    private float audioVolume;
    private final ComponentListener componentListener = new ComponentListener();
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<MetadataOutput> metadataOutputs = new CopyOnWriteArraySet<>();
    private boolean ownsSurface;
    private final ExoPlayer player;
    protected final Renderer[] renderers;
    /* access modifiers changed from: private */
    public Surface surface;
    private SurfaceHolder surfaceHolder;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<TextOutput> textOutputs = new CopyOnWriteArraySet<>();
    private TextureView textureView;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<VideoRendererEventListener> videoDebugListeners = new CopyOnWriteArraySet<>();
    /* access modifiers changed from: private */
    public DecoderCounters videoDecoderCounters;
    /* access modifiers changed from: private */
    public Format videoFormat;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<VideoListener> videoListeners = new CopyOnWriteArraySet<>();
    private final int videoRendererCount;
    private int videoScalingMode;

    public interface VideoListener {
        void onRenderedFirstFrame();

        void onVideoSizeChanged(int i, int i2, int i3, float f);
    }

    protected SimpleExoPlayer(RenderersFactory renderersFactory, TrackSelector trackSelector, LoadControl loadControl) {
        this.renderers = renderersFactory.createRenderers(new Handler(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper()), this.componentListener, this.componentListener, this.componentListener, this.componentListener);
        int videoRendererCount2 = 0;
        int audioRendererCount2 = 0;
        for (Renderer renderer : this.renderers) {
            switch (renderer.getTrackType()) {
                case 1:
                    audioRendererCount2++;
                    break;
                case 2:
                    videoRendererCount2++;
                    break;
            }
        }
        this.videoRendererCount = videoRendererCount2;
        this.audioRendererCount = audioRendererCount2;
        this.audioVolume = 1.0f;
        this.audioSessionId = 0;
        this.audioAttributes = AudioAttributes.DEFAULT;
        this.videoScalingMode = 1;
        this.player = createExoPlayerImpl(this.renderers, trackSelector, loadControl);
    }

    public void setVideoScalingMode(int videoScalingMode2) {
        int count;
        this.videoScalingMode = videoScalingMode2;
        ExoPlayer.ExoPlayerMessage[] messages = new ExoPlayer.ExoPlayerMessage[this.videoRendererCount];
        Renderer[] rendererArr = this.renderers;
        int length = rendererArr.length;
        int i = 0;
        int count2 = 0;
        while (i < length) {
            Renderer renderer = rendererArr[i];
            if (renderer.getTrackType() == 2) {
                count = count2 + 1;
                messages[count2] = new ExoPlayer.ExoPlayerMessage(renderer, 4, Integer.valueOf(videoScalingMode2));
            } else {
                count = count2;
            }
            i++;
            count2 = count;
        }
        this.player.sendMessages(messages);
    }

    public int getVideoScalingMode() {
        return this.videoScalingMode;
    }

    public void clearVideoSurface() {
        setVideoSurface((Surface) null);
    }

    public void setVideoSurface(Surface surface2) {
        removeSurfaceCallbacks();
        setVideoSurfaceInternal(surface2, false);
    }

    public void clearVideoSurface(Surface surface2) {
        if (surface2 != null && surface2 == this.surface) {
            setVideoSurface((Surface) null);
        }
    }

    public void setVideoSurfaceHolder(SurfaceHolder surfaceHolder2) {
        removeSurfaceCallbacks();
        this.surfaceHolder = surfaceHolder2;
        if (surfaceHolder2 == null) {
            setVideoSurfaceInternal((Surface) null, false);
            return;
        }
        surfaceHolder2.addCallback(this.componentListener);
        Surface surface2 = surfaceHolder2.getSurface();
        if (surface2 == null || !surface2.isValid()) {
            surface2 = null;
        }
        setVideoSurfaceInternal(surface2, false);
    }

    public void clearVideoSurfaceHolder(SurfaceHolder surfaceHolder2) {
        if (surfaceHolder2 != null && surfaceHolder2 == this.surfaceHolder) {
            setVideoSurfaceHolder((SurfaceHolder) null);
        }
    }

    public void setVideoSurfaceView(SurfaceView surfaceView) {
        setVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }

    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        clearVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }

    public void setVideoTextureView(TextureView textureView2) {
        SurfaceTexture surfaceTexture;
        Surface surface2 = null;
        removeSurfaceCallbacks();
        this.textureView = textureView2;
        if (textureView2 == null) {
            setVideoSurfaceInternal((Surface) null, true);
            return;
        }
        if (textureView2.getSurfaceTextureListener() != null) {
            Log.w(TAG, "Replacing existing SurfaceTextureListener.");
        }
        textureView2.setSurfaceTextureListener(this.componentListener);
        if (textureView2.isAvailable()) {
            surfaceTexture = textureView2.getSurfaceTexture();
        } else {
            surfaceTexture = null;
        }
        if (surfaceTexture != null) {
            surface2 = new Surface(surfaceTexture);
        }
        setVideoSurfaceInternal(surface2, true);
    }

    public void clearVideoTextureView(TextureView textureView2) {
        if (textureView2 != null && textureView2 == this.textureView) {
            setVideoTextureView((TextureView) null);
        }
    }

    @Deprecated
    public void setAudioStreamType(int streamType) {
        int usage = Util.getAudioUsageForStreamType(streamType);
        setAudioAttributes(new AudioAttributes.Builder().setUsage(usage).setContentType(Util.getAudioContentTypeForStreamType(streamType)).build());
    }

    @Deprecated
    public int getAudioStreamType() {
        return Util.getStreamTypeForAudioUsage(this.audioAttributes.usage);
    }

    public void setAudioAttributes(AudioAttributes audioAttributes2) {
        int count;
        this.audioAttributes = audioAttributes2;
        ExoPlayer.ExoPlayerMessage[] messages = new ExoPlayer.ExoPlayerMessage[this.audioRendererCount];
        Renderer[] rendererArr = this.renderers;
        int length = rendererArr.length;
        int i = 0;
        int count2 = 0;
        while (i < length) {
            Renderer renderer = rendererArr[i];
            if (renderer.getTrackType() == 1) {
                count = count2 + 1;
                messages[count2] = new ExoPlayer.ExoPlayerMessage(renderer, 3, audioAttributes2);
            } else {
                count = count2;
            }
            i++;
            count2 = count;
        }
        this.player.sendMessages(messages);
    }

    public AudioAttributes getAudioAttributes() {
        return this.audioAttributes;
    }

    public void setVolume(float audioVolume2) {
        int count;
        this.audioVolume = audioVolume2;
        ExoPlayer.ExoPlayerMessage[] messages = new ExoPlayer.ExoPlayerMessage[this.audioRendererCount];
        Renderer[] rendererArr = this.renderers;
        int length = rendererArr.length;
        int i = 0;
        int count2 = 0;
        while (i < length) {
            Renderer renderer = rendererArr[i];
            if (renderer.getTrackType() == 1) {
                count = count2 + 1;
                messages[count2] = new ExoPlayer.ExoPlayerMessage(renderer, 2, Float.valueOf(audioVolume2));
            } else {
                count = count2;
            }
            i++;
            count2 = count;
        }
        this.player.sendMessages(messages);
    }

    public float getVolume() {
        return this.audioVolume;
    }

    @TargetApi(23)
    @Deprecated
    public void setPlaybackParams(@Nullable PlaybackParams params) {
        PlaybackParameters playbackParameters;
        if (params != null) {
            params.allowDefaults();
            playbackParameters = new PlaybackParameters(params.getSpeed(), params.getPitch());
        } else {
            playbackParameters = null;
        }
        setPlaybackParameters(playbackParameters);
    }

    public Format getVideoFormat() {
        return this.videoFormat;
    }

    public Format getAudioFormat() {
        return this.audioFormat;
    }

    public int getAudioSessionId() {
        return this.audioSessionId;
    }

    public DecoderCounters getVideoDecoderCounters() {
        return this.videoDecoderCounters;
    }

    public DecoderCounters getAudioDecoderCounters() {
        return this.audioDecoderCounters;
    }

    public void addVideoListener(VideoListener listener) {
        this.videoListeners.add(listener);
    }

    public void removeVideoListener(VideoListener listener) {
        this.videoListeners.remove(listener);
    }

    @Deprecated
    public void setVideoListener(VideoListener listener) {
        this.videoListeners.clear();
        if (listener != null) {
            addVideoListener(listener);
        }
    }

    @Deprecated
    public void clearVideoListener(VideoListener listener) {
        removeVideoListener(listener);
    }

    public void addTextOutput(TextOutput listener) {
        this.textOutputs.add(listener);
    }

    public void removeTextOutput(TextOutput listener) {
        this.textOutputs.remove(listener);
    }

    @Deprecated
    public void setTextOutput(TextOutput output) {
        this.textOutputs.clear();
        if (output != null) {
            addTextOutput(output);
        }
    }

    @Deprecated
    public void clearTextOutput(TextOutput output) {
        removeTextOutput(output);
    }

    public void addMetadataOutput(MetadataOutput listener) {
        this.metadataOutputs.add(listener);
    }

    public void removeMetadataOutput(MetadataOutput listener) {
        this.metadataOutputs.remove(listener);
    }

    @Deprecated
    public void setMetadataOutput(MetadataOutput output) {
        this.metadataOutputs.clear();
        if (output != null) {
            addMetadataOutput(output);
        }
    }

    @Deprecated
    public void clearMetadataOutput(MetadataOutput output) {
        removeMetadataOutput(output);
    }

    @Deprecated
    public void setVideoDebugListener(VideoRendererEventListener listener) {
        this.videoDebugListeners.clear();
        if (listener != null) {
            addVideoDebugListener(listener);
        }
    }

    public void addVideoDebugListener(VideoRendererEventListener listener) {
        this.videoDebugListeners.add(listener);
    }

    public void removeVideoDebugListener(VideoRendererEventListener listener) {
        this.videoDebugListeners.remove(listener);
    }

    @Deprecated
    public void setAudioDebugListener(AudioRendererEventListener listener) {
        this.audioDebugListeners.clear();
        if (listener != null) {
            addAudioDebugListener(listener);
        }
    }

    public void addAudioDebugListener(AudioRendererEventListener listener) {
        this.audioDebugListeners.add(listener);
    }

    public void removeAudioDebugListener(AudioRendererEventListener listener) {
        this.audioDebugListeners.remove(listener);
    }

    public Looper getPlaybackLooper() {
        return this.player.getPlaybackLooper();
    }

    public void addListener(Player.EventListener listener) {
        this.player.addListener(listener);
    }

    public void removeListener(Player.EventListener listener) {
        this.player.removeListener(listener);
    }

    public int getPlaybackState() {
        return this.player.getPlaybackState();
    }

    public void prepare(MediaSource mediaSource) {
        this.player.prepare(mediaSource);
    }

    public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
        this.player.prepare(mediaSource, resetPosition, resetState);
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        this.player.setPlayWhenReady(playWhenReady);
    }

    public boolean getPlayWhenReady() {
        return this.player.getPlayWhenReady();
    }

    public int getRepeatMode() {
        return this.player.getRepeatMode();
    }

    public void setRepeatMode(int repeatMode) {
        this.player.setRepeatMode(repeatMode);
    }

    public void setShuffleModeEnabled(boolean shuffleModeEnabled) {
        this.player.setShuffleModeEnabled(shuffleModeEnabled);
    }

    public boolean getShuffleModeEnabled() {
        return this.player.getShuffleModeEnabled();
    }

    public boolean isLoading() {
        return this.player.isLoading();
    }

    public void seekToDefaultPosition() {
        this.player.seekToDefaultPosition();
    }

    public void seekToDefaultPosition(int windowIndex) {
        this.player.seekToDefaultPosition(windowIndex);
    }

    public void seekTo(long positionMs) {
        this.player.seekTo(positionMs);
    }

    public void seekTo(int windowIndex, long positionMs) {
        this.player.seekTo(windowIndex, positionMs);
    }

    public void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters) {
        this.player.setPlaybackParameters(playbackParameters);
    }

    public PlaybackParameters getPlaybackParameters() {
        return this.player.getPlaybackParameters();
    }

    public void stop() {
        this.player.stop();
    }

    public void release() {
        this.player.release();
        removeSurfaceCallbacks();
        if (this.surface != null) {
            if (this.ownsSurface) {
                this.surface.release();
            }
            this.surface = null;
        }
    }

    public void sendMessages(ExoPlayer.ExoPlayerMessage... messages) {
        this.player.sendMessages(messages);
    }

    public void blockingSendMessages(ExoPlayer.ExoPlayerMessage... messages) {
        this.player.blockingSendMessages(messages);
    }

    public int getRendererCount() {
        return this.player.getRendererCount();
    }

    public int getRendererType(int index) {
        return this.player.getRendererType(index);
    }

    public TrackGroupArray getCurrentTrackGroups() {
        return this.player.getCurrentTrackGroups();
    }

    public TrackSelectionArray getCurrentTrackSelections() {
        return this.player.getCurrentTrackSelections();
    }

    public Timeline getCurrentTimeline() {
        return this.player.getCurrentTimeline();
    }

    public Object getCurrentManifest() {
        return this.player.getCurrentManifest();
    }

    public int getCurrentPeriodIndex() {
        return this.player.getCurrentPeriodIndex();
    }

    public int getCurrentWindowIndex() {
        return this.player.getCurrentWindowIndex();
    }

    public int getNextWindowIndex() {
        return this.player.getNextWindowIndex();
    }

    public int getPreviousWindowIndex() {
        return this.player.getPreviousWindowIndex();
    }

    public long getDuration() {
        return this.player.getDuration();
    }

    public long getCurrentPosition() {
        return this.player.getCurrentPosition();
    }

    public long getBufferedPosition() {
        return this.player.getBufferedPosition();
    }

    public int getBufferedPercentage() {
        return this.player.getBufferedPercentage();
    }

    public boolean isCurrentWindowDynamic() {
        return this.player.isCurrentWindowDynamic();
    }

    public boolean isCurrentWindowSeekable() {
        return this.player.isCurrentWindowSeekable();
    }

    public boolean isPlayingAd() {
        return this.player.isPlayingAd();
    }

    public int getCurrentAdGroupIndex() {
        return this.player.getCurrentAdGroupIndex();
    }

    public int getCurrentAdIndexInAdGroup() {
        return this.player.getCurrentAdIndexInAdGroup();
    }

    public long getContentPosition() {
        return this.player.getContentPosition();
    }

    /* access modifiers changed from: protected */
    public ExoPlayer createExoPlayerImpl(Renderer[] renderers2, TrackSelector trackSelector, LoadControl loadControl) {
        return new ExoPlayerImpl(renderers2, trackSelector, loadControl);
    }

    private void removeSurfaceCallbacks() {
        if (this.textureView != null) {
            if (this.textureView.getSurfaceTextureListener() != this.componentListener) {
                Log.w(TAG, "SurfaceTextureListener already unset or replaced.");
            } else {
                this.textureView.setSurfaceTextureListener((TextureView.SurfaceTextureListener) null);
            }
            this.textureView = null;
        }
        if (this.surfaceHolder != null) {
            this.surfaceHolder.removeCallback(this.componentListener);
            this.surfaceHolder = null;
        }
    }

    /* access modifiers changed from: private */
    public void setVideoSurfaceInternal(Surface surface2, boolean ownsSurface2) {
        int count;
        ExoPlayer.ExoPlayerMessage[] messages = new ExoPlayer.ExoPlayerMessage[this.videoRendererCount];
        Renderer[] rendererArr = this.renderers;
        int length = rendererArr.length;
        int i = 0;
        int count2 = 0;
        while (i < length) {
            Renderer renderer = rendererArr[i];
            if (renderer.getTrackType() == 2) {
                count = count2 + 1;
                messages[count2] = new ExoPlayer.ExoPlayerMessage(renderer, 1, surface2);
            } else {
                count = count2;
            }
            i++;
            count2 = count;
        }
        if (this.surface == null || this.surface == surface2) {
            this.player.sendMessages(messages);
        } else {
            this.player.blockingSendMessages(messages);
            if (this.ownsSurface) {
                this.surface.release();
            }
        }
        this.surface = surface2;
        this.ownsSurface = ownsSurface2;
    }

    private final class ComponentListener implements VideoRendererEventListener, AudioRendererEventListener, TextOutput, MetadataOutput, SurfaceHolder.Callback, TextureView.SurfaceTextureListener {
        private ComponentListener() {
        }

        public void onVideoEnabled(DecoderCounters counters) {
            DecoderCounters unused = SimpleExoPlayer.this.videoDecoderCounters = counters;
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onVideoEnabled(counters);
            }
        }

        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onVideoDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
            }
        }

        public void onVideoInputFormatChanged(Format format) {
            Format unused = SimpleExoPlayer.this.videoFormat = format;
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onVideoInputFormatChanged(format);
            }
        }

        public void onDroppedFrames(int count, long elapsed) {
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onDroppedFrames(count, elapsed);
            }
        }

        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            Iterator it = SimpleExoPlayer.this.videoListeners.iterator();
            while (it.hasNext()) {
                ((VideoListener) it.next()).onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
            }
            Iterator it2 = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it2.hasNext()) {
                ((VideoRendererEventListener) it2.next()).onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
            }
        }

        public void onRenderedFirstFrame(Surface surface) {
            if (SimpleExoPlayer.this.surface == surface) {
                Iterator it = SimpleExoPlayer.this.videoListeners.iterator();
                while (it.hasNext()) {
                    ((VideoListener) it.next()).onRenderedFirstFrame();
                }
            }
            Iterator it2 = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it2.hasNext()) {
                ((VideoRendererEventListener) it2.next()).onRenderedFirstFrame(surface);
            }
        }

        public void onVideoDisabled(DecoderCounters counters) {
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onVideoDisabled(counters);
            }
            Format unused = SimpleExoPlayer.this.videoFormat = null;
            DecoderCounters unused2 = SimpleExoPlayer.this.videoDecoderCounters = null;
        }

        public void onAudioEnabled(DecoderCounters counters) {
            DecoderCounters unused = SimpleExoPlayer.this.audioDecoderCounters = counters;
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioEnabled(counters);
            }
        }

        public void onAudioSessionId(int sessionId) {
            int unused = SimpleExoPlayer.this.audioSessionId = sessionId;
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioSessionId(sessionId);
            }
        }

        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
            }
        }

        public void onAudioInputFormatChanged(Format format) {
            Format unused = SimpleExoPlayer.this.audioFormat = format;
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioInputFormatChanged(format);
            }
        }

        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioSinkUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
            }
        }

        public void onAudioDisabled(DecoderCounters counters) {
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioDisabled(counters);
            }
            Format unused = SimpleExoPlayer.this.audioFormat = null;
            DecoderCounters unused2 = SimpleExoPlayer.this.audioDecoderCounters = null;
            int unused3 = SimpleExoPlayer.this.audioSessionId = 0;
        }

        public void onCues(List<Cue> cues) {
            Iterator it = SimpleExoPlayer.this.textOutputs.iterator();
            while (it.hasNext()) {
                ((TextOutput) it.next()).onCues(cues);
            }
        }

        public void onMetadata(Metadata metadata) {
            Iterator it = SimpleExoPlayer.this.metadataOutputs.iterator();
            while (it.hasNext()) {
                ((MetadataOutput) it.next()).onMetadata(metadata);
            }
        }

        public void surfaceCreated(SurfaceHolder holder) {
            SimpleExoPlayer.this.setVideoSurfaceInternal(holder.getSurface(), false);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            SimpleExoPlayer.this.setVideoSurfaceInternal((Surface) null, false);
        }

        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            SimpleExoPlayer.this.setVideoSurfaceInternal(new Surface(surfaceTexture), true);
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            SimpleExoPlayer.this.setVideoSurfaceInternal((Surface) null, true);
            return true;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    }
}
