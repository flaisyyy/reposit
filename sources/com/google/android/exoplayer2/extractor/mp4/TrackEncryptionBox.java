package com.google.android.exoplayer2.extractor.mp4;

import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.Assertions;

public final class TrackEncryptionBox {
    private static final String TAG = "TrackEncryptionBox";
    public final TrackOutput.CryptoData cryptoData;
    public final byte[] defaultInitializationVector;
    public final int initializationVectorSize;
    public final boolean isEncrypted;
    @Nullable
    public final String schemeType;

    public TrackEncryptionBox(boolean isEncrypted2, @Nullable String schemeType2, int initializationVectorSize2, byte[] keyId, int defaultEncryptedBlocks, int defaultClearBlocks, @Nullable byte[] defaultInitializationVector2) {
        boolean z = true;
        Assertions.checkArgument((defaultInitializationVector2 != null ? false : z) ^ (initializationVectorSize2 == 0));
        this.isEncrypted = isEncrypted2;
        this.schemeType = schemeType2;
        this.initializationVectorSize = initializationVectorSize2;
        this.defaultInitializationVector = defaultInitializationVector2;
        this.cryptoData = new TrackOutput.CryptoData(schemeToCryptoMode(schemeType2), keyId, defaultEncryptedBlocks, defaultClearBlocks);
    }

    private static int schemeToCryptoMode(@Nullable String schemeType2) {
        if (schemeType2 == null) {
            return 1;
        }
        char c = 65535;
        switch (schemeType2.hashCode()) {
            case 3046605:
                if (schemeType2.equals(C.CENC_TYPE_cbc1)) {
                    c = 2;
                    break;
                }
                break;
            case 3046671:
                if (schemeType2.equals(C.CENC_TYPE_cbcs)) {
                    c = 3;
                    break;
                }
                break;
            case 3049879:
                if (schemeType2.equals(C.CENC_TYPE_cenc)) {
                    c = 0;
                    break;
                }
                break;
            case 3049895:
                if (schemeType2.equals(C.CENC_TYPE_cens)) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 1:
                return 1;
            case 2:
            case 3:
                return 2;
            default:
                Log.w(TAG, "Unsupported protection scheme type '" + schemeType2 + "'. Assuming AES-CTR crypto mode.");
                return 1;
        }
    }
}
