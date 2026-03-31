package com.google.android.exoplayer2.text.cea;

import com.google.android.exoplayer2.text.SubtitleOutputBuffer;

public final class CeaOutputBuffer extends SubtitleOutputBuffer {
    private final CeaDecoder owner;

    public CeaOutputBuffer(CeaDecoder owner2) {
        this.owner = owner2;
    }

    public final void release() {
        this.owner.releaseOutputBuffer(this);
    }
}
