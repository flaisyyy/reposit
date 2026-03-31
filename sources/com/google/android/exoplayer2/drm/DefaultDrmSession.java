package com.google.android.exoplayer2.drm;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.NotProvisionedException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@TargetApi(18)
class DefaultDrmSession<T extends ExoMediaCrypto> implements DrmSession<T> {
    private static final int MAX_LICENSE_DURATION_TO_RENEW = 60;
    private static final int MSG_KEYS = 1;
    private static final int MSG_PROVISION = 0;
    private static final String TAG = "DefaultDrmSession";
    final MediaDrmCallback callback;
    private final Handler eventHandler;
    /* access modifiers changed from: private */
    public final DefaultDrmSessionManager.EventListener eventListener;
    private final byte[] initData;
    /* access modifiers changed from: private */
    public final int initialDrmRequestRetryCount;
    private DrmSession.DrmSessionException lastException;
    private T mediaCrypto;
    private final ExoMediaDrm<T> mediaDrm;
    private final String mimeType;
    private final int mode;
    private byte[] offlineLicenseKeySetId;
    private int openCount;
    private final HashMap<String, String> optionalKeyRequestParameters;
    private DefaultDrmSession<T>.PostRequestHandler postRequestHandler;
    final DefaultDrmSession<T>.PostResponseHandler postResponseHandler;
    private final ProvisioningManager<T> provisioningManager;
    private HandlerThread requestHandlerThread;
    private byte[] sessionId;
    private int state = 2;
    final UUID uuid;

    public interface ProvisioningManager<T extends ExoMediaCrypto> {
        void onProvisionCompleted();

        void onProvisionError(Exception exc);

        void provisionRequired(DefaultDrmSession<T> defaultDrmSession);
    }

    public DefaultDrmSession(UUID uuid2, ExoMediaDrm<T> mediaDrm2, ProvisioningManager<T> provisioningManager2, byte[] initData2, String mimeType2, int mode2, byte[] offlineLicenseKeySetId2, HashMap<String, String> optionalKeyRequestParameters2, MediaDrmCallback callback2, Looper playbackLooper, Handler eventHandler2, DefaultDrmSessionManager.EventListener eventListener2, int initialDrmRequestRetryCount2) {
        this.uuid = uuid2;
        this.provisioningManager = provisioningManager2;
        this.mediaDrm = mediaDrm2;
        this.mode = mode2;
        this.offlineLicenseKeySetId = offlineLicenseKeySetId2;
        this.optionalKeyRequestParameters = optionalKeyRequestParameters2;
        this.callback = callback2;
        this.initialDrmRequestRetryCount = initialDrmRequestRetryCount2;
        this.eventHandler = eventHandler2;
        this.eventListener = eventListener2;
        this.postResponseHandler = new PostResponseHandler(playbackLooper);
        this.requestHandlerThread = new HandlerThread("DrmRequestHandler");
        this.requestHandlerThread.start();
        this.postRequestHandler = new PostRequestHandler(this.requestHandlerThread.getLooper());
        if (offlineLicenseKeySetId2 == null) {
            this.initData = initData2;
            this.mimeType = mimeType2;
            return;
        }
        this.initData = null;
        this.mimeType = null;
    }

    public void acquire() {
        int i = this.openCount + 1;
        this.openCount = i;
        if (i == 1 && this.state != 1 && openInternal(true)) {
            doLicense(true);
        }
    }

    public boolean release() {
        int i = this.openCount - 1;
        this.openCount = i;
        if (i != 0) {
            return false;
        }
        this.state = 0;
        this.postResponseHandler.removeCallbacksAndMessages((Object) null);
        this.postRequestHandler.removeCallbacksAndMessages((Object) null);
        this.postRequestHandler = null;
        this.requestHandlerThread.quit();
        this.requestHandlerThread = null;
        this.mediaCrypto = null;
        this.lastException = null;
        if (this.sessionId != null) {
            this.mediaDrm.closeSession(this.sessionId);
            this.sessionId = null;
        }
        return true;
    }

    public boolean hasInitData(byte[] initData2) {
        return Arrays.equals(this.initData, initData2);
    }

    public boolean hasSessionId(byte[] sessionId2) {
        return Arrays.equals(this.sessionId, sessionId2);
    }

    public void provision() {
        this.postRequestHandler.obtainMessage(0, this.mediaDrm.getProvisionRequest(), true).sendToTarget();
    }

    public void onProvisionCompleted() {
        if (openInternal(false)) {
            doLicense(true);
        }
    }

    public void onProvisionError(Exception error) {
        onError(error);
    }

    public final int getState() {
        return this.state;
    }

    public final DrmSession.DrmSessionException getError() {
        if (this.state == 1) {
            return this.lastException;
        }
        return null;
    }

    public final T getMediaCrypto() {
        return this.mediaCrypto;
    }

    public Map<String, String> queryKeyStatus() {
        if (this.sessionId == null) {
            return null;
        }
        return this.mediaDrm.queryKeyStatus(this.sessionId);
    }

    public byte[] getOfflineLicenseKeySetId() {
        return this.offlineLicenseKeySetId;
    }

    private boolean openInternal(boolean allowProvisioning) {
        if (isOpen()) {
            return true;
        }
        try {
            this.sessionId = this.mediaDrm.openSession();
            this.mediaCrypto = this.mediaDrm.createMediaCrypto(this.sessionId);
            this.state = 3;
            return true;
        } catch (NotProvisionedException e) {
            if (allowProvisioning) {
                this.provisioningManager.provisionRequired(this);
            } else {
                onError(e);
            }
        } catch (Exception e2) {
            onError(e2);
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void onProvisionResponse(Object response) {
        if (this.state != 2 && !isOpen()) {
            return;
        }
        if (response instanceof Exception) {
            this.provisioningManager.onProvisionError((Exception) response);
            return;
        }
        try {
            this.mediaDrm.provideProvisionResponse((byte[]) response);
            this.provisioningManager.onProvisionCompleted();
        } catch (Exception e) {
            this.provisioningManager.onProvisionError(e);
        }
    }

    private void doLicense(boolean allowRetry) {
        switch (this.mode) {
            case 0:
            case 1:
                if (this.offlineLicenseKeySetId == null) {
                    postKeyRequest(1, allowRetry);
                    return;
                } else if (this.state == 4 || restoreKeys()) {
                    long licenseDurationRemainingSec = getLicenseDurationRemainingSec();
                    if (this.mode == 0 && licenseDurationRemainingSec <= 60) {
                        Log.d(TAG, "Offline license has expired or will expire soon. Remaining seconds: " + licenseDurationRemainingSec);
                        postKeyRequest(2, allowRetry);
                        return;
                    } else if (licenseDurationRemainingSec <= 0) {
                        onError(new KeysExpiredException());
                        return;
                    } else {
                        this.state = 4;
                        if (this.eventHandler != null && this.eventListener != null) {
                            this.eventHandler.post(new Runnable() {
                                public void run() {
                                    DefaultDrmSession.this.eventListener.onDrmKeysRestored();
                                }
                            });
                            return;
                        }
                        return;
                    }
                } else {
                    return;
                }
            case 2:
                if (this.offlineLicenseKeySetId == null) {
                    postKeyRequest(2, allowRetry);
                    return;
                } else if (restoreKeys()) {
                    postKeyRequest(2, allowRetry);
                    return;
                } else {
                    return;
                }
            case 3:
                if (restoreKeys()) {
                    postKeyRequest(3, allowRetry);
                    return;
                }
                return;
            default:
                return;
        }
    }

    private boolean restoreKeys() {
        try {
            this.mediaDrm.restoreKeys(this.sessionId, this.offlineLicenseKeySetId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error trying to restore Widevine keys.", e);
            onError(e);
            return false;
        }
    }

    private long getLicenseDurationRemainingSec() {
        if (!C.WIDEVINE_UUID.equals(this.uuid)) {
            return Long.MAX_VALUE;
        }
        Pair<Long, Long> pair = WidevineUtil.getLicenseDurationRemainingSec(this);
        return Math.min(((Long) pair.first).longValue(), ((Long) pair.second).longValue());
    }

    private void postKeyRequest(int type, boolean allowRetry) {
        try {
            ExoMediaDrm.KeyRequest request = this.mediaDrm.getKeyRequest(type == 3 ? this.offlineLicenseKeySetId : this.sessionId, this.initData, this.mimeType, type, this.optionalKeyRequestParameters);
            if (C.CLEARKEY_UUID.equals(this.uuid)) {
                request = new ExoMediaDrm.DefaultKeyRequest(ClearKeyUtil.adjustRequestData(request.getData()), request.getDefaultUrl());
            }
            this.postRequestHandler.obtainMessage(1, request, allowRetry).sendToTarget();
        } catch (Exception e) {
            onKeysError(e);
        }
    }

    /* access modifiers changed from: private */
    public void onKeyResponse(Object response) {
        if (isOpen()) {
            if (response instanceof Exception) {
                onKeysError((Exception) response);
                return;
            }
            try {
                byte[] responseData = (byte[]) response;
                if (C.CLEARKEY_UUID.equals(this.uuid)) {
                    responseData = ClearKeyUtil.adjustResponseData(responseData);
                }
                if (this.mode == 3) {
                    this.mediaDrm.provideKeyResponse(this.offlineLicenseKeySetId, responseData);
                    if (this.eventHandler != null && this.eventListener != null) {
                        this.eventHandler.post(new Runnable() {
                            public void run() {
                                DefaultDrmSession.this.eventListener.onDrmKeysRemoved();
                            }
                        });
                        return;
                    }
                    return;
                }
                byte[] keySetId = this.mediaDrm.provideKeyResponse(this.sessionId, responseData);
                if (!((this.mode != 2 && (this.mode != 0 || this.offlineLicenseKeySetId == null)) || keySetId == null || keySetId.length == 0)) {
                    this.offlineLicenseKeySetId = keySetId;
                }
                this.state = 4;
                if (this.eventHandler != null && this.eventListener != null) {
                    this.eventHandler.post(new Runnable() {
                        public void run() {
                            DefaultDrmSession.this.eventListener.onDrmKeysLoaded();
                        }
                    });
                }
            } catch (Exception e) {
                onKeysError(e);
            }
        }
    }

    private void onKeysExpired() {
        if (this.state == 4) {
            this.state = 3;
            onError(new KeysExpiredException());
        }
    }

    private void onKeysError(Exception e) {
        if (e instanceof NotProvisionedException) {
            this.provisioningManager.provisionRequired(this);
        } else {
            onError(e);
        }
    }

    private void onError(final Exception e) {
        this.lastException = new DrmSession.DrmSessionException(e);
        if (!(this.eventHandler == null || this.eventListener == null)) {
            this.eventHandler.post(new Runnable() {
                public void run() {
                    DefaultDrmSession.this.eventListener.onDrmSessionManagerError(e);
                }
            });
        }
        if (this.state != 4) {
            this.state = 1;
        }
    }

    private boolean isOpen() {
        return this.state == 3 || this.state == 4;
    }

    public void onMediaDrmEvent(int what) {
        if (isOpen()) {
            switch (what) {
                case 1:
                    this.state = 3;
                    this.provisioningManager.provisionRequired(this);
                    return;
                case 2:
                    doLicense(false);
                    return;
                case 3:
                    onKeysExpired();
                    return;
                default:
                    return;
            }
        }
    }

    @SuppressLint({"HandlerLeak"})
    private class PostResponseHandler extends Handler {
        public PostResponseHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    DefaultDrmSession.this.onProvisionResponse(msg.obj);
                    return;
                case 1:
                    DefaultDrmSession.this.onKeyResponse(msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    @SuppressLint({"HandlerLeak"})
    private class PostRequestHandler extends Handler {
        public PostRequestHandler(Looper backgroundLooper) {
            super(backgroundLooper);
        }

        /* access modifiers changed from: package-private */
        public Message obtainMessage(int what, Object object, boolean allowRetry) {
            return obtainMessage(what, allowRetry ? 1 : 0, 0, object);
        }

        /* JADX WARNING: Can't fix incorrect switch cases order */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(android.os.Message r6) {
            /*
                r5 = this;
                int r2 = r6.what     // Catch:{ Exception -> 0x000b }
                switch(r2) {
                    case 0: goto L_0x0013;
                    case 1: goto L_0x0031;
                    default: goto L_0x0005;
                }     // Catch:{ Exception -> 0x000b }
            L_0x0005:
                java.lang.RuntimeException r2 = new java.lang.RuntimeException     // Catch:{ Exception -> 0x000b }
                r2.<init>()     // Catch:{ Exception -> 0x000b }
                throw r2     // Catch:{ Exception -> 0x000b }
            L_0x000b:
                r0 = move-exception
                boolean r2 = r5.maybeRetryRequest(r6)
                if (r2 == 0) goto L_0x0042
            L_0x0012:
                return
            L_0x0013:
                com.google.android.exoplayer2.drm.DefaultDrmSession r2 = com.google.android.exoplayer2.drm.DefaultDrmSession.this     // Catch:{ Exception -> 0x000b }
                com.google.android.exoplayer2.drm.MediaDrmCallback r3 = r2.callback     // Catch:{ Exception -> 0x000b }
                com.google.android.exoplayer2.drm.DefaultDrmSession r2 = com.google.android.exoplayer2.drm.DefaultDrmSession.this     // Catch:{ Exception -> 0x000b }
                java.util.UUID r4 = r2.uuid     // Catch:{ Exception -> 0x000b }
                java.lang.Object r2 = r6.obj     // Catch:{ Exception -> 0x000b }
                com.google.android.exoplayer2.drm.ExoMediaDrm$ProvisionRequest r2 = (com.google.android.exoplayer2.drm.ExoMediaDrm.ProvisionRequest) r2     // Catch:{ Exception -> 0x000b }
                byte[] r1 = r3.executeProvisionRequest(r4, r2)     // Catch:{ Exception -> 0x000b }
            L_0x0023:
                com.google.android.exoplayer2.drm.DefaultDrmSession r2 = com.google.android.exoplayer2.drm.DefaultDrmSession.this
                com.google.android.exoplayer2.drm.DefaultDrmSession<T>$PostResponseHandler r2 = r2.postResponseHandler
                int r3 = r6.what
                android.os.Message r2 = r2.obtainMessage(r3, r1)
                r2.sendToTarget()
                goto L_0x0012
            L_0x0031:
                com.google.android.exoplayer2.drm.DefaultDrmSession r2 = com.google.android.exoplayer2.drm.DefaultDrmSession.this     // Catch:{ Exception -> 0x000b }
                com.google.android.exoplayer2.drm.MediaDrmCallback r3 = r2.callback     // Catch:{ Exception -> 0x000b }
                com.google.android.exoplayer2.drm.DefaultDrmSession r2 = com.google.android.exoplayer2.drm.DefaultDrmSession.this     // Catch:{ Exception -> 0x000b }
                java.util.UUID r4 = r2.uuid     // Catch:{ Exception -> 0x000b }
                java.lang.Object r2 = r6.obj     // Catch:{ Exception -> 0x000b }
                com.google.android.exoplayer2.drm.ExoMediaDrm$KeyRequest r2 = (com.google.android.exoplayer2.drm.ExoMediaDrm.KeyRequest) r2     // Catch:{ Exception -> 0x000b }
                byte[] r1 = r3.executeKeyRequest(r4, r2)     // Catch:{ Exception -> 0x000b }
                goto L_0x0023
            L_0x0042:
                r1 = r0
                goto L_0x0023
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.drm.DefaultDrmSession.PostRequestHandler.handleMessage(android.os.Message):void");
        }

        private boolean maybeRetryRequest(Message originalMsg) {
            boolean allowRetry;
            int errorCount;
            if (originalMsg.arg1 == 1) {
                allowRetry = true;
            } else {
                allowRetry = false;
            }
            if (!allowRetry || (errorCount = originalMsg.arg2 + 1) > DefaultDrmSession.this.initialDrmRequestRetryCount) {
                return false;
            }
            Message retryMsg = Message.obtain(originalMsg);
            retryMsg.arg2 = errorCount;
            sendMessageDelayed(retryMsg, getRetryDelayMillis(errorCount));
            return true;
        }

        private long getRetryDelayMillis(int errorCount) {
            return (long) Math.min((errorCount - 1) * 1000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        }
    }
}
