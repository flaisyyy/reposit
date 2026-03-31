package com.google.android.exoplayer2.upstream.cache;

import android.os.ConditionVariable;
import android.util.Log;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.util.Assertions;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public final class SimpleCache implements Cache {
    private static final String TAG = "SimpleCache";
    private final File cacheDir;
    /* access modifiers changed from: private */
    public final CacheEvictor evictor;
    private final CachedContentIndex index;
    private final HashMap<String, ArrayList<Cache.Listener>> listeners;
    private final HashMap<String, CacheSpan> lockedSpans;
    private long totalSpace;

    public SimpleCache(File cacheDir2, CacheEvictor evictor2) {
        this(cacheDir2, evictor2, (byte[]) null, false);
    }

    /* JADX INFO: this call moved to the top of the method (can break code semantics) */
    public SimpleCache(File cacheDir2, CacheEvictor evictor2, byte[] secretKey) {
        this(cacheDir2, evictor2, secretKey, secretKey != null);
    }

    public SimpleCache(File cacheDir2, CacheEvictor evictor2, byte[] secretKey, boolean encrypt) {
        this(cacheDir2, evictor2, new CachedContentIndex(cacheDir2, secretKey, encrypt));
    }

    SimpleCache(File cacheDir2, CacheEvictor evictor2, CachedContentIndex index2) {
        this.totalSpace = 0;
        this.cacheDir = cacheDir2;
        this.evictor = evictor2;
        this.lockedSpans = new HashMap<>();
        this.index = index2;
        this.listeners = new HashMap<>();
        final ConditionVariable conditionVariable = new ConditionVariable();
        new Thread("SimpleCache.initialize()") {
            public void run() {
                synchronized (SimpleCache.this) {
                    conditionVariable.open();
                    SimpleCache.this.initialize();
                    SimpleCache.this.evictor.onCacheInitialized();
                }
            }
        }.start();
        conditionVariable.block();
    }

    public synchronized NavigableSet<CacheSpan> addListener(String key, Cache.Listener listener) {
        ArrayList<Cache.Listener> listenersForKey = this.listeners.get(key);
        if (listenersForKey == null) {
            listenersForKey = new ArrayList<>();
            this.listeners.put(key, listenersForKey);
        }
        listenersForKey.add(listener);
        return getCachedSpans(key);
    }

    public synchronized void removeListener(String key, Cache.Listener listener) {
        ArrayList<Cache.Listener> listenersForKey = this.listeners.get(key);
        if (listenersForKey != null) {
            listenersForKey.remove(listener);
            if (listenersForKey.isEmpty()) {
                this.listeners.remove(key);
            }
        }
    }

    public synchronized NavigableSet<CacheSpan> getCachedSpans(String key) {
        TreeSet treeSet;
        CachedContent cachedContent = this.index.get(key);
        if (cachedContent == null || cachedContent.isEmpty()) {
            treeSet = null;
        } else {
            treeSet = new TreeSet(cachedContent.getSpans());
        }
        return treeSet;
    }

    public synchronized Set<String> getKeys() {
        return new HashSet(this.index.getKeys());
    }

    public synchronized long getCacheSpace() {
        return this.totalSpace;
    }

    public synchronized SimpleCacheSpan startReadWrite(String key, long position) throws InterruptedException, Cache.CacheException {
        SimpleCacheSpan span;
        while (true) {
            span = startReadWriteNonBlocking(key, position);
            if (span == null) {
                wait();
            }
        }
        return span;
    }

    public synchronized SimpleCacheSpan startReadWriteNonBlocking(String key, long position) throws Cache.CacheException {
        SimpleCacheSpan newCacheSpan;
        SimpleCacheSpan cacheSpan = getSpan(key, position);
        if (cacheSpan.isCached) {
            newCacheSpan = this.index.get(key).touch(cacheSpan);
            notifySpanTouched(cacheSpan, newCacheSpan);
        } else if (!this.lockedSpans.containsKey(key)) {
            this.lockedSpans.put(key, cacheSpan);
            newCacheSpan = cacheSpan;
        } else {
            newCacheSpan = null;
        }
        return newCacheSpan;
    }

    public synchronized File startFile(String key, long position, long maxLength) throws Cache.CacheException {
        Assertions.checkState(this.lockedSpans.containsKey(key));
        if (!this.cacheDir.exists()) {
            removeStaleSpansAndCachedContents();
            this.cacheDir.mkdirs();
        }
        this.evictor.onStartFile(this, key, position, maxLength);
        return SimpleCacheSpan.getCacheFile(this.cacheDir, this.index.assignIdForKey(key), position, System.currentTimeMillis());
    }

    public synchronized void commitFile(File file) throws Cache.CacheException {
        boolean z = true;
        synchronized (this) {
            SimpleCacheSpan span = SimpleCacheSpan.createCacheEntry(file, this.index);
            Assertions.checkState(span != null);
            Assertions.checkState(this.lockedSpans.containsKey(span.key));
            if (file.exists()) {
                if (file.length() == 0) {
                    file.delete();
                } else {
                    Long length = Long.valueOf(getContentLength(span.key));
                    if (length.longValue() != -1) {
                        if (span.position + span.length > length.longValue()) {
                            z = false;
                        }
                        Assertions.checkState(z);
                    }
                    addSpan(span);
                    this.index.store();
                    notifyAll();
                }
            }
        }
    }

    public synchronized void releaseHoleSpan(CacheSpan holeSpan) {
        Assertions.checkState(holeSpan == this.lockedSpans.remove(holeSpan.key));
        notifyAll();
    }

    private SimpleCacheSpan getSpan(String key, long position) throws Cache.CacheException {
        CachedContent cachedContent = this.index.get(key);
        if (cachedContent == null) {
            return SimpleCacheSpan.createOpenHole(key, position);
        }
        while (true) {
            SimpleCacheSpan span = cachedContent.getSpan(position);
            if (!span.isCached || span.file.exists()) {
                return span;
            }
            removeStaleSpansAndCachedContents();
        }
    }

    /* access modifiers changed from: private */
    public void initialize() {
        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
            return;
        }
        this.index.load();
        File[] files = this.cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().equals(CachedContentIndex.FILE_NAME)) {
                    SimpleCacheSpan span = file.length() > 0 ? SimpleCacheSpan.createCacheEntry(file, this.index) : null;
                    if (span != null) {
                        addSpan(span);
                    } else {
                        file.delete();
                    }
                }
            }
            this.index.removeEmpty();
            try {
                this.index.store();
            } catch (Cache.CacheException e) {
                Log.e(TAG, "Storing index file failed", e);
            }
        }
    }

    private void addSpan(SimpleCacheSpan span) {
        this.index.add(span.key).addSpan(span);
        this.totalSpace += span.length;
        notifySpanAdded(span);
    }

    private void removeSpan(CacheSpan span, boolean removeEmptyCachedContent) throws Cache.CacheException {
        CachedContent cachedContent = this.index.get(span.key);
        if (cachedContent != null && cachedContent.removeSpan(span)) {
            this.totalSpace -= span.length;
            if (removeEmptyCachedContent) {
                try {
                    if (cachedContent.isEmpty()) {
                        this.index.removeEmpty(cachedContent.key);
                        this.index.store();
                    }
                } catch (Throwable th) {
                    notifySpanRemoved(span);
                    throw th;
                }
            }
            notifySpanRemoved(span);
        }
    }

    public synchronized void removeSpan(CacheSpan span) throws Cache.CacheException {
        removeSpan(span, true);
    }

    private void removeStaleSpansAndCachedContents() throws Cache.CacheException {
        ArrayList<CacheSpan> spansToBeRemoved = new ArrayList<>();
        for (CachedContent cachedContent : this.index.getAll()) {
            Iterator<SimpleCacheSpan> it = cachedContent.getSpans().iterator();
            while (it.hasNext()) {
                CacheSpan span = it.next();
                if (!span.file.exists()) {
                    spansToBeRemoved.add(span);
                }
            }
        }
        for (int i = 0; i < spansToBeRemoved.size(); i++) {
            removeSpan(spansToBeRemoved.get(i), false);
        }
        this.index.removeEmpty();
        this.index.store();
    }

    private void notifySpanRemoved(CacheSpan span) {
        ArrayList<Cache.Listener> keyListeners = this.listeners.get(span.key);
        if (keyListeners != null) {
            for (int i = keyListeners.size() - 1; i >= 0; i--) {
                keyListeners.get(i).onSpanRemoved(this, span);
            }
        }
        this.evictor.onSpanRemoved(this, span);
    }

    private void notifySpanAdded(SimpleCacheSpan span) {
        ArrayList<Cache.Listener> keyListeners = this.listeners.get(span.key);
        if (keyListeners != null) {
            for (int i = keyListeners.size() - 1; i >= 0; i--) {
                keyListeners.get(i).onSpanAdded(this, span);
            }
        }
        this.evictor.onSpanAdded(this, span);
    }

    private void notifySpanTouched(SimpleCacheSpan oldSpan, CacheSpan newSpan) {
        ArrayList<Cache.Listener> keyListeners = this.listeners.get(oldSpan.key);
        if (keyListeners != null) {
            for (int i = keyListeners.size() - 1; i >= 0; i--) {
                keyListeners.get(i).onSpanTouched(this, oldSpan, newSpan);
            }
        }
        this.evictor.onSpanTouched(this, oldSpan, newSpan);
    }

    public synchronized boolean isCached(String key, long position, long length) {
        CachedContent cachedContent;
        cachedContent = this.index.get(key);
        return cachedContent != null && cachedContent.getCachedBytes(position, length) >= length;
    }

    public synchronized long getCachedBytes(String key, long position, long length) {
        CachedContent cachedContent;
        cachedContent = this.index.get(key);
        return cachedContent != null ? cachedContent.getCachedBytes(position, length) : -length;
    }

    public synchronized void setContentLength(String key, long length) throws Cache.CacheException {
        this.index.setContentLength(key, length);
        this.index.store();
    }

    public synchronized long getContentLength(String key) {
        return this.index.getContentLength(key);
    }
}
