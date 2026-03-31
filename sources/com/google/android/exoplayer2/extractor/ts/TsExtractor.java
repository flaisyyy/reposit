package com.google.android.exoplayer2.extractor.ts;

import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.ts.TsPayloadReader;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ParsableBitArray;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.TimestampAdjuster;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TsExtractor implements Extractor {
    /* access modifiers changed from: private */
    public static final long AC3_FORMAT_IDENTIFIER = ((long) Util.getIntegerCodeForString("AC-3"));
    private static final int BUFFER_SIZE = 9400;
    /* access modifiers changed from: private */
    public static final long E_AC3_FORMAT_IDENTIFIER = ((long) Util.getIntegerCodeForString("EAC3"));
    public static final ExtractorsFactory FACTORY = new ExtractorsFactory() {
        public Extractor[] createExtractors() {
            return new Extractor[]{new TsExtractor()};
        }
    };
    /* access modifiers changed from: private */
    public static final long HEVC_FORMAT_IDENTIFIER = ((long) Util.getIntegerCodeForString("HEVC"));
    private static final int MAX_PID_PLUS_ONE = 8192;
    public static final int MODE_HLS = 2;
    public static final int MODE_MULTI_PMT = 0;
    public static final int MODE_SINGLE_PMT = 1;
    private static final int SNIFF_TS_PACKET_COUNT = 5;
    private static final int TS_PACKET_SIZE = 188;
    private static final int TS_PAT_PID = 0;
    public static final int TS_STREAM_TYPE_AAC_ADTS = 15;
    public static final int TS_STREAM_TYPE_AAC_LATM = 17;
    public static final int TS_STREAM_TYPE_AC3 = 129;
    public static final int TS_STREAM_TYPE_DTS = 138;
    public static final int TS_STREAM_TYPE_DVBSUBS = 89;
    public static final int TS_STREAM_TYPE_E_AC3 = 135;
    public static final int TS_STREAM_TYPE_H262 = 2;
    public static final int TS_STREAM_TYPE_H264 = 27;
    public static final int TS_STREAM_TYPE_H265 = 36;
    public static final int TS_STREAM_TYPE_HDMV_DTS = 130;
    public static final int TS_STREAM_TYPE_ID3 = 21;
    public static final int TS_STREAM_TYPE_MPA = 3;
    public static final int TS_STREAM_TYPE_MPA_LSF = 4;
    public static final int TS_STREAM_TYPE_SPLICE_INFO = 134;
    private static final int TS_SYNC_BYTE = 71;
    private final SparseIntArray continuityCounters;
    /* access modifiers changed from: private */
    public TsPayloadReader id3Reader;
    /* access modifiers changed from: private */
    public final int mode;
    /* access modifiers changed from: private */
    public ExtractorOutput output;
    /* access modifiers changed from: private */
    public final TsPayloadReader.Factory payloadReaderFactory;
    /* access modifiers changed from: private */
    public int remainingPmts;
    /* access modifiers changed from: private */
    public final List<TimestampAdjuster> timestampAdjusters;
    /* access modifiers changed from: private */
    public final SparseBooleanArray trackIds;
    /* access modifiers changed from: private */
    public boolean tracksEnded;
    private final ParsableByteArray tsPacketBuffer;
    /* access modifiers changed from: private */
    public final SparseArray<TsPayloadReader> tsPayloadReaders;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    static /* synthetic */ int access$108(TsExtractor x0) {
        int i = x0.remainingPmts;
        x0.remainingPmts = i + 1;
        return i;
    }

    public TsExtractor() {
        this(0);
    }

    public TsExtractor(int defaultTsPayloadReaderFlags) {
        this(1, defaultTsPayloadReaderFlags);
    }

    public TsExtractor(int mode2, int defaultTsPayloadReaderFlags) {
        this(mode2, new TimestampAdjuster(0), new DefaultTsPayloadReaderFactory(defaultTsPayloadReaderFlags));
    }

    public TsExtractor(int mode2, TimestampAdjuster timestampAdjuster, TsPayloadReader.Factory payloadReaderFactory2) {
        this.payloadReaderFactory = (TsPayloadReader.Factory) Assertions.checkNotNull(payloadReaderFactory2);
        this.mode = mode2;
        if (mode2 == 1 || mode2 == 2) {
            this.timestampAdjusters = Collections.singletonList(timestampAdjuster);
        } else {
            this.timestampAdjusters = new ArrayList();
            this.timestampAdjusters.add(timestampAdjuster);
        }
        this.tsPacketBuffer = new ParsableByteArray((int) BUFFER_SIZE);
        this.trackIds = new SparseBooleanArray();
        this.tsPayloadReaders = new SparseArray<>();
        this.continuityCounters = new SparseIntArray();
        resetPayloadReaders();
    }

    public boolean sniff(ExtractorInput input) throws IOException, InterruptedException {
        byte[] buffer = this.tsPacketBuffer.data;
        input.peekFully(buffer, 0, 940);
        int j = 0;
        while (j < TS_PACKET_SIZE) {
            int i = 0;
            while (i != 5) {
                if (buffer[(i * TS_PACKET_SIZE) + j] != 71) {
                    j++;
                } else {
                    i++;
                }
            }
            input.skipFully(j);
            return true;
        }
        return false;
    }

    public void init(ExtractorOutput output2) {
        this.output = output2;
        output2.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
    }

    public void seek(long position, long timeUs) {
        int timestampAdjustersCount = this.timestampAdjusters.size();
        for (int i = 0; i < timestampAdjustersCount; i++) {
            this.timestampAdjusters.get(i).reset();
        }
        this.tsPacketBuffer.reset();
        this.continuityCounters.clear();
        resetPayloadReaders();
    }

    public void release() {
    }

    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {
        byte[] data = this.tsPacketBuffer.data;
        if (9400 - this.tsPacketBuffer.getPosition() < TS_PACKET_SIZE) {
            int bytesLeft = this.tsPacketBuffer.bytesLeft();
            if (bytesLeft > 0) {
                System.arraycopy(data, this.tsPacketBuffer.getPosition(), data, 0, bytesLeft);
            }
            this.tsPacketBuffer.reset(data, bytesLeft);
        }
        while (this.tsPacketBuffer.bytesLeft() < TS_PACKET_SIZE) {
            int limit = this.tsPacketBuffer.limit();
            int read = input.read(data, limit, 9400 - limit);
            if (read == -1) {
                return -1;
            }
            this.tsPacketBuffer.setLimit(limit + read);
        }
        int limit2 = this.tsPacketBuffer.limit();
        int position = this.tsPacketBuffer.getPosition();
        while (position < limit2 && data[position] != 71) {
            position++;
        }
        this.tsPacketBuffer.setPosition(position);
        int endOfPacket = position + TS_PACKET_SIZE;
        if (endOfPacket > limit2) {
            return 0;
        }
        int tsPacketHeader = this.tsPacketBuffer.readInt();
        if ((8388608 & tsPacketHeader) != 0) {
            this.tsPacketBuffer.setPosition(endOfPacket);
            return 0;
        }
        boolean payloadUnitStartIndicator = (4194304 & tsPacketHeader) != 0;
        int pid = (2096896 & tsPacketHeader) >> 8;
        boolean adaptationFieldExists = (tsPacketHeader & 32) != 0;
        TsPayloadReader payloadReader = (tsPacketHeader & 16) != 0 ? this.tsPayloadReaders.get(pid) : null;
        if (payloadReader == null) {
            this.tsPacketBuffer.setPosition(endOfPacket);
            return 0;
        }
        if (this.mode != 2) {
            int continuityCounter = tsPacketHeader & 15;
            int previousCounter = this.continuityCounters.get(pid, continuityCounter - 1);
            this.continuityCounters.put(pid, continuityCounter);
            if (previousCounter == continuityCounter) {
                this.tsPacketBuffer.setPosition(endOfPacket);
                return 0;
            } else if (continuityCounter != ((previousCounter + 1) & 15)) {
                payloadReader.seek();
            }
        }
        if (adaptationFieldExists) {
            this.tsPacketBuffer.skipBytes(this.tsPacketBuffer.readUnsignedByte());
        }
        this.tsPacketBuffer.setLimit(endOfPacket);
        payloadReader.consume(this.tsPacketBuffer, payloadUnitStartIndicator);
        this.tsPacketBuffer.setLimit(limit2);
        this.tsPacketBuffer.setPosition(endOfPacket);
        return 0;
    }

    private void resetPayloadReaders() {
        this.trackIds.clear();
        this.tsPayloadReaders.clear();
        SparseArray<TsPayloadReader> initialPayloadReaders = this.payloadReaderFactory.createInitialPayloadReaders();
        int initialPayloadReadersSize = initialPayloadReaders.size();
        for (int i = 0; i < initialPayloadReadersSize; i++) {
            this.tsPayloadReaders.put(initialPayloadReaders.keyAt(i), initialPayloadReaders.valueAt(i));
        }
        this.tsPayloadReaders.put(0, new SectionReader(new PatReader()));
        this.id3Reader = null;
    }

    private class PatReader implements SectionPayloadReader {
        private final ParsableBitArray patScratch = new ParsableBitArray(new byte[4]);

        public PatReader() {
        }

        public void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        }

        public void consume(ParsableByteArray sectionData) {
            if (sectionData.readUnsignedByte() == 0) {
                sectionData.skipBytes(7);
                int programCount = sectionData.bytesLeft() / 4;
                for (int i = 0; i < programCount; i++) {
                    sectionData.readBytes(this.patScratch, 4);
                    int programNumber = this.patScratch.readBits(16);
                    this.patScratch.skipBits(3);
                    if (programNumber == 0) {
                        this.patScratch.skipBits(13);
                    } else {
                        int pid = this.patScratch.readBits(13);
                        TsExtractor.this.tsPayloadReaders.put(pid, new SectionReader(new PmtReader(pid)));
                        TsExtractor.access$108(TsExtractor.this);
                    }
                }
                if (TsExtractor.this.mode != 2) {
                    TsExtractor.this.tsPayloadReaders.remove(0);
                }
            }
        }
    }

    private class PmtReader implements SectionPayloadReader {
        private static final int TS_PMT_DESC_AC3 = 106;
        private static final int TS_PMT_DESC_DTS = 123;
        private static final int TS_PMT_DESC_DVBSUBS = 89;
        private static final int TS_PMT_DESC_EAC3 = 122;
        private static final int TS_PMT_DESC_ISO639_LANG = 10;
        private static final int TS_PMT_DESC_REGISTRATION = 5;
        private final int pid;
        private final ParsableBitArray pmtScratch = new ParsableBitArray(new byte[5]);
        private final SparseIntArray trackIdToPidScratch = new SparseIntArray();
        private final SparseArray<TsPayloadReader> trackIdToReaderScratch = new SparseArray<>();

        public PmtReader(int pid2) {
            this.pid = pid2;
        }

        public void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
        }

        public void consume(ParsableByteArray sectionData) {
            TimestampAdjuster timestampAdjuster;
            int trackId;
            TsPayloadReader reader;
            if (sectionData.readUnsignedByte() == 2) {
                if (TsExtractor.this.mode == 1 || TsExtractor.this.mode == 2 || TsExtractor.this.remainingPmts == 1) {
                    timestampAdjuster = (TimestampAdjuster) TsExtractor.this.timestampAdjusters.get(0);
                } else {
                    timestampAdjuster = new TimestampAdjuster(((TimestampAdjuster) TsExtractor.this.timestampAdjusters.get(0)).getFirstSampleTimestampUs());
                    TsExtractor.this.timestampAdjusters.add(timestampAdjuster);
                }
                sectionData.skipBytes(2);
                int programNumber = sectionData.readUnsignedShort();
                sectionData.skipBytes(5);
                sectionData.readBytes(this.pmtScratch, 2);
                this.pmtScratch.skipBits(4);
                sectionData.skipBytes(this.pmtScratch.readBits(12));
                if (TsExtractor.this.mode == 2 && TsExtractor.this.id3Reader == null) {
                    TsPayloadReader unused = TsExtractor.this.id3Reader = TsExtractor.this.payloadReaderFactory.createPayloadReader(21, new TsPayloadReader.EsInfo(21, (String) null, (List<TsPayloadReader.DvbSubtitleInfo>) null, new byte[0]));
                    TsExtractor.this.id3Reader.init(timestampAdjuster, TsExtractor.this.output, new TsPayloadReader.TrackIdGenerator(programNumber, 21, 8192));
                }
                this.trackIdToReaderScratch.clear();
                this.trackIdToPidScratch.clear();
                int remainingEntriesLength = sectionData.bytesLeft();
                while (remainingEntriesLength > 0) {
                    sectionData.readBytes(this.pmtScratch, 5);
                    int streamType = this.pmtScratch.readBits(8);
                    this.pmtScratch.skipBits(3);
                    int elementaryPid = this.pmtScratch.readBits(13);
                    this.pmtScratch.skipBits(4);
                    int esInfoLength = this.pmtScratch.readBits(12);
                    TsPayloadReader.EsInfo esInfo = readEsInfo(sectionData, esInfoLength);
                    if (streamType == 6) {
                        streamType = esInfo.streamType;
                    }
                    remainingEntriesLength -= esInfoLength + 5;
                    if (TsExtractor.this.mode == 2) {
                        trackId = streamType;
                    } else {
                        trackId = elementaryPid;
                    }
                    if (!TsExtractor.this.trackIds.get(trackId)) {
                        if (TsExtractor.this.mode == 2 && streamType == 21) {
                            reader = TsExtractor.this.id3Reader;
                        } else {
                            reader = TsExtractor.this.payloadReaderFactory.createPayloadReader(streamType, esInfo);
                        }
                        if (TsExtractor.this.mode != 2 || elementaryPid < this.trackIdToPidScratch.get(trackId, 8192)) {
                            this.trackIdToPidScratch.put(trackId, elementaryPid);
                            this.trackIdToReaderScratch.put(trackId, reader);
                        }
                    }
                }
                int trackIdCount = this.trackIdToPidScratch.size();
                for (int i = 0; i < trackIdCount; i++) {
                    int trackId2 = this.trackIdToPidScratch.keyAt(i);
                    TsExtractor.this.trackIds.put(trackId2, true);
                    TsPayloadReader reader2 = this.trackIdToReaderScratch.valueAt(i);
                    if (reader2 != null) {
                        if (reader2 != TsExtractor.this.id3Reader) {
                            reader2.init(timestampAdjuster, TsExtractor.this.output, new TsPayloadReader.TrackIdGenerator(programNumber, trackId2, 8192));
                        }
                        TsExtractor.this.tsPayloadReaders.put(this.trackIdToPidScratch.valueAt(i), reader2);
                    }
                }
                if (TsExtractor.this.mode != 2) {
                    TsExtractor.this.tsPayloadReaders.remove(this.pid);
                    int unused2 = TsExtractor.this.remainingPmts = TsExtractor.this.mode == 1 ? 0 : TsExtractor.this.remainingPmts - 1;
                    if (TsExtractor.this.remainingPmts == 0) {
                        TsExtractor.this.output.endTracks();
                        boolean unused3 = TsExtractor.this.tracksEnded = true;
                    }
                } else if (!TsExtractor.this.tracksEnded) {
                    TsExtractor.this.output.endTracks();
                    int unused4 = TsExtractor.this.remainingPmts = 0;
                    boolean unused5 = TsExtractor.this.tracksEnded = true;
                }
            }
        }

        private TsPayloadReader.EsInfo readEsInfo(ParsableByteArray data, int length) {
            int descriptorsStartPosition = data.getPosition();
            int descriptorsEndPosition = descriptorsStartPosition + length;
            int streamType = -1;
            String language = null;
            List<TsPayloadReader.DvbSubtitleInfo> dvbSubtitleInfos = null;
            while (data.getPosition() < descriptorsEndPosition) {
                int descriptorTag = data.readUnsignedByte();
                int positionOfNextDescriptor = data.getPosition() + data.readUnsignedByte();
                if (descriptorTag == 5) {
                    long formatIdentifier = data.readUnsignedInt();
                    if (formatIdentifier == TsExtractor.AC3_FORMAT_IDENTIFIER) {
                        streamType = TsExtractor.TS_STREAM_TYPE_AC3;
                    } else if (formatIdentifier == TsExtractor.E_AC3_FORMAT_IDENTIFIER) {
                        streamType = TsExtractor.TS_STREAM_TYPE_E_AC3;
                    } else if (formatIdentifier == TsExtractor.HEVC_FORMAT_IDENTIFIER) {
                        streamType = 36;
                    }
                } else if (descriptorTag == 106) {
                    streamType = TsExtractor.TS_STREAM_TYPE_AC3;
                } else if (descriptorTag == TS_PMT_DESC_EAC3) {
                    streamType = TsExtractor.TS_STREAM_TYPE_E_AC3;
                } else if (descriptorTag == TS_PMT_DESC_DTS) {
                    streamType = TsExtractor.TS_STREAM_TYPE_DTS;
                } else if (descriptorTag == 10) {
                    language = data.readString(3).trim();
                } else if (descriptorTag == 89) {
                    streamType = 89;
                    dvbSubtitleInfos = new ArrayList<>();
                    while (data.getPosition() < positionOfNextDescriptor) {
                        String dvbLanguage = data.readString(3).trim();
                        int dvbSubtitlingType = data.readUnsignedByte();
                        byte[] initializationData = new byte[4];
                        data.readBytes(initializationData, 0, 4);
                        dvbSubtitleInfos.add(new TsPayloadReader.DvbSubtitleInfo(dvbLanguage, dvbSubtitlingType, initializationData));
                    }
                }
                data.skipBytes(positionOfNextDescriptor - data.getPosition());
            }
            data.setPosition(descriptorsEndPosition);
            return new TsPayloadReader.EsInfo(streamType, language, dvbSubtitleInfos, Arrays.copyOfRange(data.data, descriptorsStartPosition, descriptorsEndPosition));
        }
    }
}
