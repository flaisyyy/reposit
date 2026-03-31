package com.ua.mytrinity.verticalslidevar;

import android.content.Context;
import android.util.AttributeSet;
import com.ua.mytrinity.verticalslidevar.DefaultSeekBar;

public class VerticalSeekBar extends AbsVerticalSeekBar implements DefaultSeekBar {
    private OnSeekBarChangeListener m_on_seek_bar_change_listener;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(VerticalSeekBar verticalSeekBar, int i, boolean z);

        void onStartTrackingTouch(VerticalSeekBar verticalSeekBar);

        void onStopTrackingTouch(VerticalSeekBar verticalSeekBar);
    }

    public VerticalSeekBar(Context context) {
        this(context, (AttributeSet) null);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 16842875);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /* access modifiers changed from: protected */
    public void onProgressRefresh(float scale, boolean fromUser) {
        super.onProgressRefresh(scale, fromUser);
        if (this.m_on_seek_bar_change_listener != null) {
            this.m_on_seek_bar_change_listener.onProgressChanged(this, getProgress(), fromUser);
        }
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        this.m_on_seek_bar_change_listener = l;
    }

    /* access modifiers changed from: package-private */
    public void onStartTrackingTouch() {
        if (this.m_on_seek_bar_change_listener != null) {
            this.m_on_seek_bar_change_listener.onStartTrackingTouch(this);
        }
    }

    /* access modifiers changed from: package-private */
    public void onStopTrackingTouch() {
        if (this.m_on_seek_bar_change_listener != null) {
            this.m_on_seek_bar_change_listener.onStopTrackingTouch(this);
        }
    }

    public void setOnDefaultSeekBarChangeListener(DefaultSeekBar.DefaultSeekBarChangeListener l) {
        setOnSeekBarChangeListener(new SeekBarChangeListener(l));
    }

    class SeekBarChangeListener implements OnSeekBarChangeListener {
        DefaultSeekBar.DefaultSeekBarChangeListener l;

        public SeekBarChangeListener(DefaultSeekBar.DefaultSeekBarChangeListener l2) {
            this.l = l2;
        }

        public void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser) {
            this.l.onProgressChanged(progress, fromUser);
        }

        public void onStartTrackingTouch(VerticalSeekBar seekBar) {
        }

        public void onStopTrackingTouch(VerticalSeekBar seekBar) {
        }
    }
}
