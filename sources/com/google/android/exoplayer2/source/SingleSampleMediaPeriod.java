package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

final class SingleSampleMediaPeriod implements MediaPeriod, Loader.Callback<SourceLoadable> {
    private static final int INITIAL_SAMPLE_SIZE = 1024;
    private final DataSource.Factory dataSourceFactory;
    private final DataSpec dataSpec;
    private final long durationUs;
    private int errorCount;
    private final MediaSourceEventListener.EventDispatcher eventDispatcher;
    final Format format;
    final Loader loader = new Loader("Loader:SingleSampleMediaPeriod");
    boolean loadingFinished;
    boolean loadingSucceeded;
    private final int minLoadableRetryCount;
    byte[] sampleData;
    int sampleSize;
    private final ArrayList<SampleStreamImpl> sampleStreams = new ArrayList<>();
    private final TrackGroupArray tracks;
    final boolean treatLoadErrorsAsEndOfStream;

    public SingleSampleMediaPeriod(DataSpec dataSpec2, DataSource.Factory dataSourceFactory2, Format format2, long durationUs2, int minLoadableRetryCount2, MediaSourceEventListener.EventDispatcher eventDispatcher2, boolean treatLoadErrorsAsEndOfStream2) {
        this.dataSpec = dataSpec2;
        this.dataSourceFactory = dataSourceFactory2;
        this.format = format2;
        this.durationUs = durationUs2;
        this.minLoadableRetryCount = minLoadableRetryCount2;
        this.eventDispatcher = eventDispatcher2;
        this.treatLoadErrorsAsEndOfStream = treatLoadErrorsAsEndOfStream2;
        this.tracks = new TrackGroupArray(new TrackGroup(format2));
    }

    public void release() {
        this.loader.release();
    }

    public void prepare(MediaPeriod.Callback callback, long positionUs) {
        callback.onPrepared(this);
    }

    public void maybeThrowPrepareError() throws IOException {
    }

    public TrackGroupArray getTrackGroups() {
        return this.tracks;
    }

    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        for (int i = 0; i < selections.length; i++) {
            if (streams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
                this.sampleStreams.remove(streams[i]);
                streams[i] = null;
            }
            if (streams[i] == null && selections[i] != null) {
                SampleStreamImpl stream = new SampleStreamImpl();
                this.sampleStreams.add(stream);
                streams[i] = stream;
                streamResetFlags[i] = true;
            }
        }
        return positionUs;
    }

    public void discardBuffer(long positionUs) {
    }

    public boolean continueLoading(long positionUs) {
        if (this.loadingFinished || this.loader.isLoading()) {
            return false;
        }
        this.loader.startLoading(new SourceLoadable(this.dataSpec, this.dataSourceFactory.createDataSource()), this, this.minLoadableRetryCount);
        return true;
    }

    public long readDiscontinuity() {
        return C.TIME_UNSET;
    }

    public long getNextLoadPositionUs() {
        return (this.loadingFinished || this.loader.isLoading()) ? Long.MIN_VALUE : 0;
    }

    public long getBufferedPositionUs() {
        return this.loadingFinished ? Long.MIN_VALUE : 0;
    }

    public long seekToUs(long positionUs) {
        for (int i = 0; i < this.sampleStreams.size(); i++) {
            this.sampleStreams.get(i).seekToUs(positionUs);
        }
        return positionUs;
    }

    public void onLoadCompleted(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {
        this.eventDispatcher.loadCompleted(loadable.dataSpec, 1, -1, this.format, 0, (Object) null, 0, this.durationUs, elapsedRealtimeMs, loadDurationMs, (long) loadable.sampleSize);
        this.sampleSize = loadable.sampleSize;
        this.sampleData = loadable.sampleData;
        this.loadingFinished = true;
        this.loadingSucceeded = true;
    }

    public void onLoadCanceled(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        this.eventDispatcher.loadCanceled(loadable.dataSpec, 1, -1, (Format) null, 0, (Object) null, 0, this.durationUs, elapsedRealtimeMs, loadDurationMs, (long) loadable.sampleSize);
    }

    public int onLoadError(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error) {
        this.errorCount++;
        boolean cancel = this.treatLoadErrorsAsEndOfStream && this.errorCount >= this.minLoadableRetryCount;
        this.eventDispatcher.loadError(loadable.dataSpec, 1, -1, this.format, 0, (Object) null, 0, this.durationUs, elapsedRealtimeMs, loadDurationMs, (long) loadable.sampleSize, error, cancel);
        if (!cancel) {
            return 0;
        }
        this.loadingFinished = true;
        return 2;
    }

    private final class SampleStreamImpl implements SampleStream {
        private static final int STREAM_STATE_END_OF_STREAM = 2;
        private static final int STREAM_STATE_SEND_FORMAT = 0;
        private static final int STREAM_STATE_SEND_SAMPLE = 1;
        private int streamState;

        private SampleStreamImpl() {
        }

        public void seekToUs(long positionUs) {
            if (this.streamState == 2) {
                this.streamState = 1;
            }
        }

        public boolean isReady() {
            return SingleSampleMediaPeriod.this.loadingFinished;
        }

        public void maybeThrowError() throws IOException {
            if (!SingleSampleMediaPeriod.this.treatLoadErrorsAsEndOfStream) {
                SingleSampleMediaPeriod.this.loader.maybeThrowError();
            }
        }

        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean requireFormat) {
            if (this.streamState == 2) {
                buffer.addFlag(4);
                return -4;
            } else if (requireFormat || this.streamState == 0) {
                formatHolder.format = SingleSampleMediaPeriod.this.format;
                this.streamState = 1;
                return -5;
            } else if (!SingleSampleMediaPeriod.this.loadingFinished) {
                return -3;
            } else {
                if (SingleSampleMediaPeriod.this.loadingSucceeded) {
                    buffer.timeUs = 0;
                    buffer.addFlag(1);
                    buffer.ensureSpaceForWrite(SingleSampleMediaPeriod.this.sampleSize);
                    buffer.data.put(SingleSampleMediaPeriod.this.sampleData, 0, SingleSampleMediaPeriod.this.sampleSize);
                } else {
                    buffer.addFlag(4);
                }
                this.streamState = 2;
                return -4;
            }
        }

        public int skipData(long positionUs) {
            if (positionUs <= 0 || this.streamState == 2) {
                return 0;
            }
            this.streamState = 2;
            return 1;
        }
    }

    static final class SourceLoadable implements Loader.Loadable {
        private final DataSource dataSource;
        public final DataSpec dataSpec;
        /* access modifiers changed from: private */
        public byte[] sampleData;
        /* access modifiers changed from: private */
        public int sampleSize;

        public SourceLoadable(DataSpec dataSpec2, DataSource dataSource2) {
            this.dataSpec = dataSpec2;
            this.dataSource = dataSource2;
        }

        public void cancelLoad() {
        }

        public boolean isLoadCanceled() {
            return false;
        }

        public void load() throws IOException, InterruptedException {
            this.sampleSize = 0;
            try {
                this.dataSource.open(this.dataSpec);
                int result = 0;
                while (result != -1) {
                    this.sampleSize += result;
                    if (this.sampleData == null) {
                        this.sampleData = new byte[1024];
                    } else if (this.sampleSize == this.sampleData.length) {
                        this.sampleData = Arrays.copyOf(this.sampleData, this.sampleData.length * 2);
                    }
                    result = this.dataSource.read(this.sampleData, this.sampleSize, this.sampleData.length - this.sampleSize);
                }
            } finally {
                Util.closeQuietly(this.dataSource);
            }
        }
    }
}
