package com.google.android.exoplayer2.offline;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.upstream.DataSink;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DummyDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.PriorityDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.PriorityTaskManager;

public final class DownloaderConstructorHelper {
    private final Cache cache;
    private final DataSource.Factory cacheReadDataSourceFactory;
    private final DataSink.Factory cacheWriteDataSinkFactory;
    private final PriorityTaskManager priorityTaskManager;
    private final DataSource.Factory upstreamDataSourceFactory;

    public DownloaderConstructorHelper(Cache cache2, DataSource.Factory upstreamDataSourceFactory2) {
        this(cache2, upstreamDataSourceFactory2, (DataSource.Factory) null, (DataSink.Factory) null, (PriorityTaskManager) null);
    }

    public DownloaderConstructorHelper(Cache cache2, DataSource.Factory upstreamDataSourceFactory2, @Nullable DataSource.Factory cacheReadDataSourceFactory2, @Nullable DataSink.Factory cacheWriteDataSinkFactory2, @Nullable PriorityTaskManager priorityTaskManager2) {
        Assertions.checkNotNull(upstreamDataSourceFactory2);
        this.cache = cache2;
        this.upstreamDataSourceFactory = upstreamDataSourceFactory2;
        this.cacheReadDataSourceFactory = cacheReadDataSourceFactory2;
        this.cacheWriteDataSinkFactory = cacheWriteDataSinkFactory2;
        this.priorityTaskManager = priorityTaskManager2;
    }

    public Cache getCache() {
        return this.cache;
    }

    public PriorityTaskManager getPriorityTaskManager() {
        return this.priorityTaskManager != null ? this.priorityTaskManager : new PriorityTaskManager();
    }

    public CacheDataSource buildCacheDataSource(boolean offline) {
        DataSource cacheReadDataSource = this.cacheReadDataSourceFactory != null ? this.cacheReadDataSourceFactory.createDataSource() : new FileDataSource();
        if (offline) {
            return new CacheDataSource(this.cache, DummyDataSource.INSTANCE, cacheReadDataSource, (DataSink) null, 1, (CacheDataSource.EventListener) null);
        }
        DataSink cacheWriteDataSink = this.cacheWriteDataSinkFactory != null ? this.cacheWriteDataSinkFactory.createDataSink() : new CacheDataSink(this.cache, 2097152);
        DataSource upstream = this.upstreamDataSourceFactory.createDataSource();
        if (this.priorityTaskManager != null) {
            upstream = new PriorityDataSource(upstream, this.priorityTaskManager, -1000);
        }
        return new CacheDataSource(this.cache, upstream, cacheReadDataSource, cacheWriteDataSink, 1, (CacheDataSource.EventListener) null);
    }
}
