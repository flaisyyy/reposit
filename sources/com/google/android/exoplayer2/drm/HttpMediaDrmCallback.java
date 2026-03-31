package com.google.android.exoplayer2.drm;

import android.annotation.TargetApi;
import android.net.Uri;
import android.text.TextUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@TargetApi(18)
public final class HttpMediaDrmCallback implements MediaDrmCallback {
    private final HttpDataSource.Factory dataSourceFactory;
    private final String defaultLicenseUrl;
    private final boolean forceDefaultLicenseUrl;
    private final Map<String, String> keyRequestProperties;

    public HttpMediaDrmCallback(String defaultLicenseUrl2, HttpDataSource.Factory dataSourceFactory2) {
        this(defaultLicenseUrl2, false, dataSourceFactory2);
    }

    public HttpMediaDrmCallback(String defaultLicenseUrl2, boolean forceDefaultLicenseUrl2, HttpDataSource.Factory dataSourceFactory2) {
        this.dataSourceFactory = dataSourceFactory2;
        this.defaultLicenseUrl = defaultLicenseUrl2;
        this.forceDefaultLicenseUrl = forceDefaultLicenseUrl2;
        this.keyRequestProperties = new HashMap();
    }

    public void setKeyRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.put(name, value);
        }
    }

    public void clearKeyRequestProperty(String name) {
        Assertions.checkNotNull(name);
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.remove(name);
        }
    }

    public void clearAllKeyRequestProperties() {
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.clear();
        }
    }

    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws IOException {
        return executePost(this.dataSourceFactory, request.getDefaultUrl() + "&signedRequest=" + new String(request.getData()), new byte[0], (Map<String, String>) null);
    }

    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
        String contentType;
        String url = request.getDefaultUrl();
        if (this.forceDefaultLicenseUrl || TextUtils.isEmpty(url)) {
            url = this.defaultLicenseUrl;
        }
        Map<String, String> requestProperties = new HashMap<>();
        if (C.PLAYREADY_UUID.equals(uuid)) {
            contentType = "text/xml";
        } else {
            contentType = C.CLEARKEY_UUID.equals(uuid) ? "application/json" : "application/octet-stream";
        }
        requestProperties.put("Content-Type", contentType);
        if (C.PLAYREADY_UUID.equals(uuid)) {
            requestProperties.put("SOAPAction", "http://schemas.microsoft.com/DRM/2007/03/protocols/AcquireLicense");
        }
        synchronized (this.keyRequestProperties) {
            requestProperties.putAll(this.keyRequestProperties);
        }
        return executePost(this.dataSourceFactory, url, request.getData(), requestProperties);
    }

    private static byte[] executePost(HttpDataSource.Factory dataSourceFactory2, String url, byte[] data, Map<String, String> requestProperties) throws IOException {
        HttpDataSource dataSource = dataSourceFactory2.createDataSource();
        if (requestProperties != null) {
            for (Map.Entry<String, String> requestProperty : requestProperties.entrySet()) {
                dataSource.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }
        }
        DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, new DataSpec(Uri.parse(url), data, 0, 0, -1, (String) null, 1));
        try {
            return Util.toByteArray(inputStream);
        } finally {
            Util.closeQuietly((Closeable) inputStream);
        }
    }
}
