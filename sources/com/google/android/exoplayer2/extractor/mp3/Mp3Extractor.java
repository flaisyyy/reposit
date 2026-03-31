package com.google.android.exoplayer2.extractor.mp3;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.GaplessInfoHolder;
import com.google.android.exoplayer2.extractor.MpegAudioHeader;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.id3.Id3Decoder;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.io.EOFException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public final class Mp3Extractor implements Extractor {
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() {
        public Extractor[] createExtractors() {
            return new Extractor[]{new Mp3Extractor()};
        }
    };
    public static final int FLAG_DISABLE_ID3_METADATA = 2;
    public static final int FLAG_ENABLE_CONSTANT_BITRATE_SEEKING = 1;
    private static final int MAX_SNIFF_BYTES = 16384;
    private static final int MAX_SYNC_BYTES = 131072;
    private static final int MPEG_AUDIO_HEADER_MASK = -128000;
    private static final int SCRATCH_LENGTH = 10;
    private static final int SEEK_HEADER_INFO = Util.getIntegerCodeForString("Info");
    private static final int SEEK_HEADER_UNSET = 0;
    private static final int SEEK_HEADER_VBRI = Util.getIntegerCodeForString("VBRI");
    private static final int SEEK_HEADER_XING = Util.getIntegerCodeForString("Xing");
    private long basisTimeUs;
    private ExtractorOutput extractorOutput;
    private final int flags;
    private final long forcedFirstSampleTimestampUs;
    private final GaplessInfoHolder gaplessInfoHolder;
    private Metadata metadata;
    private int sampleBytesRemaining;
    private long samplesRead;
    private final ParsableByteArray scratch;
    private Seeker seeker;
    private final MpegAudioHeader synchronizedHeader;
    private int synchronizedHeaderData;
    private TrackOutput trackOutput;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    interface Seeker extends SeekMap {
        long getTimeUs(long j);
    }

    public Mp3Extractor() {
        this(0);
    }

    public Mp3Extractor(int flags2) {
        this(flags2, C.TIME_UNSET);
    }

    public Mp3Extractor(int flags2, long forcedFirstSampleTimestampUs2) {
        this.flags = flags2;
        this.forcedFirstSampleTimestampUs = forcedFirstSampleTimestampUs2;
        this.scratch = new ParsableByteArray(10);
        this.synchronizedHeader = new MpegAudioHeader();
        this.gaplessInfoHolder = new GaplessInfoHolder();
        this.basisTimeUs = C.TIME_UNSET;
    }

    public boolean sniff(ExtractorInput input) throws IOException, InterruptedException {
        return synchronize(input, true);
    }

    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
        this.trackOutput = this.extractorOutput.track(0, 1);
        this.extractorOutput.endTracks();
    }

    public void seek(long position, long timeUs) {
        this.synchronizedHeaderData = 0;
        this.basisTimeUs = C.TIME_UNSET;
        this.samplesRead = 0;
        this.sampleBytesRemaining = 0;
    }

    public void release() {
    }

    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {
        if (this.synchronizedHeaderData == 0) {
            try {
                synchronize(input, false);
            } catch (EOFException e) {
                return -1;
            }
        }
        if (this.seeker == null) {
            this.seeker = maybeReadSeekFrame(input);
            if (this.seeker == null || (!this.seeker.isSeekable() && (this.flags & 1) != 0)) {
                this.seeker = getConstantBitrateSeeker(input);
            }
            this.extractorOutput.seekMap(this.seeker);
            this.trackOutput.format(Format.createAudioSampleFormat((String) null, this.synchronizedHeader.mimeType, (String) null, -1, 4096, this.synchronizedHeader.channels, this.synchronizedHeader.sampleRate, -1, this.gaplessInfoHolder.encoderDelay, this.gaplessInfoHolder.encoderPadding, (List<byte[]>) null, (DrmInitData) null, 0, (String) null, (this.flags & 2) != 0 ? null : this.metadata));
        }
        return readSample(input);
    }

    private int readSample(ExtractorInput extractorInput) throws IOException, InterruptedException {
        if (this.sampleBytesRemaining == 0) {
            extractorInput.resetPeekPosition();
            if (!extractorInput.peekFully(this.scratch.data, 0, 4, true)) {
                return -1;
            }
            this.scratch.setPosition(0);
            int sampleHeaderData = this.scratch.readInt();
            if (!headersMatch(sampleHeaderData, (long) this.synchronizedHeaderData) || MpegAudioHeader.getFrameSize(sampleHeaderData) == -1) {
                extractorInput.skipFully(1);
                this.synchronizedHeaderData = 0;
                return 0;
            }
            MpegAudioHeader.populateHeader(sampleHeaderData, this.synchronizedHeader);
            if (this.basisTimeUs == C.TIME_UNSET) {
                this.basisTimeUs = this.seeker.getTimeUs(extractorInput.getPosition());
                if (this.forcedFirstSampleTimestampUs != C.TIME_UNSET) {
                    this.basisTimeUs += this.forcedFirstSampleTimestampUs - this.seeker.getTimeUs(0);
                }
            }
            this.sampleBytesRemaining = this.synchronizedHeader.frameSize;
        }
        int bytesAppended = this.trackOutput.sampleData(extractorInput, this.sampleBytesRemaining, true);
        if (bytesAppended == -1) {
            return -1;
        }
        this.sampleBytesRemaining -= bytesAppended;
        if (this.sampleBytesRemaining > 0) {
            return 0;
        }
        this.trackOutput.sampleMetadata(this.basisTimeUs + ((this.samplesRead * C.MICROS_PER_SECOND) / ((long) this.synchronizedHeader.sampleRate)), 1, this.synchronizedHeader.frameSize, 0, (TrackOutput.CryptoData) null);
        this.samplesRead += (long) this.synchronizedHeader.samplesPerFrame;
        this.sampleBytesRemaining = 0;
        return 0;
    }

    private boolean synchronize(ExtractorInput input, boolean sniffing) throws IOException, InterruptedException {
        boolean z;
        int frameSize;
        int validFrameCount = 0;
        int candidateSynchronizedHeaderData = 0;
        int peekedId3Bytes = 0;
        int searchedBytes = 0;
        int searchLimitBytes = sniffing ? 16384 : 131072;
        input.resetPeekPosition();
        if (input.getPosition() == 0) {
            peekId3Data(input);
            peekedId3Bytes = (int) input.getPeekPosition();
            if (!sniffing) {
                input.skipFully(peekedId3Bytes);
            }
        }
        while (true) {
            byte[] bArr = this.scratch.data;
            if (validFrameCount > 0) {
                z = true;
            } else {
                z = false;
            }
            if (!input.peekFully(bArr, 0, 4, z)) {
                break;
            }
            this.scratch.setPosition(0);
            int headerData = this.scratch.readInt();
            if ((candidateSynchronizedHeaderData == 0 || headersMatch(headerData, (long) candidateSynchronizedHeaderData)) && (frameSize = MpegAudioHeader.getFrameSize(headerData)) != -1) {
                validFrameCount++;
                if (validFrameCount != 1) {
                    if (validFrameCount == 4) {
                        break;
                    }
                } else {
                    MpegAudioHeader.populateHeader(headerData, this.synchronizedHeader);
                    candidateSynchronizedHeaderData = headerData;
                }
                input.advancePeekPosition(frameSize - 4);
            } else {
                int searchedBytes2 = searchedBytes + 1;
                if (searchedBytes != searchLimitBytes) {
                    validFrameCount = 0;
                    candidateSynchronizedHeaderData = 0;
                    if (sniffing) {
                        input.resetPeekPosition();
                        input.advancePeekPosition(peekedId3Bytes + searchedBytes2);
                        searchedBytes = searchedBytes2;
                    } else {
                        input.skipFully(1);
                        searchedBytes = searchedBytes2;
                    }
                } else if (!sniffing) {
                    throw new ParserException("Searched too many bytes.");
                } else {
                    int i = searchedBytes2;
                    return false;
                }
            }
        }
        if (sniffing) {
            input.skipFully(peekedId3Bytes + searchedBytes);
        } else {
            input.resetPeekPosition();
        }
        this.synchronizedHeaderData = candidateSynchronizedHeaderData;
        return true;
    }

    private void peekId3Data(ExtractorInput input) throws IOException, InterruptedException {
        int peekedId3Bytes = 0;
        while (true) {
            input.peekFully(this.scratch.data, 0, 10);
            this.scratch.setPosition(0);
            if (this.scratch.readUnsignedInt24() != Id3Decoder.ID3_TAG) {
                input.resetPeekPosition();
                input.advancePeekPosition(peekedId3Bytes);
                return;
            }
            this.scratch.skipBytes(3);
            int framesLength = this.scratch.readSynchSafeInt();
            int tagLength = framesLength + 10;
            if (this.metadata == null) {
                byte[] id3Data = new byte[tagLength];
                System.arraycopy(this.scratch.data, 0, id3Data, 0, 10);
                input.peekFully(id3Data, 10, framesLength);
                this.metadata = new Id3Decoder((this.flags & 2) != 0 ? GaplessInfoHolder.GAPLESS_INFO_ID3_FRAME_PREDICATE : null).decode(id3Data, tagLength);
                if (this.metadata != null) {
                    this.gaplessInfoHolder.setFromMetadata(this.metadata);
                }
            } else {
                input.advancePeekPosition(framesLength);
            }
            peekedId3Bytes += tagLength;
        }
    }

    private Seeker maybeReadSeekFrame(ExtractorInput input) throws IOException, InterruptedException {
        Seeker seeker2;
        int xingBase = 21;
        ParsableByteArray frame = new ParsableByteArray(this.synchronizedHeader.frameSize);
        input.peekFully(frame.data, 0, this.synchronizedHeader.frameSize);
        if ((this.synchronizedHeader.version & 1) != 0) {
            if (this.synchronizedHeader.channels != 1) {
                xingBase = 36;
            }
        } else if (this.synchronizedHeader.channels == 1) {
            xingBase = 13;
        }
        int seekHeader = getSeekFrameHeader(frame, xingBase);
        if (seekHeader == SEEK_HEADER_XING || seekHeader == SEEK_HEADER_INFO) {
            seeker2 = XingSeeker.create(input.getLength(), input.getPosition(), this.synchronizedHeader, frame);
            if (seeker2 != null && !this.gaplessInfoHolder.hasGaplessInfo()) {
                input.resetPeekPosition();
                input.advancePeekPosition(xingBase + 141);
                input.peekFully(this.scratch.data, 0, 3);
                this.scratch.setPosition(0);
                this.gaplessInfoHolder.setFromXingHeaderValue(this.scratch.readUnsignedInt24());
            }
            input.skipFully(this.synchronizedHeader.frameSize);
            if (seeker2 != null && !seeker2.isSeekable() && seekHeader == SEEK_HEADER_INFO) {
                return getConstantBitrateSeeker(input);
            }
        } else if (seekHeader == SEEK_HEADER_VBRI) {
            seeker2 = VbriSeeker.create(input.getLength(), input.getPosition(), this.synchronizedHeader, frame);
            input.skipFully(this.synchronizedHeader.frameSize);
        } else {
            seeker2 = null;
            input.resetPeekPosition();
        }
        return seeker2;
    }

    private Seeker getConstantBitrateSeeker(ExtractorInput input) throws IOException, InterruptedException {
        input.peekFully(this.scratch.data, 0, 4);
        this.scratch.setPosition(0);
        MpegAudioHeader.populateHeader(this.scratch.readInt(), this.synchronizedHeader);
        return new ConstantBitrateSeeker(input.getLength(), input.getPosition(), this.synchronizedHeader);
    }

    private static boolean headersMatch(int headerA, long headerB) {
        return ((long) (MPEG_AUDIO_HEADER_MASK & headerA)) == (-128000 & headerB);
    }

    private static int getSeekFrameHeader(ParsableByteArray frame, int xingBase) {
        if (frame.limit() >= xingBase + 4) {
            frame.setPosition(xingBase);
            int headerData = frame.readInt();
            if (headerData == SEEK_HEADER_XING || headerData == SEEK_HEADER_INFO) {
                return headerData;
            }
        }
        if (frame.limit() >= 40) {
            frame.setPosition(36);
            if (frame.readInt() == SEEK_HEADER_VBRI) {
                return SEEK_HEADER_VBRI;
            }
        }
        return 0;
    }
}
