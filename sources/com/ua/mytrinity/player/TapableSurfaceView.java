package com.ua.mytrinity.player;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class TapableSurfaceView extends SurfaceView {
    private static final String TAG = "TapableSurfaceView";
    private OnEndScrollListener m_end_scroll_listener = null;
    private GestureDetector m_gestureDetector;
    private GestureDetector.SimpleOnGestureListener m_gestureListener;
    /* access modifiers changed from: private */
    public boolean m_is_scrolling = false;
    /* access modifiers changed from: private */
    public OnScrollListener m_scroll_listener = null;
    /* access modifiers changed from: private */
    public float m_scroll_start_x;
    /* access modifiers changed from: private */
    public float m_scroll_start_y;
    /* access modifiers changed from: private */
    public OnTapListener m_tap_listener = null;
    private OnUpListener m_up_listener = null;

    public interface OnEndScrollListener {
        void onScrollEnd(float f, float f2, float f3, float f4);
    }

    public interface OnScrollListener {
        void onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);
    }

    public interface OnTapListener {
        void onTap(MotionEvent motionEvent);
    }

    public interface OnUpListener {
        void onUp(MotionEvent motionEvent);
    }

    public TapableSurfaceView(Context context) {
        super(context);
        initGestureDetector(context);
    }

    public TapableSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGestureDetector(context);
    }

    public TapableSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initGestureDetector(context);
    }

    public void initGestureDetector(Context context) {
        Log.d(TAG, "initGestureDetector()");
        this.m_gestureListener = new GestureDetector.SimpleOnGestureListener() {
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (TapableSurfaceView.this.m_tap_listener == null) {
                    return true;
                }
                TapableSurfaceView.this.m_tap_listener.onTap(e);
                return true;
            }

            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                boolean unused = TapableSurfaceView.this.m_is_scrolling = true;
                if (TapableSurfaceView.this.m_scroll_listener != null) {
                    float unused2 = TapableSurfaceView.this.m_scroll_start_x = e1.getX();
                    float unused3 = TapableSurfaceView.this.m_scroll_start_y = e1.getY();
                    TapableSurfaceView.this.m_scroll_listener.onScroll(e1, e2, distanceX, distanceY);
                }
                return true;
            }
        };
        this.m_gestureDetector = new GestureDetector(context, this.m_gestureListener);
    }

    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent() " + event);
        this.m_gestureDetector.onTouchEvent(event);
        if (event.getAction() == 1) {
            if (this.m_up_listener != null) {
                this.m_up_listener.onUp(event);
            }
            if (this.m_is_scrolling) {
                this.m_is_scrolling = false;
                if (this.m_end_scroll_listener != null) {
                    this.m_end_scroll_listener.onScrollEnd(this.m_scroll_start_x, this.m_scroll_start_y, event.getX() - this.m_scroll_start_x, event.getY() - this.m_scroll_start_y);
                }
            }
        }
        return true;
    }

    public void setOnTapListener(OnTapListener listener) {
        this.m_tap_listener = listener;
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.m_scroll_listener = listener;
    }

    public void setOnEndScrollListener(OnEndScrollListener listener) {
        this.m_end_scroll_listener = listener;
    }

    public void setOnUpListener(OnUpListener listener) {
        this.m_up_listener = listener;
    }
}
