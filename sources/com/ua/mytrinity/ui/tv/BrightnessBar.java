package com.ua.mytrinity.ui.tv;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Window;
import android.view.WindowManager;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.verticalslidevar.ScreenVerticalBar;

public class BrightnessBar extends ScreenVerticalBar {
    private Drawable m_brightness;
    private Window m_window;

    public BrightnessBar(Context context) {
        super(context);
    }

    public BrightnessBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BrightnessBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setWindow(Window window) {
        this.m_window = window;
        setMax(15);
        setCurrent((int) ((((double) getMax()) * (((double) this.m_window.getAttributes().screenBrightness) - 0.1d)) / 0.9d));
    }

    /* access modifiers changed from: protected */
    public void onProgressChanged(int value) {
        if (this.m_brightness == null) {
            this.m_brightness = getContext().getResources().getDrawable(R.drawable.brightness);
        }
        setText(String.valueOf(value), this.m_brightness);
        if (this.m_window != null) {
            WindowManager.LayoutParams attr = this.m_window.getAttributes();
            attr.screenBrightness = 0.1f + (((float) value) / ((float) getMax()));
            this.m_window.setAttributes(attr);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }
}
