package com.google.android.exoplayer2.source;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.SampleQueue;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ConditionVariable;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

final class ExtractorMediaPeriod implements MediaPeriod, ExtractorOutput, Loader.Callback<ExtractingLoadable>, Loader.ReleaseCallback, SampleQueue.UpstreamFormatChangedListener {
    private static final long DEFAULT_LAST_SAMPLE_DURATION_US = 10000;
    private int actualMinLoadableRetryCount;
    private final Allocator allocator;
    /* access modifiers changed from: private */
    public MediaPeriod.Callback callback;
    /* access modifiers changed from: private */
    public final long continueLoadingCheckIntervalBytes;
    /* access modifiers changed from: private */
    @Nullable
    public final String customCacheKey;
    private final DataSource dataSource;
    private long durationUs;
    private int enabledTrackCount;
    private final MediaSourceEventListener.EventDispatcher eventDispatcher;
    private int extractedSamplesCountAtStartOfLoad;
    private final ExtractorHolder extractorHolder;
    /* access modifiers changed from: private */
    public final Handler handler;
    private boolean haveAudioVideoTracks;
    private long lastSeekPositionUs;
    private long length;
    private final Listener listener;
    private final ConditionVariable loadCondition;
    private final Loader loader = new Loader("Loader:ExtractorMediaPeriod");
    private boolean loadingFinished;
    private final Runnable maybeFinishPrepareRunnable;
    private final int minLoadableRetryCount;
    private boolean notifyDiscontinuity;
    /* access modifiers changed from: private */
    public final Runnable onContinueLoadingRequestedRunnable;
    private long pendingResetPositionUs;
    private boolean prepared;
    /* access modifiers changed from: private */
    public boolean released;
    private int[] sampleQueueTrackIds;
    private SampleQueue[] sampleQueues;
    private boolean sampleQueuesBuilt;
    private SeekMap seekMap;
    private boolean seenFirstTrackSelection;
    private boolean[] trackEnabledStates;
    private boolean[] trackIsAudioVideoFlags;
    private TrackGroupArray tracks;
    private final Uri uri;

    interface Listener {
        void onSourceInfoRefreshed(long j, boolean z);
    }

    public ExtractorMediaPeriod(Uri uri2, DataSource dataSource2, Extractor[] extractors, int minLoadableRetryCount2, MediaSourceEventListener.EventDispatcher eventDispatcher2, Listener listener2, Allocator allocator2, @Nullable String customCacheKey2, int continueLoadingCheckIntervalBytes2) {
        this.uri = uri2;
        this.dataSource = dataSource2;
        this.minLoadableRetryCount = minLoadableRetryCount2;
        this.eventDispatcher = eventDispatcher2;
        this.listener = listener2;
        this.allocator = allocator2;
        this.customCacheKey = customCacheKey2;
        this.continueLoadingCheckIntervalBytes = (long) continueLoadingCheckIntervalBytes2;
        this.extractorHolder = new ExtractorHolder(extractors, this);
        this.loadCondition = new ConditionVariable();
        this.maybeFinishPrepareRunnable = new Runnable() {
            public void run() {
                ExtractorMediaPeriod.this.maybeFinishPrepare();
            }
        };
        this.onContinueLoadingRequestedRunnable = new Runnable() {
            public void run() {
                if (!ExtractorMediaPeriod.this.released) {
                    ExtractorMediaPeriod.this.callback.onContinueLoadingRequested(ExtractorMediaPeriod.this);
                }
            }
        };
        this.handler = new Handler();
        this.sampleQueueTrackIds = new int[0];
        this.sampleQueues = new SampleQueue[0];
        this.pendingResetPositionUs = C.TIME_UNSET;
        this.length = -1;
        this.actualMinLoadableRetryCount = minLoadableRetryCount2 == -1 ? 3 : minLoadableRetryCount2;
    }

    public void release() {
        boolean releasedSynchronously = this.loader.release(this);
        if (this.prepared && !releasedSynchronously) {
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.discardToEnd();
            }
        }
        this.handler.removeCallbacksAndMessages((Object) null);
        this.released = true;
    }

    public void onLoaderReleased() {
        this.extractorHolder.release();
        for (SampleQueue sampleQueue : this.sampleQueues) {
            sampleQueue.reset();
        }
    }

    public void prepare(MediaPeriod.Callback callback2, long positionUs) {
        this.callback = callback2;
        this.loadCondition.open();
        startLoading();
    }

    public void maybeThrowPrepareError() throws IOException {
        maybeThrowError();
    }

    public TrackGroupArray getTrackGroups() {
        return this.tracks;
    }

    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        Assertions.checkState(this.prepared);
        int oldEnabledTrackCount = this.enabledTrackCount;
        for (int i = 0; i < selections.length; i++) {
            if (streams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
                int track = streams[i].track;
                Assertions.checkState(this.trackEnabledStates[track]);
                this.enabledTrackCount--;
                this.trackEnabledStates[track] = false;
                streams[i] = null;
            }
        }
        boolean seekRequired = this.seenFirstTrackSelection ? oldEnabledTrackCount == 0 : positionUs != 0;
        for (int i2 = 0; i2 < selections.length; i2++) {
            if (streams[i2] == null && selections[i2] != null) {
                TrackSelection selection = selections[i2];
                Assertions.checkState(selection.length() == 1);
                Assertions.checkState(selection.getIndexInTrackGroup(0) == 0);
                int track2 = this.tracks.indexOf(selection.getTrackGroup());
                Assertions.checkState(!this.trackEnabledStates[track2]);
                this.enabledTrackCount++;
                this.trackEnabledStates[track2] = true;
                streams[i2] = new SampleStreamImpl(track2);
                streamResetFlags[i2] = true;
                if (!seekRequired) {
                    SampleQueue sampleQueue = this.sampleQueues[track2];
                    sampleQueue.rewind();
                    if (sampleQueue.advanceTo(positionUs, true, true) != -1 || sampleQueue.getReadIndex() == 0) {
                        seekRequired = false;
                    } else {
                        seekRequired = true;
                    }
                }
            }
        }
        if (this.enabledTrackCount == 0) {
            this.notifyDiscontinuity = false;
            if (this.loader.isLoading()) {
                for (SampleQueue sampleQueue2 : this.sampleQueues) {
                    sampleQueue2.discardToEnd();
                }
                this.loader.cancelLoading();
            } else {
                for (SampleQueue sampleQueue3 : this.sampleQueues) {
                    sampleQueue3.reset();
                }
            }
        } else if (seekRequired) {
            positionUs = seekToUs(positionUs);
            for (int i3 = 0; i3 < streams.length; i3++) {
                if (streams[i3] != null) {
                    streamResetFlags[i3] = true;
                }
            }
        }
        this.seenFirstTrackSelection = true;
        return positionUs;
    }

    public void discardBuffer(long positionUs) {
        int trackCount = this.sampleQueues.length;
        for (int i = 0; i < trackCount; i++) {
            this.sampleQueues[i].discardTo(positionUs, false, this.trackEnabledStates[i]);
        }
    }

    public boolean continueLoading(long playbackPositionUs) {
        if (this.loadingFinished || (this.prepared && this.enabledTrackCount == 0)) {
            return false;
        }
        boolean open = this.loadCondition.open();
        if (this.loader.isLoading()) {
            return open;
        }
        startLoading();
        return true;
    }

    public long getNextLoadPositionUs() {
        if (this.enabledTrackCount == 0) {
            return Long.MIN_VALUE;
        }
        return getBufferedPositionUs();
    }

    public long readDiscontinuity() {
        if (!this.notifyDiscontinuity || (!this.loadingFinished && getExtractedSamplesCount() <= this.extractedSamplesCountAtStartOfLoad)) {
            return C.TIME_UNSET;
        }
        this.notifyDiscontinuity = false;
        return this.lastSeekPositionUs;
    }

    public long getBufferedPositionUs() {
        long largestQueuedTimestampUs;
        if (this.loadingFinished) {
            return Long.MIN_VALUE;
        }
        if (isPendingReset()) {
            return this.pendingResetPositionUs;
        }
        if (this.haveAudioVideoTracks) {
            largestQueuedTimestampUs = Long.MAX_VALUE;
            int trackCount = this.sampleQueues.length;
            for (int i = 0; i < trackCount; i++) {
                if (this.trackIsAudioVideoFlags[i]) {
                    largestQueuedTimestampUs = Math.min(largestQueuedTimestampUs, this.sampleQueues[i].getLargestQueuedTimestampUs());
                }
            }
        } else {
            largestQueuedTimestampUs = getLargestQueuedTimestampUs();
        }
        return largestQueuedTimestampUs == Long.MIN_VALUE ? this.lastSeekPositionUs : largestQueuedTimestampUs;
    }

    public long seekToUs(long positionUs) {
        if (!this.seekMap.isSeekable()) {
            positionUs = 0;
        }
        this.lastSeekPositionUs = positionUs;
        this.notifyDiscontinuity = false;
        if (isPendingReset() || !seekInsideBufferUs(positionUs)) {
            this.pendingResetPositionUs = positionUs;
            this.loadingFinished = false;
            if (this.loader.isLoading()) {
                this.loader.cancelLoading();
            } else {
                for (SampleQueue sampleQueue : this.sampleQueues) {
                    sampleQueue.reset();
                }
            }
        }
        return positionUs;
    }

    /* access modifiers changed from: package-private */
    public boolean isReady(int track) {
        return !suppressRead() && (this.loadingFinished || this.sampleQueues[track].hasNextSample());
    }

    /* access modifiers changed from: package-private */
    public void maybeThrowError() throws IOException {
        this.loader.maybeThrowError(this.actualMinLoadableRetryCount);
    }

    /* access modifiers changed from: package-private */
    public int readData(int track, FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired) {
        if (suppressRead()) {
            return -3;
        }
        return this.sampleQueues[track].read(formatHolder, buffer, formatRequired, this.loadingFinished, this.lastSeekPositionUs);
    }

    /* access modifiers changed from: package-private */
    public int skipData(int track, long positionUs) {
        if (suppressRead()) {
            return 0;
        }
        SampleQueue sampleQueue = this.sampleQueues[track];
        if (this.loadingFinished && positionUs > sampleQueue.getLargestQueuedTimestampUs()) {
            return sampleQueue.advanceToEnd();
        }
        int skipCount = sampleQueue.advanceTo(positionUs, true, true);
        if (skipCount == -1) {
            skipCount = 0;
        }
        return skipCount;
    }

    private boolean suppressRead() {
        return this.notifyDiscontinuity || isPendingReset();
    }

    public void onLoadCompleted(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {
        if (this.durationUs == C.TIME_UNSET) {
            long largestQueuedTimestampUs = getLargestQueuedTimestampUs();
            this.durationUs = largestQueuedTimestampUs == Long.MIN_VALUE ? 0 : DEFAULT_LAST_SAMPLE_DURATION_US + largestQueuedTimestampUs;
            this.listener.onSourceInfoRefreshed(this.durationUs, this.seekMap.isSeekable());
        }
        this.eventDispatcher.loadCompleted(loadable.dataSpec, 1, -1, (Format) null, 0, (Object) null, 0, this.durationUs, elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded);
        copyLengthFromLoader(loadable);
        this.loadingFinished = true;
        this.callback.onContinueLoadingRequested(this);
    }

    public void onLoadCanceled(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released2) {
        this.eventDispatcher.loadCanceled(loadable.dataSpec, 1, -1, (Format) null, 0, (Object) null, 0, this.durationUs, elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded);
        if (!released2) {
            copyLengthFromLoader(loadable);
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.reset();
            }
            if (this.enabledTrackCount > 0) {
                this.callback.onContinueLoadingRequested(this);
            }
        }
    }

    public int onLoadError(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error) {
        boolean isErrorFatal = isLoadableExceptionFatal(error);
        this.eventDispatcher.loadError(loadable.dataSpec, 1, -1, (Format) null, 0, (Object) null, 0, this.durationUs, elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded, error, isErrorFatal);
        copyLengthFromLoader(loadable);
        if (isErrorFatal) {
            return 3;
        }
        boolean madeProgress = getExtractedSamplesCount() > this.extractedSamplesCountAtStartOfLoad;
        configureRetry(loadable);
        this.extractedSamplesCountAtStartOfLoad = getExtractedSamplesCount();
        return madeProgress ? 1 : 0;
    }

    public TrackOutput track(int id, int type) {
        int trackCount = this.sampleQueues.length;
        for (int i = 0; i < trackCount; i++) {
            if (this.sampleQueueTrackIds[i] == id) {
                return this.sampleQueues[i];
            }
        }
        SampleQueue trackOutput = new SampleQueue(this.allocator);
        trackOutput.setUpstreamFormatChangeListener(this);
        this.sampleQueueTrackIds = Arrays.copyOf(this.sampleQueueTrackIds, trackCount + 1);
        this.sampleQueueTrackIds[trackCount] = id;
        this.sampleQueues = (SampleQueue[]) Arrays.copyOf(this.sampleQueues, trackCount + 1);
        this.sampleQueues[trackCount] = trackOutput;
        return trackOutput;
    }

    public void endTracks() {
        this.sampleQueuesBuilt = true;
        this.handler.post(this.maybeFinishPrepareRunnable);
    }

    public void seekMap(SeekMap seekMap2) {
        this.seekMap = seekMap2;
        this.handler.post(this.maybeFinishPrepareRunnable);
    }

    public void onUpstreamFormatChanged(Format format) {
        this.handler.post(this.maybeFinishPrepareRunnable);
    }

    /* access modifiers changed from: private */
    public void maybeFinishPrepare() {
        boolean isAudioVideo;
        if (!this.released && !this.prepared && this.seekMap != null && this.sampleQueuesBuilt) {
            SampleQueue[] sampleQueueArr = this.sampleQueues;
            int length2 = sampleQueueArr.length;
            int i = 0;
            while (i < length2) {
                if (sampleQueueArr[i].getUpstreamFormat() != null) {
                    i++;
                } else {
                    return;
                }
            }
            this.loadCondition.close();
            int trackCount = this.sampleQueues.length;
            TrackGroup[] trackArray = new TrackGroup[trackCount];
            this.trackIsAudioVideoFlags = new boolean[trackCount];
            this.trackEnabledStates = new boolean[trackCount];
            this.durationUs = this.seekMap.getDurationUs();
            for (int i2 = 0; i2 < trackCount; i2++) {
                Format trackFormat = this.sampleQueues[i2].getUpstreamFormat();
                trackArray[i2] = new TrackGroup(trackFormat);
                String mimeType = trackFormat.sampleMimeType;
                if (MimeTypes.isVideo(mimeType) || MimeTypes.isAudio(mimeType)) {
                    isAudioVideo = true;
                } else {
                    isAudioVideo = false;
                }
                this.trackIsAudioVideoFlags[i2] = isAudioVideo;
                this.haveAudioVideoTracks |= isAudioVideo;
            }
            this.tracks = new TrackGroupArray(trackArray);
            if (this.minLoadableRetryCount == -1 && this.length == -1 && this.seekMap.getDurationUs() == C.TIME_UNSET) {
                this.actualMinLoadableRetryCount = 6;
            }
            this.prepared = true;
            this.listener.onSourceInfoRefreshed(this.durationUs, this.seekMap.isSeekable());
            this.callback.onPrepared(this);
        }
    }

    private void copyLengthFromLoader(ExtractingLoadable loadable) {
        if (this.length == -1) {
            this.length = loadable.length;
        }
    }

    private void startLoading() {
        ExtractingLoadable loadable = new ExtractingLoadable(this.uri, this.dataSource, this.extractorHolder, this.loadCondition);
        if (this.prepared) {
            Assertions.checkState(isPendingReset());
            if (this.durationUs == C.TIME_UNSET || this.pendingResetPositionUs < this.durationUs) {
                loadable.setLoadPosition(this.seekMap.getPosition(this.pendingResetPositionUs), this.pendingResetPositionUs);
                this.pendingResetPositionUs = C.TIME_UNSET;
            } else {
                this.loadingFinished = true;
                this.pendingResetPositionUs = C.TIME_UNSET;
                return;
            }
        }
        this.extractedSamplesCountAtStartOfLoad = getExtractedSamplesCount();
        this.loader.startLoading(loadable, this, this.actualMinLoadableRetryCount);
    }

    private void configureRetry(ExtractingLoadable loadable) {
        if (this.length != -1) {
            return;
        }
        if (this.seekMap == null || this.seekMap.getDurationUs() == C.TIME_UNSET) {
            this.lastSeekPositionUs = 0;
            this.notifyDiscontinuity = this.prepared;
            for (SampleQueue sampleQueue : this.sampleQueues) {
                sampleQueue.reset();
            }
            loadable.setLoadPosition(0, 0);
        }
    }

    private boolean seekInsideBufferUs(long positionUs) {
        boolean seekInsideQueue;
        int trackCount = this.sampleQueues.length;
        for (int i = 0; i < trackCount; i++) {
            SampleQueue sampleQueue = this.sampleQueues[i];
            sampleQueue.rewind();
            if (sampleQueue.advanceTo(positionUs, true, false) != -1) {
                seekInsideQueue = true;
            } else {
                seekInsideQueue = false;
            }
            if (!seekInsideQueue && (this.trackIsAudioVideoFlags[i] || !this.haveAudioVideoTracks)) {
                return false;
            }
            sampleQueue.discardToRead();
        }
        return true;
    }

    private int getExtractedSamplesCount() {
        int extractedSamplesCount = 0;
        for (SampleQueue sampleQueue : this.sampleQueues) {
            extractedSamplesCount += sampleQueue.getWriteIndex();
        }
        return extractedSamplesCount;
    }

    private long getLargestQueuedTimestampUs() {
        long largestQueuedTimestampUs = Long.MIN_VALUE;
        for (SampleQueue sampleQueue : this.sampleQueues) {
            largestQueuedTimestampUs = Math.max(largestQueuedTimestampUs, sampleQueue.getLargestQueuedTimestampUs());
        }
        return largestQueuedTimestampUs;
    }

    private boolean isPendingReset() {
        return this.pendingResetPositionUs != C.TIME_UNSET;
    }

    private boolean isLoadableExceptionFatal(IOException e) {
        return e instanceof UnrecognizedInputFormatException;
    }

    private final class SampleStreamImpl implements SampleStream {
        /* access modifiers changed from: private */
        public final int track;

        public SampleStreamImpl(int track2) {
            this.track = track2;
        }

        public boolean isReady() {
            return ExtractorMediaPeriod.this.isReady(this.track);
        }

        public void maybeThrowError() throws IOException {
            ExtractorMediaPeriod.this.maybeThrowError();
        }

        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired) {
            return ExtractorMediaPeriod.this.readData(this.track, formatHolder, buffer, formatRequired);
        }

        public int skipData(long positionUs) {
            return ExtractorMediaPeriod.this.skipData(this.track, positionUs);
        }
    }

    final class ExtractingLoadable implements Loader.Loadable {
        /* access modifiers changed from: private */
        public long bytesLoaded;
        private final DataSource dataSource;
        /* access modifiers changed from: private */
        public DataSpec dataSpec;
        private final ExtractorHolder extractorHolder;
        /* access modifiers changed from: private */
        public long length = -1;
        private volatile boolean loadCanceled;
        private final ConditionVariable loadCondition;
        private boolean pendingExtractorSeek = true;
        private final PositionHolder positionHolder = new PositionHolder();
        private long seekTimeUs;
        private final Uri uri;

        public ExtractingLoadable(Uri uri2, DataSource dataSource2, ExtractorHolder extractorHolder2, ConditionVariable loadCondition2) {
            this.uri = (Uri) Assertions.checkNotNull(uri2);
            this.dataSource = (DataSource) Assertions.checkNotNull(dataSource2);
            this.extractorHolder = (ExtractorHolder) Assertions.checkNotNull(extractorHolder2);
            this.loadCondition = loadCondition2;
        }

        public void setLoadPosition(long position, long timeUs) {
            this.positionHolder.position = position;
            this.seekTimeUs = timeUs;
            this.pendingExtractorSeek = true;
        }

        public void cancelLoad() {
            this.loadCanceled = true;
        }

        public boolean isLoadCanceled() {
            return this.loadCanceled;
        }

        /* JADX WARNING: Removed duplicated region for block: B:23:0x0092  */
        /* JADX WARNING: Removed duplicated region for block: B:32:0x00ba  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void load() throws java.io.IOException, java.lang.InterruptedException {
            /*
                r12 = this;
                r9 = 0
            L_0x0001:
                if (r9 != 0) goto L_0x00d0
                boolean r1 = r12.loadCanceled
                if (r1 != 0) goto L_0x00d0
                r8 = 0
                com.google.android.exoplayer2.extractor.PositionHolder r1 = r12.positionHolder     // Catch:{ all -> 0x00d1 }
                long r2 = r1.position     // Catch:{ all -> 0x00d1 }
                com.google.android.exoplayer2.upstream.DataSpec r0 = new com.google.android.exoplayer2.upstream.DataSpec     // Catch:{ all -> 0x00d1 }
                android.net.Uri r1 = r12.uri     // Catch:{ all -> 0x00d1 }
                r4 = -1
                com.google.android.exoplayer2.source.ExtractorMediaPeriod r6 = com.google.android.exoplayer2.source.ExtractorMediaPeriod.this     // Catch:{ all -> 0x00d1 }
                java.lang.String r6 = r6.customCacheKey     // Catch:{ all -> 0x00d1 }
                r0.<init>(r1, r2, r4, r6)     // Catch:{ all -> 0x00d1 }
                r12.dataSpec = r0     // Catch:{ all -> 0x00d1 }
                com.google.android.exoplayer2.upstream.DataSource r1 = r12.dataSource     // Catch:{ all -> 0x00d1 }
                com.google.android.exoplayer2.upstream.DataSpec r4 = r12.dataSpec     // Catch:{ all -> 0x00d1 }
                long r4 = r1.open(r4)     // Catch:{ all -> 0x00d1 }
                r12.length = r4     // Catch:{ all -> 0x00d1 }
                long r4 = r12.length     // Catch:{ all -> 0x00d1 }
                r10 = -1
                int r1 = (r4 > r10 ? 1 : (r4 == r10 ? 0 : -1))
                if (r1 == 0) goto L_0x0034
                long r4 = r12.length     // Catch:{ all -> 0x00d1 }
                long r4 = r4 + r2
                r12.length = r4     // Catch:{ all -> 0x00d1 }
            L_0x0034:
                com.google.android.exoplayer2.extractor.DefaultExtractorInput r0 = new com.google.android.exoplayer2.extractor.DefaultExtractorInput     // Catch:{ all -> 0x00d1 }
                com.google.android.exoplayer2.upstream.DataSource r1 = r12.dataSource     // Catch:{ all -> 0x00d1 }
                long r4 = r12.length     // Catch:{ all -> 0x00d1 }
                r0.<init>(r1, r2, r4)     // Catch:{ all -> 0x00d1 }
                com.google.android.exoplayer2.source.ExtractorMediaPeriod$ExtractorHolder r1 = r12.extractorHolder     // Catch:{ all -> 0x008e }
                com.google.android.exoplayer2.upstream.DataSource r4 = r12.dataSource     // Catch:{ all -> 0x008e }
                android.net.Uri r4 = r4.getUri()     // Catch:{ all -> 0x008e }
                com.google.android.exoplayer2.extractor.Extractor r7 = r1.selectExtractor(r0, r4)     // Catch:{ all -> 0x008e }
                boolean r1 = r12.pendingExtractorSeek     // Catch:{ all -> 0x008e }
                if (r1 == 0) goto L_0x0055
                long r4 = r12.seekTimeUs     // Catch:{ all -> 0x008e }
                r7.seek(r2, r4)     // Catch:{ all -> 0x008e }
                r1 = 0
                r12.pendingExtractorSeek = r1     // Catch:{ all -> 0x008e }
            L_0x0055:
                if (r9 != 0) goto L_0x0099
                boolean r1 = r12.loadCanceled     // Catch:{ all -> 0x008e }
                if (r1 != 0) goto L_0x0099
                com.google.android.exoplayer2.util.ConditionVariable r1 = r12.loadCondition     // Catch:{ all -> 0x008e }
                r1.block()     // Catch:{ all -> 0x008e }
                com.google.android.exoplayer2.extractor.PositionHolder r1 = r12.positionHolder     // Catch:{ all -> 0x008e }
                int r9 = r7.read(r0, r1)     // Catch:{ all -> 0x008e }
                long r4 = r0.getPosition()     // Catch:{ all -> 0x008e }
                com.google.android.exoplayer2.source.ExtractorMediaPeriod r1 = com.google.android.exoplayer2.source.ExtractorMediaPeriod.this     // Catch:{ all -> 0x008e }
                long r10 = r1.continueLoadingCheckIntervalBytes     // Catch:{ all -> 0x008e }
                long r10 = r10 + r2
                int r1 = (r4 > r10 ? 1 : (r4 == r10 ? 0 : -1))
                if (r1 <= 0) goto L_0x0055
                long r2 = r0.getPosition()     // Catch:{ all -> 0x008e }
                com.google.android.exoplayer2.util.ConditionVariable r1 = r12.loadCondition     // Catch:{ all -> 0x008e }
                r1.close()     // Catch:{ all -> 0x008e }
                com.google.android.exoplayer2.source.ExtractorMediaPeriod r1 = com.google.android.exoplayer2.source.ExtractorMediaPeriod.this     // Catch:{ all -> 0x008e }
                android.os.Handler r1 = r1.handler     // Catch:{ all -> 0x008e }
                com.google.android.exoplayer2.source.ExtractorMediaPeriod r4 = com.google.android.exoplayer2.source.ExtractorMediaPeriod.this     // Catch:{ all -> 0x008e }
                java.lang.Runnable r4 = r4.onContinueLoadingRequestedRunnable     // Catch:{ all -> 0x008e }
                r1.post(r4)     // Catch:{ all -> 0x008e }
                goto L_0x0055
            L_0x008e:
                r1 = move-exception
            L_0x008f:
                r4 = 1
                if (r9 != r4) goto L_0x00ba
                r9 = 0
            L_0x0093:
                com.google.android.exoplayer2.upstream.DataSource r4 = r12.dataSource
                com.google.android.exoplayer2.util.Util.closeQuietly((com.google.android.exoplayer2.upstream.DataSource) r4)
                throw r1
            L_0x0099:
                r1 = 1
                if (r9 != r1) goto L_0x00a4
                r9 = 0
            L_0x009d:
                com.google.android.exoplayer2.upstream.DataSource r1 = r12.dataSource
                com.google.android.exoplayer2.util.Util.closeQuietly((com.google.android.exoplayer2.upstream.DataSource) r1)
                goto L_0x0001
            L_0x00a4:
                if (r0 == 0) goto L_0x009d
                com.google.android.exoplayer2.extractor.PositionHolder r1 = r12.positionHolder
                long r4 = r0.getPosition()
                r1.position = r4
                com.google.android.exoplayer2.extractor.PositionHolder r1 = r12.positionHolder
                long r4 = r1.position
                com.google.android.exoplayer2.upstream.DataSpec r1 = r12.dataSpec
                long r10 = r1.absoluteStreamPosition
                long r4 = r4 - r10
                r12.bytesLoaded = r4
                goto L_0x009d
            L_0x00ba:
                if (r0 == 0) goto L_0x0093
                com.google.android.exoplayer2.extractor.PositionHolder r4 = r12.positionHolder
                long r10 = r0.getPosition()
                r4.position = r10
                com.google.android.exoplayer2.extractor.PositionHolder r4 = r12.positionHolder
                long r4 = r4.position
                com.google.android.exoplayer2.upstream.DataSpec r6 = r12.dataSpec
                long r10 = r6.absoluteStreamPosition
                long r4 = r4 - r10
                r12.bytesLoaded = r4
                goto L_0x0093
            L_0x00d0:
                return
            L_0x00d1:
                r1 = move-exception
                r0 = r8
                goto L_0x008f
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.source.ExtractorMediaPeriod.ExtractingLoadable.load():void");
        }
    }

    private static final class ExtractorHolder {
        private Extractor extractor;
        private final ExtractorOutput extractorOutput;
        private final Extractor[] extractors;

        public ExtractorHolder(Extractor[] extractors2, ExtractorOutput extractorOutput2) {
            this.extractors = extractors2;
            this.extractorOutput = extractorOutput2;
        }

        public Extractor selectExtractor(ExtractorInput input, Uri uri) throws IOException, InterruptedException {
            if (this.extractor != null) {
                return this.extractor;
            }
            Extractor[] extractorArr = this.extractors;
            int length = extractorArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                Extractor extractor2 = extractorArr[i];
                try {
                    if (extractor2.sniff(input)) {
                        this.extractor = extractor2;
                        input.resetPeekPosition();
                        break;
                    }
                    i++;
                } catch (EOFException e) {
                } finally {
                    input.resetPeekPosition();
                }
            }
            if (this.extractor == null) {
                throw new UnrecognizedInputFormatException("None of the available extractors (" + Util.getCommaDelimitedSimpleClassNames(this.extractors) + ") could read the stream.", uri);
            }
            this.extractor.init(this.extractorOutput);
            return this.extractor;
        }

        public void release() {
            if (this.extractor != null) {
                this.extractor.release();
                this.extractor = null;
            }
        }
    }
}
