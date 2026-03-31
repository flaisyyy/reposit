package com.google.android.exoplayer2.mediacodec;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.media.MediaCodecInfo;
import android.util.Log;
import android.util.Pair;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

@TargetApi(16)
public final class MediaCodecInfo {
    public static final String TAG = "MediaCodecInfo";
    public final boolean adaptive;
    private final MediaCodecInfo.CodecCapabilities capabilities;
    private final String mimeType;
    public final String name;
    public final boolean secure;
    public final boolean tunneling;

    public static MediaCodecInfo newPassthroughInstance(String name2) {
        return new MediaCodecInfo(name2, (String) null, (MediaCodecInfo.CodecCapabilities) null, false, false);
    }

    public static MediaCodecInfo newInstance(String name2, String mimeType2, MediaCodecInfo.CodecCapabilities capabilities2) {
        return new MediaCodecInfo(name2, mimeType2, capabilities2, false, false);
    }

    public static MediaCodecInfo newInstance(String name2, String mimeType2, MediaCodecInfo.CodecCapabilities capabilities2, boolean forceDisableAdaptive, boolean forceSecure) {
        return new MediaCodecInfo(name2, mimeType2, capabilities2, forceDisableAdaptive, forceSecure);
    }

    private MediaCodecInfo(String name2, String mimeType2, MediaCodecInfo.CodecCapabilities capabilities2, boolean forceDisableAdaptive, boolean forceSecure) {
        boolean z;
        boolean z2;
        boolean z3 = false;
        this.name = (String) Assertions.checkNotNull(name2);
        this.mimeType = mimeType2;
        this.capabilities = capabilities2;
        if (forceDisableAdaptive || capabilities2 == null || !isAdaptive(capabilities2)) {
            z = false;
        } else {
            z = true;
        }
        this.adaptive = z;
        if (capabilities2 == null || !isTunneling(capabilities2)) {
            z2 = false;
        } else {
            z2 = true;
        }
        this.tunneling = z2;
        if (forceSecure || (capabilities2 != null && isSecure(capabilities2))) {
            z3 = true;
        }
        this.secure = z3;
    }

    public MediaCodecInfo.CodecProfileLevel[] getProfileLevels() {
        return (this.capabilities == null || this.capabilities.profileLevels == null) ? new MediaCodecInfo.CodecProfileLevel[0] : this.capabilities.profileLevels;
    }

    public boolean isCodecSupported(String codec) {
        if (codec == null || this.mimeType == null) {
            return true;
        }
        String codecMimeType = MimeTypes.getMediaMimeType(codec);
        if (codecMimeType == null) {
            return true;
        }
        if (!this.mimeType.equals(codecMimeType)) {
            logNoSupport("codec.mime " + codec + ", " + codecMimeType);
            return false;
        }
        Pair<Integer, Integer> codecProfileAndLevel = MediaCodecUtil.getCodecProfileAndLevel(codec);
        if (codecProfileAndLevel == null) {
            return true;
        }
        for (MediaCodecInfo.CodecProfileLevel capabilities2 : getProfileLevels()) {
            if (capabilities2.profile == ((Integer) codecProfileAndLevel.first).intValue() && capabilities2.level >= ((Integer) codecProfileAndLevel.second).intValue()) {
                return true;
            }
        }
        logNoSupport("codec.profileLevel, " + codec + ", " + codecMimeType);
        return false;
    }

    @TargetApi(21)
    public boolean isVideoSizeAndRateSupportedV21(int width, int height, double frameRate) {
        if (this.capabilities == null) {
            logNoSupport("sizeAndRate.caps");
            return false;
        }
        MediaCodecInfo.VideoCapabilities videoCapabilities = this.capabilities.getVideoCapabilities();
        if (videoCapabilities == null) {
            logNoSupport("sizeAndRate.vCaps");
            return false;
        }
        if (!areSizeAndRateSupportedV21(videoCapabilities, width, height, frameRate)) {
            if (width >= height || !areSizeAndRateSupportedV21(videoCapabilities, height, width, frameRate)) {
                logNoSupport("sizeAndRate.support, " + width + "x" + height + "x" + frameRate);
                return false;
            }
            logAssumedSupport("sizeAndRate.rotated, " + width + "x" + height + "x" + frameRate);
        }
        return true;
    }

    @TargetApi(21)
    public Point alignVideoSizeV21(int width, int height) {
        if (this.capabilities == null) {
            logNoSupport("align.caps");
            return null;
        }
        MediaCodecInfo.VideoCapabilities videoCapabilities = this.capabilities.getVideoCapabilities();
        if (videoCapabilities == null) {
            logNoSupport("align.vCaps");
            return null;
        }
        int widthAlignment = videoCapabilities.getWidthAlignment();
        int heightAlignment = videoCapabilities.getHeightAlignment();
        return new Point(Util.ceilDivide(width, widthAlignment) * widthAlignment, Util.ceilDivide(height, heightAlignment) * heightAlignment);
    }

    @TargetApi(21)
    public boolean isAudioSampleRateSupportedV21(int sampleRate) {
        if (this.capabilities == null) {
            logNoSupport("sampleRate.caps");
            return false;
        }
        MediaCodecInfo.AudioCapabilities audioCapabilities = this.capabilities.getAudioCapabilities();
        if (audioCapabilities == null) {
            logNoSupport("sampleRate.aCaps");
            return false;
        } else if (audioCapabilities.isSampleRateSupported(sampleRate)) {
            return true;
        } else {
            logNoSupport("sampleRate.support, " + sampleRate);
            return false;
        }
    }

    @TargetApi(21)
    public boolean isAudioChannelCountSupportedV21(int channelCount) {
        if (this.capabilities == null) {
            logNoSupport("channelCount.caps");
            return false;
        }
        MediaCodecInfo.AudioCapabilities audioCapabilities = this.capabilities.getAudioCapabilities();
        if (audioCapabilities == null) {
            logNoSupport("channelCount.aCaps");
            return false;
        } else if (adjustMaxInputChannelCount(this.name, this.mimeType, audioCapabilities.getMaxInputChannelCount()) >= channelCount) {
            return true;
        } else {
            logNoSupport("channelCount.support, " + channelCount);
            return false;
        }
    }

    private void logNoSupport(String message) {
        Log.d(TAG, "NoSupport [" + message + "] [" + this.name + ", " + this.mimeType + "] [" + Util.DEVICE_DEBUG_INFO + "]");
    }

    private void logAssumedSupport(String message) {
        Log.d(TAG, "AssumedSupport [" + message + "] [" + this.name + ", " + this.mimeType + "] [" + Util.DEVICE_DEBUG_INFO + "]");
    }

    private static int adjustMaxInputChannelCount(String name2, String mimeType2, int maxChannelCount) {
        int assumedMaxChannelCount;
        if (maxChannelCount > 1) {
            return maxChannelCount;
        }
        if ((Util.SDK_INT >= 26 && maxChannelCount > 0) || MimeTypes.AUDIO_MPEG.equals(mimeType2) || MimeTypes.AUDIO_AMR_NB.equals(mimeType2) || MimeTypes.AUDIO_AMR_WB.equals(mimeType2) || MimeTypes.AUDIO_AAC.equals(mimeType2) || MimeTypes.AUDIO_VORBIS.equals(mimeType2) || MimeTypes.AUDIO_OPUS.equals(mimeType2) || MimeTypes.AUDIO_RAW.equals(mimeType2) || MimeTypes.AUDIO_FLAC.equals(mimeType2) || MimeTypes.AUDIO_ALAW.equals(mimeType2) || MimeTypes.AUDIO_MLAW.equals(mimeType2) || MimeTypes.AUDIO_MSGSM.equals(mimeType2)) {
            return maxChannelCount;
        }
        if (MimeTypes.AUDIO_AC3.equals(mimeType2)) {
            assumedMaxChannelCount = 6;
        } else if (MimeTypes.AUDIO_E_AC3.equals(mimeType2)) {
            assumedMaxChannelCount = 16;
        } else {
            assumedMaxChannelCount = 30;
        }
        Log.w(TAG, "AssumedMaxChannelAdjustment: " + name2 + ", [" + maxChannelCount + " to " + assumedMaxChannelCount + "]");
        return assumedMaxChannelCount;
    }

    private static boolean isAdaptive(MediaCodecInfo.CodecCapabilities capabilities2) {
        return Util.SDK_INT >= 19 && isAdaptiveV19(capabilities2);
    }

    @TargetApi(19)
    private static boolean isAdaptiveV19(MediaCodecInfo.CodecCapabilities capabilities2) {
        return capabilities2.isFeatureSupported("adaptive-playback");
    }

    private static boolean isTunneling(MediaCodecInfo.CodecCapabilities capabilities2) {
        return Util.SDK_INT >= 21 && isTunnelingV21(capabilities2);
    }

    @TargetApi(21)
    private static boolean isTunnelingV21(MediaCodecInfo.CodecCapabilities capabilities2) {
        return capabilities2.isFeatureSupported("tunneled-playback");
    }

    private static boolean isSecure(MediaCodecInfo.CodecCapabilities capabilities2) {
        return Util.SDK_INT >= 21 && isSecureV21(capabilities2);
    }

    @TargetApi(21)
    private static boolean isSecureV21(MediaCodecInfo.CodecCapabilities capabilities2) {
        return capabilities2.isFeatureSupported("secure-playback");
    }

    @TargetApi(21)
    private static boolean areSizeAndRateSupportedV21(MediaCodecInfo.VideoCapabilities capabilities2, int width, int height, double frameRate) {
        if (frameRate == -1.0d || frameRate <= 0.0d) {
            return capabilities2.isSizeSupported(width, height);
        }
        return capabilities2.areSizeAndRateSupported(width, height, frameRate);
    }
}
