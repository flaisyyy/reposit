package com.ua.mytrinity.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import java.io.IOException;

public class TrinityPlayerSystem extends TrinityPlayer {
    private static final String TAG = "Player/System";
    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener;
    private MediaPlayer.OnCompletionListener mCompletionListener;
    private MediaPlayer.OnErrorListener mErrorListener;
    private MediaPlayer mMediaPlayer;
    private MediaPlayer.OnPreparedListener mPreparedListener;
    private MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener;

    public TrinityPlayerSystem(Context context) {
        super(context);
        this.mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                TrinityPlayerSystem.this.onVideoSizeChanged(mp.getVideoWidth(), mp.getVideoHeight());
            }
        };
        this.mPreparedListener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                TrinityPlayerSystem.this.onPrepared(mp.getVideoWidth(), mp.getVideoHeight());
            }
        };
        this.mCompletionListener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                TrinityPlayerSystem.this.onCompletion();
            }
        };
        this.mErrorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
                return TrinityPlayerSystem.this.onError(framework_err, impl_err);
            }
        };
        this.mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                TrinityPlayerSystem.this.onBufferingUpdate(percent);
            }
        };
    }

    public TrinityPlayerSystem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrinityPlayerSystem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                TrinityPlayerSystem.this.onVideoSizeChanged(mp.getVideoWidth(), mp.getVideoHeight());
            }
        };
        this.mPreparedListener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                TrinityPlayerSystem.this.onPrepared(mp.getVideoWidth(), mp.getVideoHeight());
            }
        };
        this.mCompletionListener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                TrinityPlayerSystem.this.onCompletion();
            }
        };
        this.mErrorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
                return TrinityPlayerSystem.this.onError(framework_err, impl_err);
            }
        };
        this.mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                TrinityPlayerSystem.this.onBufferingUpdate(percent);
            }
        };
    }

    /* access modifiers changed from: protected */
    public boolean createMediaPlayer() {
        try {
            this.mMediaPlayer = new MediaPlayer();
            this.mMediaPlayer.setOnPreparedListener(this.mPreparedListener);
            this.mMediaPlayer.setOnVideoSizeChangedListener(this.mSizeChangedListener);
            this.mDuration = -1;
            this.mMediaPlayer.setOnCompletionListener(this.mCompletionListener);
            this.mMediaPlayer.setOnErrorListener(this.mErrorListener);
            this.mMediaPlayer.setOnBufferingUpdateListener(this.mBufferingUpdateListener);
            this.mCurrentBufferPercentage = 0;
            this.mMediaPlayer.setDataSource(getContext(), this.mUri);
            this.mMediaPlayer.setDisplay(this.mSurfaceHolder);
            this.mMediaPlayer.setScreenOnWhilePlaying(true);
            this.mMediaPlayer.prepareAsync();
            return true;
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + this.mUri, ex);
            onError(1, 0);
        } catch (IllegalArgumentException ex2) {
            Log.w(TAG, "Unable to open content: " + this.mUri, ex2);
            onError(1, 0);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isMediaPlayerExists() {
        return this.mMediaPlayer != null;
    }

    /* access modifiers changed from: protected */
    public void releaseMediaPlayer() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.reset();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }
    }

    /* access modifiers changed from: protected */
    public boolean mpIsPlaying() {
        return this.mMediaPlayer.isPlaying();
    }

    /* access modifiers changed from: protected */
    public void mpPlay() {
        this.mMediaPlayer.start();
    }

    /* access modifiers changed from: protected */
    public void mpPause() {
        this.mMediaPlayer.pause();
    }

    /* access modifiers changed from: protected */
    public void mpStop() {
        this.mMediaPlayer.stop();
    }

    public int getAudioSessionId() {
        if (this.mMediaPlayer != null) {
            return this.mMediaPlayer.getAudioSessionId();
        }
        return 0;
    }
}
