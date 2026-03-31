package com.google.android.exoplayer2.extractor.ogg;

import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.util.Assertions;
import java.io.EOFException;
import java.io.IOException;

final class DefaultOggSeeker implements OggSeeker {
    private static final int DEFAULT_OFFSET = 30000;
    public static final int MATCH_BYTE_RANGE = 100000;
    public static final int MATCH_RANGE = 72000;
    private static final int STATE_IDLE = 3;
    private static final int STATE_READ_LAST_PAGE = 1;
    private static final int STATE_SEEK = 2;
    private static final int STATE_SEEK_TO_END = 0;
    private long end;
    private long endGranule;
    private final long endPosition;
    private final OggPageHeader pageHeader = new OggPageHeader();
    private long positionBeforeSeekToEnd;
    private long start;
    private long startGranule;
    /* access modifiers changed from: private */
    public final long startPosition;
    private int state;
    /* access modifiers changed from: private */
    public final StreamReader streamReader;
    private long targetGranule;
    /* access modifiers changed from: private */
    public long totalGranules;

    public DefaultOggSeeker(long startPosition2, long endPosition2, StreamReader streamReader2, int firstPayloadPageSize, long firstPayloadPageGranulePosition) {
        Assertions.checkArgument(startPosition2 >= 0 && endPosition2 > startPosition2);
        this.streamReader = streamReader2;
        this.startPosition = startPosition2;
        this.endPosition = endPosition2;
        if (((long) firstPayloadPageSize) == endPosition2 - startPosition2) {
            this.totalGranules = firstPayloadPageGranulePosition;
            this.state = 3;
            return;
        }
        this.state = 0;
    }

    public long read(ExtractorInput input) throws IOException, InterruptedException {
        long currentGranule;
        switch (this.state) {
            case 0:
                this.positionBeforeSeekToEnd = input.getPosition();
                this.state = 1;
                long lastPageSearchPosition = this.endPosition - 65307;
                if (lastPageSearchPosition > this.positionBeforeSeekToEnd) {
                    return lastPageSearchPosition;
                }
                break;
            case 1:
                break;
            case 2:
                if (this.targetGranule == 0) {
                    currentGranule = 0;
                } else {
                    long position = getNextSeekPosition(this.targetGranule, input);
                    if (position >= 0) {
                        return position;
                    }
                    ExtractorInput extractorInput = input;
                    currentGranule = skipToPageOfGranule(extractorInput, this.targetGranule, -(2 + position));
                }
                this.state = 3;
                return -(2 + currentGranule);
            case 3:
                return -1;
            default:
                throw new IllegalStateException();
        }
        this.totalGranules = readGranuleOfLastPage(input);
        this.state = 3;
        return this.positionBeforeSeekToEnd;
    }

    public long startSeek(long timeUs) {
        Assertions.checkArgument(this.state == 3 || this.state == 2);
        this.targetGranule = timeUs == 0 ? 0 : this.streamReader.convertTimeToGranule(timeUs);
        this.state = 2;
        resetSeeking();
        return this.targetGranule;
    }

    public OggSeekMap createSeekMap() {
        if (this.totalGranules != 0) {
            return new OggSeekMap();
        }
        return null;
    }

    public void resetSeeking() {
        this.start = this.startPosition;
        this.end = this.endPosition;
        this.startGranule = 0;
        this.endGranule = this.totalGranules;
    }

    public long getNextSeekPosition(long targetGranule2, ExtractorInput input) throws IOException, InterruptedException {
        if (this.start == this.end) {
            return -(this.startGranule + 2);
        }
        long initialPosition = input.getPosition();
        if (skipToNextPage(input, this.end)) {
            this.pageHeader.populate(input, false);
            input.resetPeekPosition();
            long granuleDistance = targetGranule2 - this.pageHeader.granulePosition;
            int pageSize = this.pageHeader.headerSize + this.pageHeader.bodySize;
            if (granuleDistance < 0 || granuleDistance > 72000) {
                if (granuleDistance < 0) {
                    this.end = initialPosition;
                    this.endGranule = this.pageHeader.granulePosition;
                } else {
                    this.start = input.getPosition() + ((long) pageSize);
                    this.startGranule = this.pageHeader.granulePosition;
                    if ((this.end - this.start) + ((long) pageSize) < 100000) {
                        input.skipFully(pageSize);
                        return -(this.startGranule + 2);
                    }
                }
                if (this.end - this.start < 100000) {
                    this.end = this.start;
                    return this.start;
                }
                return Math.min(Math.max((input.getPosition() - (((long) pageSize) * (granuleDistance <= 0 ? 2 : 1))) + (((this.end - this.start) * granuleDistance) / (this.endGranule - this.startGranule)), this.start), this.end - 1);
            }
            input.skipFully(pageSize);
            return -(this.pageHeader.granulePosition + 2);
        } else if (this.start != initialPosition) {
            return this.start;
        } else {
            throw new IOException("No ogg page can be found.");
        }
    }

    /* access modifiers changed from: private */
    public long getEstimatedPosition(long position, long granuleDistance, long offset) {
        long position2 = position + ((((this.endPosition - this.startPosition) * granuleDistance) / this.totalGranules) - offset);
        if (position2 < this.startPosition) {
            position2 = this.startPosition;
        }
        if (position2 >= this.endPosition) {
            return this.endPosition - 1;
        }
        return position2;
    }

    private class OggSeekMap implements SeekMap {
        private OggSeekMap() {
        }

        public boolean isSeekable() {
            return true;
        }

        public long getPosition(long timeUs) {
            if (timeUs == 0) {
                return DefaultOggSeeker.this.startPosition;
            }
            return DefaultOggSeeker.this.getEstimatedPosition(DefaultOggSeeker.this.startPosition, DefaultOggSeeker.this.streamReader.convertTimeToGranule(timeUs), 30000);
        }

        public long getDurationUs() {
            return DefaultOggSeeker.this.streamReader.convertGranuleToTime(DefaultOggSeeker.this.totalGranules);
        }
    }

    /* access modifiers changed from: package-private */
    public void skipToNextPage(ExtractorInput input) throws IOException, InterruptedException {
        if (!skipToNextPage(input, this.endPosition)) {
            throw new EOFException();
        }
    }

    /* access modifiers changed from: package-private */
    public boolean skipToNextPage(ExtractorInput input, long until) throws IOException, InterruptedException {
        long until2 = Math.min(3 + until, this.endPosition);
        byte[] buffer = new byte[2048];
        int peekLength = buffer.length;
        while (true) {
            if (input.getPosition() + ((long) peekLength) > until2 && (peekLength = (int) (until2 - input.getPosition())) < 4) {
                return false;
            }
            input.peekFully(buffer, 0, peekLength, false);
            for (int i = 0; i < peekLength - 3; i++) {
                if (buffer[i] == 79 && buffer[i + 1] == 103 && buffer[i + 2] == 103 && buffer[i + 3] == 83) {
                    input.skipFully(i);
                    return true;
                }
            }
            input.skipFully(peekLength - 3);
        }
    }

    /* access modifiers changed from: package-private */
    public long readGranuleOfLastPage(ExtractorInput input) throws IOException, InterruptedException {
        skipToNextPage(input);
        this.pageHeader.reset();
        while ((this.pageHeader.type & 4) != 4 && input.getPosition() < this.endPosition) {
            this.pageHeader.populate(input, false);
            input.skipFully(this.pageHeader.headerSize + this.pageHeader.bodySize);
        }
        return this.pageHeader.granulePosition;
    }

    /* access modifiers changed from: package-private */
    public long skipToPageOfGranule(ExtractorInput input, long targetGranule2, long currentGranule) throws IOException, InterruptedException {
        this.pageHeader.populate(input, false);
        while (this.pageHeader.granulePosition < targetGranule2) {
            input.skipFully(this.pageHeader.headerSize + this.pageHeader.bodySize);
            currentGranule = this.pageHeader.granulePosition;
            this.pageHeader.populate(input, false);
        }
        input.resetPeekPosition();
        return currentGranule;
    }
}
