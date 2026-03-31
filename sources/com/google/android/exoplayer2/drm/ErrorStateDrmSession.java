package com.google.android.exoplayer2.drm;

import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.util.Assertions;
import java.util.Map;

final class ErrorStateDrmSession<T extends ExoMediaCrypto> implements DrmSession<T> {
    private final DrmSession.DrmSessionException error;

    public ErrorStateDrmSession(DrmSession.DrmSessionException error2) {
        this.error = (DrmSession.DrmSessionException) Assertions.checkNotNull(error2);
    }

    public int getState() {
        return 1;
    }

    public DrmSession.DrmSessionException getError() {
        return this.error;
    }

    public T getMediaCrypto() {
        return null;
    }

    public Map<String, String> queryKeyStatus() {
        return null;
    }

    public byte[] getOfflineLicenseKeySetId() {
        return null;
    }
}
