package com.google.android.exoplayer2.extractor.ts;

import android.util.SparseArray;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.extractor.ts.TsPayloadReader;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultTsPayloadReaderFactory implements TsPayloadReader.Factory {
    private static final int DESCRIPTOR_TAG_CAPTION_SERVICE = 134;
    public static final int FLAG_ALLOW_NON_IDR_KEYFRAMES = 1;
    public static final int FLAG_DETECT_ACCESS_UNITS = 8;
    public static final int FLAG_IGNORE_AAC_STREAM = 2;
    public static final int FLAG_IGNORE_H264_STREAM = 4;
    public static final int FLAG_IGNORE_SPLICE_INFO_STREAM = 16;
    public static final int FLAG_OVERRIDE_CAPTION_DESCRIPTORS = 32;
    private final List<Format> closedCaptionFormats;
    private final int flags;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    public DefaultTsPayloadReaderFactory() {
        this(0);
    }

    public DefaultTsPayloadReaderFactory(int flags2) {
        this(flags2, Collections.emptyList());
    }

    public DefaultTsPayloadReaderFactory(int flags2, List<Format> closedCaptionFormats2) {
        this.flags = flags2;
        if (!isSet(32) && closedCaptionFormats2.isEmpty()) {
            closedCaptionFormats2 = Collections.singletonList(Format.createTextSampleFormat((String) null, MimeTypes.APPLICATION_CEA608, 0, (String) null));
        }
        this.closedCaptionFormats = closedCaptionFormats2;
    }

    public SparseArray<TsPayloadReader> createInitialPayloadReaders() {
        return new SparseArray<>();
    }

    public TsPayloadReader createPayloadReader(int streamType, TsPayloadReader.EsInfo esInfo) {
        switch (streamType) {
            case 2:
                return new PesReader(new H262Reader());
            case 3:
            case 4:
                return new PesReader(new MpegAudioReader(esInfo.language));
            case 15:
                if (!isSet(2)) {
                    return new PesReader(new AdtsReader(false, esInfo.language));
                }
                return null;
            case 17:
                if (!isSet(2)) {
                    return new PesReader(new LatmReader(esInfo.language));
                }
                return null;
            case 21:
                return new PesReader(new Id3Reader());
            case 27:
                if (!isSet(4)) {
                    return new PesReader(new H264Reader(buildSeiReader(esInfo), isSet(1), isSet(8)));
                }
                return null;
            case 36:
                return new PesReader(new H265Reader(buildSeiReader(esInfo)));
            case 89:
                return new PesReader(new DvbSubtitleReader(esInfo.dvbSubtitleInfos));
            case TsExtractor.TS_STREAM_TYPE_AC3:
            case TsExtractor.TS_STREAM_TYPE_E_AC3:
                return new PesReader(new Ac3Reader(esInfo.language));
            case TsExtractor.TS_STREAM_TYPE_HDMV_DTS:
            case TsExtractor.TS_STREAM_TYPE_DTS:
                return new PesReader(new DtsReader(esInfo.language));
            case 134:
                if (!isSet(16)) {
                    return new SectionReader(new SpliceInfoSectionReader());
                }
                return null;
            default:
                return null;
        }
    }

    private SeiReader buildSeiReader(TsPayloadReader.EsInfo esInfo) {
        String mimeType;
        int accessibilityChannel;
        if (isSet(32)) {
            return new SeiReader(this.closedCaptionFormats);
        }
        ParsableByteArray parsableByteArray = new ParsableByteArray(esInfo.descriptorBytes);
        List<Format> closedCaptionFormats2 = this.closedCaptionFormats;
        while (parsableByteArray.bytesLeft() > 0) {
            int descriptorTag = parsableByteArray.readUnsignedByte();
            int nextDescriptorPosition = parsableByteArray.getPosition() + parsableByteArray.readUnsignedByte();
            if (descriptorTag == 134) {
                closedCaptionFormats2 = new ArrayList<>();
                int numberOfServices = parsableByteArray.readUnsignedByte() & 31;
                for (int i = 0; i < numberOfServices; i++) {
                    String language = parsableByteArray.readString(3);
                    int captionTypeByte = parsableByteArray.readUnsignedByte();
                    if ((captionTypeByte & 128) != 0) {
                        mimeType = MimeTypes.APPLICATION_CEA708;
                        accessibilityChannel = captionTypeByte & 63;
                    } else {
                        mimeType = MimeTypes.APPLICATION_CEA608;
                        accessibilityChannel = 1;
                    }
                    closedCaptionFormats2.add(Format.createTextSampleFormat((String) null, mimeType, (String) null, -1, 0, language, accessibilityChannel, (DrmInitData) null));
                    parsableByteArray.skipBytes(2);
                }
            }
            parsableByteArray.setPosition(nextDescriptorPosition);
        }
        return new SeiReader(closedCaptionFormats2);
    }

    private boolean isSet(int flag) {
        return (this.flags & flag) != 0;
    }
}
