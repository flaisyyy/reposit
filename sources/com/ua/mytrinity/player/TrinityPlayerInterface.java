package com.ua.mytrinity.player;

import android.media.MediaPlayer;
import android.net.Uri;
import java.util.Map;

public interface TrinityPlayerInterface {
    boolean canPause();

    boolean canSeekBackward();

    boolean canSeekForward();

    int getBufferPercentage();

    int getCurrentPosition();

    int getDuration();

    boolean isPlaying();

    void pause();

    int resolveAdjustedSize(int i, int i2);

    void resume();

    void seekTo(int i);

    void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener);

    void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener);

    void setOnPreparedListener(MediaPlayer.OnPreparedListener onPreparedListener);

    void setVideoPath(String str);

    void setVideoURI(Uri uri);

    void setVideoURI(Uri uri, Map<String, String> map);

    void start();

    void stopPlayback();

    void suspend();
}
