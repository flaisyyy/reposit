package com.google.android.exoplayer2.extractor;

import com.google.android.exoplayer2.util.Util;

public final class ChunkIndex implements SeekMap {
    private final long durationUs;
    public final long[] durationsUs;
    public final int length;
    public final long[] offsets;
    public final int[] sizes;
    public final long[] timesUs;

    public ChunkIndex(int[] sizes2, long[] offsets2, long[] durationsUs2, long[] timesUs2) {
        this.sizes = sizes2;
        this.offsets = offsets2;
        this.durationsUs = durationsUs2;
        this.timesUs = timesUs2;
        this.length = sizes2.length;
        if (this.length > 0) {
            this.durationUs = durationsUs2[this.length - 1] + timesUs2[this.length - 1];
        } else {
            this.durationUs = 0;
        }
    }

    public int getChunkIndex(long timeUs) {
        return Util.binarySearchFloor(this.timesUs, timeUs, true, true);
    }

    public boolean isSeekable() {
        return true;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public long getPosition(long timeUs) {
        return this.offsets[getChunkIndex(timeUs)];
    }
}
