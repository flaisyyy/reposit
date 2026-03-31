package com.ua.mytrinity.ui.tv;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.util.AttributeSet;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.verticalslidevar.ScreenVerticalBar;

public class SoundBar extends ScreenVerticalBar {
    private AudioManager m_audio_manager;
    private int m_max;
    private Drawable m_mute;
    private Drawable m_volume;

    public SoundBar(Context context) {
        super(context);
    }

    public SoundBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SoundBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAudioManager(AudioManager manager) {
        this.m_audio_manager = manager;
        this.m_max = this.m_audio_manager.getStreamMaxVolume(3);
        setMax(15);
        setCurrent((this.m_audio_manager.getStreamVolume(3) * getMax()) / this.m_max);
    }

    /* access modifiers changed from: protected */
    public void onProgressChanged(int value) {
        if (value > 0) {
            if (this.m_volume == null) {
                this.m_volume = getContext().getResources().getDrawable(R.drawable.volume);
            }
            setText(String.valueOf(value), this.m_volume);
        } else {
            if (this.m_mute == null) {
                this.m_mute = getContext().getResources().getDrawable(R.drawable.volume_mute);
            }
            setText("", this.m_mute);
        }
        if (this.m_audio_manager != null) {
            this.m_audio_manager.setStreamVolume(3, (this.m_max * value) / getMax(), 0);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.m_bar.setMax(15);
    }
}
