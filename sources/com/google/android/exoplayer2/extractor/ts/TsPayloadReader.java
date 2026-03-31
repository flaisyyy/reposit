package com.google.android.exoplayer2.extractor.ts;

import android.util.SparseArray;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.TimestampAdjuster;
import java.util.Collections;
import java.util.List;

public interface TsPayloadReader {

    public interface Factory {
        SparseArray<TsPayloadReader> createInitialPayloadReaders();

        TsPayloadReader createPayloadReader(int i, EsInfo esInfo);
    }

    void consume(ParsableByteArray parsableByteArray, boolean z) throws ParserException;

    void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput, TrackIdGenerator trackIdGenerator);

    void seek();

    public static final class EsInfo {
        public final byte[] descriptorBytes;
        public final List<DvbSubtitleInfo> dvbSubtitleInfos;
        public final String language;
        public final int streamType;

        public EsInfo(int streamType2, String language2, List<DvbSubtitleInfo> dvbSubtitleInfos2, byte[] descriptorBytes2) {
            List<DvbSubtitleInfo> unmodifiableList;
            this.streamType = streamType2;
            this.language = language2;
            if (dvbSubtitleInfos2 == null) {
                unmodifiableList = Collections.emptyList();
            } else {
                unmodifiableList = Collections.unmodifiableList(dvbSubtitleInfos2);
            }
            this.dvbSubtitleInfos = unmodifiableList;
            this.descriptorBytes = descriptorBytes2;
        }
    }

    public static final class DvbSubtitleInfo {
        public final byte[] initializationData;
        public final String language;
        public final int type;

        public DvbSubtitleInfo(String language2, int type2, byte[] initializationData2) {
            this.language = language2;
            this.type = type2;
            this.initializationData = initializationData2;
        }
    }

    public static final class TrackIdGenerator {
        private static final int ID_UNSET = Integer.MIN_VALUE;
        private final int firstTrackId;
        private String formatId;
        private final String formatIdPrefix;
        private int trackId;
        private final int trackIdIncrement;

        public TrackIdGenerator(int firstTrackId2, int trackIdIncrement2) {
            this(Integer.MIN_VALUE, firstTrackId2, trackIdIncrement2);
        }

        public TrackIdGenerator(int programNumber, int firstTrackId2, int trackIdIncrement2) {
            this.formatIdPrefix = programNumber != Integer.MIN_VALUE ? programNumber + "/" : "";
            this.firstTrackId = firstTrackId2;
            this.trackIdIncrement = trackIdIncrement2;
            this.trackId = Integer.MIN_VALUE;
        }

        public void generateNewId() {
            this.trackId = this.trackId == Integer.MIN_VALUE ? this.firstTrackId : this.trackId + this.trackIdIncrement;
            this.formatId = this.formatIdPrefix + this.trackId;
        }

        public int getTrackId() {
            maybeThrowUninitializedError();
            return this.trackId;
        }

        public String getFormatId() {
            maybeThrowUninitializedError();
            return this.formatId;
        }

        private void maybeThrowUninitializedError() {
            if (this.trackId == Integer.MIN_VALUE) {
                throw new IllegalStateException("generateNewId() must be called before retrieving ids.");
            }
        }
    }
}
