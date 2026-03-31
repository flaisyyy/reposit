package com.google.android.exoplayer2.mediacodec;

import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;

public interface MediaCodecSelector {
    public static final MediaCodecSelector DEFAULT = new MediaCodecSelector() {
        public MediaCodecInfo getDecoderInfo(String mimeType, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
            return MediaCodecUtil.getDecoderInfo(mimeType, requiresSecureDecoder);
        }

        public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
            return MediaCodecUtil.getPassthroughDecoderInfo();
        }
    };

    MediaCodecInfo getDecoderInfo(String str, boolean z) throws MediaCodecUtil.DecoderQueryException;

    MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException;
}
