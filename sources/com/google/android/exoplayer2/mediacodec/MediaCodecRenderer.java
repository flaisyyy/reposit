package com.google.android.exoplayer2.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.exoplayer2.BaseRenderer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.NalUnitUtil;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.util.Util;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@TargetApi(16)
public abstract class MediaCodecRenderer extends BaseRenderer {
    private static final byte[] ADAPTATION_WORKAROUND_BUFFER = Util.getBytesFromHexString("0000016742C00BDA259000000168CE0F13200000016588840DCE7118A0002FBF1C31C3275D78");
    private static final int ADAPTATION_WORKAROUND_MODE_ALWAYS = 2;
    private static final int ADAPTATION_WORKAROUND_MODE_NEVER = 0;
    private static final int ADAPTATION_WORKAROUND_MODE_SAME_RESOLUTION = 1;
    private static final int ADAPTATION_WORKAROUND_SLICE_WIDTH_HEIGHT = 32;
    private static final long MAX_CODEC_HOTSWAP_TIME_MS = 1000;
    private static final int RECONFIGURATION_STATE_NONE = 0;
    private static final int RECONFIGURATION_STATE_QUEUE_PENDING = 2;
    private static final int RECONFIGURATION_STATE_WRITE_PENDING = 1;
    private static final int REINITIALIZATION_STATE_NONE = 0;
    private static final int REINITIALIZATION_STATE_SIGNAL_END_OF_STREAM = 1;
    private static final int REINITIALIZATION_STATE_WAIT_END_OF_STREAM = 2;
    private static final String TAG = "MediaCodecRenderer";
    private final DecoderInputBuffer buffer;
    private MediaCodec codec;
    private int codecAdaptationWorkaroundMode;
    private long codecHotswapDeadlineMs;
    private MediaCodecInfo codecInfo;
    private boolean codecNeedsAdaptationWorkaroundBuffer;
    private boolean codecNeedsDiscardToSpsWorkaround;
    private boolean codecNeedsEosFlushWorkaround;
    private boolean codecNeedsEosOutputExceptionWorkaround;
    private boolean codecNeedsEosPropagationWorkaround;
    private boolean codecNeedsFlushWorkaround;
    private boolean codecNeedsMonoChannelCountWorkaround;
    private boolean codecReceivedBuffers;
    private boolean codecReceivedEos;
    private int codecReconfigurationState;
    private boolean codecReconfigured;
    private int codecReinitializationState;
    private final List<Long> decodeOnlyPresentationTimestamps;
    protected DecoderCounters decoderCounters;
    private DrmSession<FrameworkMediaCrypto> drmSession;
    @Nullable
    private final DrmSessionManager<FrameworkMediaCrypto> drmSessionManager;
    private final DecoderInputBuffer flagsOnlyBuffer;
    private Format format;
    private final FormatHolder formatHolder;
    private ByteBuffer[] inputBuffers;
    private int inputIndex;
    private boolean inputStreamEnded;
    private final MediaCodecSelector mediaCodecSelector;
    private final MediaCodec.BufferInfo outputBufferInfo;
    private ByteBuffer[] outputBuffers;
    private int outputIndex;
    private boolean outputStreamEnded;
    private DrmSession<FrameworkMediaCrypto> pendingDrmSession;
    private final boolean playClearSamplesWithoutKeys;
    private boolean shouldSkipAdaptationWorkaroundOutputBuffer;
    private boolean shouldSkipOutputBuffer;
    private boolean waitingForFirstSyncFrame;
    private boolean waitingForKeys;

    @Retention(RetentionPolicy.SOURCE)
    private @interface AdaptationWorkaroundMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface ReconfigurationState {
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface ReinitializationState {
    }

    /* access modifiers changed from: protected */
    public abstract void configureCodec(MediaCodecInfo mediaCodecInfo, MediaCodec mediaCodec, Format format2, MediaCrypto mediaCrypto) throws MediaCodecUtil.DecoderQueryException;

    /* access modifiers changed from: protected */
    public abstract boolean processOutputBuffer(long j, long j2, MediaCodec mediaCodec, ByteBuffer byteBuffer, int i, int i2, long j3, boolean z) throws ExoPlaybackException;

    /* access modifiers changed from: protected */
    public abstract int supportsFormat(MediaCodecSelector mediaCodecSelector2, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, Format format2) throws MediaCodecUtil.DecoderQueryException;

    public static class DecoderInitializationException extends Exception {
        private static final int CUSTOM_ERROR_CODE_BASE = -50000;
        private static final int DECODER_QUERY_ERROR = -49998;
        private static final int NO_SUITABLE_DECODER_ERROR = -49999;
        public final String decoderName;
        public final String diagnosticInfo;
        public final String mimeType;
        public final boolean secureDecoderRequired;

        public DecoderInitializationException(Format format, Throwable cause, boolean secureDecoderRequired2, int errorCode) {
            super("Decoder init failed: [" + errorCode + "], " + format, cause);
            this.mimeType = format.sampleMimeType;
            this.secureDecoderRequired = secureDecoderRequired2;
            this.decoderName = null;
            this.diagnosticInfo = buildCustomDiagnosticInfo(errorCode);
        }

        public DecoderInitializationException(Format format, Throwable cause, boolean secureDecoderRequired2, String decoderName2) {
            super("Decoder init failed: " + decoderName2 + ", " + format, cause);
            this.mimeType = format.sampleMimeType;
            this.secureDecoderRequired = secureDecoderRequired2;
            this.decoderName = decoderName2;
            this.diagnosticInfo = Util.SDK_INT >= 21 ? getDiagnosticInfoV21(cause) : null;
        }

        @TargetApi(21)
        private static String getDiagnosticInfoV21(Throwable cause) {
            if (cause instanceof MediaCodec.CodecException) {
                return ((MediaCodec.CodecException) cause).getDiagnosticInfo();
            }
            return null;
        }

        private static String buildCustomDiagnosticInfo(int errorCode) {
            return "com.google.android.exoplayer.MediaCodecTrackRenderer_" + (errorCode < 0 ? "neg_" : "") + Math.abs(errorCode);
        }
    }

    public MediaCodecRenderer(int trackType, MediaCodecSelector mediaCodecSelector2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, boolean playClearSamplesWithoutKeys2) {
        super(trackType);
        Assertions.checkState(Util.SDK_INT >= 16);
        this.mediaCodecSelector = (MediaCodecSelector) Assertions.checkNotNull(mediaCodecSelector2);
        this.drmSessionManager = drmSessionManager2;
        this.playClearSamplesWithoutKeys = playClearSamplesWithoutKeys2;
        this.buffer = new DecoderInputBuffer(0);
        this.flagsOnlyBuffer = DecoderInputBuffer.newFlagsOnlyInstance();
        this.formatHolder = new FormatHolder();
        this.decodeOnlyPresentationTimestamps = new ArrayList();
        this.outputBufferInfo = new MediaCodec.BufferInfo();
        this.codecReconfigurationState = 0;
        this.codecReinitializationState = 0;
    }

    public final int supportsMixedMimeTypeAdaptation() {
        return 8;
    }

    public final int supportsFormat(Format format2) throws ExoPlaybackException {
        try {
            return supportsFormat(this.mediaCodecSelector, this.drmSessionManager, format2);
        } catch (MediaCodecUtil.DecoderQueryException e) {
            throw ExoPlaybackException.createForRenderer(e, getIndex());
        }
    }

    /* access modifiers changed from: protected */
    public MediaCodecInfo getDecoderInfo(MediaCodecSelector mediaCodecSelector2, Format format2, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
        return mediaCodecSelector2.getDecoderInfo(format2.sampleMimeType, requiresSecureDecoder);
    }

    /* access modifiers changed from: protected */
    public final void maybeInitCodec() throws ExoPlaybackException {
        if (this.codec == null && this.format != null) {
            this.drmSession = this.pendingDrmSession;
            String mimeType = this.format.sampleMimeType;
            MediaCrypto wrappedMediaCrypto = null;
            boolean drmSessionRequiresSecureDecoder = false;
            if (this.drmSession != null) {
                FrameworkMediaCrypto mediaCrypto = this.drmSession.getMediaCrypto();
                if (mediaCrypto == null) {
                    DrmSession.DrmSessionException drmError = this.drmSession.getError();
                    if (drmError != null) {
                        throw ExoPlaybackException.createForRenderer(drmError, getIndex());
                    }
                    return;
                }
                wrappedMediaCrypto = mediaCrypto.getWrappedMediaCrypto();
                drmSessionRequiresSecureDecoder = mediaCrypto.requiresSecureDecoderComponent(mimeType);
            }
            if (this.codecInfo == null) {
                try {
                    this.codecInfo = getDecoderInfo(this.mediaCodecSelector, this.format, drmSessionRequiresSecureDecoder);
                    if (this.codecInfo == null && drmSessionRequiresSecureDecoder) {
                        this.codecInfo = getDecoderInfo(this.mediaCodecSelector, this.format, false);
                        if (this.codecInfo != null) {
                            Log.w(TAG, "Drm session requires secure decoder for " + mimeType + ", but no secure decoder available. Trying to proceed with " + this.codecInfo.name + ".");
                        }
                    }
                } catch (MediaCodecUtil.DecoderQueryException e) {
                    throwDecoderInitError(new DecoderInitializationException(this.format, (Throwable) e, drmSessionRequiresSecureDecoder, -49998));
                }
                if (this.codecInfo == null) {
                    throwDecoderInitError(new DecoderInitializationException(this.format, (Throwable) null, drmSessionRequiresSecureDecoder, -49999));
                }
            }
            if (shouldInitCodec(this.codecInfo)) {
                String codecName = this.codecInfo.name;
                this.codecAdaptationWorkaroundMode = codecAdaptationWorkaroundMode(codecName);
                this.codecNeedsDiscardToSpsWorkaround = codecNeedsDiscardToSpsWorkaround(codecName, this.format);
                this.codecNeedsFlushWorkaround = codecNeedsFlushWorkaround(codecName);
                this.codecNeedsEosPropagationWorkaround = codecNeedsEosPropagationWorkaround(codecName);
                this.codecNeedsEosFlushWorkaround = codecNeedsEosFlushWorkaround(codecName);
                this.codecNeedsEosOutputExceptionWorkaround = codecNeedsEosOutputExceptionWorkaround(codecName);
                this.codecNeedsMonoChannelCountWorkaround = codecNeedsMonoChannelCountWorkaround(codecName, this.format);
                try {
                    long codecInitializingTimestamp = SystemClock.elapsedRealtime();
                    TraceUtil.beginSection("createCodec:" + codecName);
                    this.codec = MediaCodec.createByCodecName(codecName);
                    TraceUtil.endSection();
                    TraceUtil.beginSection("configureCodec");
                    configureCodec(this.codecInfo, this.codec, this.format, wrappedMediaCrypto);
                    TraceUtil.endSection();
                    TraceUtil.beginSection("startCodec");
                    this.codec.start();
                    TraceUtil.endSection();
                    long codecInitializedTimestamp = SystemClock.elapsedRealtime();
                    onCodecInitialized(codecName, codecInitializedTimestamp, codecInitializedTimestamp - codecInitializingTimestamp);
                    this.inputBuffers = this.codec.getInputBuffers();
                    this.outputBuffers = this.codec.getOutputBuffers();
                } catch (Exception e2) {
                    throwDecoderInitError(new DecoderInitializationException(this.format, (Throwable) e2, drmSessionRequiresSecureDecoder, codecName));
                }
                this.codecHotswapDeadlineMs = getState() == 2 ? SystemClock.elapsedRealtime() + MAX_CODEC_HOTSWAP_TIME_MS : C.TIME_UNSET;
                this.inputIndex = -1;
                this.outputIndex = -1;
                this.waitingForFirstSyncFrame = true;
                this.decoderCounters.decoderInitCount++;
            }
        }
    }

    private void throwDecoderInitError(DecoderInitializationException e) throws ExoPlaybackException {
        throw ExoPlaybackException.createForRenderer(e, getIndex());
    }

    /* access modifiers changed from: protected */
    public boolean shouldInitCodec(MediaCodecInfo codecInfo2) {
        return true;
    }

    /* access modifiers changed from: protected */
    public final MediaCodec getCodec() {
        return this.codec;
    }

    /* access modifiers changed from: protected */
    public final MediaCodecInfo getCodecInfo() {
        return this.codecInfo;
    }

    /* access modifiers changed from: protected */
    public void onEnabled(boolean joining) throws ExoPlaybackException {
        this.decoderCounters = new DecoderCounters();
    }

    /* access modifiers changed from: protected */
    public void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        this.inputStreamEnded = false;
        this.outputStreamEnded = false;
        if (this.codec != null) {
            flushCodec();
        }
    }

    /* access modifiers changed from: protected */
    public void onDisabled() {
        this.format = null;
        try {
            releaseCodec();
            try {
                if (this.drmSession != null) {
                    this.drmSessionManager.releaseSession(this.drmSession);
                }
                try {
                    if (!(this.pendingDrmSession == null || this.pendingDrmSession == this.drmSession)) {
                        this.drmSessionManager.releaseSession(this.pendingDrmSession);
                    }
                } finally {
                    this.drmSession = null;
                    this.pendingDrmSession = null;
                }
            } catch (Throwable th) {
                if (!(this.pendingDrmSession == null || this.pendingDrmSession == this.drmSession)) {
                    this.drmSessionManager.releaseSession(this.pendingDrmSession);
                }
                throw th;
            } finally {
                this.drmSession = null;
                this.pendingDrmSession = null;
            }
        } catch (Throwable th2) {
            try {
                if (!(this.pendingDrmSession == null || this.pendingDrmSession == this.drmSession)) {
                    this.drmSessionManager.releaseSession(this.pendingDrmSession);
                }
                throw th2;
            } finally {
                this.drmSession = null;
                this.pendingDrmSession = null;
            }
        } finally {
        }
    }

    /* access modifiers changed from: protected */
    public void releaseCodec() {
        this.codecHotswapDeadlineMs = C.TIME_UNSET;
        this.inputIndex = -1;
        this.outputIndex = -1;
        this.waitingForKeys = false;
        this.shouldSkipOutputBuffer = false;
        this.decodeOnlyPresentationTimestamps.clear();
        this.inputBuffers = null;
        this.outputBuffers = null;
        this.codecInfo = null;
        this.codecReconfigured = false;
        this.codecReceivedBuffers = false;
        this.codecNeedsDiscardToSpsWorkaround = false;
        this.codecNeedsFlushWorkaround = false;
        this.codecAdaptationWorkaroundMode = 0;
        this.codecNeedsEosPropagationWorkaround = false;
        this.codecNeedsEosFlushWorkaround = false;
        this.codecNeedsMonoChannelCountWorkaround = false;
        this.codecNeedsAdaptationWorkaroundBuffer = false;
        this.shouldSkipAdaptationWorkaroundOutputBuffer = false;
        this.codecReceivedEos = false;
        this.codecReconfigurationState = 0;
        this.codecReinitializationState = 0;
        this.buffer.data = null;
        if (this.codec != null) {
            this.decoderCounters.decoderReleaseCount++;
            try {
                this.codec.stop();
                try {
                    this.codec.release();
                    this.codec = null;
                    if (this.drmSession != null && this.pendingDrmSession != this.drmSession) {
                        try {
                            this.drmSessionManager.releaseSession(this.drmSession);
                        } finally {
                            this.drmSession = null;
                        }
                    }
                } catch (Throwable th) {
                    this.codec = null;
                    if (!(this.drmSession == null || this.pendingDrmSession == this.drmSession)) {
                        this.drmSessionManager.releaseSession(this.drmSession);
                    }
                    throw th;
                } finally {
                    this.drmSession = null;
                }
            } catch (Throwable th2) {
                this.codec = null;
                if (!(this.drmSession == null || this.pendingDrmSession == this.drmSession)) {
                    try {
                        this.drmSessionManager.releaseSession(this.drmSession);
                    } finally {
                        this.drmSession = null;
                    }
                }
                throw th2;
            } finally {
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onStarted() {
    }

    /* access modifiers changed from: protected */
    public void onStopped() {
    }

    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        if (this.outputStreamEnded) {
            renderToEndOfStream();
            return;
        }
        if (this.format == null) {
            this.flagsOnlyBuffer.clear();
            int result = readSource(this.formatHolder, this.flagsOnlyBuffer, true);
            if (result == -5) {
                onInputFormatChanged(this.formatHolder.format);
            } else if (result == -4) {
                Assertions.checkState(this.flagsOnlyBuffer.isEndOfStream());
                this.inputStreamEnded = true;
                processEndOfStream();
                return;
            } else {
                return;
            }
        }
        maybeInitCodec();
        if (this.codec != null) {
            TraceUtil.beginSection("drainAndFeed");
            do {
            } while (drainOutputBuffer(positionUs, elapsedRealtimeUs));
            do {
            } while (feedInputBuffer());
            TraceUtil.endSection();
        } else {
            this.decoderCounters.skippedInputBufferCount += skipSource(positionUs);
            this.flagsOnlyBuffer.clear();
            int result2 = readSource(this.formatHolder, this.flagsOnlyBuffer, false);
            if (result2 == -5) {
                onInputFormatChanged(this.formatHolder.format);
            } else if (result2 == -4) {
                Assertions.checkState(this.flagsOnlyBuffer.isEndOfStream());
                this.inputStreamEnded = true;
                processEndOfStream();
            }
        }
        this.decoderCounters.ensureUpdated();
    }

    /* access modifiers changed from: protected */
    public void flushCodec() throws ExoPlaybackException {
        this.codecHotswapDeadlineMs = C.TIME_UNSET;
        this.inputIndex = -1;
        this.outputIndex = -1;
        this.waitingForFirstSyncFrame = true;
        this.waitingForKeys = false;
        this.shouldSkipOutputBuffer = false;
        this.decodeOnlyPresentationTimestamps.clear();
        this.codecNeedsAdaptationWorkaroundBuffer = false;
        this.shouldSkipAdaptationWorkaroundOutputBuffer = false;
        if (this.codecNeedsFlushWorkaround || (this.codecNeedsEosFlushWorkaround && this.codecReceivedEos)) {
            releaseCodec();
            maybeInitCodec();
        } else if (this.codecReinitializationState != 0) {
            releaseCodec();
            maybeInitCodec();
        } else {
            this.codec.flush();
            this.codecReceivedBuffers = false;
        }
        if (this.codecReconfigured && this.format != null) {
            this.codecReconfigurationState = 1;
        }
    }

    private boolean feedInputBuffer() throws ExoPlaybackException {
        int result;
        if (this.codec == null || this.codecReinitializationState == 2 || this.inputStreamEnded) {
            return false;
        }
        if (this.inputIndex < 0) {
            this.inputIndex = this.codec.dequeueInputBuffer(0);
            if (this.inputIndex < 0) {
                return false;
            }
            this.buffer.data = this.inputBuffers[this.inputIndex];
            this.buffer.clear();
        }
        if (this.codecReinitializationState == 1) {
            if (!this.codecNeedsEosPropagationWorkaround) {
                this.codecReceivedEos = true;
                this.codec.queueInputBuffer(this.inputIndex, 0, 0, 0, 4);
                this.inputIndex = -1;
            }
            this.codecReinitializationState = 2;
            return false;
        } else if (this.codecNeedsAdaptationWorkaroundBuffer) {
            this.codecNeedsAdaptationWorkaroundBuffer = false;
            this.buffer.data.put(ADAPTATION_WORKAROUND_BUFFER);
            this.codec.queueInputBuffer(this.inputIndex, 0, ADAPTATION_WORKAROUND_BUFFER.length, 0, 0);
            this.inputIndex = -1;
            this.codecReceivedBuffers = true;
            return true;
        } else {
            int adaptiveReconfigurationBytes = 0;
            if (this.waitingForKeys) {
                result = -4;
            } else {
                if (this.codecReconfigurationState == 1) {
                    for (int i = 0; i < this.format.initializationData.size(); i++) {
                        this.buffer.data.put(this.format.initializationData.get(i));
                    }
                    this.codecReconfigurationState = 2;
                }
                adaptiveReconfigurationBytes = this.buffer.data.position();
                result = readSource(this.formatHolder, this.buffer, false);
            }
            if (result == -3) {
                return false;
            }
            if (result == -5) {
                if (this.codecReconfigurationState == 2) {
                    this.buffer.clear();
                    this.codecReconfigurationState = 1;
                }
                onInputFormatChanged(this.formatHolder.format);
                return true;
            } else if (this.buffer.isEndOfStream()) {
                if (this.codecReconfigurationState == 2) {
                    this.buffer.clear();
                    this.codecReconfigurationState = 1;
                }
                this.inputStreamEnded = true;
                if (!this.codecReceivedBuffers) {
                    processEndOfStream();
                    return false;
                }
                try {
                    if (!this.codecNeedsEosPropagationWorkaround) {
                        this.codecReceivedEos = true;
                        this.codec.queueInputBuffer(this.inputIndex, 0, 0, 0, 4);
                        this.inputIndex = -1;
                    }
                    return false;
                } catch (MediaCodec.CryptoException e) {
                    throw ExoPlaybackException.createForRenderer(e, getIndex());
                }
            } else if (!this.waitingForFirstSyncFrame || this.buffer.isKeyFrame()) {
                this.waitingForFirstSyncFrame = false;
                boolean bufferEncrypted = this.buffer.isEncrypted();
                this.waitingForKeys = shouldWaitForKeys(bufferEncrypted);
                if (this.waitingForKeys) {
                    return false;
                }
                if (this.codecNeedsDiscardToSpsWorkaround && !bufferEncrypted) {
                    NalUnitUtil.discardToSps(this.buffer.data);
                    if (this.buffer.data.position() == 0) {
                        return true;
                    }
                    this.codecNeedsDiscardToSpsWorkaround = false;
                }
                try {
                    long presentationTimeUs = this.buffer.timeUs;
                    if (this.buffer.isDecodeOnly()) {
                        this.decodeOnlyPresentationTimestamps.add(Long.valueOf(presentationTimeUs));
                    }
                    this.buffer.flip();
                    onQueueInputBuffer(this.buffer);
                    if (bufferEncrypted) {
                        this.codec.queueSecureInputBuffer(this.inputIndex, 0, getFrameworkCryptoInfo(this.buffer, adaptiveReconfigurationBytes), presentationTimeUs, 0);
                    } else {
                        this.codec.queueInputBuffer(this.inputIndex, 0, this.buffer.data.limit(), presentationTimeUs, 0);
                    }
                    this.inputIndex = -1;
                    this.codecReceivedBuffers = true;
                    this.codecReconfigurationState = 0;
                    this.decoderCounters.inputBufferCount++;
                    return true;
                } catch (MediaCodec.CryptoException e2) {
                    throw ExoPlaybackException.createForRenderer(e2, getIndex());
                }
            } else {
                this.buffer.clear();
                if (this.codecReconfigurationState == 2) {
                    this.codecReconfigurationState = 1;
                }
                return true;
            }
        }
    }

    private static MediaCodec.CryptoInfo getFrameworkCryptoInfo(DecoderInputBuffer buffer2, int adaptiveReconfigurationBytes) {
        MediaCodec.CryptoInfo cryptoInfo = buffer2.cryptoInfo.getFrameworkCryptoInfoV16();
        if (adaptiveReconfigurationBytes != 0) {
            if (cryptoInfo.numBytesOfClearData == null) {
                cryptoInfo.numBytesOfClearData = new int[1];
            }
            int[] iArr = cryptoInfo.numBytesOfClearData;
            iArr[0] = iArr[0] + adaptiveReconfigurationBytes;
        }
        return cryptoInfo;
    }

    private boolean shouldWaitForKeys(boolean bufferEncrypted) throws ExoPlaybackException {
        if (this.drmSession == null || (!bufferEncrypted && this.playClearSamplesWithoutKeys)) {
            return false;
        }
        int drmSessionState = this.drmSession.getState();
        if (drmSessionState == 1) {
            throw ExoPlaybackException.createForRenderer(this.drmSession.getError(), getIndex());
        } else if (drmSessionState == 4) {
            return false;
        } else {
            return true;
        }
    }

    /* access modifiers changed from: protected */
    public void onCodecInitialized(String name, long initializedTimestampMs, long initializationDurationMs) {
    }

    /* access modifiers changed from: protected */
    public void onInputFormatChanged(Format newFormat) throws ExoPlaybackException {
        boolean z;
        Format oldFormat = this.format;
        this.format = newFormat;
        if (!Util.areEqual(this.format.drmInitData, oldFormat == null ? null : oldFormat.drmInitData)) {
            if (this.format.drmInitData == null) {
                this.pendingDrmSession = null;
            } else if (this.drmSessionManager == null) {
                throw ExoPlaybackException.createForRenderer(new IllegalStateException("Media requires a DrmSessionManager"), getIndex());
            } else {
                this.pendingDrmSession = this.drmSessionManager.acquireSession(Looper.myLooper(), this.format.drmInitData);
                if (this.pendingDrmSession == this.drmSession) {
                    this.drmSessionManager.releaseSession(this.pendingDrmSession);
                }
            }
        }
        if (this.pendingDrmSession == this.drmSession && this.codec != null && canReconfigureCodec(this.codec, this.codecInfo.adaptive, oldFormat, this.format)) {
            this.codecReconfigured = true;
            this.codecReconfigurationState = 1;
            if (this.codecAdaptationWorkaroundMode == 2 || (this.codecAdaptationWorkaroundMode == 1 && this.format.width == oldFormat.width && this.format.height == oldFormat.height)) {
                z = true;
            } else {
                z = false;
            }
            this.codecNeedsAdaptationWorkaroundBuffer = z;
        } else if (this.codecReceivedBuffers) {
            this.codecReinitializationState = 1;
        } else {
            releaseCodec();
            maybeInitCodec();
        }
    }

    /* access modifiers changed from: protected */
    public void onOutputFormatChanged(MediaCodec codec2, MediaFormat outputFormat) throws ExoPlaybackException {
    }

    /* access modifiers changed from: protected */
    public void onQueueInputBuffer(DecoderInputBuffer buffer2) {
    }

    /* access modifiers changed from: protected */
    public void onProcessedOutputBuffer(long presentationTimeUs) {
    }

    /* access modifiers changed from: protected */
    public boolean canReconfigureCodec(MediaCodec codec2, boolean codecIsAdaptive, Format oldFormat, Format newFormat) {
        return false;
    }

    public boolean isEnded() {
        return this.outputStreamEnded;
    }

    public boolean isReady() {
        return this.format != null && !this.waitingForKeys && (isSourceReady() || this.outputIndex >= 0 || (this.codecHotswapDeadlineMs != C.TIME_UNSET && SystemClock.elapsedRealtime() < this.codecHotswapDeadlineMs));
    }

    /* access modifiers changed from: protected */
    public long getDequeueOutputBufferTimeoutUs() {
        return 0;
    }

    private boolean drainOutputBuffer(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        boolean processedOutputBuffer;
        if (this.outputIndex < 0) {
            if (!this.codecNeedsEosOutputExceptionWorkaround || !this.codecReceivedEos) {
                this.outputIndex = this.codec.dequeueOutputBuffer(this.outputBufferInfo, getDequeueOutputBufferTimeoutUs());
            } else {
                try {
                    this.outputIndex = this.codec.dequeueOutputBuffer(this.outputBufferInfo, getDequeueOutputBufferTimeoutUs());
                } catch (IllegalStateException e) {
                    processEndOfStream();
                    if (this.outputStreamEnded) {
                        releaseCodec();
                    }
                    return false;
                }
            }
            if (this.outputIndex >= 0) {
                if (this.shouldSkipAdaptationWorkaroundOutputBuffer) {
                    this.shouldSkipAdaptationWorkaroundOutputBuffer = false;
                    this.codec.releaseOutputBuffer(this.outputIndex, false);
                    this.outputIndex = -1;
                    return true;
                } else if ((this.outputBufferInfo.flags & 4) != 0) {
                    processEndOfStream();
                    this.outputIndex = -1;
                    return false;
                } else {
                    ByteBuffer outputBuffer = this.outputBuffers[this.outputIndex];
                    if (outputBuffer != null) {
                        outputBuffer.position(this.outputBufferInfo.offset);
                        outputBuffer.limit(this.outputBufferInfo.offset + this.outputBufferInfo.size);
                    }
                    this.shouldSkipOutputBuffer = shouldSkipOutputBuffer(this.outputBufferInfo.presentationTimeUs);
                }
            } else if (this.outputIndex == -2) {
                processOutputFormat();
                return true;
            } else if (this.outputIndex == -3) {
                processOutputBuffersChanged();
                return true;
            } else {
                if (this.codecNeedsEosPropagationWorkaround && (this.inputStreamEnded || this.codecReinitializationState == 2)) {
                    processEndOfStream();
                }
                return false;
            }
        }
        if (!this.codecNeedsEosOutputExceptionWorkaround || !this.codecReceivedEos) {
            processedOutputBuffer = processOutputBuffer(positionUs, elapsedRealtimeUs, this.codec, this.outputBuffers[this.outputIndex], this.outputIndex, this.outputBufferInfo.flags, this.outputBufferInfo.presentationTimeUs, this.shouldSkipOutputBuffer);
        } else {
            try {
                processedOutputBuffer = processOutputBuffer(positionUs, elapsedRealtimeUs, this.codec, this.outputBuffers[this.outputIndex], this.outputIndex, this.outputBufferInfo.flags, this.outputBufferInfo.presentationTimeUs, this.shouldSkipOutputBuffer);
            } catch (IllegalStateException e2) {
                processEndOfStream();
                if (this.outputStreamEnded) {
                    releaseCodec();
                }
                return false;
            }
        }
        if (!processedOutputBuffer) {
            return false;
        }
        onProcessedOutputBuffer(this.outputBufferInfo.presentationTimeUs);
        this.outputIndex = -1;
        return true;
    }

    private void processOutputFormat() throws ExoPlaybackException {
        MediaFormat format2 = this.codec.getOutputFormat();
        if (this.codecAdaptationWorkaroundMode != 0 && format2.getInteger("width") == 32 && format2.getInteger("height") == 32) {
            this.shouldSkipAdaptationWorkaroundOutputBuffer = true;
            return;
        }
        if (this.codecNeedsMonoChannelCountWorkaround) {
            format2.setInteger("channel-count", 1);
        }
        onOutputFormatChanged(this.codec, format2);
    }

    private void processOutputBuffersChanged() {
        this.outputBuffers = this.codec.getOutputBuffers();
    }

    /* access modifiers changed from: protected */
    public void renderToEndOfStream() throws ExoPlaybackException {
    }

    private void processEndOfStream() throws ExoPlaybackException {
        if (this.codecReinitializationState == 2) {
            releaseCodec();
            maybeInitCodec();
            return;
        }
        this.outputStreamEnded = true;
        renderToEndOfStream();
    }

    private boolean shouldSkipOutputBuffer(long presentationTimeUs) {
        int size = this.decodeOnlyPresentationTimestamps.size();
        for (int i = 0; i < size; i++) {
            if (this.decodeOnlyPresentationTimestamps.get(i).longValue() == presentationTimeUs) {
                this.decodeOnlyPresentationTimestamps.remove(i);
                return true;
            }
        }
        return false;
    }

    private static boolean codecNeedsFlushWorkaround(String name) {
        return Util.SDK_INT < 18 || (Util.SDK_INT == 18 && ("OMX.SEC.avc.dec".equals(name) || "OMX.SEC.avc.dec.secure".equals(name))) || (Util.SDK_INT == 19 && Util.MODEL.startsWith("SM-G800") && ("OMX.Exynos.avc.dec".equals(name) || "OMX.Exynos.avc.dec.secure".equals(name)));
    }

    private int codecAdaptationWorkaroundMode(String name) {
        if (Util.SDK_INT <= 25 && "OMX.Exynos.avc.dec.secure".equals(name) && (Util.MODEL.startsWith("SM-T585") || Util.MODEL.startsWith("SM-A510") || Util.MODEL.startsWith("SM-A520") || Util.MODEL.startsWith("SM-J700"))) {
            return 2;
        }
        if (Util.SDK_INT >= 24 || ((!"OMX.Nvidia.h264.decode".equals(name) && !"OMX.Nvidia.h264.decode.secure".equals(name)) || (!"flounder".equals(Util.DEVICE) && !"flounder_lte".equals(Util.DEVICE) && !"grouper".equals(Util.DEVICE) && !"tilapia".equals(Util.DEVICE)))) {
            return 0;
        }
        return 1;
    }

    private static boolean codecNeedsDiscardToSpsWorkaround(String name, Format format2) {
        return Util.SDK_INT < 21 && format2.initializationData.isEmpty() && "OMX.MTK.VIDEO.DECODER.AVC".equals(name);
    }

    private static boolean codecNeedsEosPropagationWorkaround(String name) {
        return Util.SDK_INT <= 17 && ("OMX.rk.video_decoder.avc".equals(name) || "OMX.allwinner.video.decoder.avc".equals(name));
    }

    private static boolean codecNeedsEosFlushWorkaround(String name) {
        return (Util.SDK_INT <= 23 && "OMX.google.vorbis.decoder".equals(name)) || (Util.SDK_INT <= 19 && "hb2000".equals(Util.DEVICE) && ("OMX.amlogic.avc.decoder.awesome".equals(name) || "OMX.amlogic.avc.decoder.awesome.secure".equals(name)));
    }

    private static boolean codecNeedsEosOutputExceptionWorkaround(String name) {
        return Util.SDK_INT == 21 && "OMX.google.aac.decoder".equals(name);
    }

    private static boolean codecNeedsMonoChannelCountWorkaround(String name, Format format2) {
        if (Util.SDK_INT > 18 || format2.channelCount != 1 || !"OMX.MTK.AUDIO.DECODER.MP3".equals(name)) {
            return false;
        }
        return true;
    }
}
