package com.google.android.exoplayer2.upstream;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Predicate;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public final class DefaultDataSource implements DataSource {
    private static final String SCHEME_ASSET = "asset";
    private static final String SCHEME_CONTENT = "content";
    private static final String SCHEME_RTMP = "rtmp";
    private static final String TAG = "DefaultDataSource";
    private DataSource assetDataSource;
    private final DataSource baseDataSource;
    private DataSource contentDataSource;
    private final Context context;
    private DataSource dataSchemeDataSource;
    private DataSource dataSource;
    private DataSource fileDataSource;
    private final TransferListener<? super DataSource> listener;
    private DataSource rtmpDataSource;

    public DefaultDataSource(Context context2, TransferListener<? super DataSource> listener2, String userAgent, boolean allowCrossProtocolRedirects) {
        this(context2, listener2, userAgent, 8000, 8000, allowCrossProtocolRedirects);
    }

    public DefaultDataSource(Context context2, TransferListener<? super DataSource> listener2, String userAgent, int connectTimeoutMillis, int readTimeoutMillis, boolean allowCrossProtocolRedirects) {
        this(context2, listener2, new DefaultHttpDataSource(userAgent, (Predicate<String>) null, listener2, connectTimeoutMillis, readTimeoutMillis, allowCrossProtocolRedirects, (HttpDataSource.RequestProperties) null));
    }

    public DefaultDataSource(Context context2, TransferListener<? super DataSource> listener2, DataSource baseDataSource2) {
        this.context = context2.getApplicationContext();
        this.listener = listener2;
        this.baseDataSource = (DataSource) Assertions.checkNotNull(baseDataSource2);
    }

    public long open(DataSpec dataSpec) throws IOException {
        Assertions.checkState(this.dataSource == null);
        String scheme = dataSpec.uri.getScheme();
        if (Util.isLocalFileUri(dataSpec.uri)) {
            if (dataSpec.uri.getPath().startsWith("/android_asset/")) {
                this.dataSource = getAssetDataSource();
            } else {
                this.dataSource = getFileDataSource();
            }
        } else if (SCHEME_ASSET.equals(scheme)) {
            this.dataSource = getAssetDataSource();
        } else if (SCHEME_CONTENT.equals(scheme)) {
            this.dataSource = getContentDataSource();
        } else if (SCHEME_RTMP.equals(scheme)) {
            this.dataSource = getRtmpDataSource();
        } else if (DataSchemeDataSource.SCHEME_DATA.equals(scheme)) {
            this.dataSource = getDataSchemeDataSource();
        } else {
            this.dataSource = this.baseDataSource;
        }
        return this.dataSource.open(dataSpec);
    }

    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return this.dataSource.read(buffer, offset, readLength);
    }

    public Uri getUri() {
        if (this.dataSource == null) {
            return null;
        }
        return this.dataSource.getUri();
    }

    public void close() throws IOException {
        if (this.dataSource != null) {
            try {
                this.dataSource.close();
            } finally {
                this.dataSource = null;
            }
        }
    }

    private DataSource getFileDataSource() {
        if (this.fileDataSource == null) {
            this.fileDataSource = new FileDataSource(this.listener);
        }
        return this.fileDataSource;
    }

    private DataSource getAssetDataSource() {
        if (this.assetDataSource == null) {
            this.assetDataSource = new AssetDataSource(this.context, this.listener);
        }
        return this.assetDataSource;
    }

    private DataSource getContentDataSource() {
        if (this.contentDataSource == null) {
            this.contentDataSource = new ContentDataSource(this.context, this.listener);
        }
        return this.contentDataSource;
    }

    private DataSource getRtmpDataSource() {
        if (this.rtmpDataSource == null) {
            try {
                this.rtmpDataSource = (DataSource) Class.forName("com.google.android.exoplayer2.ext.rtmp.RtmpDataSource").getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "Attempting to play RTMP stream without depending on the RTMP extension");
            } catch (InstantiationException e2) {
                Log.e(TAG, "Error instantiating RtmpDataSource", e2);
            } catch (IllegalAccessException e3) {
                Log.e(TAG, "Error instantiating RtmpDataSource", e3);
            } catch (NoSuchMethodException e4) {
                Log.e(TAG, "Error instantiating RtmpDataSource", e4);
            } catch (InvocationTargetException e5) {
                Log.e(TAG, "Error instantiating RtmpDataSource", e5);
            }
            if (this.rtmpDataSource == null) {
                this.rtmpDataSource = this.baseDataSource;
            }
        }
        return this.rtmpDataSource;
    }

    private DataSource getDataSchemeDataSource() {
        if (this.dataSchemeDataSource == null) {
            this.dataSchemeDataSource = new DataSchemeDataSource();
        }
        return this.dataSchemeDataSource;
    }
}
