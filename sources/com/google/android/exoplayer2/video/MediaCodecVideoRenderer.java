package com.google.android.exoplayer2.video;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import java.nio.ByteBuffer;

@TargetApi(16)
public class MediaCodecVideoRenderer extends MediaCodecRenderer {
    private static final String KEY_CROP_BOTTOM = "crop-bottom";
    private static final String KEY_CROP_LEFT = "crop-left";
    private static final String KEY_CROP_RIGHT = "crop-right";
    private static final String KEY_CROP_TOP = "crop-top";
    private static final int MAX_PENDING_OUTPUT_STREAM_OFFSET_COUNT = 10;
    private static final int[] STANDARD_LONG_EDGE_VIDEO_PX = {1920, 1600, 1440, 1280, 960, 854, 640, 540, 480};
    private static final String TAG = "MediaCodecVideoRenderer";
    private final long allowedJoiningTimeMs;
    private int buffersInCodecCount;
    private CodecMaxValues codecMaxValues;
    private boolean codecNeedsSetOutputSurfaceWorkaround;
    private int consecutiveDroppedFrameCount;
    private final Context context;
    private int currentHeight;
    private float currentPixelWidthHeightRatio;
    private int currentUnappliedRotationDegrees;
    private int currentWidth;
    private final boolean deviceNeedsAutoFrcWorkaround;
    private long droppedFrameAccumulationStartTimeMs;
    private int droppedFrames;
    private Surface dummySurface;
    private final VideoRendererEventListener.EventDispatcher eventDispatcher;
    private boolean forceRenderFrame;
    private final VideoFrameReleaseTimeHelper frameReleaseTimeHelper;
    private long joiningDeadlineMs;
    private final int maxDroppedFramesToNotify;
    private long outputStreamOffsetUs;
    private int pendingOutputStreamOffsetCount;
    private final long[] pendingOutputStreamOffsetsUs;
    private float pendingPixelWidthHeightRatio;
    private int pendingRotationDegrees;
    private boolean renderedFirstFrame;
    private int reportedHeight;
    private float reportedPixelWidthHeightRatio;
    private int reportedUnappliedRotationDegrees;
    private int reportedWidth;
    private int scalingMode;
    private Format[] streamFormats;
    private Surface surface;
    private boolean tunneling;
    private int tunnelingAudioSessionId;
    OnFrameRenderedListenerV23 tunnelingOnFrameRenderedListener;

    public MediaCodecVideoRenderer(Context context2, MediaCodecSelector mediaCodecSelector) {
        this(context2, mediaCodecSelector, 0);
    }

    public MediaCodecVideoRenderer(Context context2, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs2) {
        this(context2, mediaCodecSelector, allowedJoiningTimeMs2, (Handler) null, (VideoRendererEventListener) null, -1);
    }

    public MediaCodecVideoRenderer(Context context2, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs2, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFrameCountToNotify) {
        this(context2, mediaCodecSelector, allowedJoiningTimeMs2, (DrmSessionManager<FrameworkMediaCrypto>) null, false, eventHandler, eventListener, maxDroppedFrameCountToNotify);
    }

    public MediaCodecVideoRenderer(Context context2, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify2) {
        super(2, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys);
        this.allowedJoiningTimeMs = allowedJoiningTimeMs2;
        this.maxDroppedFramesToNotify = maxDroppedFramesToNotify2;
        this.context = context2.getApplicationContext();
        this.frameReleaseTimeHelper = new VideoFrameReleaseTimeHelper(context2);
        this.eventDispatcher = new VideoRendererEventListener.EventDispatcher(eventHandler, eventListener);
        this.deviceNeedsAutoFrcWorkaround = deviceNeedsAutoFrcWorkaround();
        this.pendingOutputStreamOffsetsUs = new long[10];
        this.outputStreamOffsetUs = C.TIME_UNSET;
        this.joiningDeadlineMs = C.TIME_UNSET;
        this.currentWidth = -1;
        this.currentHeight = -1;
        this.currentPixelWidthHeightRatio = -1.0f;
        this.pendingPixelWidthHeightRatio = -1.0f;
        this.scalingMode = 1;
        clearReportedVideoSize();
    }

    /* access modifiers changed from: protected */
    public int supportsFormat(MediaCodecSelector mediaCodecSelector, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, Format format) throws MediaCodecUtil.DecoderQueryException {
        String mimeType = format.sampleMimeType;
        if (!MimeTypes.isVideo(mimeType)) {
            return 0;
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
        if (!supportsFormatDrm(drmSessionManager, drmInitData)) {
            return 2;
        }
        boolean decoderCapable = decoderInfo.isCodecSupported(format.codecs);
        if (decoderCapable && format.width > 0 && format.height > 0) {
            if (Util.SDK_INT >= 21) {
                decoderCapable = decoderInfo.isVideoSizeAndRateSupportedV21(format.width, format.height, (double) format.frameRate);
            } else {
                decoderCapable = format.width * format.height <= MediaCodecUtil.maxH264DecodableFrameSize();
                if (!decoderCapable) {
                    Log.d(TAG, "FalseCheck [legacyFrameSize, " + format.width + "x" + format.height + "] [" + Util.DEVICE_DEBUG_INFO + "]");
                }
            }
        }
        return (decoderInfo.adaptive ? 16 : 8) | (decoderInfo.tunneling ? 32 : 0) | (decoderCapable ? 4 : 3);
    }

    /* access modifiers changed from: protected */
    public void onEnabled(boolean joining) throws ExoPlaybackException {
        super.onEnabled(joining);
        this.tunnelingAudioSessionId = getConfiguration().tunnelingAudioSessionId;
        this.tunneling = this.tunnelingAudioSessionId != 0;
        this.eventDispatcher.enabled(this.decoderCounters);
        this.frameReleaseTimeHelper.enable();
    }

    /* access modifiers changed from: protected */
    public void onStreamChanged(Format[] formats, long offsetUs) throws ExoPlaybackException {
        this.streamFormats = formats;
        if (this.outputStreamOffsetUs == C.TIME_UNSET) {
            this.outputStreamOffsetUs = offsetUs;
        } else {
            if (this.pendingOutputStreamOffsetCount == this.pendingOutputStreamOffsetsUs.length) {
                Log.w(TAG, "Too many stream changes, so dropping offset: " + this.pendingOutputStreamOffsetsUs[this.pendingOutputStreamOffsetCount - 1]);
            } else {
                this.pendingOutputStreamOffsetCount++;
            }
            this.pendingOutputStreamOffsetsUs[this.pendingOutputStreamOffsetCount - 1] = offsetUs;
        }
        super.onStreamChanged(formats, offsetUs);
    }

    /* access modifiers changed from: protected */
    public void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        super.onPositionReset(positionUs, joining);
        clearRenderedFirstFrame();
        this.consecutiveDroppedFrameCount = 0;
        if (this.pendingOutputStreamOffsetCount != 0) {
            this.outputStreamOffsetUs = this.pendingOutputStreamOffsetsUs[this.pendingOutputStreamOffsetCount - 1];
            this.pendingOutputStreamOffsetCount = 0;
        }
        if (joining) {
            setJoiningDeadlineMs();
        } else {
            this.joiningDeadlineMs = C.TIME_UNSET;
        }
    }

    public boolean isReady() {
        if (super.isReady() && (this.renderedFirstFrame || ((this.dummySurface != null && this.surface == this.dummySurface) || getCodec() == null || this.tunneling))) {
            this.joiningDeadlineMs = C.TIME_UNSET;
            return true;
        } else if (this.joiningDeadlineMs == C.TIME_UNSET) {
            return false;
        } else {
            if (SystemClock.elapsedRealtime() < this.joiningDeadlineMs) {
                return true;
            }
            this.joiningDeadlineMs = C.TIME_UNSET;
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public void onStarted() {
        super.onStarted();
        this.droppedFrames = 0;
        this.droppedFrameAccumulationStartTimeMs = SystemClock.elapsedRealtime();
    }

    /* access modifiers changed from: protected */
    public void onStopped() {
        this.joiningDeadlineMs = C.TIME_UNSET;
        maybeNotifyDroppedFrames();
        super.onStopped();
    }

    /* access modifiers changed from: protected */
    public void onDisabled() {
        this.currentWidth = -1;
        this.currentHeight = -1;
        this.currentPixelWidthHeightRatio = -1.0f;
        this.pendingPixelWidthHeightRatio = -1.0f;
        this.outputStreamOffsetUs = C.TIME_UNSET;
        this.pendingOutputStreamOffsetCount = 0;
        clearReportedVideoSize();
        clearRenderedFirstFrame();
        this.frameReleaseTimeHelper.disable();
        this.tunnelingOnFrameRenderedListener = null;
        this.tunneling = false;
        try {
            super.onDisabled();
        } finally {
            this.decoderCounters.ensureUpdated();
            this.eventDispatcher.disabled(this.decoderCounters);
        }
    }

    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        if (messageType == 1) {
            setSurface((Surface) message);
        } else if (messageType == 4) {
            this.scalingMode = ((Integer) message).intValue();
            MediaCodec codec = getCodec();
            if (codec != null) {
                setVideoScalingMode(codec, this.scalingMode);
            }
        } else {
            super.handleMessage(messageType, message);
        }
    }

    private void setSurface(Surface surface2) throws ExoPlaybackException {
        if (surface2 == null) {
            if (this.dummySurface != null) {
                surface2 = this.dummySurface;
            } else {
                MediaCodecInfo codecInfo = getCodecInfo();
                if (codecInfo != null && shouldUseDummySurface(codecInfo.secure)) {
                    this.dummySurface = DummySurface.newInstanceV17(this.context, codecInfo.secure);
                    surface2 = this.dummySurface;
                }
            }
        }
        if (this.surface != surface2) {
            this.surface = surface2;
            int state = getState();
            if (state == 1 || state == 2) {
                MediaCodec codec = getCodec();
                if (Util.SDK_INT < 23 || codec == null || surface2 == null || this.codecNeedsSetOutputSurfaceWorkaround) {
                    releaseCodec();
                    maybeInitCodec();
                } else {
                    setOutputSurfaceV23(codec, surface2);
                }
            }
            if (surface2 == null || surface2 == this.dummySurface) {
                clearReportedVideoSize();
                clearRenderedFirstFrame();
                return;
            }
            maybeRenotifyVideoSizeChanged();
            clearRenderedFirstFrame();
            if (state == 2) {
                setJoiningDeadlineMs();
            }
        } else if (surface2 != null && surface2 != this.dummySurface) {
            maybeRenotifyVideoSizeChanged();
            maybeRenotifyRenderedFirstFrame();
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldInitCodec(MediaCodecInfo codecInfo) {
        return this.surface != null || shouldUseDummySurface(codecInfo.secure);
    }

    /* access modifiers changed from: protected */
    public void configureCodec(MediaCodecInfo codecInfo, MediaCodec codec, Format format, MediaCrypto crypto) throws MediaCodecUtil.DecoderQueryException {
        this.codecMaxValues = getCodecMaxValues(codecInfo, format, this.streamFormats);
        MediaFormat mediaFormat = getMediaFormat(format, this.codecMaxValues, this.deviceNeedsAutoFrcWorkaround, this.tunnelingAudioSessionId);
        if (this.surface == null) {
            Assertions.checkState(shouldUseDummySurface(codecInfo.secure));
            if (this.dummySurface == null) {
                this.dummySurface = DummySurface.newInstanceV17(this.context, codecInfo.secure);
            }
            this.surface = this.dummySurface;
        }
        codec.configure(mediaFormat, this.surface, crypto, 0);
        if (Util.SDK_INT >= 23 && this.tunneling) {
            this.tunnelingOnFrameRenderedListener = new OnFrameRenderedListenerV23(codec);
        }
    }

    /* access modifiers changed from: protected */
    @CallSuper
    public void releaseCodec() {
        try {
            super.releaseCodec();
        } finally {
            this.buffersInCodecCount = 0;
            this.forceRenderFrame = false;
            if (this.dummySurface != null) {
                if (this.surface == this.dummySurface) {
                    this.surface = null;
                }
                this.dummySurface.release();
                this.dummySurface = null;
            }
        }
    }

    /* access modifiers changed from: protected */
    @CallSuper
    public void flushCodec() throws ExoPlaybackException {
        super.flushCodec();
        this.buffersInCodecCount = 0;
        this.forceRenderFrame = false;
    }

    /* access modifiers changed from: protected */
    public void onCodecInitialized(String name, long initializedTimestampMs, long initializationDurationMs) {
        this.eventDispatcher.decoderInitialized(name, initializedTimestampMs, initializationDurationMs);
        this.codecNeedsSetOutputSurfaceWorkaround = codecNeedsSetOutputSurfaceWorkaround(name);
    }

    /* access modifiers changed from: protected */
    public void onInputFormatChanged(Format newFormat) throws ExoPlaybackException {
        super.onInputFormatChanged(newFormat);
        this.eventDispatcher.inputFormatChanged(newFormat);
        this.pendingPixelWidthHeightRatio = getPixelWidthHeightRatio(newFormat);
        this.pendingRotationDegrees = getRotationDegrees(newFormat);
    }

    /* access modifiers changed from: protected */
    @CallSuper
    public void onQueueInputBuffer(DecoderInputBuffer buffer) {
        this.buffersInCodecCount++;
        if (Util.SDK_INT < 23 && this.tunneling) {
            maybeNotifyRenderedFirstFrame();
        }
    }

    /* access modifiers changed from: protected */
    public void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) {
        int integer;
        int integer2;
        boolean hasCrop = outputFormat.containsKey(KEY_CROP_RIGHT) && outputFormat.containsKey(KEY_CROP_LEFT) && outputFormat.containsKey(KEY_CROP_BOTTOM) && outputFormat.containsKey(KEY_CROP_TOP);
        if (hasCrop) {
            integer = (outputFormat.getInteger(KEY_CROP_RIGHT) - outputFormat.getInteger(KEY_CROP_LEFT)) + 1;
        } else {
            integer = outputFormat.getInteger("width");
        }
        this.currentWidth = integer;
        if (hasCrop) {
            integer2 = (outputFormat.getInteger(KEY_CROP_BOTTOM) - outputFormat.getInteger(KEY_CROP_TOP)) + 1;
        } else {
            integer2 = outputFormat.getInteger("height");
        }
        this.currentHeight = integer2;
        this.currentPixelWidthHeightRatio = this.pendingPixelWidthHeightRatio;
        if (Util.SDK_INT < 21) {
            this.currentUnappliedRotationDegrees = this.pendingRotationDegrees;
        } else if (this.pendingRotationDegrees == 90 || this.pendingRotationDegrees == 270) {
            int rotatedHeight = this.currentWidth;
            this.currentWidth = this.currentHeight;
            this.currentHeight = rotatedHeight;
            this.currentPixelWidthHeightRatio = 1.0f / this.currentPixelWidthHeightRatio;
        }
        setVideoScalingMode(codec, this.scalingMode);
    }

    /* access modifiers changed from: protected */
    public boolean canReconfigureCodec(MediaCodec codec, boolean codecIsAdaptive, Format oldFormat, Format newFormat) {
        return areAdaptationCompatible(codecIsAdaptive, oldFormat, newFormat) && newFormat.width <= this.codecMaxValues.width && newFormat.height <= this.codecMaxValues.height && getMaxInputSize(newFormat) <= this.codecMaxValues.inputSize;
    }

    /* access modifiers changed from: protected */
    public boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs, boolean shouldSkip) throws ExoPlaybackException {
        while (this.pendingOutputStreamOffsetCount != 0 && bufferPresentationTimeUs >= this.pendingOutputStreamOffsetsUs[0]) {
            this.outputStreamOffsetUs = this.pendingOutputStreamOffsetsUs[0];
            this.pendingOutputStreamOffsetCount--;
            System.arraycopy(this.pendingOutputStreamOffsetsUs, 1, this.pendingOutputStreamOffsetsUs, 0, this.pendingOutputStreamOffsetCount);
        }
        long presentationTimeUs = bufferPresentationTimeUs - this.outputStreamOffsetUs;
        if (shouldSkip) {
            skipOutputBuffer(codec, bufferIndex, presentationTimeUs);
            return true;
        }
        long earlyUs = bufferPresentationTimeUs - positionUs;
        if (this.surface == this.dummySurface) {
            if (!isBufferLate(earlyUs)) {
                return false;
            }
            this.forceRenderFrame = false;
            skipOutputBuffer(codec, bufferIndex, presentationTimeUs);
            return true;
        } else if (!this.renderedFirstFrame || this.forceRenderFrame) {
            this.forceRenderFrame = false;
            if (Util.SDK_INT >= 21) {
                renderOutputBufferV21(codec, bufferIndex, presentationTimeUs, System.nanoTime());
            } else {
                renderOutputBuffer(codec, bufferIndex, presentationTimeUs);
            }
            return true;
        } else if (getState() != 2) {
            return false;
        } else {
            long systemTimeNs = System.nanoTime();
            long adjustedReleaseTimeNs = this.frameReleaseTimeHelper.adjustReleaseTime(bufferPresentationTimeUs, systemTimeNs + (1000 * (earlyUs - ((SystemClock.elapsedRealtime() * 1000) - elapsedRealtimeUs))));
            long earlyUs2 = (adjustedReleaseTimeNs - systemTimeNs) / 1000;
            if (shouldDropBuffersToKeyframe(earlyUs2, elapsedRealtimeUs) && maybeDropBuffersToKeyframe(codec, bufferIndex, presentationTimeUs, positionUs)) {
                this.forceRenderFrame = true;
                return false;
            } else if (shouldDropOutputBuffer(earlyUs2, elapsedRealtimeUs)) {
                dropOutputBuffer(codec, bufferIndex, presentationTimeUs);
                return true;
            } else {
                if (Util.SDK_INT >= 21) {
                    if (earlyUs2 < 50000) {
                        renderOutputBufferV21(codec, bufferIndex, presentationTimeUs, adjustedReleaseTimeNs);
                        return true;
                    }
                } else if (earlyUs2 < 30000) {
                    if (earlyUs2 > 11000) {
                        try {
                            Thread.sleep((earlyUs2 - 10000) / 1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    renderOutputBuffer(codec, bufferIndex, presentationTimeUs);
                    return true;
                }
                return false;
            }
        }
    }

    /* access modifiers changed from: protected */
    @CallSuper
    public void onProcessedOutputBuffer(long presentationTimeUs) {
        this.buffersInCodecCount--;
    }

    /* access modifiers changed from: protected */
    public boolean shouldDropOutputBuffer(long earlyUs, long elapsedRealtimeUs) {
        return isBufferLate(earlyUs);
    }

    /* access modifiers changed from: protected */
    public boolean shouldDropBuffersToKeyframe(long earlyUs, long elapsedRealtimeUs) {
        return isBufferVeryLate(earlyUs);
    }

    /* access modifiers changed from: protected */
    public void skipOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("skipVideoBuffer");
        codec.releaseOutputBuffer(index, false);
        TraceUtil.endSection();
        this.decoderCounters.skippedOutputBufferCount++;
    }

    /* access modifiers changed from: protected */
    public void dropOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("dropVideoBuffer");
        codec.releaseOutputBuffer(index, false);
        TraceUtil.endSection();
        updateDroppedBufferCounters(1);
    }

    /* access modifiers changed from: protected */
    public boolean maybeDropBuffersToKeyframe(MediaCodec codec, int index, long presentationTimeUs, long positionUs) throws ExoPlaybackException {
        int droppedSourceBufferCount = skipSource(positionUs);
        if (droppedSourceBufferCount == 0) {
            return false;
        }
        this.decoderCounters.droppedToKeyframeCount++;
        updateDroppedBufferCounters(this.buffersInCodecCount + droppedSourceBufferCount);
        flushCodec();
        return true;
    }

    /* access modifiers changed from: protected */
    public void updateDroppedBufferCounters(int droppedBufferCount) {
        this.decoderCounters.droppedBufferCount += droppedBufferCount;
        this.droppedFrames += droppedBufferCount;
        this.consecutiveDroppedFrameCount += droppedBufferCount;
        this.decoderCounters.maxConsecutiveDroppedBufferCount = Math.max(this.consecutiveDroppedFrameCount, this.decoderCounters.maxConsecutiveDroppedBufferCount);
        if (this.droppedFrames >= this.maxDroppedFramesToNotify) {
            maybeNotifyDroppedFrames();
        }
    }

    /* access modifiers changed from: protected */
    public void renderOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        maybeNotifyVideoSizeChanged();
        TraceUtil.beginSection("releaseOutputBuffer");
        codec.releaseOutputBuffer(index, true);
        TraceUtil.endSection();
        this.decoderCounters.renderedOutputBufferCount++;
        this.consecutiveDroppedFrameCount = 0;
        maybeNotifyRenderedFirstFrame();
    }

    /* access modifiers changed from: protected */
    @TargetApi(21)
    public void renderOutputBufferV21(MediaCodec codec, int index, long presentationTimeUs, long releaseTimeNs) {
        maybeNotifyVideoSizeChanged();
        TraceUtil.beginSection("releaseOutputBuffer");
        codec.releaseOutputBuffer(index, releaseTimeNs);
        TraceUtil.endSection();
        this.decoderCounters.renderedOutputBufferCount++;
        this.consecutiveDroppedFrameCount = 0;
        maybeNotifyRenderedFirstFrame();
    }

    private boolean shouldUseDummySurface(boolean codecIsSecure) {
        return Util.SDK_INT >= 23 && !this.tunneling && (!codecIsSecure || DummySurface.isSecureSupported(this.context));
    }

    private void setJoiningDeadlineMs() {
        this.joiningDeadlineMs = this.allowedJoiningTimeMs > 0 ? SystemClock.elapsedRealtime() + this.allowedJoiningTimeMs : C.TIME_UNSET;
    }

    private void clearRenderedFirstFrame() {
        MediaCodec codec;
        this.renderedFirstFrame = false;
        if (Util.SDK_INT >= 23 && this.tunneling && (codec = getCodec()) != null) {
            this.tunnelingOnFrameRenderedListener = new OnFrameRenderedListenerV23(codec);
        }
    }

    /* access modifiers changed from: package-private */
    public void maybeNotifyRenderedFirstFrame() {
        if (!this.renderedFirstFrame) {
            this.renderedFirstFrame = true;
            this.eventDispatcher.renderedFirstFrame(this.surface);
        }
    }

    private void maybeRenotifyRenderedFirstFrame() {
        if (this.renderedFirstFrame) {
            this.eventDispatcher.renderedFirstFrame(this.surface);
        }
    }

    private void clearReportedVideoSize() {
        this.reportedWidth = -1;
        this.reportedHeight = -1;
        this.reportedPixelWidthHeightRatio = -1.0f;
        this.reportedUnappliedRotationDegrees = -1;
    }

    private void maybeNotifyVideoSizeChanged() {
        if (this.currentWidth != -1 || this.currentHeight != -1) {
            if (this.reportedWidth != this.currentWidth || this.reportedHeight != this.currentHeight || this.reportedUnappliedRotationDegrees != this.currentUnappliedRotationDegrees || this.reportedPixelWidthHeightRatio != this.currentPixelWidthHeightRatio) {
                this.eventDispatcher.videoSizeChanged(this.currentWidth, this.currentHeight, this.currentUnappliedRotationDegrees, this.currentPixelWidthHeightRatio);
                this.reportedWidth = this.currentWidth;
                this.reportedHeight = this.currentHeight;
                this.reportedUnappliedRotationDegrees = this.currentUnappliedRotationDegrees;
                this.reportedPixelWidthHeightRatio = this.currentPixelWidthHeightRatio;
            }
        }
    }

    private void maybeRenotifyVideoSizeChanged() {
        if (this.reportedWidth != -1 || this.reportedHeight != -1) {
            this.eventDispatcher.videoSizeChanged(this.reportedWidth, this.reportedHeight, this.reportedUnappliedRotationDegrees, this.reportedPixelWidthHeightRatio);
        }
    }

    private void maybeNotifyDroppedFrames() {
        if (this.droppedFrames > 0) {
            long now = SystemClock.elapsedRealtime();
            this.eventDispatcher.droppedFrames(this.droppedFrames, now - this.droppedFrameAccumulationStartTimeMs);
            this.droppedFrames = 0;
            this.droppedFrameAccumulationStartTimeMs = now;
        }
    }

    private static boolean isBufferLate(long earlyUs) {
        return earlyUs < -30000;
    }

    private static boolean isBufferVeryLate(long earlyUs) {
        return earlyUs < -500000;
    }

    @TargetApi(23)
    private static void setOutputSurfaceV23(MediaCodec codec, Surface surface2) {
        codec.setOutputSurface(surface2);
    }

    @TargetApi(21)
    private static void configureTunnelingV21(MediaFormat mediaFormat, int tunnelingAudioSessionId2) {
        mediaFormat.setFeatureEnabled("tunneled-playback", true);
        mediaFormat.setInteger("audio-session-id", tunnelingAudioSessionId2);
    }

    /* access modifiers changed from: protected */
    public CodecMaxValues getCodecMaxValues(MediaCodecInfo codecInfo, Format format, Format[] streamFormats2) throws MediaCodecUtil.DecoderQueryException {
        boolean z;
        int maxWidth = format.width;
        int maxHeight = format.height;
        int maxInputSize = getMaxInputSize(format);
        if (streamFormats2.length == 1) {
            return new CodecMaxValues(maxWidth, maxHeight, maxInputSize);
        }
        boolean haveUnknownDimensions = false;
        for (Format streamFormat : streamFormats2) {
            if (areAdaptationCompatible(codecInfo.adaptive, format, streamFormat)) {
                if (streamFormat.width == -1 || streamFormat.height == -1) {
                    z = true;
                } else {
                    z = false;
                }
                haveUnknownDimensions |= z;
                maxWidth = Math.max(maxWidth, streamFormat.width);
                maxHeight = Math.max(maxHeight, streamFormat.height);
                maxInputSize = Math.max(maxInputSize, getMaxInputSize(streamFormat));
            }
        }
        if (haveUnknownDimensions) {
            Log.w(TAG, "Resolutions unknown. Codec max resolution: " + maxWidth + "x" + maxHeight);
            Point codecMaxSize = getCodecMaxSize(codecInfo, format);
            if (codecMaxSize != null) {
                maxWidth = Math.max(maxWidth, codecMaxSize.x);
                maxHeight = Math.max(maxHeight, codecMaxSize.y);
                maxInputSize = Math.max(maxInputSize, getMaxInputSize(format.sampleMimeType, maxWidth, maxHeight));
                Log.w(TAG, "Codec max resolution adjusted to: " + maxWidth + "x" + maxHeight);
            }
        }
        return new CodecMaxValues(maxWidth, maxHeight, maxInputSize);
    }

    /* access modifiers changed from: protected */
    @SuppressLint({"InlinedApi"})
    public MediaFormat getMediaFormat(Format format, CodecMaxValues codecMaxValues2, boolean deviceNeedsAutoFrcWorkaround2, int tunnelingAudioSessionId2) {
        MediaFormat frameworkMediaFormat = format.getFrameworkMediaFormatV16();
        frameworkMediaFormat.setInteger("max-width", codecMaxValues2.width);
        frameworkMediaFormat.setInteger("max-height", codecMaxValues2.height);
        if (codecMaxValues2.inputSize != -1) {
            frameworkMediaFormat.setInteger("max-input-size", codecMaxValues2.inputSize);
        }
        if (deviceNeedsAutoFrcWorkaround2) {
            frameworkMediaFormat.setInteger("auto-frc", 0);
        }
        if (tunnelingAudioSessionId2 != 0) {
            configureTunnelingV21(frameworkMediaFormat, tunnelingAudioSessionId2);
        }
        return frameworkMediaFormat;
    }

    private static Point getCodecMaxSize(MediaCodecInfo codecInfo, Format format) throws MediaCodecUtil.DecoderQueryException {
        int i;
        boolean isVerticalVideo = format.height > format.width;
        int formatLongEdgePx = isVerticalVideo ? format.height : format.width;
        int formatShortEdgePx = isVerticalVideo ? format.width : format.height;
        float aspectRatio = ((float) formatShortEdgePx) / ((float) formatLongEdgePx);
        int[] iArr = STANDARD_LONG_EDGE_VIDEO_PX;
        int length = iArr.length;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 >= length) {
                return null;
            }
            int longEdgePx = iArr[i3];
            int shortEdgePx = (int) (((float) longEdgePx) * aspectRatio);
            if (longEdgePx <= formatLongEdgePx || shortEdgePx <= formatShortEdgePx) {
                return null;
            }
            if (Util.SDK_INT >= 21) {
                if (isVerticalVideo) {
                    i = shortEdgePx;
                } else {
                    i = longEdgePx;
                }
                Point alignedSize = codecInfo.alignVideoSizeV21(i, isVerticalVideo ? longEdgePx : shortEdgePx);
                if (codecInfo.isVideoSizeAndRateSupportedV21(alignedSize.x, alignedSize.y, (double) format.frameRate)) {
                    return alignedSize;
                }
            } else {
                int longEdgePx2 = Util.ceilDivide(longEdgePx, 16) * 16;
                int shortEdgePx2 = Util.ceilDivide(shortEdgePx, 16) * 16;
                if (longEdgePx2 * shortEdgePx2 <= MediaCodecUtil.maxH264DecodableFrameSize()) {
                    int i4 = isVerticalVideo ? shortEdgePx2 : longEdgePx2;
                    if (!isVerticalVideo) {
                        longEdgePx2 = shortEdgePx2;
                    }
                    return new Point(i4, longEdgePx2);
                }
            }
            i2 = i3 + 1;
        }
        return null;
    }

    private static int getMaxInputSize(Format format) {
        if (format.maxInputSize == -1) {
            return getMaxInputSize(format.sampleMimeType, format.width, format.height);
        }
        int totalInitializationDataSize = 0;
        int initializationDataCount = format.initializationData.size();
        for (int i = 0; i < initializationDataCount; i++) {
            totalInitializationDataSize += format.initializationData.get(i).length;
        }
        return format.maxInputSize + totalInitializationDataSize;
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int getMaxInputSize(java.lang.String r6, int r7, int r8) {
        /*
            r5 = 16
            r2 = -1
            if (r7 == r2) goto L_0x0007
            if (r8 != r2) goto L_0x0008
        L_0x0007:
            return r2
        L_0x0008:
            int r3 = r6.hashCode()
            switch(r3) {
                case -1664118616: goto L_0x001d;
                case -1662541442: goto L_0x0045;
                case 1187890754: goto L_0x0027;
                case 1331836730: goto L_0x0031;
                case 1599127256: goto L_0x003b;
                case 1599127257: goto L_0x004f;
                default: goto L_0x000f;
            }
        L_0x000f:
            r3 = r2
        L_0x0010:
            switch(r3) {
                case 0: goto L_0x0014;
                case 1: goto L_0x0014;
                case 2: goto L_0x0059;
                case 3: goto L_0x0072;
                case 4: goto L_0x0076;
                case 5: goto L_0x0076;
                default: goto L_0x0013;
            }
        L_0x0013:
            goto L_0x0007
        L_0x0014:
            int r0 = r7 * r8
            r1 = 2
        L_0x0017:
            int r2 = r0 * 3
            int r3 = r1 * 2
            int r2 = r2 / r3
            goto L_0x0007
        L_0x001d:
            java.lang.String r3 = "video/3gpp"
            boolean r3 = r6.equals(r3)
            if (r3 == 0) goto L_0x000f
            r3 = 0
            goto L_0x0010
        L_0x0027:
            java.lang.String r3 = "video/mp4v-es"
            boolean r3 = r6.equals(r3)
            if (r3 == 0) goto L_0x000f
            r3 = 1
            goto L_0x0010
        L_0x0031:
            java.lang.String r3 = "video/avc"
            boolean r3 = r6.equals(r3)
            if (r3 == 0) goto L_0x000f
            r3 = 2
            goto L_0x0010
        L_0x003b:
            java.lang.String r3 = "video/x-vnd.on2.vp8"
            boolean r3 = r6.equals(r3)
            if (r3 == 0) goto L_0x000f
            r3 = 3
            goto L_0x0010
        L_0x0045:
            java.lang.String r3 = "video/hevc"
            boolean r3 = r6.equals(r3)
            if (r3 == 0) goto L_0x000f
            r3 = 4
            goto L_0x0010
        L_0x004f:
            java.lang.String r3 = "video/x-vnd.on2.vp9"
            boolean r3 = r6.equals(r3)
            if (r3 == 0) goto L_0x000f
            r3 = 5
            goto L_0x0010
        L_0x0059:
            java.lang.String r3 = "BRAVIA 4K 2015"
            java.lang.String r4 = com.google.android.exoplayer2.util.Util.MODEL
            boolean r3 = r3.equals(r4)
            if (r3 != 0) goto L_0x0007
            int r2 = com.google.android.exoplayer2.util.Util.ceilDivide((int) r7, (int) r5)
            int r3 = com.google.android.exoplayer2.util.Util.ceilDivide((int) r8, (int) r5)
            int r2 = r2 * r3
            int r2 = r2 * 16
            int r0 = r2 * 16
            r1 = 2
            goto L_0x0017
        L_0x0072:
            int r0 = r7 * r8
            r1 = 2
            goto L_0x0017
        L_0x0076:
            int r0 = r7 * r8
            r1 = 4
            goto L_0x0017
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.video.MediaCodecVideoRenderer.getMaxInputSize(java.lang.String, int, int):int");
    }

    private static void setVideoScalingMode(MediaCodec codec, int scalingMode2) {
        codec.setVideoScalingMode(scalingMode2);
    }

    private static boolean deviceNeedsAutoFrcWorkaround() {
        return Util.SDK_INT <= 22 && "foster".equals(Util.DEVICE) && "NVIDIA".equals(Util.MANUFACTURER);
    }

    private static boolean codecNeedsSetOutputSurfaceWorkaround(String name) {
        return (("deb".equals(Util.DEVICE) || "flo".equals(Util.DEVICE)) && "OMX.qcom.video.decoder.avc".equals(name)) || (("tcl_eu".equals(Util.DEVICE) || "SVP-DTV15".equals(Util.DEVICE) || "BRAVIA_ATV2".equals(Util.DEVICE)) && "OMX.MTK.VIDEO.DECODER.AVC".equals(name));
    }

    private static boolean areAdaptationCompatible(boolean codecIsAdaptive, Format first, Format second) {
        return first.sampleMimeType.equals(second.sampleMimeType) && getRotationDegrees(first) == getRotationDegrees(second) && (codecIsAdaptive || (first.width == second.width && first.height == second.height));
    }

    private static float getPixelWidthHeightRatio(Format format) {
        if (format.pixelWidthHeightRatio == -1.0f) {
            return 1.0f;
        }
        return format.pixelWidthHeightRatio;
    }

    private static int getRotationDegrees(Format format) {
        if (format.rotationDegrees == -1) {
            return 0;
        }
        return format.rotationDegrees;
    }

    protected static final class CodecMaxValues {
        public final int height;
        public final int inputSize;
        public final int width;

        public CodecMaxValues(int width2, int height2, int inputSize2) {
            this.width = width2;
            this.height = height2;
            this.inputSize = inputSize2;
        }
    }

    @TargetApi(23)
    private final class OnFrameRenderedListenerV23 implements MediaCodec.OnFrameRenderedListener {
        private OnFrameRenderedListenerV23(MediaCodec codec) {
            codec.setOnFrameRenderedListener(this, new Handler());
        }

        public void onFrameRendered(@NonNull MediaCodec codec, long presentationTimeUs, long nanoTime) {
            if (this == MediaCodecVideoRenderer.this.tunnelingOnFrameRenderedListener) {
                MediaCodecVideoRenderer.this.maybeNotifyRenderedFirstFrame();
            }
        }
    }
}
