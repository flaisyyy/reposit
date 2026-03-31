package com.google.android.exoplayer2.ext.ffmpeg;

import android.os.Handler;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.audio.DefaultAudioSink;
import com.google.android.exoplayer2.audio.SimpleDecoderAudioRenderer;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.util.MimeTypes;
import java.util.List;

public final class FfmpegAudioRenderer extends SimpleDecoderAudioRenderer {
    private static final int INITIAL_INPUT_BUFFER_SIZE = 5760;
    private static final int NUM_BUFFERS = 16;
    private FfmpegDecoder decoder;
    private final boolean enableFloatOutput;

    public FfmpegAudioRenderer() {
        this((Handler) null, (AudioRendererEventListener) null, new AudioProcessor[0]);
    }

    public FfmpegAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener, AudioProcessor... audioProcessors) {
        this(eventHandler, eventListener, new DefaultAudioSink((AudioCapabilities) null, audioProcessors), false);
    }

    public FfmpegAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener, AudioSink audioSink, boolean enableFloatOutput2) {
        super(eventHandler, eventListener, (DrmSessionManager<ExoMediaCrypto>) null, false, audioSink);
        this.enableFloatOutput = enableFloatOutput2;
    }

    /* access modifiers changed from: protected */
    public int supportsFormatInternal(DrmSessionManager<ExoMediaCrypto> drmSessionManager, Format format) {
        String sampleMimeType = format.sampleMimeType;
        if (!FfmpegLibrary.isAvailable() || !MimeTypes.isAudio(sampleMimeType)) {
            return 0;
        }
        if (!FfmpegLibrary.supportsFormat(sampleMimeType) || !isOutputSupported(format)) {
            return 1;
        }
        if (!supportsFormatDrm(drmSessionManager, format.drmInitData)) {
            return 2;
        }
        return 4;
    }

    public final int supportsMixedMimeTypeAdaptation() throws ExoPlaybackException {
        return 8;
    }

    /* access modifiers changed from: protected */
    public FfmpegDecoder createDecoder(Format format, ExoMediaCrypto mediaCrypto) throws FfmpegDecoderException {
        this.decoder = new FfmpegDecoder(16, 16, INITIAL_INPUT_BUFFER_SIZE, format.sampleMimeType, format.initializationData, shouldUseFloatOutput(format));
        return this.decoder;
    }

    public Format getOutputFormat() {
        return Format.createAudioSampleFormat((String) null, MimeTypes.AUDIO_RAW, (String) null, -1, -1, this.decoder.getChannelCount(), this.decoder.getSampleRate(), this.decoder.getEncoding(), (List<byte[]>) null, (DrmInitData) null, 0, (String) null);
    }

    private boolean isOutputSupported(Format inputFormat) {
        return shouldUseFloatOutput(inputFormat) || supportsOutputEncoding(2);
    }

    private boolean shouldUseFloatOutput(Format inputFormat) {
        if (!this.enableFloatOutput || !supportsOutputEncoding(4)) {
            return false;
        }
        String str = inputFormat.sampleMimeType;
        char c = 65535;
        switch (str.hashCode()) {
            case 187078296:
                if (str.equals(MimeTypes.AUDIO_AC3)) {
                    c = 1;
                    break;
                }
                break;
            case 187094639:
                if (str.equals(MimeTypes.AUDIO_RAW)) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                if (inputFormat.pcmEncoding == Integer.MIN_VALUE || inputFormat.pcmEncoding == 1073741824 || inputFormat.pcmEncoding == 4) {
                    return true;
                }
                return false;
            case 1:
                return false;
            default:
                return true;
        }
    }
}
