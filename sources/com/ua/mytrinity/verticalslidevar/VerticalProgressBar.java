package com.ua.mytrinity.verticalslidevar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import com.ua.mytrinity.player.R;

public class VerticalProgressBar extends View {
    private static final int MAX_LEVEL = 10000;
    private Drawable m_current_drawable;
    private boolean m_in_drawing;
    private int m_max;
    protected int m_max_height;
    protected int m_max_width;
    protected int m_min_height;
    protected int m_min_width;
    private boolean m_no_invalidate;
    protected int m_padding_bottom;
    protected int m_padding_left;
    protected int m_padding_right;
    protected int m_padding_top;
    protected ViewParent m_parent;
    private int m_progress;
    private Drawable m_progress_drawable;
    /* access modifiers changed from: private */
    public RefreshProgressRunnable m_refresh_progress_runnable;
    private Bitmap m_sample_tile;
    protected int m_scroll_X;
    protected int m_scroll_Y;
    private int m_secondary_progress;
    private long m_ui_thread_id;

    public VerticalProgressBar(Context context) {
        this(context, (AttributeSet) null);
    }

    public VerticalProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 16842871);
    }

    public VerticalProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.m_ui_thread_id = Thread.currentThread().getId();
        initProgressBar();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressBar, defStyle, 0);
        this.m_no_invalidate = true;
        Drawable drawable = a.getDrawable(5);
        if (drawable != null) {
            setProgressDrawable(tileify(drawable, false));
        }
        this.m_min_width = a.getDimensionPixelSize(6, this.m_min_width);
        this.m_max_width = a.getDimensionPixelSize(0, this.m_max_width);
        this.m_min_height = a.getDimensionPixelSize(7, this.m_min_height);
        this.m_max_height = a.getDimensionPixelSize(1, this.m_max_height);
        setMax(a.getInt(2, this.m_max));
        setProgress(a.getInt(3, this.m_progress));
        setSecondaryProgress(a.getInt(4, this.m_secondary_progress));
        this.m_no_invalidate = false;
        a.recycle();
    }

    private Drawable tileify(Drawable drawable, boolean clip) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable background = (LayerDrawable) drawable;
            int N = background.getNumberOfLayers();
            Drawable[] outDrawables = new Drawable[N];
            for (int i = 0; i < N; i++) {
                int id = background.getId(i);
                outDrawables[i] = tileify(background.getDrawable(i), id == 16908301 || id == 16908303);
            }
            LayerDrawable newBg = new LayerDrawable(outDrawables);
            for (int i2 = 0; i2 < N; i2++) {
                newBg.setId(i2, background.getId(i2));
            }
            return newBg;
        } else if (drawable instanceof StateListDrawable) {
            return new StateListDrawable();
        } else {
            if (!(drawable instanceof BitmapDrawable)) {
                return drawable;
            }
            Bitmap tileBitmap = ((BitmapDrawable) drawable).getBitmap();
            if (this.m_sample_tile == null) {
                this.m_sample_tile = tileBitmap;
            }
            Drawable shapeDrawable = new ShapeDrawable(getDrawableShape());
            if (clip) {
                shapeDrawable = new ClipDrawable(shapeDrawable, 3, 1);
            }
            return shapeDrawable;
        }
    }

    /* access modifiers changed from: package-private */
    public Shape getDrawableShape() {
        return new RoundRectShape(new float[]{5.0f, 5.0f, 5.0f, 5.0f, 5.0f, 5.0f, 5.0f, 5.0f}, (RectF) null, (float[]) null);
    }

    private void initProgressBar() {
        this.m_max = 100;
        this.m_progress = 0;
        this.m_secondary_progress = 0;
        this.m_min_width = 24;
        this.m_max_width = 48;
        this.m_min_height = 24;
        this.m_max_height = 48;
    }

    public Drawable getProgressDrawable() {
        return this.m_progress_drawable;
    }

    public void setProgressDrawable(Drawable drawable) {
        if (drawable != null) {
            drawable.setCallback(this);
            int drawableHeight = drawable.getMinimumHeight();
            if (this.m_max_height < drawableHeight) {
                this.m_max_height = drawableHeight;
                requestLayout();
            }
        }
        this.m_progress_drawable = drawable;
        this.m_current_drawable = drawable;
        postInvalidate();
    }

    public Drawable getCurrentDrawable() {
        return this.m_current_drawable;
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable who) {
        return who == this.m_progress_drawable || super.verifyDrawable(who);
    }

    public void postInvalidate() {
        if (!this.m_no_invalidate) {
            super.postInvalidate();
        }
    }

    private class RefreshProgressRunnable implements Runnable {
        private boolean m_from_user;
        private int m_id;
        private int m_progress;

        RefreshProgressRunnable(int id, int progress, boolean fromUser) {
            this.m_id = id;
            this.m_progress = progress;
            this.m_from_user = fromUser;
        }

        public void run() {
            VerticalProgressBar.this.doRefreshProgress(this.m_id, this.m_progress, this.m_from_user);
            RefreshProgressRunnable unused = VerticalProgressBar.this.m_refresh_progress_runnable = this;
        }

        public void setup(int id, int progress, boolean fromUser) {
            this.m_id = id;
            this.m_progress = progress;
            this.m_from_user = fromUser;
        }
    }

    /* access modifiers changed from: private */
    public synchronized void doRefreshProgress(int id, int progress, boolean fromUser) {
        float scale = this.m_max > 0 ? ((float) progress) / ((float) this.m_max) : 0.0f;
        Drawable d = this.m_current_drawable;
        if (d != null) {
            Drawable progressDrawable = null;
            if (d instanceof LayerDrawable) {
                progressDrawable = ((LayerDrawable) d).findDrawableByLayerId(id);
            }
            int level = (int) (10000.0f * scale);
            if (progressDrawable == null) {
                progressDrawable = d;
            }
            progressDrawable.setLevel(level);
        } else {
            invalidate();
        }
        if (id == 16908301) {
            onProgressRefresh(scale, fromUser);
        }
    }

    /* access modifiers changed from: protected */
    public void onProgressRefresh(float scale, boolean fromUser) {
    }

    private synchronized void refreshProgress(int id, int progress, boolean fromUser) {
        RefreshProgressRunnable r;
        if (this.m_ui_thread_id == Thread.currentThread().getId()) {
            doRefreshProgress(id, progress, fromUser);
        } else {
            if (this.m_refresh_progress_runnable != null) {
                r = this.m_refresh_progress_runnable;
                this.m_refresh_progress_runnable = null;
                r.setup(id, progress, fromUser);
            } else {
                r = new RefreshProgressRunnable(id, progress, fromUser);
            }
            post(r);
        }
    }

    public synchronized void setProgress(int progress) {
        setProgress(progress, false);
    }

    /* access modifiers changed from: package-private */
    public synchronized void setProgress(int progress, boolean fromUser) {
        if (progress < 0) {
            progress = 0;
        }
        if (progress > this.m_max) {
            progress = this.m_max;
        }
        if (progress != this.m_progress) {
            this.m_progress = progress;
            refreshProgress(16908301, this.m_progress, fromUser);
        }
    }

    public synchronized void setSecondaryProgress(int secondaryProgress) {
        if (secondaryProgress < 0) {
            secondaryProgress = 0;
        }
        if (secondaryProgress > this.m_max) {
            secondaryProgress = this.m_max;
        }
        if (secondaryProgress != this.m_secondary_progress) {
            this.m_secondary_progress = secondaryProgress;
            refreshProgress(16908303, this.m_secondary_progress, false);
        }
    }

    public synchronized int getProgress() {
        return this.m_progress;
    }

    public synchronized int getSecondaryProgress() {
        return this.m_secondary_progress;
    }

    public synchronized int getMax() {
        return this.m_max;
    }

    public synchronized void setMax(int max) {
        if (max < 0) {
            max = 0;
        }
        if (max != this.m_max) {
            this.m_max = max;
            postInvalidate();
            if (this.m_progress > max) {
                this.m_progress = max;
                refreshProgress(16908301, this.m_progress, false);
            }
        }
    }

    public final synchronized void incrementProgressBy(int diff) {
        setProgress(this.m_progress + diff);
    }

    public final synchronized void incrementSecondaryProgressBy(int diff) {
        setSecondaryProgress(this.m_secondary_progress + diff);
    }

    public void setVisibility(int v) {
        if (getVisibility() != v) {
            super.setVisibility(v);
        }
    }

    public void invalidateDrawable(Drawable dr) {
        if (this.m_in_drawing) {
            return;
        }
        if (verifyDrawable(dr)) {
            Rect dirty = dr.getBounds();
            int scrollX = this.m_scroll_X + this.m_padding_left;
            int scrollY = this.m_scroll_Y + this.m_padding_top;
            invalidate(dirty.left + scrollX, dirty.top + scrollY, dirty.right + scrollX, dirty.bottom + scrollY);
            return;
        }
        super.invalidateDrawable(dr);
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        int right = (w - this.m_padding_right) - this.m_padding_left;
        int bottom = (h - this.m_padding_bottom) - this.m_padding_top;
        if (this.m_progress_drawable != null) {
            this.m_progress_drawable.setBounds(0, 0, right, bottom);
        }
    }

    /* access modifiers changed from: protected */
    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Drawable d = this.m_current_drawable;
        if (d != null) {
            canvas.save();
            canvas.translate((float) this.m_padding_left, (float) this.m_padding_top);
            d.draw(canvas);
            canvas.restore();
        }
    }

    /* access modifiers changed from: protected */
    public synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = this.m_current_drawable;
        int dw = 0;
        int dh = 0;
        if (d != null) {
            dw = Math.max(this.m_min_width, Math.min(this.m_max_width, d.getIntrinsicWidth()));
            dh = Math.max(this.m_min_height, Math.min(this.m_max_height, d.getIntrinsicHeight()));
        }
        setMeasuredDimension(resolveSize(dw + this.m_padding_left + this.m_padding_right, widthMeasureSpec), resolveSize(dh + this.m_padding_top + this.m_padding_bottom, heightMeasureSpec));
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        int[] state = getDrawableState();
        if (this.m_progress_drawable != null && this.m_progress_drawable.isStateful()) {
            this.m_progress_drawable.setState(state);
        }
    }

    public Parcelable onSaveInstanceState() {
        DefaultSavedState ss = new DefaultSavedState(super.onSaveInstanceState());
        ss.setProgress(this.m_progress);
        ss.setSecondaryProgress(this.m_secondary_progress);
        return ss;
    }

    public void onRestoreInstanceState(Parcelable state) {
        DefaultSavedState ss = (DefaultSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setProgress(ss.getProgress());
        setSecondaryProgress(ss.getSecondaryProgress());
    }
}
