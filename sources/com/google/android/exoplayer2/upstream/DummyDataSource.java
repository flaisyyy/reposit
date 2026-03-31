package com.google.android.exoplayer2.upstream;

import android.net.Uri;
import com.google.android.exoplayer2.upstream.DataSource;
import java.io.IOException;

public final class DummyDataSource implements DataSource {
    public static final DataSource.Factory FACTORY = new DataSource.Factory() {
        public DataSource createDataSource() {
            return new DummyDataSource();
        }
    };
    public static final DummyDataSource INSTANCE = new DummyDataSource();

    private DummyDataSource() {
    }

    public long open(DataSpec dataSpec) throws IOException {
        throw new IOException("Dummy source");
    }

    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        throw new UnsupportedOperationException();
    }

    public Uri getUri() {
        return null;
    }

    public void close() throws IOException {
    }
}
