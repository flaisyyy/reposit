package com.ua.mytrinity.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import java.util.Map;

public class TrinityPlayer extends TapableSurfaceView implements MediaController.MediaPlayerControl, TrinityPlayerInterface {
    protected static final int STATE_ERROR = -1;
    protected static final int STATE_IDLE = 0;
    protected static final int STATE_PAUSED = 4;
    protected static final int STATE_PLAYBACK_COMPLETED = 5;
    protected static final int STATE_PLAYING = 3;
    protected static final int STATE_PREPARED = 2;
    protected static final int STATE_PREPARING = 1;
    private static final int STATE_RESUME = 7;
    private static final int STATE_SUSPEND = 6;
    private static final int STATE_SUSPEND_UNSUPPORTED = 8;
    private static final String TAG = "Player";
    protected boolean mCanPause;
    protected boolean mCanSeekBack;
    protected boolean mCanSeekForward;
    protected Context mContext;
    protected int mCurrentBufferPercentage;
    protected int mCurrentState = 0;
    protected int mDuration;
    protected MediaPlayer.OnCompletionListener mOnCompletionListener;
    protected MediaPlayer.OnErrorListener mOnErrorListener;
    protected MediaPlayer.OnPreparedListener mOnPreparedListener;
    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            boolean isValidState;
            boolean hasValidSize;
            Log.i(TrinityPlayer.TAG, "SurfaceChanged");
            TrinityPlayer.this.mSurfaceWidth = w;
            TrinityPlayer.this.mSurfaceHeight = h;
            if (TrinityPlayer.this.mTargetState == 3) {
                isValidState = true;
            } else {
                isValidState = false;
            }
            if (TrinityPlayer.this.mVideoWidth == w && TrinityPlayer.this.mVideoHeight == h) {
                hasValidSize = true;
            } else {
                hasValidSize = false;
            }
            if (TrinityPlayer.this.isMediaPlayerExists() && isValidState && hasValidSize) {
                TrinityPlayer.this.start();
            }
        }

        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TrinityPlayer.TAG, "surfaceCreated(SurfaceHolder holder)");
            TrinityPlayer.this.mSurfaceHolder = holder;
            TrinityPlayer.this.openVideo();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TrinityPlayer.TAG, "surfaceDestroyed(SurfaceHolder holder)");
            TrinityPlayer.this.mSurfaceHolder = null;
            if (TrinityPlayer.this.mCurrentState != 6) {
                TrinityPlayer.this.release(true);
            }
        }
    };
    protected int mSurfaceHeight;
    protected SurfaceHolder mSurfaceHolder = null;
    protected int mSurfaceWidth;
    protected int mTargetState = 0;
    protected Uri mUri;
    protected float mVideoAspectRatio;
    protected int mVideoHeight;
    protected int mVideoWidth;
    private DisplayMode screenMode = DisplayMode.BEST_FIT;

    public TrinityPlayer(Context context) {
        super(context);
        initVideoView(context);
    }

    public TrinityPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public TrinityPlayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initVideoView(context);
    }

    public enum DisplayMode {
        BEST_FIT,
        FIT_HORIZONTAL,
        FIT_VERTICAL,
        FILL,
        ORIGINAL;

        public DisplayMode getNext() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(this.mVideoWidth, widthMeasureSpec), getDefaultSize(this.mVideoHeight, heightMeasureSpec));
    }

    public void setVideoLayout() {
        int i;
        int i2;
        if (this.mVideoWidth * this.mVideoHeight != 0) {
            ViewGroup.LayoutParams lp = getLayoutParams();
            DisplayMetrics disp = this.mContext.getResources().getDisplayMetrics();
            int windowWidth = disp.widthPixels;
            int windowHeight = disp.heightPixels;
            float windowRatio = ((float) windowWidth) / ((float) windowHeight);
            float videoRatio = this.mVideoAspectRatio;
            this.mSurfaceHeight = this.mVideoHeight;
            this.mSurfaceWidth = this.mVideoWidth;
            switch (this.screenMode) {
                case FIT_HORIZONTAL:
                    lp.width = windowWidth;
                    lp.height = (int) (((float) windowWidth) / videoRatio);
                    break;
                case FIT_VERTICAL:
                    lp.width = (int) (((float) windowHeight) * videoRatio);
                    lp.height = windowHeight;
                    break;
                case FILL:
                    lp.width = windowWidth;
                    lp.height = windowHeight;
                    break;
                case ORIGINAL:
                    if (this.mSurfaceWidth < windowWidth && this.mSurfaceHeight < windowHeight) {
                        lp.width = (int) (((float) this.mSurfaceHeight) * videoRatio);
                        lp.height = this.mSurfaceHeight;
                        break;
                    } else {
                        lp.width = windowRatio < videoRatio ? windowWidth : (int) (((float) windowHeight) * videoRatio);
                        if (windowRatio > videoRatio) {
                            i = windowHeight;
                        } else {
                            i = (int) (((float) windowWidth) / videoRatio);
                        }
                        lp.height = i;
                        break;
                    }
                default:
                    lp.width = windowRatio > videoRatio ? windowWidth : (int) (((float) windowHeight) * videoRatio);
                    if (windowRatio < videoRatio) {
                        i2 = windowHeight;
                    } else {
                        i2 = (int) (((float) windowWidth) / videoRatio);
                    }
                    lp.height = i2;
                    break;
            }
            setLayoutParams(lp);
            getHolder().setFixedSize(this.mSurfaceWidth, this.mSurfaceHeight);
            Log.i(TAG, String.format("VIDEO: %dx%dx%f, Surface: %dx%d, LP: %dx%d, Window: %dx%dx%f", new Object[]{Integer.valueOf(this.mVideoWidth), Integer.valueOf(this.mVideoHeight), Float.valueOf(this.mVideoAspectRatio), Integer.valueOf(this.mSurfaceWidth), Integer.valueOf(this.mSurfaceHeight), Integer.valueOf(lp.width), Integer.valueOf(lp.height), Integer.valueOf(windowWidth), Integer.valueOf(windowHeight), Float.valueOf(windowRatio)}));
        }
    }

    public void setDisplayMode(DisplayMode mode) {
        this.screenMode = mode;
        setVideoLayout();
        invalidate();
    }

    public DisplayMode getDisplayMode() {
        return this.screenMode;
    }

    public int resolveAdjustedSize(int desiredSize, int measureSpec) {
        Log.i(TAG, "resolveAdjustedSize(int desiredSize, int measureSpec)");
        int result = desiredSize;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case Integer.MIN_VALUE:
                return Math.min(desiredSize, specSize);
            case 0:
                return desiredSize;
            case 1073741824:
                return specSize;
            default:
                return result;
        }
    }

    public void setVideoPath(String path) {
        Log.i(TAG, "setVideoPath(" + path + ")");
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {
        Log.i(TAG, "setVideoURI(" + uri.toString() + ")");
        setVideoURI(uri, (Map<String, String>) null);
    }

    public void setVideoURI(Uri uri, Map<String, String> map) {
        Log.i(TAG, "setVideoURI(Uri uri, Map<String, String> headers)");
        this.mUri = uri;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void stopPlayback() {
        Log.i(TAG, "stopPlayback()");
        if (isMediaPlayerExists()) {
            mpStop();
            releaseMediaPlayer();
            this.mCurrentState = 0;
            this.mTargetState = 0;
        }
    }

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
        this.mOnPreparedListener = l;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener l) {
        this.mOnCompletionListener = l;
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener l) {
        this.mOnErrorListener = l;
    }

    public void suspend() {
        if (isInPlaybackState()) {
            release(false);
            this.mCurrentState = 8;
            Log.d(TAG, "Unable to suspend video. Release MediaPlayer.");
        }
    }

    public void resume() {
        if (this.mSurfaceHolder == null && this.mCurrentState == 6) {
            this.mTargetState = 7;
        } else if (this.mCurrentState == 8) {
            openVideo();
        }
    }

    public boolean canPause() {
        Log.i(TAG, "canPause()");
        return this.mCanPause;
    }

    public boolean canSeekBackward() {
        Log.i(TAG, "canSeekBackward()");
        return this.mCanSeekBack;
    }

    public boolean canSeekForward() {
        Log.i(TAG, "canSeekForward()");
        return this.mCanSeekForward;
    }

    public int getAudioSessionId() {
        return 0;
    }

    public int getBufferPercentage() {
        Log.i(TAG, "getBufferPercentage()");
        if (isMediaPlayerExists()) {
            return this.mCurrentBufferPercentage;
        }
        return 0;
    }

    public int getCurrentPosition() {
        Log.i(TAG, "getCurrentPosition()");
        if (isInPlaybackState()) {
        }
        return 0;
    }

    public int getDuration() {
        Log.i(TAG, "getDuration()");
        if (!isInPlaybackState()) {
            this.mDuration = -1;
            return this.mDuration;
        } else if (this.mDuration > 0) {
            return this.mDuration;
        } else {
            return this.mDuration;
        }
    }

    public boolean isPlaying() {
        Log.i(TAG, "isPlaying()");
        return isInPlaybackState() && mpIsPlaying();
    }

    public void pause() {
        Log.i(TAG, "pause()");
        if (isInPlaybackState() && mpIsPlaying()) {
            mpPause();
            this.mCurrentState = 4;
        }
        this.mTargetState = 4;
    }

    public void seekTo(int arg0) {
        Log.i(TAG, "seekTo(int pos)");
    }

    public void start() {
        Log.i(TAG, "start()");
        if (isInPlaybackState()) {
            mpPlay();
            this.mCurrentState = 3;
            postInvalidateDelayed(100);
        }
        this.mTargetState = 3;
    }

    /* access modifiers changed from: protected */
    public void onVideoSizeChanged(int width, int height) {
        Log.i(TAG, "onVideoSizeChanged(MediaPlayer mp, int width, int height)");
        this.mVideoWidth = width;
        this.mVideoHeight = height;
        this.mVideoAspectRatio = ((float) width) / ((float) height);
        if (this.mVideoWidth != 0 && this.mVideoHeight != 0) {
            setVideoLayout();
        }
    }

    /* access modifiers changed from: protected */
    public void onPrepared(int width, int height) {
        Log.i(TAG, "onPrepared(MediaPlayer mp)");
        this.mCurrentState = 2;
        this.mCanSeekForward = false;
        this.mCanSeekBack = false;
        this.mCanPause = false;
        if (this.mOnPreparedListener != null) {
            this.mOnPreparedListener.onPrepared((MediaPlayer) null);
        }
        this.mVideoWidth = width;
        this.mVideoHeight = height;
        this.mVideoAspectRatio = ((float) width) / ((float) height);
        if (this.mVideoWidth != 0 && this.mVideoHeight != 0) {
            setVideoLayout();
            if (this.mSurfaceWidth != this.mVideoWidth || this.mSurfaceHeight != this.mVideoHeight) {
                return;
            }
            if (this.mTargetState == 3) {
                start();
                return;
            }
            if (isPlaying() || getCurrentPosition() > 0) {
            }
        } else if (this.mTargetState == 3) {
            start();
        }
    }

    /* access modifiers changed from: protected */
    public void onCompletion() {
        Log.i(TAG, "onCompletion(MediaPlayer mp)");
        this.mCurrentState = 5;
        this.mTargetState = 5;
        if (this.mOnCompletionListener != null) {
        }
    }

    /* access modifiers changed from: protected */
    public boolean onError(int framework_err, int impl_err) {
        Log.i(TAG, "onError(framework_err=" + framework_err + ", int impl_err=" + impl_err + ")");
        this.mCurrentState = -1;
        this.mTargetState = -1;
        if (this.mOnErrorListener != null) {
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onBufferingUpdate(int percent) {
        this.mCurrentBufferPercentage = percent;
    }

    private void initVideoView(Context context) {
        Log.i(TAG, "initVideoView()");
        this.mContext = context;
        this.mVideoWidth = 0;
        this.mVideoHeight = 0;
        getHolder().addCallback(this.mSHCallback);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        this.mCurrentState = 0;
        this.mTargetState = 0;
    }

    /* access modifiers changed from: private */
    public void openVideo() {
        Log.i(TAG, "openVideo()");
        if (this.mUri != null && this.mSurfaceHolder != null) {
            release(false);
            this.mDuration = -1;
            this.mCurrentBufferPercentage = 0;
            if (createMediaPlayer()) {
                this.mCurrentState = 1;
            }
        }
    }

    /* access modifiers changed from: private */
    public void release(boolean cleartargetstate) {
        Log.i(TAG, "release(boolean cleartargetstate)");
        if (isMediaPlayerExists()) {
            releaseMediaPlayer();
            this.mCurrentState = 0;
            if (cleartargetstate) {
                this.mTargetState = 0;
            }
        }
    }

    private boolean isInPlaybackState() {
        Log.i(TAG, "isInPlaybackState()");
        if (!isMediaPlayerExists() || this.mCurrentState == -1 || this.mCurrentState == 0 || this.mCurrentState == 1) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean createMediaPlayer() {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isMediaPlayerExists() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void releaseMediaPlayer() {
    }

    /* access modifiers changed from: protected */
    public boolean mpIsPlaying() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void mpPlay() {
    }

    /* access modifiers changed from: protected */
    public void mpPause() {
    }

    /* access modifiers changed from: protected */
    public void mpStop() {
    }
}
