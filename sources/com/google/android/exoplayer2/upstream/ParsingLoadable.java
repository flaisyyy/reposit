package com.google.android.exoplayer2.upstream;

import android.net.Uri;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.util.Util;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public final class ParsingLoadable<T> implements Loader.Loadable {
    private volatile long bytesLoaded;
    private final DataSource dataSource;
    public final DataSpec dataSpec;
    private volatile boolean isCanceled;
    private final Parser<? extends T> parser;
    private volatile T result;
    public final int type;

    public interface Parser<T> {
        T parse(Uri uri, InputStream inputStream) throws IOException;
    }

    public ParsingLoadable(DataSource dataSource2, Uri uri, int type2, Parser<? extends T> parser2) {
        this(dataSource2, new DataSpec(uri, 1), type2, parser2);
    }

    public ParsingLoadable(DataSource dataSource2, DataSpec dataSpec2, int type2, Parser<? extends T> parser2) {
        this.dataSource = dataSource2;
        this.dataSpec = dataSpec2;
        this.type = type2;
        this.parser = parser2;
    }

    public final T getResult() {
        return this.result;
    }

    public long bytesLoaded() {
        return this.bytesLoaded;
    }

    public final void cancelLoad() {
        this.isCanceled = true;
    }

    public final boolean isLoadCanceled() {
        return this.isCanceled;
    }

    public final void load() throws IOException {
        DataSourceInputStream inputStream = new DataSourceInputStream(this.dataSource, this.dataSpec);
        try {
            inputStream.open();
            this.result = this.parser.parse(this.dataSource.getUri(), inputStream);
        } finally {
            this.bytesLoaded = inputStream.bytesRead();
            Util.closeQuietly((Closeable) inputStream);
        }
    }
}
