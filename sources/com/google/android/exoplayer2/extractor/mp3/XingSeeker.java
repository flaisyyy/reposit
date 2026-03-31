package com.google.android.exoplayer2.extractor.mp3;

import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.MpegAudioHeader;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;

final class XingSeeker implements Mp3Extractor.Seeker {
    private static final String TAG = "XingSeeker";
    private final long dataSize;
    private final long dataStartPosition;
    private final long durationUs;
    private final long[] tableOfContents;
    private final int xingFrameSize;

    public static XingSeeker create(long inputLength, long position, MpegAudioHeader mpegAudioHeader, ParsableByteArray frame) {
        int frameCount;
        int samplesPerFrame = mpegAudioHeader.samplesPerFrame;
        int sampleRate = mpegAudioHeader.sampleRate;
        int flags = frame.readInt();
        if ((flags & 1) != 1 || (frameCount = frame.readUnsignedIntToInt()) == 0) {
            return null;
        }
        long durationUs2 = Util.scaleLargeTimestamp((long) frameCount, ((long) samplesPerFrame) * C.MICROS_PER_SECOND, (long) sampleRate);
        if ((flags & 6) != 6) {
            return new XingSeeker(position, mpegAudioHeader.frameSize, durationUs2);
        }
        long dataSize2 = (long) frame.readUnsignedIntToInt();
        long[] tableOfContents2 = new long[100];
        for (int i = 0; i < 100; i++) {
            tableOfContents2[i] = (long) frame.readUnsignedByte();
        }
        if (!(inputLength == -1 || inputLength == position + dataSize2)) {
            Log.w(TAG, "XING data size mismatch: " + inputLength + ", " + (position + dataSize2));
        }
        return new XingSeeker(position, mpegAudioHeader.frameSize, durationUs2, dataSize2, tableOfContents2);
    }

    private XingSeeker(long dataStartPosition2, int xingFrameSize2, long durationUs2) {
        this(dataStartPosition2, xingFrameSize2, durationUs2, -1, (long[]) null);
    }

    private XingSeeker(long dataStartPosition2, int xingFrameSize2, long durationUs2, long dataSize2, long[] tableOfContents2) {
        this.dataStartPosition = dataStartPosition2;
        this.xingFrameSize = xingFrameSize2;
        this.durationUs = durationUs2;
        this.dataSize = dataSize2;
        this.tableOfContents = tableOfContents2;
    }

    public boolean isSeekable() {
        return this.tableOfContents != null;
    }

    public long getPosition(long timeUs) {
        double scaledPosition;
        if (!isSeekable()) {
            return this.dataStartPosition + ((long) this.xingFrameSize);
        }
        double percent = (((double) timeUs) * 100.0d) / ((double) this.durationUs);
        if (percent <= 0.0d) {
            scaledPosition = 0.0d;
        } else if (percent >= 100.0d) {
            scaledPosition = 256.0d;
        } else {
            int prevTableIndex = (int) percent;
            double prevScaledPosition = (double) this.tableOfContents[prevTableIndex];
            scaledPosition = prevScaledPosition + (((prevTableIndex == 99 ? 256.0d : (double) this.tableOfContents[prevTableIndex + 1]) - prevScaledPosition) * (percent - ((double) prevTableIndex)));
        }
        return this.dataStartPosition + Util.constrainValue(Math.round((scaledPosition / 256.0d) * ((double) this.dataSize)), (long) this.xingFrameSize, this.dataSize - 1);
    }

    public long getTimeUs(long position) {
        long positionOffset = position - this.dataStartPosition;
        if (!isSeekable() || positionOffset <= ((long) this.xingFrameSize)) {
            return 0;
        }
        double scaledPosition = (((double) positionOffset) * 256.0d) / ((double) this.dataSize);
        int prevTableIndex = Util.binarySearchFloor(this.tableOfContents, (long) scaledPosition, true, true);
        long prevTimeUs = getTimeUsForTableIndex(prevTableIndex);
        long prevScaledPosition = this.tableOfContents[prevTableIndex];
        long nextTimeUs = getTimeUsForTableIndex(prevTableIndex + 1);
        long nextScaledPosition = prevTableIndex == 99 ? 256 : this.tableOfContents[prevTableIndex + 1];
        return Math.round(((double) (nextTimeUs - prevTimeUs)) * (prevScaledPosition == nextScaledPosition ? 0.0d : (scaledPosition - ((double) prevScaledPosition)) / ((double) (nextScaledPosition - prevScaledPosition)))) + prevTimeUs;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    private long getTimeUsForTableIndex(int tableIndex) {
        return (this.durationUs * ((long) tableIndex)) / 100;
    }
}
