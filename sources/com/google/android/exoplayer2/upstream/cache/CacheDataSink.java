package com.google.android.exoplayer2.upstream.cache;

import com.google.android.exoplayer2.upstream.DataSink;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ReusableBufferedOutputStream;
import com.google.android.exoplayer2.util.Util;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class CacheDataSink implements DataSink {
    public static final int DEFAULT_BUFFER_SIZE = 20480;
    private final int bufferSize;
    private ReusableBufferedOutputStream bufferedOutputStream;
    private final Cache cache;
    private DataSpec dataSpec;
    private long dataSpecBytesWritten;
    private File file;
    private final long maxCacheFileSize;
    private OutputStream outputStream;
    private long outputStreamBytesWritten;
    private FileOutputStream underlyingFileOutputStream;

    public static class CacheDataSinkException extends Cache.CacheException {
        public CacheDataSinkException(IOException cause) {
            super((Throwable) cause);
        }
    }

    public CacheDataSink(Cache cache2, long maxCacheFileSize2) {
        this(cache2, maxCacheFileSize2, DEFAULT_BUFFER_SIZE);
    }

    public CacheDataSink(Cache cache2, long maxCacheFileSize2, int bufferSize2) {
        this.cache = (Cache) Assertions.checkNotNull(cache2);
        this.maxCacheFileSize = maxCacheFileSize2;
        this.bufferSize = bufferSize2;
    }

    public void open(DataSpec dataSpec2) throws CacheDataSinkException {
        if (dataSpec2.length != -1 || dataSpec2.isFlagSet(2)) {
            this.dataSpec = dataSpec2;
            this.dataSpecBytesWritten = 0;
            try {
                openNextOutputStream();
            } catch (IOException e) {
                throw new CacheDataSinkException(e);
            }
        } else {
            this.dataSpec = null;
        }
    }

    public void write(byte[] buffer, int offset, int length) throws CacheDataSinkException {
        if (this.dataSpec != null) {
            int bytesWritten = 0;
            while (bytesWritten < length) {
                try {
                    if (this.outputStreamBytesWritten == this.maxCacheFileSize) {
                        closeCurrentOutputStream();
                        openNextOutputStream();
                    }
                    int bytesToWrite = (int) Math.min((long) (length - bytesWritten), this.maxCacheFileSize - this.outputStreamBytesWritten);
                    this.outputStream.write(buffer, offset + bytesWritten, bytesToWrite);
                    bytesWritten += bytesToWrite;
                    this.outputStreamBytesWritten += (long) bytesToWrite;
                    this.dataSpecBytesWritten += (long) bytesToWrite;
                } catch (IOException e) {
                    throw new CacheDataSinkException(e);
                }
            }
        }
    }

    public void close() throws CacheDataSinkException {
        if (this.dataSpec != null) {
            try {
                closeCurrentOutputStream();
            } catch (IOException e) {
                throw new CacheDataSinkException(e);
            }
        }
    }

    private void openNextOutputStream() throws IOException {
        long maxLength;
        if (this.dataSpec.length == -1) {
            maxLength = this.maxCacheFileSize;
        } else {
            maxLength = Math.min(this.dataSpec.length - this.dataSpecBytesWritten, this.maxCacheFileSize);
        }
        this.file = this.cache.startFile(this.dataSpec.key, this.dataSpec.absoluteStreamPosition + this.dataSpecBytesWritten, maxLength);
        this.underlyingFileOutputStream = new FileOutputStream(this.file);
        if (this.bufferSize > 0) {
            if (this.bufferedOutputStream == null) {
                this.bufferedOutputStream = new ReusableBufferedOutputStream(this.underlyingFileOutputStream, this.bufferSize);
            } else {
                this.bufferedOutputStream.reset(this.underlyingFileOutputStream);
            }
            this.outputStream = this.bufferedOutputStream;
        } else {
            this.outputStream = this.underlyingFileOutputStream;
        }
        this.outputStreamBytesWritten = 0;
    }

    private void closeCurrentOutputStream() throws IOException {
        File fileToCommit;
        if (this.outputStream != null) {
            boolean success = false;
            try {
                this.outputStream.flush();
                this.underlyingFileOutputStream.getFD().sync();
                success = true;
                if (!success) {
                    fileToCommit.delete();
                }
            } finally {
                Util.closeQuietly((Closeable) this.outputStream);
                this.outputStream = null;
                fileToCommit = this.file;
                this.file = null;
                if (success) {
                    this.cache.commitFile(fileToCommit);
                } else {
                    fileToCommit.delete();
                }
            }
        }
    }
}
