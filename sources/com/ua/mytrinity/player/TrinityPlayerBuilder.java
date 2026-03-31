package com.ua.mytrinity.player;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;

public abstract class TrinityPlayerBuilder {
    protected Activity activity;
    protected AttributeSet attrs;
    protected Context context;
    protected int defStyle;

    public TrinityPlayerBuilder setContext(Context context2) {
        this.context = context2;
        return this;
    }

    public TrinityPlayerBuilder setAttrs(AttributeSet attrs2) {
        this.attrs = attrs2;
        return this;
    }

    public TrinityPlayerBuilder setDefStyle(int defStyle2) {
        this.defStyle = defStyle2;
        return this;
    }

    public TrinityPlayerBuilder setActivity(Activity activity2) {
        this.activity = activity2;
        this.context = activity2;
        return this;
    }

    public TrinityPlayer build() {
        return null;
    }
}
