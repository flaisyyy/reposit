package com.google.android.exoplayer2.drm;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.DefaultDrmSession;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.extractor.mp4.PsshAtomUtil;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@TargetApi(18)
public class DefaultDrmSessionManager<T extends ExoMediaCrypto> implements DrmSessionManager<T>, DefaultDrmSession.ProvisioningManager<T> {
    private static final String CENC_SCHEME_MIME_TYPE = "cenc";
    public static final int INITIAL_DRM_REQUEST_RETRY_COUNT = 3;
    public static final int MODE_DOWNLOAD = 2;
    public static final int MODE_PLAYBACK = 0;
    public static final int MODE_QUERY = 1;
    public static final int MODE_RELEASE = 3;
    public static final String PLAYREADY_CUSTOM_DATA_KEY = "PRCustomData";
    private final MediaDrmCallback callback;
    private final Handler eventHandler;
    /* access modifiers changed from: private */
    public final EventListener eventListener;
    private final int initialDrmRequestRetryCount;
    private final ExoMediaDrm<T> mediaDrm;
    volatile DefaultDrmSessionManager<T>.MediaDrmHandler mediaDrmHandler;
    /* access modifiers changed from: private */
    public int mode;
    private final boolean multiSession;
    private byte[] offlineLicenseKeySetId;
    private final HashMap<String, String> optionalKeyRequestParameters;
    private Looper playbackLooper;
    private final List<DefaultDrmSession<T>> provisioningSessions;
    /* access modifiers changed from: private */
    public final List<DefaultDrmSession<T>> sessions;
    private final UUID uuid;

    public interface EventListener {
        void onDrmKeysLoaded();

        void onDrmKeysRemoved();

        void onDrmKeysRestored();

        void onDrmSessionManagerError(Exception exc);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newWidevineInstance(MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler2, EventListener eventListener2) throws UnsupportedDrmException {
        return newFrameworkInstance(C.WIDEVINE_UUID, callback2, optionalKeyRequestParameters2, eventHandler2, eventListener2);
    }

    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newPlayReadyInstance(MediaDrmCallback callback2, String customData, Handler eventHandler2, EventListener eventListener2) throws UnsupportedDrmException {
        HashMap<String, String> optionalKeyRequestParameters2;
        if (!TextUtils.isEmpty(customData)) {
            optionalKeyRequestParameters2 = new HashMap<>();
            optionalKeyRequestParameters2.put(PLAYREADY_CUSTOM_DATA_KEY, customData);
        } else {
            optionalKeyRequestParameters2 = null;
        }
        return newFrameworkInstance(C.PLAYREADY_UUID, callback2, optionalKeyRequestParameters2, eventHandler2, eventListener2);
    }

    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newFrameworkInstance(UUID uuid2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler2, EventListener eventListener2) throws UnsupportedDrmException {
        return new DefaultDrmSessionManager<>(uuid2, FrameworkMediaDrm.newInstance(uuid2), callback2, optionalKeyRequestParameters2, eventHandler2, eventListener2, false, 3);
    }

    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler2, EventListener eventListener2) {
        this(uuid2, mediaDrm2, callback2, optionalKeyRequestParameters2, eventHandler2, eventListener2, false, 3);
    }

    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler2, EventListener eventListener2, boolean multiSession2) {
        this(uuid2, mediaDrm2, callback2, optionalKeyRequestParameters2, eventHandler2, eventListener2, multiSession2, 3);
    }

    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler2, EventListener eventListener2, boolean multiSession2, int initialDrmRequestRetryCount2) {
        Assertions.checkNotNull(uuid2);
        Assertions.checkNotNull(mediaDrm2);
        Assertions.checkArgument(!C.COMMON_PSSH_UUID.equals(uuid2), "Use C.CLEARKEY_UUID instead");
        this.uuid = uuid2;
        this.mediaDrm = mediaDrm2;
        this.callback = callback2;
        this.optionalKeyRequestParameters = optionalKeyRequestParameters2;
        this.eventHandler = eventHandler2;
        this.eventListener = eventListener2;
        this.multiSession = multiSession2;
        this.initialDrmRequestRetryCount = initialDrmRequestRetryCount2;
        this.mode = 0;
        this.sessions = new ArrayList();
        this.provisioningSessions = new ArrayList();
        if (multiSession2) {
            mediaDrm2.setPropertyString("sessionSharing", "enable");
        }
        mediaDrm2.setOnEventListener(new MediaDrmEventListener());
    }

    public final String getPropertyString(String key) {
        return this.mediaDrm.getPropertyString(key);
    }

    public final void setPropertyString(String key, String value) {
        this.mediaDrm.setPropertyString(key, value);
    }

    public final byte[] getPropertyByteArray(String key) {
        return this.mediaDrm.getPropertyByteArray(key);
    }

    public final void setPropertyByteArray(String key, byte[] value) {
        this.mediaDrm.setPropertyByteArray(key, value);
    }

    public void setMode(int mode2, byte[] offlineLicenseKeySetId2) {
        Assertions.checkState(this.sessions.isEmpty());
        if (mode2 == 1 || mode2 == 3) {
            Assertions.checkNotNull(offlineLicenseKeySetId2);
        }
        this.mode = mode2;
        this.offlineLicenseKeySetId = offlineLicenseKeySetId2;
    }

    public boolean canAcquireSession(@NonNull DrmInitData drmInitData) {
        if (getSchemeData(drmInitData, this.uuid, true) == null) {
            return false;
        }
        String schemeType = drmInitData.schemeType;
        if (schemeType == null || "cenc".equals(schemeType)) {
            return true;
        }
        if ((C.CENC_TYPE_cbc1.equals(schemeType) || C.CENC_TYPE_cbcs.equals(schemeType) || C.CENC_TYPE_cens.equals(schemeType)) && Util.SDK_INT < 24) {
            return false;
        }
        return true;
    }

    public DrmSession<T> acquireSession(Looper playbackLooper2, DrmInitData drmInitData) {
        DefaultDrmSession<T> session;
        Assertions.checkState(this.playbackLooper == null || this.playbackLooper == playbackLooper2);
        if (this.sessions.isEmpty()) {
            this.playbackLooper = playbackLooper2;
            if (this.mediaDrmHandler == null) {
                this.mediaDrmHandler = new MediaDrmHandler(playbackLooper2);
            }
        }
        byte[] initData = null;
        String mimeType = null;
        if (this.offlineLicenseKeySetId == null) {
            DrmInitData.SchemeData data = getSchemeData(drmInitData, this.uuid, false);
            if (data == null) {
                IllegalStateException illegalStateException = new IllegalStateException("Media does not support uuid: " + this.uuid);
                if (!(this.eventHandler == null || this.eventListener == null)) {
                    final IllegalStateException illegalStateException2 = illegalStateException;
                    this.eventHandler.post(new Runnable() {
                        public void run() {
                            DefaultDrmSessionManager.this.eventListener.onDrmSessionManagerError(illegalStateException2);
                        }
                    });
                }
                return new ErrorStateDrmSession(new DrmSession.DrmSessionException(illegalStateException));
            }
            initData = getSchemeInitData(data, this.uuid);
            mimeType = getSchemeMimeType(data, this.uuid);
        }
        if (this.multiSession) {
            session = null;
            Iterator<DefaultDrmSession<T>> it = this.sessions.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                DefaultDrmSession<T> existingSession = it.next();
                if (existingSession.hasInitData(initData)) {
                    session = existingSession;
                    break;
                }
            }
        } else if (this.sessions.isEmpty()) {
            session = null;
        } else {
            session = this.sessions.get(0);
        }
        if (session == null) {
            session = new DefaultDrmSession<>(this.uuid, this.mediaDrm, this, initData, mimeType, this.mode, this.offlineLicenseKeySetId, this.optionalKeyRequestParameters, this.callback, playbackLooper2, this.eventHandler, this.eventListener, this.initialDrmRequestRetryCount);
            this.sessions.add(session);
        }
        session.acquire();
        return session;
    }

    public void releaseSession(DrmSession<T> session) {
        if (!(session instanceof ErrorStateDrmSession)) {
            DefaultDrmSession<T> drmSession = (DefaultDrmSession) session;
            if (drmSession.release()) {
                this.sessions.remove(drmSession);
                if (this.provisioningSessions.size() > 1 && this.provisioningSessions.get(0) == drmSession) {
                    this.provisioningSessions.get(1).provision();
                }
                this.provisioningSessions.remove(drmSession);
            }
        }
    }

    public void provisionRequired(DefaultDrmSession<T> session) {
        this.provisioningSessions.add(session);
        if (this.provisioningSessions.size() == 1) {
            session.provision();
        }
    }

    public void onProvisionCompleted() {
        for (DefaultDrmSession<T> session : this.provisioningSessions) {
            session.onProvisionCompleted();
        }
        this.provisioningSessions.clear();
    }

    public void onProvisionError(Exception error) {
        for (DefaultDrmSession<T> session : this.provisioningSessions) {
            session.onProvisionError(error);
        }
        this.provisioningSessions.clear();
    }

    private static DrmInitData.SchemeData getSchemeData(DrmInitData drmInitData, UUID uuid2, boolean allowMissingData) {
        boolean uuidMatches;
        List<DrmInitData.SchemeData> matchingSchemeDatas = new ArrayList<>(drmInitData.schemeDataCount);
        for (int i = 0; i < drmInitData.schemeDataCount; i++) {
            DrmInitData.SchemeData schemeData = drmInitData.get(i);
            if (schemeData.matches(uuid2) || (C.CLEARKEY_UUID.equals(uuid2) && schemeData.matches(C.COMMON_PSSH_UUID))) {
                uuidMatches = true;
            } else {
                uuidMatches = false;
            }
            if (uuidMatches && (schemeData.data != null || allowMissingData)) {
                matchingSchemeDatas.add(schemeData);
            }
        }
        if (matchingSchemeDatas.isEmpty()) {
            return null;
        }
        if (C.WIDEVINE_UUID.equals(uuid2)) {
            for (int i2 = 0; i2 < matchingSchemeDatas.size(); i2++) {
                DrmInitData.SchemeData matchingSchemeData = matchingSchemeDatas.get(i2);
                int version = matchingSchemeData.hasData() ? PsshAtomUtil.parseVersion(matchingSchemeData.data) : -1;
                if (Util.SDK_INT < 23 && version == 0) {
                    return matchingSchemeData;
                }
                if (Util.SDK_INT >= 23 && version == 1) {
                    return matchingSchemeData;
                }
            }
        }
        return matchingSchemeDatas.get(0);
    }

    private static byte[] getSchemeInitData(DrmInitData.SchemeData data, UUID uuid2) {
        byte[] psshData;
        byte[] schemeInitData = data.data;
        if (Util.SDK_INT >= 21 || (psshData = PsshAtomUtil.parseSchemeSpecificData(schemeInitData, uuid2)) == null) {
            return schemeInitData;
        }
        return psshData;
    }

    private static String getSchemeMimeType(DrmInitData.SchemeData data, UUID uuid2) {
        String schemeMimeType = data.mimeType;
        if (Util.SDK_INT >= 26 || !C.CLEARKEY_UUID.equals(uuid2)) {
            return schemeMimeType;
        }
        if (MimeTypes.VIDEO_MP4.equals(schemeMimeType) || MimeTypes.AUDIO_MP4.equals(schemeMimeType)) {
            return "cenc";
        }
        return schemeMimeType;
    }

    @SuppressLint({"HandlerLeak"})
    private class MediaDrmHandler extends Handler {
        public MediaDrmHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            byte[] sessionId = (byte[]) msg.obj;
            for (DefaultDrmSession<T> session : DefaultDrmSessionManager.this.sessions) {
                if (session.hasSessionId(sessionId)) {
                    session.onMediaDrmEvent(msg.what);
                    return;
                }
            }
        }
    }

    private class MediaDrmEventListener implements ExoMediaDrm.OnEventListener<T> {
        private MediaDrmEventListener() {
        }

        public void onEvent(ExoMediaDrm<? extends T> exoMediaDrm, byte[] sessionId, int event, int extra, byte[] data) {
            if (DefaultDrmSessionManager.this.mode == 0) {
                DefaultDrmSessionManager.this.mediaDrmHandler.obtainMessage(event, sessionId).sendToTarget();
            }
        }
    }
}
