package com.google.android.exoplayer2.drm;

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Assertions;
import java.util.HashMap;
import java.util.UUID;

public final class OfflineLicenseHelper<T extends ExoMediaCrypto> {
    /* access modifiers changed from: private */
    public final ConditionVariable conditionVariable;
    private final DefaultDrmSessionManager<T> drmSessionManager;
    private final HandlerThread handlerThread = new HandlerThread("OfflineLicenseHelper");

    public static OfflineLicenseHelper<FrameworkMediaCrypto> newWidevineInstance(String defaultLicenseUrl, HttpDataSource.Factory httpDataSourceFactory) throws UnsupportedDrmException {
        return newWidevineInstance(defaultLicenseUrl, false, httpDataSourceFactory, (HashMap<String, String>) null);
    }

    public static OfflineLicenseHelper<FrameworkMediaCrypto> newWidevineInstance(String defaultLicenseUrl, boolean forceDefaultLicenseUrl, HttpDataSource.Factory httpDataSourceFactory) throws UnsupportedDrmException {
        return newWidevineInstance(defaultLicenseUrl, forceDefaultLicenseUrl, httpDataSourceFactory, (HashMap<String, String>) null);
    }

    public static OfflineLicenseHelper<FrameworkMediaCrypto> newWidevineInstance(String defaultLicenseUrl, boolean forceDefaultLicenseUrl, HttpDataSource.Factory httpDataSourceFactory, HashMap<String, String> optionalKeyRequestParameters) throws UnsupportedDrmException {
        return new OfflineLicenseHelper<>(C.WIDEVINE_UUID, FrameworkMediaDrm.newInstance(C.WIDEVINE_UUID), new HttpMediaDrmCallback(defaultLicenseUrl, forceDefaultLicenseUrl, httpDataSourceFactory), optionalKeyRequestParameters);
    }

    public OfflineLicenseHelper(UUID uuid, ExoMediaDrm<T> mediaDrm, MediaDrmCallback callback, HashMap<String, String> optionalKeyRequestParameters) {
        this.handlerThread.start();
        this.conditionVariable = new ConditionVariable();
        DefaultDrmSessionManager.EventListener eventListener = new DefaultDrmSessionManager.EventListener() {
            public void onDrmKeysLoaded() {
                OfflineLicenseHelper.this.conditionVariable.open();
            }

            public void onDrmSessionManagerError(Exception e) {
                OfflineLicenseHelper.this.conditionVariable.open();
            }

            public void onDrmKeysRestored() {
                OfflineLicenseHelper.this.conditionVariable.open();
            }

            public void onDrmKeysRemoved() {
                OfflineLicenseHelper.this.conditionVariable.open();
            }
        };
        this.drmSessionManager = new DefaultDrmSessionManager<>(uuid, mediaDrm, callback, optionalKeyRequestParameters, new Handler(this.handlerThread.getLooper()), eventListener);
    }

    public synchronized byte[] getPropertyByteArray(String key) {
        return this.drmSessionManager.getPropertyByteArray(key);
    }

    public synchronized void setPropertyByteArray(String key, byte[] value) {
        this.drmSessionManager.setPropertyByteArray(key, value);
    }

    public synchronized String getPropertyString(String key) {
        return this.drmSessionManager.getPropertyString(key);
    }

    public synchronized void setPropertyString(String key, String value) {
        this.drmSessionManager.setPropertyString(key, value);
    }

    public synchronized byte[] downloadLicense(DrmInitData drmInitData) throws DrmSession.DrmSessionException {
        Assertions.checkArgument(drmInitData != null);
        return blockingKeyRequest(2, (byte[]) null, drmInitData);
    }

    public synchronized byte[] renewLicense(byte[] offlineLicenseKeySetId) throws DrmSession.DrmSessionException {
        Assertions.checkNotNull(offlineLicenseKeySetId);
        return blockingKeyRequest(2, offlineLicenseKeySetId, (DrmInitData) null);
    }

    public synchronized void releaseLicense(byte[] offlineLicenseKeySetId) throws DrmSession.DrmSessionException {
        Assertions.checkNotNull(offlineLicenseKeySetId);
        blockingKeyRequest(3, offlineLicenseKeySetId, (DrmInitData) null);
    }

    public synchronized Pair<Long, Long> getLicenseDurationRemainingSec(byte[] offlineLicenseKeySetId) throws DrmSession.DrmSessionException {
        Pair<Long, Long> licenseDurationRemainingSec;
        Assertions.checkNotNull(offlineLicenseKeySetId);
        DrmSession<T> drmSession = openBlockingKeyRequest(1, offlineLicenseKeySetId, (DrmInitData) null);
        DrmSession.DrmSessionException error = drmSession.getError();
        licenseDurationRemainingSec = WidevineUtil.getLicenseDurationRemainingSec(drmSession);
        this.drmSessionManager.releaseSession(drmSession);
        if (error != null) {
            if (error.getCause() instanceof KeysExpiredException) {
                licenseDurationRemainingSec = Pair.create(0L, 0L);
            } else {
                throw error;
            }
        }
        return licenseDurationRemainingSec;
    }

    public void release() {
        this.handlerThread.quit();
    }

    private byte[] blockingKeyRequest(int licenseMode, byte[] offlineLicenseKeySetId, DrmInitData drmInitData) throws DrmSession.DrmSessionException {
        DrmSession<T> drmSession = openBlockingKeyRequest(licenseMode, offlineLicenseKeySetId, drmInitData);
        DrmSession.DrmSessionException error = drmSession.getError();
        byte[] keySetId = drmSession.getOfflineLicenseKeySetId();
        this.drmSessionManager.releaseSession(drmSession);
        if (error == null) {
            return keySetId;
        }
        throw error;
    }

    private DrmSession<T> openBlockingKeyRequest(int licenseMode, byte[] offlineLicenseKeySetId, DrmInitData drmInitData) {
        this.drmSessionManager.setMode(licenseMode, offlineLicenseKeySetId);
        this.conditionVariable.close();
        DrmSession<T> drmSession = this.drmSessionManager.acquireSession(this.handlerThread.getLooper(), drmInitData);
        this.conditionVariable.block();
        return drmSession;
    }
}
