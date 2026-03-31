package com.google.android.exoplayer2.upstream.cache;

import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.util.Assertions;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.TreeSet;

final class CachedContent {
    private final TreeSet<SimpleCacheSpan> cachedSpans;
    public final int id;
    public final String key;
    private long length;

    public CachedContent(DataInputStream input) throws IOException {
        this(input.readInt(), input.readUTF(), input.readLong());
    }

    public CachedContent(int id2, String key2, long length2) {
        this.id = id2;
        this.key = key2;
        this.length = length2;
        this.cachedSpans = new TreeSet<>();
    }

    public void writeToStream(DataOutputStream output) throws IOException {
        output.writeInt(this.id);
        output.writeUTF(this.key);
        output.writeLong(this.length);
    }

    public long getLength() {
        return this.length;
    }

    public void setLength(long length2) {
        this.length = length2;
    }

    public void addSpan(SimpleCacheSpan span) {
        this.cachedSpans.add(span);
    }

    public TreeSet<SimpleCacheSpan> getSpans() {
        return this.cachedSpans;
    }

    public SimpleCacheSpan getSpan(long position) {
        SimpleCacheSpan floorSpan;
        SimpleCacheSpan lookupSpan = SimpleCacheSpan.createLookup(this.key, position);
        SimpleCacheSpan floorSpan2 = this.cachedSpans.floor(lookupSpan);
        if (floorSpan2 != null && floorSpan2.position + floorSpan2.length > position) {
            return floorSpan2;
        }
        SimpleCacheSpan ceilSpan = this.cachedSpans.ceiling(lookupSpan);
        if (ceilSpan == null) {
            floorSpan = SimpleCacheSpan.createOpenHole(this.key, position);
        } else {
            floorSpan = SimpleCacheSpan.createClosedHole(this.key, position, ceilSpan.position - position);
        }
        return floorSpan;
    }

    public long getCachedBytes(long position, long length2) {
        long j;
        SimpleCacheSpan span = getSpan(position);
        if (span.isHoleSpan()) {
            if (span.isOpenEnded()) {
                j = Long.MAX_VALUE;
            } else {
                j = span.length;
            }
            return -Math.min(j, length2);
        }
        long queryEndPosition = position + length2;
        long currentEndPosition = span.position + span.length;
        if (currentEndPosition < queryEndPosition) {
            for (SimpleCacheSpan next : this.cachedSpans.tailSet(span, false)) {
                if (next.position <= currentEndPosition) {
                    currentEndPosition = Math.max(currentEndPosition, next.position + next.length);
                    if (currentEndPosition >= queryEndPosition) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return Math.min(currentEndPosition - position, length2);
    }

    public SimpleCacheSpan touch(SimpleCacheSpan cacheSpan) throws Cache.CacheException {
        Assertions.checkState(this.cachedSpans.remove(cacheSpan));
        SimpleCacheSpan newCacheSpan = cacheSpan.copyWithUpdatedLastAccessTime(this.id);
        if (!cacheSpan.file.renameTo(newCacheSpan.file)) {
            throw new Cache.CacheException("Renaming of " + cacheSpan.file + " to " + newCacheSpan.file + " failed.");
        }
        this.cachedSpans.add(newCacheSpan);
        return newCacheSpan;
    }

    public boolean isEmpty() {
        return this.cachedSpans.isEmpty();
    }

    public boolean removeSpan(CacheSpan span) {
        if (!this.cachedSpans.remove(span)) {
            return false;
        }
        span.file.delete();
        return true;
    }

    public int headerHashCode() {
        return (((this.id * 31) + this.key.hashCode()) * 31) + ((int) (this.length ^ (this.length >>> 32)));
    }
}
