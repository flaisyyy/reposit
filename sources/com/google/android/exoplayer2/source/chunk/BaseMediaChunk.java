package com.google.android.exoplayer2.source.chunk;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

public abstract class BaseMediaChunk extends MediaChunk {
    private int[] firstSampleIndices;
    private BaseMediaChunkOutput output;

    public BaseMediaChunk(DataSource dataSource, DataSpec dataSpec, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long startTimeUs, long endTimeUs, int chunkIndex) {
        super(dataSource, dataSpec, trackFormat, trackSelectionReason, trackSelectionData, startTimeUs, endTimeUs, chunkIndex);
    }

    public void init(BaseMediaChunkOutput output2) {
        this.output = output2;
        this.firstSampleIndices = output2.getWriteIndices();
    }

    public final int getFirstSampleIndex(int trackIndex) {
        return this.firstSampleIndices[trackIndex];
    }

    /* access modifiers changed from: protected */
    public final BaseMediaChunkOutput getOutput() {
        return this.output;
    }
}
