package com.ua.mytrinity.player;

import android.content.Context;
import android.util.AttributeSet;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class TrinityPlayerExo extends TrinityPlayer {
    private SimpleExoPlayer mPlayer;

    public TrinityPlayerExo(Context context) {
        super(context);
    }

    public TrinityPlayerExo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrinityPlayerExo(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /* access modifiers changed from: protected */
    public boolean createMediaPlayer() {
        this.mPlayer = ExoPlayerFactory.newSimpleInstance((RenderersFactory) new DefaultRenderersFactory(getContext(), (DrmSessionManager<FrameworkMediaCrypto>) null, 2), new DefaultTrackSelector());
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(), "myApp"));
        extractorsFactory.setTsExtractorFlags(1);
        this.mPlayer.prepare(new ExtractorMediaSource.Factory(dataSourceFactory).setExtractorsFactory(extractorsFactory).createMediaSource(this.mUri));
        PlayerEventListener playerEventListener = new PlayerEventListener();
        this.mPlayer.addListener(playerEventListener);
        this.mPlayer.addVideoListener(playerEventListener);
        this.mPlayer.setVideoSurfaceHolder(this.mSurfaceHolder);
        this.mPlayer.setPlayWhenReady(true);
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean isMediaPlayerExists() {
        return this.mPlayer != null;
    }

    /* access modifiers changed from: protected */
    public void releaseMediaPlayer() {
        if (this.mPlayer != null) {
            this.mPlayer.release();
            this.mPlayer = null;
        }
    }

    private class PlayerEventListener extends Player.DefaultEventListener implements SimpleExoPlayer.VideoListener {
        private PlayerEventListener() {
        }

        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == 4) {
                TrinityPlayerExo.this.onCompletion();
            }
        }

        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            TrinityPlayerExo.this.onVideoSizeChanged(width, height);
        }

        public void onRenderedFirstFrame() {
        }
    }

    /* access modifiers changed from: protected */
    public boolean mpIsPlaying() {
        return this.mPlayer.getPlayWhenReady();
    }

    /* access modifiers changed from: protected */
    public void mpPlay() {
        this.mPlayer.setPlayWhenReady(true);
    }

    /* access modifiers changed from: protected */
    public void mpPause() {
        this.mPlayer.setPlayWhenReady(false);
    }

    /* access modifiers changed from: protected */
    public void mpStop() {
        this.mPlayer.stop();
    }
}
