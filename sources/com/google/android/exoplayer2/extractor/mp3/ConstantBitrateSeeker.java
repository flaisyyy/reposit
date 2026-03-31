package com.google.android.exoplayer2.extractor.mp3;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.MpegAudioHeader;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.util.Util;

final class ConstantBitrateSeeker implements Mp3Extractor.Seeker {
    private static final int BITS_PER_BYTE = 8;
    private final int bitrate;
    private final long dataSize;
    private final long durationUs;
    private final long firstFramePosition;
    private final int frameSize;

    public ConstantBitrateSeeker(long inputLength, long firstFramePosition2, MpegAudioHeader mpegAudioHeader) {
        this.firstFramePosition = firstFramePosition2;
        this.frameSize = mpegAudioHeader.frameSize;
        this.bitrate = mpegAudioHeader.bitrate;
        if (inputLength == -1) {
            this.dataSize = -1;
            this.durationUs = C.TIME_UNSET;
            return;
        }
        this.dataSize = inputLength - firstFramePosition2;
        this.durationUs = getTimeUs(inputLength);
    }

    public boolean isSeekable() {
        return this.dataSize != -1;
    }

    public long getPosition(long timeUs) {
        if (this.dataSize == -1) {
            return this.firstFramePosition;
        }
        return this.firstFramePosition + Util.constrainValue((((((long) this.bitrate) * timeUs) / 8000000) / ((long) this.frameSize)) * ((long) this.frameSize), 0, this.dataSize - ((long) this.frameSize));
    }

    public long getTimeUs(long position) {
        return ((Math.max(0, position - this.firstFramePosition) * C.MICROS_PER_SECOND) * 8) / ((long) this.bitrate);
    }

    public long getDurationUs() {
        return this.durationUs;
    }
}
