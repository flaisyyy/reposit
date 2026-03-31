package com.google.android.exoplayer2.drm;

import android.annotation.TargetApi;
import android.media.DeniedByServerException;
import android.media.MediaCrypto;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.media.NotProvisionedException;
import android.media.UnsupportedSchemeException;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@TargetApi(23)
public final class FrameworkMediaDrm implements ExoMediaDrm<FrameworkMediaCrypto> {
    private final MediaDrm mediaDrm;
    private final UUID uuid;

    public static FrameworkMediaDrm newInstance(UUID uuid2) throws UnsupportedDrmException {
        try {
            return new FrameworkMediaDrm(uuid2);
        } catch (UnsupportedSchemeException e) {
            throw new UnsupportedDrmException(1, e);
        } catch (Exception e2) {
            throw new UnsupportedDrmException(2, e2);
        }
    }

    private FrameworkMediaDrm(UUID uuid2) throws UnsupportedSchemeException {
        Assertions.checkNotNull(uuid2);
        Assertions.checkArgument(!C.COMMON_PSSH_UUID.equals(uuid2), "Use C.CLEARKEY_UUID instead");
        if (Util.SDK_INT < 27 && C.CLEARKEY_UUID.equals(uuid2)) {
            uuid2 = C.COMMON_PSSH_UUID;
        }
        this.uuid = uuid2;
        this.mediaDrm = new MediaDrm(uuid2);
    }

    public void setOnEventListener(final ExoMediaDrm.OnEventListener<? super FrameworkMediaCrypto> listener) {
        this.mediaDrm.setOnEventListener(listener == null ? null : new MediaDrm.OnEventListener() {
            public void onEvent(@NonNull MediaDrm md, @Nullable byte[] sessionId, int event, int extra, byte[] data) {
                listener.onEvent(FrameworkMediaDrm.this, sessionId, event, extra, data);
            }
        });
    }

    public void setOnKeyStatusChangeListener(final ExoMediaDrm.OnKeyStatusChangeListener<? super FrameworkMediaCrypto> listener) {
        AnonymousClass2 r0;
        if (Util.SDK_INT < 23) {
            throw new UnsupportedOperationException();
        }
        MediaDrm mediaDrm2 = this.mediaDrm;
        if (listener == null) {
            r0 = null;
        } else {
            r0 = new MediaDrm.OnKeyStatusChangeListener() {
                public void onKeyStatusChange(@NonNull MediaDrm md, @NonNull byte[] sessionId, @NonNull List<MediaDrm.KeyStatus> keyInfo, boolean hasNewUsableKey) {
                    List<ExoMediaDrm.KeyStatus> exoKeyInfo = new ArrayList<>();
                    for (MediaDrm.KeyStatus keyStatus : keyInfo) {
                        exoKeyInfo.add(new ExoMediaDrm.DefaultKeyStatus(keyStatus.getStatusCode(), keyStatus.getKeyId()));
                    }
                    listener.onKeyStatusChange(FrameworkMediaDrm.this, sessionId, exoKeyInfo, hasNewUsableKey);
                }
            };
        }
        mediaDrm2.setOnKeyStatusChangeListener(r0, (Handler) null);
    }

    public byte[] openSession() throws MediaDrmException {
        return this.mediaDrm.openSession();
    }

    public void closeSession(byte[] sessionId) {
        this.mediaDrm.closeSession(sessionId);
    }

    public ExoMediaDrm.KeyRequest getKeyRequest(byte[] scope, byte[] init, String mimeType, int keyType, HashMap<String, String> optionalParameters) throws NotProvisionedException {
        MediaDrm.KeyRequest request = this.mediaDrm.getKeyRequest(scope, init, mimeType, keyType, optionalParameters);
        return new ExoMediaDrm.DefaultKeyRequest(request.getData(), request.getDefaultUrl());
    }

    public byte[] provideKeyResponse(byte[] scope, byte[] response) throws NotProvisionedException, DeniedByServerException {
        return this.mediaDrm.provideKeyResponse(scope, response);
    }

    public ExoMediaDrm.ProvisionRequest getProvisionRequest() {
        MediaDrm.ProvisionRequest request = this.mediaDrm.getProvisionRequest();
        return new ExoMediaDrm.DefaultProvisionRequest(request.getData(), request.getDefaultUrl());
    }

    public void provideProvisionResponse(byte[] response) throws DeniedByServerException {
        this.mediaDrm.provideProvisionResponse(response);
    }

    public Map<String, String> queryKeyStatus(byte[] sessionId) {
        return this.mediaDrm.queryKeyStatus(sessionId);
    }

    public void release() {
        this.mediaDrm.release();
    }

    public void restoreKeys(byte[] sessionId, byte[] keySetId) {
        this.mediaDrm.restoreKeys(sessionId, keySetId);
    }

    public String getPropertyString(String propertyName) {
        return this.mediaDrm.getPropertyString(propertyName);
    }

    public byte[] getPropertyByteArray(String propertyName) {
        return this.mediaDrm.getPropertyByteArray(propertyName);
    }

    public void setPropertyString(String propertyName, String value) {
        this.mediaDrm.setPropertyString(propertyName, value);
    }

    public void setPropertyByteArray(String propertyName, byte[] value) {
        this.mediaDrm.setPropertyByteArray(propertyName, value);
    }

    public FrameworkMediaCrypto createMediaCrypto(byte[] initData) throws MediaCryptoException {
        return new FrameworkMediaCrypto(new MediaCrypto(this.uuid, initData), Util.SDK_INT < 21 && C.WIDEVINE_UUID.equals(this.uuid) && "L3".equals(getPropertyString("securityLevel")));
    }
}
