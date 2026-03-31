package com.google.android.exoplayer2.upstream.cache;

import android.util.SparseArray;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.AtomicFile;
import com.google.android.exoplayer2.util.ReusableBufferedOutputStream;
import com.google.android.exoplayer2.util.Util;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class CachedContentIndex {
    public static final String FILE_NAME = "cached_content_index.exi";
    private static final int FLAG_ENCRYPTED_INDEX = 1;
    private static final String TAG = "CachedContentIndex";
    private static final int VERSION = 1;
    private final AtomicFile atomicFile;
    private ReusableBufferedOutputStream bufferedOutputStream;
    private boolean changed;
    private final Cipher cipher;
    private final boolean encrypt;
    private final SparseArray<String> idToKey;
    private final HashMap<String, CachedContent> keyToContent;
    private final SecretKeySpec secretKeySpec;

    public CachedContentIndex(File cacheDir) {
        this(cacheDir, (byte[]) null);
    }

    /* JADX INFO: this call moved to the top of the method (can break code semantics) */
    public CachedContentIndex(File cacheDir, byte[] secretKey) {
        this(cacheDir, secretKey, secretKey != null);
    }

    public CachedContentIndex(File cacheDir, byte[] secretKey, boolean encrypt2) {
        boolean z = true;
        this.encrypt = encrypt2;
        if (secretKey != null) {
            Assertions.checkArgument(secretKey.length != 16 ? false : z);
            try {
                this.cipher = getCipher();
                this.secretKeySpec = new SecretKeySpec(secretKey, "AES");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new IllegalStateException(e);
            }
        } else {
            Assertions.checkState(encrypt2 ? false : z);
            this.cipher = null;
            this.secretKeySpec = null;
        }
        this.keyToContent = new HashMap<>();
        this.idToKey = new SparseArray<>();
        this.atomicFile = new AtomicFile(new File(cacheDir, FILE_NAME));
    }

    public void load() {
        Assertions.checkState(!this.changed);
        if (!readFile()) {
            this.atomicFile.delete();
            this.keyToContent.clear();
            this.idToKey.clear();
        }
    }

    public void store() throws Cache.CacheException {
        if (this.changed) {
            writeFile();
            this.changed = false;
        }
    }

    public CachedContent add(String key) {
        CachedContent cachedContent = this.keyToContent.get(key);
        if (cachedContent == null) {
            return addNew(key, -1);
        }
        return cachedContent;
    }

    public CachedContent get(String key) {
        return this.keyToContent.get(key);
    }

    public Collection<CachedContent> getAll() {
        return this.keyToContent.values();
    }

    public int assignIdForKey(String key) {
        return add(key).id;
    }

    public String getKeyForId(int id) {
        return this.idToKey.get(id);
    }

    public void removeEmpty(String key) {
        CachedContent cachedContent = this.keyToContent.remove(key);
        if (cachedContent != null) {
            Assertions.checkState(cachedContent.isEmpty());
            this.idToKey.remove(cachedContent.id);
            this.changed = true;
        }
    }

    public void removeEmpty() {
        ArrayList<String> cachedContentToBeRemoved = new ArrayList<>();
        for (CachedContent cachedContent : this.keyToContent.values()) {
            if (cachedContent.isEmpty()) {
                cachedContentToBeRemoved.add(cachedContent.key);
            }
        }
        for (int i = 0; i < cachedContentToBeRemoved.size(); i++) {
            removeEmpty(cachedContentToBeRemoved.get(i));
        }
    }

    public Set<String> getKeys() {
        return this.keyToContent.keySet();
    }

    public void setContentLength(String key, long length) {
        CachedContent cachedContent = get(key);
        if (cachedContent == null) {
            addNew(key, length);
        } else if (cachedContent.getLength() != length) {
            cachedContent.setLength(length);
            this.changed = true;
        }
    }

    public long getContentLength(String key) {
        CachedContent cachedContent = get(key);
        if (cachedContent == null) {
            return -1;
        }
        return cachedContent.getLength();
    }

    /* JADX WARNING: Removed duplicated region for block: B:62:0x00a5  */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x00ad  */
    /* JADX WARNING: Removed duplicated region for block: B:80:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean readFile() {
        /*
            r15 = this;
            r7 = 0
            java.io.BufferedInputStream r9 = new java.io.BufferedInputStream     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            com.google.android.exoplayer2.util.AtomicFile r12 = r15.atomicFile     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            java.io.InputStream r12 = r12.openRead()     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            r9.<init>(r12)     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            java.io.DataInputStream r8 = new java.io.DataInputStream     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            r8.<init>(r9)     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            int r11 = r8.readInt()     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            r12 = 1
            if (r11 == r12) goto L_0x0020
            r12 = 0
            if (r8 == 0) goto L_0x001e
            com.google.android.exoplayer2.util.Util.closeQuietly((java.io.Closeable) r8)
        L_0x001e:
            r7 = r8
        L_0x001f:
            return r12
        L_0x0020:
            int r3 = r8.readInt()     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            r12 = r3 & 1
            if (r12 == 0) goto L_0x007d
            javax.crypto.Cipher r12 = r15.cipher     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            if (r12 != 0) goto L_0x0034
            r12 = 0
            if (r8 == 0) goto L_0x0032
            com.google.android.exoplayer2.util.Util.closeQuietly((java.io.Closeable) r8)
        L_0x0032:
            r7 = r8
            goto L_0x001f
        L_0x0034:
            r12 = 16
            byte[] r6 = new byte[r12]     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            r8.readFully(r6)     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            javax.crypto.spec.IvParameterSpec r10 = new javax.crypto.spec.IvParameterSpec     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            r10.<init>(r6)     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            javax.crypto.Cipher r12 = r15.cipher     // Catch:{ InvalidKeyException -> 0x006c, InvalidAlgorithmParameterException -> 0x00b1 }
            r13 = 2
            javax.crypto.spec.SecretKeySpec r14 = r15.secretKeySpec     // Catch:{ InvalidKeyException -> 0x006c, InvalidAlgorithmParameterException -> 0x00b1 }
            r12.init(r13, r14, r10)     // Catch:{ InvalidKeyException -> 0x006c, InvalidAlgorithmParameterException -> 0x00b1 }
            java.io.DataInputStream r7 = new java.io.DataInputStream     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            javax.crypto.CipherInputStream r12 = new javax.crypto.CipherInputStream     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            javax.crypto.Cipher r13 = r15.cipher     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            r12.<init>(r9, r13)     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            r7.<init>(r12)     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
        L_0x0054:
            int r1 = r7.readInt()     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            r4 = 0
            r5 = 0
        L_0x005a:
            if (r5 >= r1) goto L_0x0086
            com.google.android.exoplayer2.upstream.cache.CachedContent r0 = new com.google.android.exoplayer2.upstream.cache.CachedContent     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            r0.<init>(r7)     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            r15.add((com.google.android.exoplayer2.upstream.cache.CachedContent) r0)     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            int r12 = r0.headerHashCode()     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            int r4 = r4 + r12
            int r5 = r5 + 1
            goto L_0x005a
        L_0x006c:
            r12 = move-exception
            r2 = r12
        L_0x006e:
            java.lang.IllegalStateException r12 = new java.lang.IllegalStateException     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            r12.<init>(r2)     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            throw r12     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
        L_0x0074:
            r2 = move-exception
            r7 = r8
        L_0x0076:
            r12 = 0
            if (r7 == 0) goto L_0x001f
            com.google.android.exoplayer2.util.Util.closeQuietly((java.io.Closeable) r7)
            goto L_0x001f
        L_0x007d:
            boolean r12 = r15.encrypt     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
            if (r12 == 0) goto L_0x0084
            r12 = 1
            r15.changed = r12     // Catch:{ FileNotFoundException -> 0x0074, IOException -> 0x00b7, all -> 0x00b4 }
        L_0x0084:
            r7 = r8
            goto L_0x0054
        L_0x0086:
            int r12 = r7.readInt()     // Catch:{ FileNotFoundException -> 0x00ba, IOException -> 0x009a }
            if (r12 == r4) goto L_0x0093
            r12 = 0
            if (r7 == 0) goto L_0x001f
            com.google.android.exoplayer2.util.Util.closeQuietly((java.io.Closeable) r7)
            goto L_0x001f
        L_0x0093:
            if (r7 == 0) goto L_0x0098
            com.google.android.exoplayer2.util.Util.closeQuietly((java.io.Closeable) r7)
        L_0x0098:
            r12 = 1
            goto L_0x001f
        L_0x009a:
            r2 = move-exception
        L_0x009b:
            java.lang.String r12 = "CachedContentIndex"
            java.lang.String r13 = "Error reading cache content index file."
            android.util.Log.e(r12, r13, r2)     // Catch:{ all -> 0x00aa }
            r12 = 0
            if (r7 == 0) goto L_0x001f
            com.google.android.exoplayer2.util.Util.closeQuietly((java.io.Closeable) r7)
            goto L_0x001f
        L_0x00aa:
            r12 = move-exception
        L_0x00ab:
            if (r7 == 0) goto L_0x00b0
            com.google.android.exoplayer2.util.Util.closeQuietly((java.io.Closeable) r7)
        L_0x00b0:
            throw r12
        L_0x00b1:
            r12 = move-exception
            r2 = r12
            goto L_0x006e
        L_0x00b4:
            r12 = move-exception
            r7 = r8
            goto L_0x00ab
        L_0x00b7:
            r2 = move-exception
            r7 = r8
            goto L_0x009b
        L_0x00ba:
            r2 = move-exception
            goto L_0x0076
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.upstream.cache.CachedContentIndex.readFile():boolean");
    }

    private void writeFile() throws Cache.CacheException {
        GeneralSecurityException e;
        int flags = 1;
        DataOutputStream output = null;
        try {
            OutputStream outputStream = this.atomicFile.startWrite();
            if (this.bufferedOutputStream == null) {
                this.bufferedOutputStream = new ReusableBufferedOutputStream(outputStream);
            } else {
                this.bufferedOutputStream.reset(outputStream);
            }
            DataOutputStream output2 = new DataOutputStream(this.bufferedOutputStream);
            try {
                output2.writeInt(1);
                if (!this.encrypt) {
                    flags = 0;
                }
                output2.writeInt(flags);
                if (this.encrypt) {
                    byte[] initializationVector = new byte[16];
                    new Random().nextBytes(initializationVector);
                    output2.write(initializationVector);
                    try {
                        this.cipher.init(1, this.secretKeySpec, new IvParameterSpec(initializationVector));
                        output2.flush();
                        output = new DataOutputStream(new CipherOutputStream(this.bufferedOutputStream, this.cipher));
                    } catch (InvalidKeyException e2) {
                        e = e2;
                        throw new IllegalStateException(e);
                    } catch (InvalidAlgorithmParameterException e3) {
                        e = e3;
                        throw new IllegalStateException(e);
                    }
                } else {
                    output = output2;
                }
                output.writeInt(this.keyToContent.size());
                int hashCode = 0;
                for (CachedContent cachedContent : this.keyToContent.values()) {
                    cachedContent.writeToStream(output);
                    hashCode += cachedContent.headerHashCode();
                }
                output.writeInt(hashCode);
                this.atomicFile.endWrite(output);
                Util.closeQuietly((Closeable) null);
            } catch (IOException e4) {
                e = e4;
                output = output2;
                try {
                    throw new Cache.CacheException((Throwable) e);
                } catch (Throwable th) {
                    th = th;
                    Util.closeQuietly((Closeable) output);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                output = output2;
                Util.closeQuietly((Closeable) output);
                throw th;
            }
        } catch (IOException e5) {
            e = e5;
            throw new Cache.CacheException((Throwable) e);
        }
    }

    private void add(CachedContent cachedContent) {
        this.keyToContent.put(cachedContent.key, cachedContent);
        this.idToKey.put(cachedContent.id, cachedContent.key);
    }

    /* access modifiers changed from: package-private */
    public void addNew(CachedContent cachedContent) {
        add(cachedContent);
        this.changed = true;
    }

    private CachedContent addNew(String key, long length) {
        CachedContent cachedContent = new CachedContent(getNewId(this.idToKey), key, length);
        addNew(cachedContent);
        return cachedContent;
    }

    private static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (Util.SDK_INT == 18) {
            try {
                return Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
            } catch (Throwable th) {
            }
        }
        return Cipher.getInstance("AES/CBC/PKCS5PADDING");
    }

    public static int getNewId(SparseArray<String> idToKey2) {
        int size = idToKey2.size();
        int id = size == 0 ? 0 : idToKey2.keyAt(size - 1) + 1;
        if (id < 0) {
            id = 0;
            while (id < size && id == idToKey2.keyAt(id)) {
                id++;
            }
        }
        return id;
    }
}
