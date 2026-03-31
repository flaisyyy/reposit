package com.google.android.exoplayer2.ext.ffmpeg;

import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleDecoder;
import com.google.android.exoplayer2.decoder.SimpleOutputBuffer;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.nio.ByteBuffer;
import java.util.List;

final class FfmpegDecoder extends SimpleDecoder<DecoderInputBuffer, SimpleOutputBuffer, FfmpegDecoderException> {
    private static final int OUTPUT_BUFFER_SIZE_16BIT = 49152;
    private static final int OUTPUT_BUFFER_SIZE_32BIT = 98304;
    private volatile int channelCount;
    private final String codecName;
    private final int encoding;
    private final byte[] extraData;
    private boolean hasOutputFormat;
    private long nativeContext;
    private final int outputBufferSize;
    private volatile int sampleRate;

    private native int ffmpegDecode(long j, ByteBuffer byteBuffer, int i, ByteBuffer byteBuffer2, int i2);

    private native int ffmpegGetChannelCount(long j);

    private native int ffmpegGetSampleRate(long j);

    private native long ffmpegInitialize(String str, byte[] bArr, boolean z);

    private native void ffmpegRelease(long j);

    private native long ffmpegReset(long j, byte[] bArr);

    public FfmpegDecoder(int numInputBuffers, int numOutputBuffers, int initialInputBufferSize, String mimeType, List<byte[]> initializationData, boolean outputFloat) throws FfmpegDecoderException {
        super(new DecoderInputBuffer[numInputBuffers], new SimpleOutputBuffer[numOutputBuffers]);
        if (!FfmpegLibrary.isAvailable()) {
            throw new FfmpegDecoderException("Failed to load decoder native libraries.");
        }
        this.codecName = FfmpegLibrary.getCodecName(mimeType);
        this.extraData = getExtraData(mimeType, initializationData);
        this.encoding = outputFloat ? 4 : 2;
        this.outputBufferSize = outputFloat ? OUTPUT_BUFFER_SIZE_32BIT : OUTPUT_BUFFER_SIZE_16BIT;
        this.nativeContext = ffmpegInitialize(this.codecName, this.extraData, outputFloat);
        if (this.nativeContext == 0) {
            throw new FfmpegDecoderException("Initialization failed.");
        }
        setInitialInputBufferSize(initialInputBufferSize);
    }

    public String getName() {
        return "ffmpeg" + FfmpegLibrary.getVersion() + "-" + this.codecName;
    }

    public DecoderInputBuffer createInputBuffer() {
        return new DecoderInputBuffer(2);
    }

    public SimpleOutputBuffer createOutputBuffer() {
        return new SimpleOutputBuffer(this);
    }

    public FfmpegDecoderException decode(DecoderInputBuffer inputBuffer, SimpleOutputBuffer outputBuffer, boolean reset) {
        if (reset) {
            this.nativeContext = ffmpegReset(this.nativeContext, this.extraData);
            if (this.nativeContext == 0) {
                return new FfmpegDecoderException("Error resetting (see logcat).");
            }
        }
        ByteBuffer inputData = inputBuffer.data;
        int result = ffmpegDecode(this.nativeContext, inputData, inputData.limit(), outputBuffer.init(inputBuffer.timeUs, this.outputBufferSize), this.outputBufferSize);
        if (result < 0) {
            return new FfmpegDecoderException("Error decoding (see logcat). Code: " + result);
        }
        if (!this.hasOutputFormat) {
            this.channelCount = ffmpegGetChannelCount(this.nativeContext);
            this.sampleRate = ffmpegGetSampleRate(this.nativeContext);
            if (this.sampleRate == 0 && "alac".equals(this.codecName)) {
                ParsableByteArray parsableExtraData = new ParsableByteArray(this.extraData);
                parsableExtraData.setPosition(this.extraData.length - 4);
                this.sampleRate = parsableExtraData.readUnsignedIntToInt();
            }
            this.hasOutputFormat = true;
        }
        outputBuffer.data.position(0);
        outputBuffer.data.limit(result);
        return null;
    }

    public void release() {
        super.release();
        ffmpegRelease(this.nativeContext);
        this.nativeContext = 0;
    }

    public int getChannelCount() {
        return this.channelCount;
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public int getEncoding() {
        return this.encoding;
    }

    private static byte[] getExtraData(String mimeType, List<byte[]> initializationData) {
        char c = 65535;
        switch (mimeType.hashCode()) {
            case -1003765268:
                if (mimeType.equals(MimeTypes.AUDIO_VORBIS)) {
                    c = 3;
                    break;
                }
                break;
            case -53558318:
                if (mimeType.equals(MimeTypes.AUDIO_AAC)) {
                    c = 0;
                    break;
                }
                break;
            case 1504470054:
                if (mimeType.equals(MimeTypes.AUDIO_ALAC)) {
                    c = 1;
                    break;
                }
                break;
            case 1504891608:
                if (mimeType.equals(MimeTypes.AUDIO_OPUS)) {
                    c = 2;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 1:
            case 2:
                return initializationData.get(0);
            case 3:
                byte[] header0 = initializationData.get(0);
                byte[] header1 = initializationData.get(1);
                byte[] extraData2 = new byte[(header0.length + header1.length + 6)];
                extraData2[0] = (byte) (header0.length >> 8);
                extraData2[1] = (byte) (header0.length & 255);
                System.arraycopy(header0, 0, extraData2, 2, header0.length);
                extraData2[header0.length + 2] = 0;
                extraData2[header0.length + 3] = 0;
                extraData2[header0.length + 4] = (byte) (header1.length >> 8);
                extraData2[header0.length + 5] = (byte) (header1.length & 255);
                System.arraycopy(header1, 0, extraData2, header0.length + 6, header1.length);
                return extraData2;
            default:
                return null;
        }
    }
}
