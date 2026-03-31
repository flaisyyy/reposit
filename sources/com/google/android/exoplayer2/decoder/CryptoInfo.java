package com.google.android.exoplayer2.decoder;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import com.google.android.exoplayer2.util.Util;

public final class CryptoInfo {
    public int clearBlocks;
    public int encryptedBlocks;
    private final MediaCodec.CryptoInfo frameworkCryptoInfo;
    public byte[] iv;
    public byte[] key;
    public int mode;
    public int[] numBytesOfClearData;
    public int[] numBytesOfEncryptedData;
    public int numSubSamples;
    private final PatternHolderV24 patternHolder;

    public CryptoInfo() {
        MediaCodec.CryptoInfo cryptoInfo;
        PatternHolderV24 patternHolderV24 = null;
        if (Util.SDK_INT >= 16) {
            cryptoInfo = newFrameworkCryptoInfoV16();
        } else {
            cryptoInfo = null;
        }
        this.frameworkCryptoInfo = cryptoInfo;
        this.patternHolder = Util.SDK_INT >= 24 ? new PatternHolderV24(this.frameworkCryptoInfo) : patternHolderV24;
    }

    public void set(int numSubSamples2, int[] numBytesOfClearData2, int[] numBytesOfEncryptedData2, byte[] key2, byte[] iv2, int mode2, int encryptedBlocks2, int clearBlocks2) {
        this.numSubSamples = numSubSamples2;
        this.numBytesOfClearData = numBytesOfClearData2;
        this.numBytesOfEncryptedData = numBytesOfEncryptedData2;
        this.key = key2;
        this.iv = iv2;
        this.mode = mode2;
        this.encryptedBlocks = encryptedBlocks2;
        this.clearBlocks = clearBlocks2;
        if (Util.SDK_INT >= 16) {
            updateFrameworkCryptoInfoV16();
        }
    }

    @TargetApi(16)
    public MediaCodec.CryptoInfo getFrameworkCryptoInfoV16() {
        return this.frameworkCryptoInfo;
    }

    @TargetApi(16)
    private MediaCodec.CryptoInfo newFrameworkCryptoInfoV16() {
        return new MediaCodec.CryptoInfo();
    }

    @TargetApi(16)
    private void updateFrameworkCryptoInfoV16() {
        this.frameworkCryptoInfo.numSubSamples = this.numSubSamples;
        this.frameworkCryptoInfo.numBytesOfClearData = this.numBytesOfClearData;
        this.frameworkCryptoInfo.numBytesOfEncryptedData = this.numBytesOfEncryptedData;
        this.frameworkCryptoInfo.key = this.key;
        this.frameworkCryptoInfo.iv = this.iv;
        this.frameworkCryptoInfo.mode = this.mode;
        if (Util.SDK_INT >= 24) {
            this.patternHolder.set(this.encryptedBlocks, this.clearBlocks);
        }
    }

    @TargetApi(24)
    private static final class PatternHolderV24 {
        private final MediaCodec.CryptoInfo frameworkCryptoInfo;
        private final MediaCodec.CryptoInfo.Pattern pattern;

        private PatternHolderV24(MediaCodec.CryptoInfo frameworkCryptoInfo2) {
            this.frameworkCryptoInfo = frameworkCryptoInfo2;
            this.pattern = new MediaCodec.CryptoInfo.Pattern(0, 0);
        }

        /* access modifiers changed from: private */
        public void set(int encryptedBlocks, int clearBlocks) {
            this.pattern.set(encryptedBlocks, clearBlocks);
            this.frameworkCryptoInfo.setPattern(this.pattern);
        }
    }
}
