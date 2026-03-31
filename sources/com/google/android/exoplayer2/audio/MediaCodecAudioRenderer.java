package com.google.android.exoplayer2.audio;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Surface;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.util.MediaClock;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;

@TargetApi(16)
public class MediaCodecAudioRenderer extends MediaCodecRenderer implements MediaClock {
    /* access modifiers changed from: private */
    public boolean allowPositionDiscontinuity;
    private final AudioSink audioSink;
    private int channelCount;
    private boolean codecNeedsDiscardChannelsWorkaround;
    private long currentPositionUs;
    private int encoderDelay;
    private int encoderPadding;
    /* access modifiers changed from: private */
    public final AudioRendererEventListener.EventDispatcher eventDispatcher;
    private boolean passthroughEnabled;
    private MediaFormat passthroughMediaFormat;
    private int pcmEncoding;

    public MediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector) {
        this(mediaCodecSelector, (DrmSessionManager<FrameworkMediaCrypto>) null, true);
    }

    public MediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys) {
        this(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, (Handler) null, (AudioRendererEventListener) null);
    }

    public MediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, @Nullable Handler eventHandler, @Nullable AudioRendererEventListener eventListener) {
        this(mediaCodecSelector, (DrmSessionManager<FrameworkMediaCrypto>) null, true, eventHandler, eventListener);
    }

    public MediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, @Nullable Handler eventHandler, @Nullable AudioRendererEventListener eventListener) {
        this(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, (AudioCapabilities) null, new AudioProcessor[0]);
    }

    public MediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, @Nullable Handler eventHandler, @Nullable AudioRendererEventListener eventListener, @Nullable AudioCapabilities audioCapabilities, AudioProcessor... audioProcessors) {
        this(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, new DefaultAudioSink(audioCapabilities, audioProcessors));
    }

    public MediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, @Nullable Handler eventHandler, @Nullable AudioRendererEventListener eventListener, AudioSink audioSink2) {
        super(1, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys);
        this.eventDispatcher = new AudioRendererEventListener.EventDispatcher(eventHandler, eventListener);
        this.audioSink = audioSink2;
        audioSink2.setListener(new AudioSinkListener());
    }

    /* access modifiers changed from: protected */
    public int supportsFormat(MediaCodecSelector mediaCodecSelector, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, Format format) throws MediaCodecUtil.DecoderQueryException {
        String mimeType = format.sampleMimeType;
        if (!MimeTypes.isAudio(mimeType)) {
            return 0;
        }
        int tunnelingSupport = Util.SDK_INT >= 21 ? 32 : 0;
        boolean supportsFormatDrm = supportsFormatDrm(drmSessionManager, format.drmInitData);
        if (supportsFormatDrm && allowPassthrough(mimeType) && mediaCodecSelector.getPassthroughDecoderInfo() != null) {
            return tunnelingSupport | 8 | 4;
        }
        if ((MimeTypes.AUDIO_RAW.equals(mimeType) && !this.audioSink.isEncodingSupported(format.pcmEncoding)) || !this.audioSink.isEncodingSupported(2)) {
            return 1;
        }
        boolean requiresSecureDecryption = false;
        DrmInitData drmInitData = format.drmInitData;
        if (drmInitData != null) {
            for (int i = 0; i < drmInitData.schemeDataCount; i++) {
                requiresSecureDecryption |= drmInitData.get(i).requiresSecureDecryption;
            }
        }
        MediaCodecInfo decoderInfo = mediaCodecSelector.getDecoderInfo(mimeType, requiresSecureDecryption);
        if (decoderInfo == null) {
            return (!requiresSecureDecryption || mediaCodecSelector.getDecoderInfo(mimeType, false) == null) ? 1 : 2;
        }
        if (!supportsFormatDrm) {
            return 2;
        }
        return tunnelingSupport | 8 | (Util.SDK_INT < 21 || ((format.sampleRate == -1 || decoderInfo.isAudioSampleRateSupportedV21(format.sampleRate)) && (format.channelCount == -1 || decoderInfo.isAudioChannelCountSupportedV21(format.channelCount))) ? 4 : 3);
    }

    /* access modifiers changed from: protected */
    public MediaCodecInfo getDecoderInfo(MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
        MediaCodecInfo passthroughDecoderInfo;
        if (!allowPassthrough(format.sampleMimeType) || (passthroughDecoderInfo = mediaCodecSelector.getPassthroughDecoderInfo()) == null) {
            this.passthroughEnabled = false;
            return super.getDecoderInfo(mediaCodecSelector, format, requiresSecureDecoder);
        }
        this.passthroughEnabled = true;
        return passthroughDecoderInfo;
    }

    /* access modifiers changed from: protected */
    public boolean allowPassthrough(String mimeType) {
        int encoding = MimeTypes.getEncoding(mimeType);
        return encoding != 0 && this.audioSink.isEncodingSupported(encoding);
    }

    /* access modifiers changed from: protected */
    public void configureCodec(MediaCodecInfo codecInfo, MediaCodec codec, Format format, MediaCrypto crypto) {
        this.codecNeedsDiscardChannelsWorkaround = codecNeedsDiscardChannelsWorkaround(codecInfo.name);
        if (this.passthroughEnabled) {
            this.passthroughMediaFormat = format.getFrameworkMediaFormatV16();
            this.passthroughMediaFormat.setString("mime", MimeTypes.AUDIO_RAW);
            codec.configure(this.passthroughMediaFormat, (Surface) null, crypto, 0);
            this.passthroughMediaFormat.setString("mime", format.sampleMimeType);
            return;
        }
        codec.configure(format.getFrameworkMediaFormatV16(), (Surface) null, crypto, 0);
        this.passthroughMediaFormat = null;
    }

    public MediaClock getMediaClock() {
        return this;
    }

    /* access modifiers changed from: protected */
    public void onCodecInitialized(String name, long initializedTimestampMs, long initializationDurationMs) {
        this.eventDispatcher.decoderInitialized(name, initializedTimestampMs, initializationDurationMs);
    }

    /* access modifiers changed from: protected */
    public void onInputFormatChanged(Format newFormat) throws ExoPlaybackException {
        int i;
        int i2 = 0;
        super.onInputFormatChanged(newFormat);
        this.eventDispatcher.inputFormatChanged(newFormat);
        this.pcmEncoding = MimeTypes.AUDIO_RAW.equals(newFormat.sampleMimeType) ? newFormat.pcmEncoding : 2;
        this.channelCount = newFormat.channelCount;
        if (newFormat.encoderDelay != -1) {
            i = newFormat.encoderDelay;
        } else {
            i = 0;
        }
        this.encoderDelay = i;
        if (newFormat.encoderPadding != -1) {
            i2 = newFormat.encoderPadding;
        }
        this.encoderPadding = i2;
    }

    /* access modifiers changed from: protected */
    public void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) throws ExoPlaybackException {
        int encoding;
        MediaFormat format;
        int[] channelMap;
        if (this.passthroughMediaFormat != null) {
            encoding = MimeTypes.getEncoding(this.passthroughMediaFormat.getString("mime"));
            format = this.passthroughMediaFormat;
        } else {
            encoding = this.pcmEncoding;
            format = outputFormat;
        }
        int channelCount2 = format.getInteger("channel-count");
        int sampleRate = format.getInteger("sample-rate");
        if (!this.codecNeedsDiscardChannelsWorkaround || channelCount2 != 6 || this.channelCount >= 6) {
            channelMap = null;
        } else {
            channelMap = new int[this.channelCount];
            for (int i = 0; i < this.channelCount; i++) {
                channelMap[i] = i;
            }
        }
        try {
            this.audioSink.configure(encoding, channelCount2, sampleRate, 0, channelMap, this.encoderDelay, this.encoderPadding);
        } catch (AudioSink.ConfigurationException e) {
            throw ExoPlaybackException.createForRenderer(e, getIndex());
        }
    }

    /* access modifiers changed from: protected */
    public void onAudioSessionId(int audioSessionId) {
    }

    /* access modifiers changed from: protected */
    public void onAudioTrackPositionDiscontinuity() {
    }

    /* access modifiers changed from: protected */
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
    }

    /* access modifiers changed from: protected */
    public void onEnabled(boolean joining) throws ExoPlaybackException {
        super.onEnabled(joining);
        this.eventDispatcher.enabled(this.decoderCounters);
        int tunnelingAudioSessionId = getConfiguration().tunnelingAudioSessionId;
        if (tunnelingAudioSessionId != 0) {
            this.audioSink.enableTunnelingV21(tunnelingAudioSessionId);
        } else {
            this.audioSink.disableTunneling();
        }
    }

    /* access modifiers changed from: protected */
    public void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        super.onPositionReset(positionUs, joining);
        this.audioSink.reset();
        this.currentPositionUs = positionUs;
        this.allowPositionDiscontinuity = true;
    }

    /* access modifiers changed from: protected */
    public void onStarted() {
        super.onStarted();
        this.audioSink.play();
    }

    /* access modifiers changed from: protected */
    public void onStopped() {
        this.audioSink.pause();
        super.onStopped();
    }

    /* access modifiers changed from: protected */
    public void onDisabled() {
        try {
            this.audioSink.release();
            try {
                super.onDisabled();
            } finally {
                this.decoderCounters.ensureUpdated();
                this.eventDispatcher.disabled(this.decoderCounters);
            }
        } catch (Throwable th) {
            super.onDisabled();
            throw th;
        } finally {
            this.decoderCounters.ensureUpdated();
            this.eventDispatcher.disabled(this.decoderCounters);
        }
    }

    public boolean isEnded() {
        return super.isEnded() && this.audioSink.isEnded();
    }

    public boolean isReady() {
        return this.audioSink.hasPendingData() || super.isReady();
    }

    public long getPositionUs() {
        long newCurrentPositionUs = this.audioSink.getCurrentPositionUs(isEnded());
        if (newCurrentPositionUs != Long.MIN_VALUE) {
            if (!this.allowPositionDiscontinuity) {
                newCurrentPositionUs = Math.max(this.currentPositionUs, newCurrentPositionUs);
            }
            this.currentPositionUs = newCurrentPositionUs;
            this.allowPositionDiscontinuity = false;
        }
        return this.currentPositionUs;
    }

    public PlaybackParameters setPlaybackParameters(PlaybackParameters playbackParameters) {
        return this.audioSink.setPlaybackParameters(playbackParameters);
    }

    public PlaybackParameters getPlaybackParameters() {
        return this.audioSink.getPlaybackParameters();
    }

    /* access modifiers changed from: protected */
    public boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs, boolean shouldSkip) throws ExoPlaybackException {
        if (this.passthroughEnabled && (bufferFlags & 2) != 0) {
            codec.releaseOutputBuffer(bufferIndex, false);
            return true;
        } else if (shouldSkip) {
            codec.releaseOutputBuffer(bufferIndex, false);
            this.decoderCounters.skippedOutputBufferCount++;
            this.audioSink.handleDiscontinuity();
            return true;
        } else {
            try {
                if (!this.audioSink.handleBuffer(buffer, bufferPresentationTimeUs)) {
                    return false;
                }
                codec.releaseOutputBuffer(bufferIndex, false);
                this.decoderCounters.renderedOutputBufferCount++;
                return true;
            } catch (AudioSink.InitializationException | AudioSink.WriteException e) {
                throw ExoPlaybackException.createForRenderer(e, getIndex());
            }
        }
    }

    /* access modifiers changed from: protected */
    public void renderToEndOfStream() throws ExoPlaybackException {
        try {
            this.audioSink.playToEndOfStream();
        } catch (AudioSink.WriteException e) {
            throw ExoPlaybackException.createForRenderer(e, getIndex());
        }
    }

    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        switch (messageType) {
            case 2:
                this.audioSink.setVolume(((Float) message).floatValue());
                return;
            case 3:
                this.audioSink.setAudioAttributes((AudioAttributes) message);
                return;
            default:
                super.handleMessage(messageType, message);
                return;
        }
    }

    private static boolean codecNeedsDiscardChannelsWorkaround(String codecName) {
        return Util.SDK_INT < 24 && "OMX.SEC.aac.dec".equals(codecName) && "samsung".equals(Util.MANUFACTURER) && (Util.DEVICE.startsWith("zeroflte") || Util.DEVICE.startsWith("herolte") || Util.DEVICE.startsWith("heroqlte"));
    }

    private final class AudioSinkListener implements AudioSink.Listener {
        private AudioSinkListener() {
        }

        public void onAudioSessionId(int audioSessionId) {
            MediaCodecAudioRenderer.this.eventDispatcher.audioSessionId(audioSessionId);
            MediaCodecAudioRenderer.this.onAudioSessionId(audioSessionId);
        }

        public void onPositionDiscontinuity() {
            MediaCodecAudioRenderer.this.onAudioTrackPositionDiscontinuity();
            boolean unused = MediaCodecAudioRenderer.this.allowPositionDiscontinuity = true;
        }

        public void onUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            MediaCodecAudioRenderer.this.eventDispatcher.audioTrackUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
            MediaCodecAudioRenderer.this.onAudioTrackUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }
    }
}
