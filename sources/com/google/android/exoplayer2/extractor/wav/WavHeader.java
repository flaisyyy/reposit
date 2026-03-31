package com.google.android.exoplayer2.extractor.wav;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.util.Util;

final class WavHeader implements SeekMap {
    private final int averageBytesPerSecond;
    private final int bitsPerSample;
    private final int blockAlignment;
    private long dataSize;
    private long dataStartPosition;
    private final int encoding;
    private final int numChannels;
    private final int sampleRateHz;

    public WavHeader(int numChannels2, int sampleRateHz2, int averageBytesPerSecond2, int blockAlignment2, int bitsPerSample2, int encoding2) {
        this.numChannels = numChannels2;
        this.sampleRateHz = sampleRateHz2;
        this.averageBytesPerSecond = averageBytesPerSecond2;
        this.blockAlignment = blockAlignment2;
        this.bitsPerSample = bitsPerSample2;
        this.encoding = encoding2;
    }

    public void setDataBounds(long dataStartPosition2, long dataSize2) {
        this.dataStartPosition = dataStartPosition2;
        this.dataSize = dataSize2;
    }

    public boolean hasDataBounds() {
        return (this.dataStartPosition == 0 || this.dataSize == 0) ? false : true;
    }

    public boolean isSeekable() {
        return true;
    }

    public long getDurationUs() {
        return (C.MICROS_PER_SECOND * (this.dataSize / ((long) this.blockAlignment))) / ((long) this.sampleRateHz);
    }

    public long getPosition(long timeUs) {
        return this.dataStartPosition + Util.constrainValue((((((long) this.averageBytesPerSecond) * timeUs) / C.MICROS_PER_SECOND) / ((long) this.blockAlignment)) * ((long) this.blockAlignment), 0, this.dataSize - ((long) this.blockAlignment));
    }

    public long getTimeUs(long position) {
        return (C.MICROS_PER_SECOND * position) / ((long) this.averageBytesPerSecond);
    }

    public int getBytesPerFrame() {
        return this.blockAlignment;
    }

    public int getBitrate() {
        return this.sampleRateHz * this.bitsPerSample * this.numChannels;
    }

    public int getSampleRateHz() {
        return this.sampleRateHz;
    }

    public int getNumChannels() {
        return this.numChannels;
    }

    public int getEncoding() {
        return this.encoding;
    }
}
