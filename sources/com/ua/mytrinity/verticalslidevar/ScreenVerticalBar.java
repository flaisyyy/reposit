package com.ua.mytrinity.verticalslidevar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.verticalslidevar.VerticalSeekBar;

public abstract class ScreenVerticalBar extends RelativeLayout implements Animation.AnimationListener, VerticalSeekBar.OnSeekBarChangeListener {
    private int h = 0;
    private Animation m_animation;
    protected VerticalSeekBar m_bar;
    private Runnable m_hide = new Runnable() {
        public void run() {
            ScreenVerticalBar.this.hide();
        }
    };
    protected ImageView m_image;
    private Runnable m_remove = new Runnable() {
        public void run() {
            ViewGroup p = (ViewGroup) ScreenVerticalBar.this.getParent();
            if (p != null) {
                p.removeView(ScreenVerticalBar.this);
            }
        }
    };
    protected TextView m_text;

    /* access modifiers changed from: protected */
    public abstract void onProgressChanged(int i);

    public ScreenVerticalBar(Context context) {
        super(context);
        init(context);
    }

    public ScreenVerticalBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScreenVerticalBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.vertical_bar, this);
        this.m_bar = (VerticalSeekBar) findViewById(R.id.bar);
        this.m_bar.setOnSeekBarChangeListener(this);
        this.m_animation = AnimationUtils.loadAnimation(getContext(), R.anim.vertical_bar);
        this.m_animation.setAnimationListener(this);
        this.m_text = (TextView) findViewById(R.id.text);
        this.m_image = (ImageView) findViewById(R.id.image);
        onProgressChanged(this.m_bar.getProgress());
    }

    public void onAnimationEnd(Animation animation) {
        if (getVisibility() != 0) {
            post(this.m_remove);
        }
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser) {
        onProgressChanged(progress);
    }

    public void onStartTrackingTouch(VerticalSeekBar seekBar) {
        disableHide();
    }

    public void onStopTrackingTouch(VerticalSeekBar seekBar) {
        enableHide();
    }

    public final void hide() {
        if (getVisibility() == 0) {
            setVisibility(4);
            startAnimation(this.m_animation);
        }
    }

    public final void show() {
        removeCallbacks(this.m_remove);
        resetHide();
        if (getVisibility() != 0) {
            setVisibility(0);
        }
    }

    public final void disableHide() {
        int i = this.h;
        this.h = i + 1;
        if (i == 0) {
            removeCallbacks(this.m_hide);
        }
    }

    public final void enableHide() {
        int i = this.h - 1;
        this.h = i;
        if (i == 0) {
            postDelayed(this.m_hide, 1000);
        }
    }

    public final void resetHide() {
        if (this.h == 0) {
            removeCallbacks(this.m_hide);
            postDelayed(this.m_hide, 1000);
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        show();
        return super.dispatchKeyEvent(event);
    }

    public boolean dispatchTouchEvent(MotionEvent paramMotionEvent) {
        show();
        return super.dispatchTouchEvent(paramMotionEvent);
    }

    public boolean dispatchTrackballEvent(MotionEvent paramMotionEvent) {
        show();
        return super.dispatchTrackballEvent(paramMotionEvent);
    }

    public final void setText(CharSequence text, Drawable draw) {
        this.m_text.setText(text);
        this.m_image.setImageDrawable(draw);
    }

    public int getCurrent() {
        return this.m_bar.getProgress();
    }

    public void setCurrent(int value) {
        this.m_bar.setProgress(value);
        resetHide();
    }

    public void incrementProgressBy(int diff) {
        this.m_bar.incrementProgressBy(diff);
        resetHide();
    }

    public int getMax() {
        return this.m_bar.getMax();
    }

    public void setMax(int value) {
        this.m_bar.setMax(value);
    }
}
