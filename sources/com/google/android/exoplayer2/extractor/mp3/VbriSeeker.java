package com.google.android.exoplayer2.extractor.mp3;

import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.MpegAudioHeader;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;

final class VbriSeeker implements Mp3Extractor.Seeker {
    private static final String TAG = "VbriSeeker";
    private final long durationUs;
    private final long[] positions;
    private final long[] timesUs;

    public static VbriSeeker create(long inputLength, long position, MpegAudioHeader mpegAudioHeader, ParsableByteArray frame) {
        int segmentSize;
        frame.skipBytes(10);
        int numFrames = frame.readInt();
        if (numFrames <= 0) {
            return null;
        }
        int sampleRate = mpegAudioHeader.sampleRate;
        long durationUs2 = Util.scaleLargeTimestamp((long) numFrames, ((long) (sampleRate >= 32000 ? 1152 : 576)) * C.MICROS_PER_SECOND, (long) sampleRate);
        int entryCount = frame.readUnsignedShort();
        int scale = frame.readUnsignedShort();
        int entrySize = frame.readUnsignedShort();
        frame.skipBytes(2);
        long minPosition = position + ((long) mpegAudioHeader.frameSize);
        long[] timesUs2 = new long[entryCount];
        long[] positions2 = new long[entryCount];
        for (int index = 0; index < entryCount; index++) {
            timesUs2[index] = (((long) index) * durationUs2) / ((long) entryCount);
            positions2[index] = Math.max(position, minPosition);
            switch (entrySize) {
                case 1:
                    segmentSize = frame.readUnsignedByte();
                    break;
                case 2:
                    segmentSize = frame.readUnsignedShort();
                    break;
                case 3:
                    segmentSize = frame.readUnsignedInt24();
                    break;
                case 4:
                    segmentSize = frame.readUnsignedIntToInt();
                    break;
                default:
                    return null;
            }
            position += (long) (segmentSize * scale);
        }
        if (!(inputLength == -1 || inputLength == position)) {
            Log.w(TAG, "VBRI data size mismatch: " + inputLength + ", " + position);
        }
        return new VbriSeeker(timesUs2, positions2, durationUs2);
    }

    private VbriSeeker(long[] timesUs2, long[] positions2, long durationUs2) {
        this.timesUs = timesUs2;
        this.positions = positions2;
        this.durationUs = durationUs2;
    }

    public boolean isSeekable() {
        return true;
    }

    public long getPosition(long timeUs) {
        return this.positions[Util.binarySearchFloor(this.timesUs, timeUs, true, true)];
    }

    public long getTimeUs(long position) {
        return this.timesUs[Util.binarySearchFloor(this.positions, position, true, true)];
    }

    public long getDurationUs() {
        return this.durationUs;
    }
}
