package com.google.android.exoplayer2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.ColorInfo;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Format implements Parcelable {
    public static final Parcelable.Creator<Format> CREATOR = new Parcelable.Creator<Format>() {
        public Format createFromParcel(Parcel in) {
            return new Format(in);
        }

        public Format[] newArray(int size) {
            return new Format[size];
        }
    };
    public static final int NO_VALUE = -1;
    public static final long OFFSET_SAMPLE_RELATIVE = Long.MAX_VALUE;
    public final int accessibilityChannel;
    public final int bitrate;
    public final int channelCount;
    public final String codecs;
    public final ColorInfo colorInfo;
    public final String containerMimeType;
    public final DrmInitData drmInitData;
    public final int encoderDelay;
    public final int encoderPadding;
    public final float frameRate;
    private int hashCode;
    public final int height;
    public final String id;
    public final List<byte[]> initializationData;
    public final String language;
    public final int maxInputSize;
    public final Metadata metadata;
    public final int pcmEncoding;
    public final float pixelWidthHeightRatio;
    public final byte[] projectionData;
    public final int rotationDegrees;
    public final String sampleMimeType;
    public final int sampleRate;
    public final int selectionFlags;
    public final int stereoMode;
    public final long subsampleOffsetUs;
    public final int width;

    public static Format createVideoContainerFormat(String id2, String containerMimeType2, String sampleMimeType2, String codecs2, int bitrate2, int width2, int height2, float frameRate2, List<byte[]> initializationData2, int selectionFlags2) {
        return new Format(id2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, -1, width2, height2, frameRate2, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, -1, -1, -1, -1, -1, selectionFlags2, (String) null, -1, Long.MAX_VALUE, initializationData2, (DrmInitData) null, (Metadata) null);
    }

    public static Format createVideoSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int maxInputSize2, int width2, int height2, float frameRate2, List<byte[]> initializationData2, DrmInitData drmInitData2) {
        return createVideoSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, maxInputSize2, width2, height2, frameRate2, initializationData2, -1, -1.0f, drmInitData2);
    }

    public static Format createVideoSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int maxInputSize2, int width2, int height2, float frameRate2, List<byte[]> initializationData2, int rotationDegrees2, float pixelWidthHeightRatio2, DrmInitData drmInitData2) {
        return createVideoSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, maxInputSize2, width2, height2, frameRate2, initializationData2, rotationDegrees2, pixelWidthHeightRatio2, (byte[]) null, -1, (ColorInfo) null, drmInitData2);
    }

    public static Format createVideoSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int maxInputSize2, int width2, int height2, float frameRate2, List<byte[]> initializationData2, int rotationDegrees2, float pixelWidthHeightRatio2, byte[] projectionData2, int stereoMode2, ColorInfo colorInfo2, DrmInitData drmInitData2) {
        return new Format(id2, (String) null, sampleMimeType2, codecs2, bitrate2, maxInputSize2, width2, height2, frameRate2, rotationDegrees2, pixelWidthHeightRatio2, projectionData2, stereoMode2, colorInfo2, -1, -1, -1, -1, -1, 0, (String) null, -1, Long.MAX_VALUE, initializationData2, drmInitData2, (Metadata) null);
    }

    public static Format createAudioContainerFormat(String id2, String containerMimeType2, String sampleMimeType2, String codecs2, int bitrate2, int channelCount2, int sampleRate2, List<byte[]> initializationData2, int selectionFlags2, String language2) {
        return new Format(id2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, channelCount2, sampleRate2, -1, -1, -1, selectionFlags2, language2, -1, Long.MAX_VALUE, initializationData2, (DrmInitData) null, (Metadata) null);
    }

    public static Format createAudioSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int maxInputSize2, int channelCount2, int sampleRate2, List<byte[]> initializationData2, DrmInitData drmInitData2, int selectionFlags2, String language2) {
        return createAudioSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, maxInputSize2, channelCount2, sampleRate2, -1, initializationData2, drmInitData2, selectionFlags2, language2);
    }

    public static Format createAudioSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int maxInputSize2, int channelCount2, int sampleRate2, int pcmEncoding2, List<byte[]> initializationData2, DrmInitData drmInitData2, int selectionFlags2, String language2) {
        return createAudioSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, maxInputSize2, channelCount2, sampleRate2, pcmEncoding2, -1, -1, initializationData2, drmInitData2, selectionFlags2, language2, (Metadata) null);
    }

    public static Format createAudioSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int maxInputSize2, int channelCount2, int sampleRate2, int pcmEncoding2, int encoderDelay2, int encoderPadding2, List<byte[]> initializationData2, DrmInitData drmInitData2, int selectionFlags2, String language2, Metadata metadata2) {
        return new Format(id2, (String) null, sampleMimeType2, codecs2, bitrate2, maxInputSize2, -1, -1, -1.0f, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, channelCount2, sampleRate2, pcmEncoding2, encoderDelay2, encoderPadding2, selectionFlags2, language2, -1, Long.MAX_VALUE, initializationData2, drmInitData2, metadata2);
    }

    public static Format createTextContainerFormat(String id2, String containerMimeType2, String sampleMimeType2, String codecs2, int bitrate2, int selectionFlags2, String language2) {
        return createTextContainerFormat(id2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, selectionFlags2, language2, -1);
    }

    public static Format createTextContainerFormat(String id2, String containerMimeType2, String sampleMimeType2, String codecs2, int bitrate2, int selectionFlags2, String language2, int accessibilityChannel2) {
        return new Format(id2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, -1, -1, -1, -1, -1, selectionFlags2, language2, accessibilityChannel2, Long.MAX_VALUE, (List<byte[]>) null, (DrmInitData) null, (Metadata) null);
    }

    public static Format createTextSampleFormat(String id2, String sampleMimeType2, int selectionFlags2, String language2) {
        return createTextSampleFormat(id2, sampleMimeType2, selectionFlags2, language2, (DrmInitData) null);
    }

    public static Format createTextSampleFormat(String id2, String sampleMimeType2, int selectionFlags2, String language2, DrmInitData drmInitData2) {
        return createTextSampleFormat(id2, sampleMimeType2, (String) null, -1, selectionFlags2, language2, -1, drmInitData2, Long.MAX_VALUE, Collections.emptyList());
    }

    public static Format createTextSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int selectionFlags2, String language2, int accessibilityChannel2, DrmInitData drmInitData2) {
        return createTextSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, selectionFlags2, language2, accessibilityChannel2, drmInitData2, Long.MAX_VALUE, Collections.emptyList());
    }

    public static Format createTextSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int selectionFlags2, String language2, DrmInitData drmInitData2, long subsampleOffsetUs2) {
        return createTextSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, selectionFlags2, language2, -1, drmInitData2, subsampleOffsetUs2, Collections.emptyList());
    }

    public static Format createTextSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, int selectionFlags2, String language2, int accessibilityChannel2, DrmInitData drmInitData2, long subsampleOffsetUs2, List<byte[]> initializationData2) {
        return new Format(id2, (String) null, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, -1, -1, -1, -1, -1, selectionFlags2, language2, accessibilityChannel2, subsampleOffsetUs2, initializationData2, drmInitData2, (Metadata) null);
    }

    public static Format createImageSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, List<byte[]> initializationData2, String language2, DrmInitData drmInitData2) {
        return new Format(id2, (String) null, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, -1, -1, -1, -1, -1, 0, language2, -1, Long.MAX_VALUE, initializationData2, drmInitData2, (Metadata) null);
    }

    public static Format createContainerFormat(String id2, String containerMimeType2, String sampleMimeType2, String codecs2, int bitrate2, int selectionFlags2, String language2) {
        return new Format(id2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, -1, -1, -1, -1, -1, selectionFlags2, language2, -1, Long.MAX_VALUE, (List<byte[]>) null, (DrmInitData) null, (Metadata) null);
    }

    public static Format createSampleFormat(String id2, String sampleMimeType2, long subsampleOffsetUs2) {
        return new Format(id2, (String) null, sampleMimeType2, (String) null, -1, -1, -1, -1, -1.0f, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, -1, -1, -1, -1, -1, 0, (String) null, -1, subsampleOffsetUs2, (List<byte[]>) null, (DrmInitData) null, (Metadata) null);
    }

    public static Format createSampleFormat(String id2, String sampleMimeType2, String codecs2, int bitrate2, DrmInitData drmInitData2) {
        return new Format(id2, (String) null, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, (byte[]) null, -1, (ColorInfo) null, -1, -1, -1, -1, -1, 0, (String) null, -1, Long.MAX_VALUE, (List<byte[]>) null, drmInitData2, (Metadata) null);
    }

    Format(String id2, String containerMimeType2, String sampleMimeType2, String codecs2, int bitrate2, int maxInputSize2, int width2, int height2, float frameRate2, int rotationDegrees2, float pixelWidthHeightRatio2, byte[] projectionData2, int stereoMode2, ColorInfo colorInfo2, int channelCount2, int sampleRate2, int pcmEncoding2, int encoderDelay2, int encoderPadding2, int selectionFlags2, String language2, int accessibilityChannel2, long subsampleOffsetUs2, List<byte[]> initializationData2, DrmInitData drmInitData2, Metadata metadata2) {
        this.id = id2;
        this.containerMimeType = containerMimeType2;
        this.sampleMimeType = sampleMimeType2;
        this.codecs = codecs2;
        this.bitrate = bitrate2;
        this.maxInputSize = maxInputSize2;
        this.width = width2;
        this.height = height2;
        this.frameRate = frameRate2;
        this.rotationDegrees = rotationDegrees2;
        this.pixelWidthHeightRatio = pixelWidthHeightRatio2;
        this.projectionData = projectionData2;
        this.stereoMode = stereoMode2;
        this.colorInfo = colorInfo2;
        this.channelCount = channelCount2;
        this.sampleRate = sampleRate2;
        this.pcmEncoding = pcmEncoding2;
        this.encoderDelay = encoderDelay2;
        this.encoderPadding = encoderPadding2;
        this.selectionFlags = selectionFlags2;
        this.language = language2;
        this.accessibilityChannel = accessibilityChannel2;
        this.subsampleOffsetUs = subsampleOffsetUs2;
        this.initializationData = initializationData2 == null ? Collections.emptyList() : initializationData2;
        this.drmInitData = drmInitData2;
        this.metadata = metadata2;
    }

    Format(Parcel in) {
        this.id = in.readString();
        this.containerMimeType = in.readString();
        this.sampleMimeType = in.readString();
        this.codecs = in.readString();
        this.bitrate = in.readInt();
        this.maxInputSize = in.readInt();
        this.width = in.readInt();
        this.height = in.readInt();
        this.frameRate = in.readFloat();
        this.rotationDegrees = in.readInt();
        this.pixelWidthHeightRatio = in.readFloat();
        this.projectionData = in.readInt() != 0 ? in.createByteArray() : null;
        this.stereoMode = in.readInt();
        this.colorInfo = (ColorInfo) in.readParcelable(ColorInfo.class.getClassLoader());
        this.channelCount = in.readInt();
        this.sampleRate = in.readInt();
        this.pcmEncoding = in.readInt();
        this.encoderDelay = in.readInt();
        this.encoderPadding = in.readInt();
        this.selectionFlags = in.readInt();
        this.language = in.readString();
        this.accessibilityChannel = in.readInt();
        this.subsampleOffsetUs = in.readLong();
        int initializationDataSize = in.readInt();
        this.initializationData = new ArrayList(initializationDataSize);
        for (int i = 0; i < initializationDataSize; i++) {
            this.initializationData.add(in.createByteArray());
        }
        this.drmInitData = (DrmInitData) in.readParcelable(DrmInitData.class.getClassLoader());
        this.metadata = (Metadata) in.readParcelable(Metadata.class.getClassLoader());
    }

    public Format copyWithMaxInputSize(int maxInputSize2) {
        return new Format(this.id, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, maxInputSize2, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, this.metadata);
    }

    public Format copyWithSubsampleOffsetUs(long subsampleOffsetUs2) {
        return new Format(this.id, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, subsampleOffsetUs2, this.initializationData, this.drmInitData, this.metadata);
    }

    public Format copyWithContainerInfo(String id2, String codecs2, int bitrate2, int width2, int height2, int selectionFlags2, String language2) {
        return new Format(id2, this.containerMimeType, this.sampleMimeType, codecs2, bitrate2, this.maxInputSize, width2, height2, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, selectionFlags2, language2, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, this.metadata);
    }

    /* Debug info: failed to restart local var, previous not found, register: 31 */
    public Format copyWithManifestFormatInfo(Format manifestFormat) {
        if (this == manifestFormat) {
            return this;
        }
        return new Format(manifestFormat.id, this.containerMimeType, this.sampleMimeType, this.codecs == null ? manifestFormat.codecs : this.codecs, this.bitrate == -1 ? manifestFormat.bitrate : this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate == -1.0f ? manifestFormat.frameRate : this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags | manifestFormat.selectionFlags, this.language == null ? manifestFormat.language : this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, manifestFormat.drmInitData != null ? getFilledManifestDrmData(manifestFormat.drmInitData) : this.drmInitData, this.metadata);
    }

    public Format copyWithGaplessInfo(int encoderDelay2, int encoderPadding2) {
        return new Format(this.id, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, encoderDelay2, encoderPadding2, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, this.metadata);
    }

    public Format copyWithDrmInitData(DrmInitData drmInitData2) {
        return new Format(this.id, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, drmInitData2, this.metadata);
    }

    public Format copyWithMetadata(Metadata metadata2) {
        return new Format(this.id, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, metadata2);
    }

    public Format copyWithRotationDegrees(int rotationDegrees2) {
        return new Format(this.id, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, rotationDegrees2, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, this.metadata);
    }

    public int getPixelCount() {
        if (this.width == -1 || this.height == -1) {
            return -1;
        }
        return this.width * this.height;
    }

    @SuppressLint({"InlinedApi"})
    @TargetApi(16)
    public final MediaFormat getFrameworkMediaFormatV16() {
        MediaFormat format = new MediaFormat();
        format.setString("mime", this.sampleMimeType);
        maybeSetStringV16(format, "language", this.language);
        maybeSetIntegerV16(format, "max-input-size", this.maxInputSize);
        maybeSetIntegerV16(format, "width", this.width);
        maybeSetIntegerV16(format, "height", this.height);
        maybeSetFloatV16(format, "frame-rate", this.frameRate);
        maybeSetIntegerV16(format, "rotation-degrees", this.rotationDegrees);
        maybeSetIntegerV16(format, "channel-count", this.channelCount);
        maybeSetIntegerV16(format, "sample-rate", this.sampleRate);
        for (int i = 0; i < this.initializationData.size(); i++) {
            format.setByteBuffer("csd-" + i, ByteBuffer.wrap(this.initializationData.get(i)));
        }
        maybeSetColorInfoV24(format, this.colorInfo);
        return format;
    }

    public String toString() {
        return "Format(" + this.id + ", " + this.containerMimeType + ", " + this.sampleMimeType + ", " + this.bitrate + ", " + this.language + ", [" + this.width + ", " + this.height + ", " + this.frameRate + "], [" + this.channelCount + ", " + this.sampleRate + "])";
    }

    public int hashCode() {
        int i = 0;
        if (this.hashCode == 0) {
            int hashCode2 = ((((((((((((((((((((((((this.id == null ? 0 : this.id.hashCode()) + 527) * 31) + (this.containerMimeType == null ? 0 : this.containerMimeType.hashCode())) * 31) + (this.sampleMimeType == null ? 0 : this.sampleMimeType.hashCode())) * 31) + (this.codecs == null ? 0 : this.codecs.hashCode())) * 31) + this.bitrate) * 31) + this.width) * 31) + this.height) * 31) + this.channelCount) * 31) + this.sampleRate) * 31) + (this.language == null ? 0 : this.language.hashCode())) * 31) + this.accessibilityChannel) * 31) + (this.drmInitData == null ? 0 : this.drmInitData.hashCode())) * 31;
            if (this.metadata != null) {
                i = this.metadata.hashCode();
            }
            this.hashCode = hashCode2 + i;
        }
        return this.hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Format other = (Format) obj;
        if (this.bitrate != other.bitrate || this.maxInputSize != other.maxInputSize || this.width != other.width || this.height != other.height || this.frameRate != other.frameRate || this.rotationDegrees != other.rotationDegrees || this.pixelWidthHeightRatio != other.pixelWidthHeightRatio || this.stereoMode != other.stereoMode || this.channelCount != other.channelCount || this.sampleRate != other.sampleRate || this.pcmEncoding != other.pcmEncoding || this.encoderDelay != other.encoderDelay || this.encoderPadding != other.encoderPadding || this.subsampleOffsetUs != other.subsampleOffsetUs || this.selectionFlags != other.selectionFlags || !Util.areEqual(this.id, other.id) || !Util.areEqual(this.language, other.language) || this.accessibilityChannel != other.accessibilityChannel || !Util.areEqual(this.containerMimeType, other.containerMimeType) || !Util.areEqual(this.sampleMimeType, other.sampleMimeType) || !Util.areEqual(this.codecs, other.codecs) || !Util.areEqual(this.drmInitData, other.drmInitData) || !Util.areEqual(this.metadata, other.metadata) || !Util.areEqual(this.colorInfo, other.colorInfo) || !Arrays.equals(this.projectionData, other.projectionData) || this.initializationData.size() != other.initializationData.size()) {
            return false;
        }
        for (int i = 0; i < this.initializationData.size(); i++) {
            if (!Arrays.equals(this.initializationData.get(i), other.initializationData.get(i))) {
                return false;
            }
        }
        return true;
    }

    @TargetApi(24)
    private static void maybeSetColorInfoV24(MediaFormat format, ColorInfo colorInfo2) {
        if (colorInfo2 != null) {
            maybeSetIntegerV16(format, "color-transfer", colorInfo2.colorTransfer);
            maybeSetIntegerV16(format, "color-standard", colorInfo2.colorSpace);
            maybeSetIntegerV16(format, "color-range", colorInfo2.colorRange);
            maybeSetByteBufferV16(format, "hdr-static-info", colorInfo2.hdrStaticInfo);
        }
    }

    @TargetApi(16)
    private static void maybeSetStringV16(MediaFormat format, String key, String value) {
        if (value != null) {
            format.setString(key, value);
        }
    }

    @TargetApi(16)
    private static void maybeSetIntegerV16(MediaFormat format, String key, int value) {
        if (value != -1) {
            format.setInteger(key, value);
        }
    }

    @TargetApi(16)
    private static void maybeSetFloatV16(MediaFormat format, String key, float value) {
        if (value != -1.0f) {
            format.setFloat(key, value);
        }
    }

    @TargetApi(16)
    private static void maybeSetByteBufferV16(MediaFormat format, String key, byte[] value) {
        if (value != null) {
            format.setByteBuffer(key, ByteBuffer.wrap(value));
        }
    }

    public static String toLogString(Format format) {
        if (format == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("id=").append(format.id).append(", mimeType=").append(format.sampleMimeType);
        if (format.bitrate != -1) {
            builder.append(", bitrate=").append(format.bitrate);
        }
        if (!(format.width == -1 || format.height == -1)) {
            builder.append(", res=").append(format.width).append("x").append(format.height);
        }
        if (format.frameRate != -1.0f) {
            builder.append(", fps=").append(format.frameRate);
        }
        if (format.channelCount != -1) {
            builder.append(", channels=").append(format.channelCount);
        }
        if (format.sampleRate != -1) {
            builder.append(", sample_rate=").append(format.sampleRate);
        }
        if (format.language != null) {
            builder.append(", language=").append(format.language);
        }
        return builder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i;
        dest.writeString(this.id);
        dest.writeString(this.containerMimeType);
        dest.writeString(this.sampleMimeType);
        dest.writeString(this.codecs);
        dest.writeInt(this.bitrate);
        dest.writeInt(this.maxInputSize);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeFloat(this.frameRate);
        dest.writeInt(this.rotationDegrees);
        dest.writeFloat(this.pixelWidthHeightRatio);
        if (this.projectionData != null) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        if (this.projectionData != null) {
            dest.writeByteArray(this.projectionData);
        }
        dest.writeInt(this.stereoMode);
        dest.writeParcelable(this.colorInfo, flags);
        dest.writeInt(this.channelCount);
        dest.writeInt(this.sampleRate);
        dest.writeInt(this.pcmEncoding);
        dest.writeInt(this.encoderDelay);
        dest.writeInt(this.encoderPadding);
        dest.writeInt(this.selectionFlags);
        dest.writeString(this.language);
        dest.writeInt(this.accessibilityChannel);
        dest.writeLong(this.subsampleOffsetUs);
        int initializationDataSize = this.initializationData.size();
        dest.writeInt(initializationDataSize);
        for (int i2 = 0; i2 < initializationDataSize; i2++) {
            dest.writeByteArray(this.initializationData.get(i2));
        }
        dest.writeParcelable(this.drmInitData, 0);
        dest.writeParcelable(this.metadata, 0);
    }

    private DrmInitData getFilledManifestDrmData(DrmInitData manifestDrmData) {
        DrmInitData manifestDrmData2;
        ArrayList<DrmInitData.SchemeData> exposedSchemeDatas = new ArrayList<>();
        ArrayList<DrmInitData.SchemeData> emptySchemeDatas = new ArrayList<>();
        for (int i = 0; i < manifestDrmData.schemeDataCount; i++) {
            DrmInitData.SchemeData schemeData = manifestDrmData.get(i);
            if (schemeData.hasData()) {
                exposedSchemeDatas.add(schemeData);
            } else {
                emptySchemeDatas.add(schemeData);
            }
        }
        if (emptySchemeDatas.isEmpty()) {
            return manifestDrmData;
        }
        if (this.drmInitData == null) {
            return null;
        }
        int needFillingCount = emptySchemeDatas.size();
        for (int i2 = 0; i2 < this.drmInitData.schemeDataCount; i2++) {
            DrmInitData.SchemeData mediaSchemeData = this.drmInitData.get(i2);
            int j = 0;
            while (true) {
                if (j >= needFillingCount) {
                    break;
                } else if (mediaSchemeData.canReplace(emptySchemeDatas.get(j))) {
                    exposedSchemeDatas.add(mediaSchemeData);
                    break;
                } else {
                    j++;
                }
            }
        }
        if (exposedSchemeDatas.isEmpty()) {
            manifestDrmData2 = null;
        } else {
            manifestDrmData2 = new DrmInitData(manifestDrmData.schemeType, (DrmInitData.SchemeData[]) exposedSchemeDatas.toArray(new DrmInitData.SchemeData[exposedSchemeDatas.size()]));
        }
        return manifestDrmData2;
    }
}
