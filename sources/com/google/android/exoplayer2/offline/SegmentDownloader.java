package com.google.android.exoplayer2.offline;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.offline.Downloader;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheUtil;
import com.google.android.exoplayer2.util.PriorityTaskManager;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class SegmentDownloader<M, K> implements Downloader {
    private static final int BUFFER_SIZE_BYTES = 131072;
    private final Cache cache;
    private final CacheDataSource dataSource;
    private volatile long downloadedBytes;
    private volatile int downloadedSegments;
    private K[] keys;
    private M manifest;
    private final Uri manifestUri;
    private final CacheDataSource offlineDataSource;
    private final PriorityTaskManager priorityTaskManager;
    private volatile int totalSegments;

    /* access modifiers changed from: protected */
    public abstract List<Segment> getAllSegments(DataSource dataSource2, M m, boolean z) throws InterruptedException, IOException;

    /* access modifiers changed from: protected */
    public abstract M getManifest(DataSource dataSource2, Uri uri) throws IOException;

    /* access modifiers changed from: protected */
    public abstract List<Segment> getSegments(DataSource dataSource2, M m, K[] kArr, boolean z) throws InterruptedException, IOException;

    protected static class Segment implements Comparable<Segment> {
        public final DataSpec dataSpec;
        public final long startTimeUs;

        public Segment(long startTimeUs2, DataSpec dataSpec2) {
            this.startTimeUs = startTimeUs2;
            this.dataSpec = dataSpec2;
        }

        public int compareTo(@NonNull Segment other) {
            long startOffsetDiff = this.startTimeUs - other.startTimeUs;
            if (startOffsetDiff == 0) {
                return 0;
            }
            return startOffsetDiff < 0 ? -1 : 1;
        }
    }

    public SegmentDownloader(Uri manifestUri2, DownloaderConstructorHelper constructorHelper) {
        this.manifestUri = manifestUri2;
        this.cache = constructorHelper.getCache();
        this.dataSource = constructorHelper.buildCacheDataSource(false);
        this.offlineDataSource = constructorHelper.buildCacheDataSource(true);
        this.priorityTaskManager = constructorHelper.getPriorityTaskManager();
        resetCounters();
    }

    public final M getManifest() throws IOException {
        return getManifestIfNeeded(false);
    }

    public final void selectRepresentations(K[] keys2) {
        this.keys = keys2 != null ? (Object[]) keys2.clone() : null;
        resetCounters();
    }

    public final void init() throws InterruptedException, IOException {
        try {
            getManifestIfNeeded(true);
            try {
                initStatus(true);
            } catch (IOException | InterruptedException e) {
                resetCounters();
                throw e;
            }
        } catch (IOException e2) {
        }
    }

    public final synchronized void download(@Nullable Downloader.ProgressListener listener) throws IOException, InterruptedException {
        this.priorityTaskManager.add(-1000);
        try {
            getManifestIfNeeded(false);
            List<Segment> segments = initStatus(false);
            notifyListener(listener);
            Collections.sort(segments);
            byte[] buffer = new byte[131072];
            CacheUtil.CachingCounters cachingCounters = new CacheUtil.CachingCounters();
            for (int i = 0; i < segments.size(); i++) {
                CacheUtil.cache(segments.get(i).dataSpec, this.cache, this.dataSource, buffer, this.priorityTaskManager, -1000, cachingCounters, true);
                this.downloadedBytes += cachingCounters.newlyCachedBytes;
                this.downloadedSegments++;
                notifyListener(listener);
            }
            this.priorityTaskManager.remove(-1000);
        } catch (Throwable th) {
            this.priorityTaskManager.remove(-1000);
            throw th;
        }
    }

    public final int getTotalSegments() {
        return this.totalSegments;
    }

    public final int getDownloadedSegments() {
        return this.downloadedSegments;
    }

    public final long getDownloadedBytes() {
        return this.downloadedBytes;
    }

    public float getDownloadPercentage() {
        int totalSegments2 = this.totalSegments;
        int downloadedSegments2 = this.downloadedSegments;
        if (totalSegments2 == -1 || downloadedSegments2 == -1) {
            return Float.NaN;
        }
        if (totalSegments2 != 0) {
            return (100.0f * ((float) downloadedSegments2)) / ((float) totalSegments2);
        }
        return 100.0f;
    }

    public final void remove() throws InterruptedException {
        try {
            getManifestIfNeeded(true);
        } catch (IOException e) {
        }
        resetCounters();
        if (this.manifest != null) {
            List<Segment> segments = null;
            try {
                segments = getAllSegments(this.offlineDataSource, this.manifest, true);
            } catch (IOException e2) {
            }
            if (segments != null) {
                for (int i = 0; i < segments.size(); i++) {
                    remove(segments.get(i).dataSpec.uri);
                }
            }
            this.manifest = null;
        }
        remove(this.manifestUri);
    }

    private void resetCounters() {
        this.totalSegments = -1;
        this.downloadedSegments = -1;
        this.downloadedBytes = -1;
    }

    private void remove(Uri uri) {
        CacheUtil.remove(this.cache, CacheUtil.generateKey(uri));
    }

    private void notifyListener(Downloader.ProgressListener listener) {
        if (listener != null) {
            listener.onDownloadProgress(this, getDownloadPercentage(), this.downloadedBytes);
        }
    }

    private synchronized List<Segment> initStatus(boolean offline) throws IOException, InterruptedException {
        List<Segment> segments;
        DataSource dataSource2 = getDataSource(offline);
        if (this.keys == null || this.keys.length <= 0) {
            segments = getAllSegments(dataSource2, this.manifest, offline);
        } else {
            segments = getSegments(dataSource2, this.manifest, this.keys, offline);
        }
        CacheUtil.CachingCounters cachingCounters = new CacheUtil.CachingCounters();
        this.totalSegments = segments.size();
        this.downloadedSegments = 0;
        this.downloadedBytes = 0;
        for (int i = segments.size() - 1; i >= 0; i--) {
            CacheUtil.getCached(segments.get(i).dataSpec, this.cache, cachingCounters);
            this.downloadedBytes += cachingCounters.alreadyCachedBytes;
            if (cachingCounters.alreadyCachedBytes == cachingCounters.contentLength) {
                this.downloadedSegments++;
                segments.remove(i);
            }
        }
        return segments;
    }

    private M getManifestIfNeeded(boolean offline) throws IOException {
        if (this.manifest == null) {
            this.manifest = getManifest(getDataSource(offline), this.manifestUri);
        }
        return this.manifest;
    }

    private DataSource getDataSource(boolean offline) {
        return offline ? this.offlineDataSource : this.dataSource;
    }
}
