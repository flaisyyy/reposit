package com.google.android.exoplayer2.upstream.cache;

import com.google.android.exoplayer2.upstream.DataSink;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;

public final class CacheDataSourceFactory implements DataSource.Factory {
    private final Cache cache;
    private final DataSource.Factory cacheReadDataSourceFactory;
    private final DataSink.Factory cacheWriteDataSinkFactory;
    private final CacheDataSource.EventListener eventListener;
    private final int flags;
    private final DataSource.Factory upstreamFactory;

    public CacheDataSourceFactory(Cache cache2, DataSource.Factory upstreamFactory2) {
        this(cache2, upstreamFactory2, 0);
    }

    public CacheDataSourceFactory(Cache cache2, DataSource.Factory upstreamFactory2, int flags2) {
        this(cache2, upstreamFactory2, flags2, 2097152);
    }

    public CacheDataSourceFactory(Cache cache2, DataSource.Factory upstreamFactory2, int flags2, long maxCacheFileSize) {
        this(cache2, upstreamFactory2, new FileDataSourceFactory(), new CacheDataSinkFactory(cache2, maxCacheFileSize), flags2, (CacheDataSource.EventListener) null);
    }

    public CacheDataSourceFactory(Cache cache2, DataSource.Factory upstreamFactory2, DataSource.Factory cacheReadDataSourceFactory2, DataSink.Factory cacheWriteDataSinkFactory2, int flags2, CacheDataSource.EventListener eventListener2) {
        this.cache = cache2;
        this.upstreamFactory = upstreamFactory2;
        this.cacheReadDataSourceFactory = cacheReadDataSourceFactory2;
        this.cacheWriteDataSinkFactory = cacheWriteDataSinkFactory2;
        this.flags = flags2;
        this.eventListener = eventListener2;
    }

    public CacheDataSource createDataSource() {
        return new CacheDataSource(this.cache, this.upstreamFactory.createDataSource(), this.cacheReadDataSourceFactory.createDataSource(), this.cacheWriteDataSinkFactory != null ? this.cacheWriteDataSinkFactory.createDataSink() : null, this.flags, this.eventListener);
    }
}
