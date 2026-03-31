package com.google.android.exoplayer2.upstream;

import android.content.Context;
import com.google.android.exoplayer2.upstream.DataSource;

public final class DefaultDataSourceFactory implements DataSource.Factory {
    private final DataSource.Factory baseDataSourceFactory;
    private final Context context;
    private final TransferListener<? super DataSource> listener;

    public DefaultDataSourceFactory(Context context2, String userAgent) {
        this(context2, userAgent, (TransferListener<? super DataSource>) null);
    }

    public DefaultDataSourceFactory(Context context2, String userAgent, TransferListener<? super DataSource> listener2) {
        this(context2, listener2, (DataSource.Factory) new DefaultHttpDataSourceFactory(userAgent, listener2));
    }

    public DefaultDataSourceFactory(Context context2, TransferListener<? super DataSource> listener2, DataSource.Factory baseDataSourceFactory2) {
        this.context = context2.getApplicationContext();
        this.listener = listener2;
        this.baseDataSourceFactory = baseDataSourceFactory2;
    }

    public DefaultDataSource createDataSource() {
        return new DefaultDataSource(this.context, this.listener, this.baseDataSourceFactory.createDataSource());
    }
}
