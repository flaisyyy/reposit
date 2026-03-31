package com.ua.mytrinity.verticalslidevar;

public interface DefaultSeekBar {

    public interface DefaultSeekBarChangeListener {
        void onProgressChanged(int i, boolean z);
    }

    void incrementProgressBy(int i);

    void setOnDefaultSeekBarChangeListener(DefaultSeekBarChangeListener defaultSeekBarChangeListener);

    void setProgress(int i);
}
