package com.ua.mytrinity.verticalslidevar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.ua.mytrinity.player.R;

public class AbsVerticalSeekBar extends VerticalProgressBar {
    private static final int NO_ALPHA = 255;
    private float m_disabled_alpha;
    boolean m_is_user_seekable = true;
    private int m_key_progress_increment = 1;
    private Drawable m_thumb;
    private int m_thumb_offset;
    float m_touch_progress_offset;

    public AbsVerticalSeekBar(Context context) {
        super(context, (AttributeSet) null);
    }

    public AbsVerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsVerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBar, defStyle, 0);
        setThumb(a.getDrawable(0));
        setThumbOffset(a.getDimensionPixelOffset(1, getThumbOffset()));
        a.recycle();
        TypedArray a2 = context.obtainStyledAttributes(attrs, R.styleable.Theme, 0, 0);
        this.m_disabled_alpha = a2.getFloat(0, 0.5f);
        a2.recycle();
    }

    public void setThumb(Drawable thumb) {
        if (thumb != null) {
            thumb.setCallback(this);
            this.m_thumb_offset = thumb.getIntrinsicHeight() / 2;
        }
        this.m_thumb = thumb;
        invalidate();
    }

    public int getThumbOffset() {
        return this.m_thumb_offset;
    }

    public void setThumbOffset(int thumbOffset) {
        this.m_thumb_offset = thumbOffset;
        invalidate();
    }

    public void setKeyProgressIncrement(int increment) {
        if (increment < 0) {
            increment = -increment;
        }
        this.m_key_progress_increment = increment;
    }

    public int getKeyProgressIncrement() {
        return this.m_key_progress_increment;
    }

    public synchronized void setMax(int max) {
        super.setMax(max);
        if (this.m_key_progress_increment == 0 || getMax() / this.m_key_progress_increment > 20) {
            setKeyProgressIncrement(Math.max(1, Math.round(((float) getMax()) / 20.0f)));
        }
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable who) {
        return who == this.m_thumb || super.verifyDrawable(who);
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null) {
            progressDrawable.setAlpha(isEnabled() ? 255 : (int) (255.0f * this.m_disabled_alpha));
        }
        if (this.m_thumb != null && this.m_thumb.isStateful()) {
            this.m_thumb.setState(getDrawableState());
        }
    }

    /* access modifiers changed from: protected */
    public void onProgressRefresh(float scale, boolean fromUser) {
        Drawable thumb = this.m_thumb;
        if (thumb != null) {
            setThumbPos(getHeight(), thumb, scale, Integer.MIN_VALUE);
            invalidate();
        }
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        Drawable d = getCurrentDrawable();
        Drawable thumb = this.m_thumb;
        int thumbWidth = thumb == null ? 0 : thumb.getIntrinsicWidth();
        int trackWidth = Math.min(this.m_max_width, (w - this.m_padding_right) - this.m_padding_left);
        int max = getMax();
        float scale = max > 0 ? ((float) getProgress()) / ((float) max) : 0.0f;
        if (thumbWidth > trackWidth) {
            int gapForCenteringTrack = (thumbWidth - trackWidth) / 2;
            if (thumb != null) {
                setThumbPos(h, thumb, scale, gapForCenteringTrack * -1);
            }
            if (d != null) {
                d.setBounds(gapForCenteringTrack, 0, ((w - this.m_padding_right) - this.m_padding_left) - gapForCenteringTrack, (h - this.m_padding_bottom) - this.m_padding_top);
                return;
            }
            return;
        }
        if (d != null) {
            d.setBounds(0, 0, (w - this.m_padding_right) - this.m_padding_left, (h - this.m_padding_bottom) - this.m_padding_top);
        }
        int gap = (trackWidth - thumbWidth) / 2;
        if (thumb != null) {
            setThumbPos(h, thumb, scale, gap);
        }
    }

    private void setThumbPos(int h, Drawable thumb, float scale, int gap) {
        int leftBound;
        int rightBound;
        int thumbWidth = thumb.getIntrinsicWidth();
        int thumbHeight = thumb.getIntrinsicHeight();
        int thumbPos = (int) ((1.0f - scale) * ((float) ((((h - this.m_padding_top) - this.m_padding_bottom) - thumbHeight) + (this.m_thumb_offset * 2))));
        if (gap == Integer.MIN_VALUE) {
            Rect oldBounds = thumb.getBounds();
            leftBound = oldBounds.left;
            rightBound = oldBounds.right;
        } else {
            leftBound = gap;
            rightBound = gap + thumbWidth;
        }
        thumb.setBounds(leftBound, thumbPos, rightBound, thumbPos + thumbHeight);
    }

    /* access modifiers changed from: protected */
    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.m_thumb != null) {
            canvas.save();
            canvas.translate((float) this.m_padding_left, (float) (this.m_padding_top - this.m_thumb_offset));
            this.m_thumb.draw(canvas);
            canvas.restore();
        }
    }

    /* access modifiers changed from: protected */
    public synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getCurrentDrawable();
        int thumbWidth = this.m_thumb == null ? 0 : this.m_thumb.getIntrinsicWidth();
        int dw = 0;
        int dh = 0;
        if (d != null) {
            int dw2 = Math.max(this.m_min_width, Math.min(this.m_max_width, d.getIntrinsicWidth()));
            dw = Math.max(thumbWidth, 0);
            dh = Math.max(this.m_min_height, Math.min(this.m_max_height, d.getIntrinsicHeight()));
        }
        setMeasuredDimension(resolveSize(dw + this.m_padding_left + this.m_padding_right, widthMeasureSpec), resolveSize(dh + this.m_padding_top + this.m_padding_bottom, heightMeasureSpec));
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.m_is_user_seekable || !isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case 0:
                setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(event);
                return true;
            case 1:
                trackTouchEvent(event);
                onStopTrackingTouch();
                setPressed(false);
                invalidate();
                return true;
            case 2:
                trackTouchEvent(event);
                attemptClaimDrag();
                return true;
            case 3:
                onStopTrackingTouch();
                setPressed(false);
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        float scale;
        int height = getHeight();
        int available = (height - this.m_padding_top) - this.m_padding_bottom;
        int y = height - ((int) event.getY());
        float progress = 0.0f;
        if (y < this.m_padding_bottom) {
            scale = 0.0f;
        } else if (y > height - this.m_padding_top) {
            scale = 1.0f;
        } else {
            scale = ((float) (y - this.m_padding_bottom)) / ((float) available);
            progress = this.m_touch_progress_offset;
        }
        setProgress((int) (progress + (((float) getMax()) * scale)), true);
    }

    private void attemptClaimDrag() {
        if (this.m_parent != null) {
            this.m_parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    /* access modifiers changed from: package-private */
    public void onStartTrackingTouch() {
    }

    /* access modifiers changed from: package-private */
    public void onStopTrackingTouch() {
    }

    /* access modifiers changed from: package-private */
    public void onKeyChange() {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int progress = getProgress();
        switch (keyCode) {
            case 19:
                if (progress < getMax()) {
                    setProgress(this.m_key_progress_increment + progress, true);
                    onKeyChange();
                    return true;
                }
                break;
            case 20:
                if (progress > 0) {
                    setProgress(progress - this.m_key_progress_increment, true);
                    onKeyChange();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}
