package com.google.android.exoplayer2.source.chunk;

import android.util.Log;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.SampleQueue;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.SequenceableLoader;
import com.google.android.exoplayer2.source.chunk.ChunkSource;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ChunkSampleStream<T extends ChunkSource> implements SampleStream, SequenceableLoader, Loader.Callback<Chunk>, Loader.ReleaseCallback {
    private static final String TAG = "ChunkSampleStream";
    private final SequenceableLoader.Callback<ChunkSampleStream<T>> callback;
    private final T chunkSource;
    private final SampleQueue[] embeddedSampleQueues;
    private final int[] embeddedTrackTypes;
    /* access modifiers changed from: private */
    public final boolean[] embeddedTracksSelected;
    private final MediaSourceEventListener.EventDispatcher eventDispatcher;
    long lastSeekPositionUs;
    private final Loader loader = new Loader("Loader:ChunkSampleStream");
    boolean loadingFinished;
    private final BaseMediaChunkOutput mediaChunkOutput;
    private final LinkedList<BaseMediaChunk> mediaChunks = new LinkedList<>();
    private final int minLoadableRetryCount;
    private final ChunkHolder nextChunkHolder = new ChunkHolder();
    private long pendingResetPositionUs;
    private Format primaryDownstreamTrackFormat;
    private final SampleQueue primarySampleQueue;
    private final int primaryTrackType;
    private final List<BaseMediaChunk> readOnlyMediaChunks = Collections.unmodifiableList(this.mediaChunks);

    public ChunkSampleStream(int primaryTrackType2, int[] embeddedTrackTypes2, T chunkSource2, SequenceableLoader.Callback<ChunkSampleStream<T>> callback2, Allocator allocator, long positionUs, int minLoadableRetryCount2, MediaSourceEventListener.EventDispatcher eventDispatcher2) {
        this.primaryTrackType = primaryTrackType2;
        this.embeddedTrackTypes = embeddedTrackTypes2;
        this.chunkSource = chunkSource2;
        this.callback = callback2;
        this.eventDispatcher = eventDispatcher2;
        this.minLoadableRetryCount = minLoadableRetryCount2;
        int embeddedTrackCount = embeddedTrackTypes2 == null ? 0 : embeddedTrackTypes2.length;
        this.embeddedSampleQueues = new SampleQueue[embeddedTrackCount];
        this.embeddedTracksSelected = new boolean[embeddedTrackCount];
        int[] trackTypes = new int[(embeddedTrackCount + 1)];
        SampleQueue[] sampleQueues = new SampleQueue[(embeddedTrackCount + 1)];
        this.primarySampleQueue = new SampleQueue(allocator);
        trackTypes[0] = primaryTrackType2;
        sampleQueues[0] = this.primarySampleQueue;
        for (int i = 0; i < embeddedTrackCount; i++) {
            SampleQueue sampleQueue = new SampleQueue(allocator);
            this.embeddedSampleQueues[i] = sampleQueue;
            sampleQueues[i + 1] = sampleQueue;
            trackTypes[i + 1] = embeddedTrackTypes2[i];
        }
        this.mediaChunkOutput = new BaseMediaChunkOutput(trackTypes, sampleQueues);
        this.pendingResetPositionUs = positionUs;
        this.lastSeekPositionUs = positionUs;
    }

    public void discardEmbeddedTracksTo(long positionUs) {
        for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
            this.embeddedSampleQueues[i].discardTo(positionUs, true, this.embeddedTracksSelected[i]);
        }
    }

    public ChunkSampleStream<T>.EmbeddedSampleStream selectEmbeddedTrack(long positionUs, int trackType) {
        for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
            if (this.embeddedTrackTypes[i] == trackType) {
                Assertions.checkState(!this.embeddedTracksSelected[i]);
                this.embeddedTracksSelected[i] = true;
                this.embeddedSampleQueues[i].rewind();
                this.embeddedSampleQueues[i].advanceTo(positionUs, true, true);
                return new EmbeddedSampleStream(this, this.embeddedSampleQueues[i], i);
            }
        }
        throw new IllegalStateException();
    }

    public T getChunkSource() {
        return this.chunkSource;
    }

    public long getBufferedPositionUs() {
        BaseMediaChunk lastCompletedMediaChunk;
        if (this.loadingFinished) {
            return Long.MIN_VALUE;
        }
        if (isPendingReset()) {
            return this.pendingResetPositionUs;
        }
        long bufferedPositionUs = this.lastSeekPositionUs;
        BaseMediaChunk lastMediaChunk = this.mediaChunks.getLast();
        if (lastMediaChunk.isLoadCompleted()) {
            lastCompletedMediaChunk = lastMediaChunk;
        } else {
            lastCompletedMediaChunk = this.mediaChunks.size() > 1 ? this.mediaChunks.get(this.mediaChunks.size() - 2) : null;
        }
        if (lastCompletedMediaChunk != null) {
            bufferedPositionUs = Math.max(bufferedPositionUs, lastCompletedMediaChunk.endTimeUs);
        }
        return Math.max(bufferedPositionUs, this.primarySampleQueue.getLargestQueuedTimestampUs());
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x0042  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x001f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void seekToUs(long r10) {
        /*
            r9 = this;
            r3 = 1
            r4 = 0
            r9.lastSeekPositionUs = r10
            boolean r2 = r9.isPendingReset()
            if (r2 != 0) goto L_0x0040
            com.google.android.exoplayer2.source.SampleQueue r5 = r9.primarySampleQueue
            long r6 = r9.getNextLoadPositionUs()
            int r2 = (r10 > r6 ? 1 : (r10 == r6 ? 0 : -1))
            if (r2 >= 0) goto L_0x003e
            r2 = r3
        L_0x0015:
            int r2 = r5.advanceTo(r10, r3, r2)
            r5 = -1
            if (r2 == r5) goto L_0x0040
            r1 = r3
        L_0x001d:
            if (r1 == 0) goto L_0x0042
            com.google.android.exoplayer2.source.SampleQueue r2 = r9.primarySampleQueue
            int r2 = r2.getReadIndex()
            r9.discardDownstreamMediaChunks(r2)
            com.google.android.exoplayer2.source.SampleQueue r2 = r9.primarySampleQueue
            r2.discardToRead()
            com.google.android.exoplayer2.source.SampleQueue[] r5 = r9.embeddedSampleQueues
            int r6 = r5.length
            r2 = r4
        L_0x0031:
            if (r2 >= r6) goto L_0x0058
            r0 = r5[r2]
            r0.rewind()
            r0.discardTo(r10, r3, r4)
            int r2 = r2 + 1
            goto L_0x0031
        L_0x003e:
            r2 = r4
            goto L_0x0015
        L_0x0040:
            r1 = r4
            goto L_0x001d
        L_0x0042:
            r9.pendingResetPositionUs = r10
            r9.loadingFinished = r4
            java.util.LinkedList<com.google.android.exoplayer2.source.chunk.BaseMediaChunk> r2 = r9.mediaChunks
            r2.clear()
            com.google.android.exoplayer2.upstream.Loader r2 = r9.loader
            boolean r2 = r2.isLoading()
            if (r2 == 0) goto L_0x0059
            com.google.android.exoplayer2.upstream.Loader r2 = r9.loader
            r2.cancelLoading()
        L_0x0058:
            return
        L_0x0059:
            com.google.android.exoplayer2.source.SampleQueue r2 = r9.primarySampleQueue
            r2.reset()
            com.google.android.exoplayer2.source.SampleQueue[] r2 = r9.embeddedSampleQueues
            int r3 = r2.length
        L_0x0061:
            if (r4 >= r3) goto L_0x0058
            r0 = r2[r4]
            r0.reset()
            int r4 = r4 + 1
            goto L_0x0061
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.source.chunk.ChunkSampleStream.seekToUs(long):void");
    }

    public void release() {
        if (!this.loader.release(this)) {
            this.primarySampleQueue.discardToEnd();
            for (SampleQueue embeddedSampleQueue : this.embeddedSampleQueues) {
                embeddedSampleQueue.discardToEnd();
            }
        }
    }

    public void onLoaderReleased() {
        this.primarySampleQueue.reset();
        for (SampleQueue embeddedSampleQueue : this.embeddedSampleQueues) {
            embeddedSampleQueue.reset();
        }
    }

    public boolean isReady() {
        return this.loadingFinished || (!isPendingReset() && this.primarySampleQueue.hasNextSample());
    }

    public void maybeThrowError() throws IOException {
        this.loader.maybeThrowError();
        if (!this.loader.isLoading()) {
            this.chunkSource.maybeThrowError();
        }
    }

    public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired) {
        if (isPendingReset()) {
            return -3;
        }
        discardDownstreamMediaChunks(this.primarySampleQueue.getReadIndex());
        int result = this.primarySampleQueue.read(formatHolder, buffer, formatRequired, this.loadingFinished, this.lastSeekPositionUs);
        if (result != -4) {
            return result;
        }
        this.primarySampleQueue.discardToRead();
        return result;
    }

    public int skipData(long positionUs) {
        int skipCount;
        if (isPendingReset()) {
            return 0;
        }
        if (!this.loadingFinished || positionUs <= this.primarySampleQueue.getLargestQueuedTimestampUs()) {
            skipCount = this.primarySampleQueue.advanceTo(positionUs, true, true);
            if (skipCount == -1) {
                skipCount = 0;
            }
        } else {
            skipCount = this.primarySampleQueue.advanceToEnd();
        }
        if (skipCount <= 0) {
            return skipCount;
        }
        this.primarySampleQueue.discardToRead();
        return skipCount;
    }

    public void onLoadCompleted(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs) {
        this.chunkSource.onChunkLoadCompleted(loadable);
        this.eventDispatcher.loadCompleted(loadable.dataSpec, loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs, elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.callback.onContinueLoadingRequested(this);
    }

    public void onLoadCanceled(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        this.eventDispatcher.loadCanceled(loadable.dataSpec, loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs, elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        if (!released) {
            this.primarySampleQueue.reset();
            for (SampleQueue embeddedSampleQueue : this.embeddedSampleQueues) {
                embeddedSampleQueue.reset();
            }
            this.callback.onContinueLoadingRequested(this);
        }
    }

    public int onLoadError(Chunk loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error) {
        long bytesLoaded = loadable.bytesLoaded();
        boolean isMediaChunk = isMediaChunk(loadable);
        boolean cancelable = bytesLoaded == 0 || !isMediaChunk || !haveReadFromLastMediaChunk();
        boolean canceled = false;
        if (this.chunkSource.onChunkLoadError(loadable, cancelable, error)) {
            if (!cancelable) {
                Log.w(TAG, "Ignoring attempt to cancel non-cancelable load.");
            } else {
                canceled = true;
                if (isMediaChunk) {
                    BaseMediaChunk removed = this.mediaChunks.removeLast();
                    Assertions.checkState(removed == loadable);
                    this.primarySampleQueue.discardUpstreamSamples(removed.getFirstSampleIndex(0));
                    for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
                        this.embeddedSampleQueues[i].discardUpstreamSamples(removed.getFirstSampleIndex(i + 1));
                    }
                    if (this.mediaChunks.isEmpty()) {
                        this.pendingResetPositionUs = this.lastSeekPositionUs;
                    }
                }
            }
        }
        this.eventDispatcher.loadError(loadable.dataSpec, loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs, elapsedRealtimeMs, loadDurationMs, bytesLoaded, error, canceled);
        if (!canceled) {
            return 0;
        }
        this.callback.onContinueLoadingRequested(this);
        return 2;
    }

    public boolean continueLoading(long positionUs) {
        MediaChunk previousChunk;
        long loadPositionUs;
        if (this.loadingFinished || this.loader.isLoading()) {
            return false;
        }
        if (isPendingReset()) {
            previousChunk = null;
            loadPositionUs = this.pendingResetPositionUs;
        } else {
            previousChunk = this.mediaChunks.getLast();
            loadPositionUs = previousChunk.endTimeUs;
        }
        this.chunkSource.getNextChunk(previousChunk, positionUs, loadPositionUs, this.nextChunkHolder);
        boolean endOfStream = this.nextChunkHolder.endOfStream;
        Chunk loadable = this.nextChunkHolder.chunk;
        this.nextChunkHolder.clear();
        if (endOfStream) {
            this.pendingResetPositionUs = C.TIME_UNSET;
            this.loadingFinished = true;
            return true;
        } else if (loadable == null) {
            return false;
        } else {
            if (isMediaChunk(loadable)) {
                this.pendingResetPositionUs = C.TIME_UNSET;
                BaseMediaChunk mediaChunk = (BaseMediaChunk) loadable;
                mediaChunk.init(this.mediaChunkOutput);
                this.mediaChunks.add(mediaChunk);
            }
            this.eventDispatcher.loadStarted(loadable.dataSpec, loadable.type, this.primaryTrackType, loadable.trackFormat, loadable.trackSelectionReason, loadable.trackSelectionData, loadable.startTimeUs, loadable.endTimeUs, this.loader.startLoading(loadable, this, this.minLoadableRetryCount));
            return true;
        }
    }

    public long getNextLoadPositionUs() {
        if (isPendingReset()) {
            return this.pendingResetPositionUs;
        }
        if (this.loadingFinished) {
            return Long.MIN_VALUE;
        }
        return this.mediaChunks.getLast().endTimeUs;
    }

    private void maybeDiscardUpstream(long positionUs) {
        discardUpstreamMediaChunks(Math.max(1, this.chunkSource.getPreferredQueueSize(positionUs, this.readOnlyMediaChunks)));
    }

    private boolean isMediaChunk(Chunk chunk) {
        return chunk instanceof BaseMediaChunk;
    }

    private boolean haveReadFromLastMediaChunk() {
        BaseMediaChunk lastChunk = this.mediaChunks.getLast();
        if (this.primarySampleQueue.getReadIndex() > lastChunk.getFirstSampleIndex(0)) {
            return true;
        }
        for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
            if (this.embeddedSampleQueues[i].getReadIndex() > lastChunk.getFirstSampleIndex(i + 1)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean isPendingReset() {
        return this.pendingResetPositionUs != C.TIME_UNSET;
    }

    private void discardDownstreamMediaChunks(int primaryStreamReadIndex) {
        if (!this.mediaChunks.isEmpty()) {
            while (this.mediaChunks.size() > 1 && this.mediaChunks.get(1).getFirstSampleIndex(0) <= primaryStreamReadIndex) {
                this.mediaChunks.removeFirst();
            }
            BaseMediaChunk currentChunk = this.mediaChunks.getFirst();
            Format trackFormat = currentChunk.trackFormat;
            if (!trackFormat.equals(this.primaryDownstreamTrackFormat)) {
                this.eventDispatcher.downstreamFormatChanged(this.primaryTrackType, trackFormat, currentChunk.trackSelectionReason, currentChunk.trackSelectionData, currentChunk.startTimeUs);
            }
            this.primaryDownstreamTrackFormat = trackFormat;
        }
    }

    private boolean discardUpstreamMediaChunks(int queueLength) {
        BaseMediaChunk removed;
        long startTimeUs;
        if (this.mediaChunks.size() <= queueLength) {
            return false;
        }
        long endTimeUs = this.mediaChunks.getLast().endTimeUs;
        do {
            removed = this.mediaChunks.removeLast();
            startTimeUs = removed.startTimeUs;
        } while (this.mediaChunks.size() > queueLength);
        this.primarySampleQueue.discardUpstreamSamples(removed.getFirstSampleIndex(0));
        for (int i = 0; i < this.embeddedSampleQueues.length; i++) {
            this.embeddedSampleQueues[i].discardUpstreamSamples(removed.getFirstSampleIndex(i + 1));
        }
        this.loadingFinished = false;
        this.eventDispatcher.upstreamDiscarded(this.primaryTrackType, startTimeUs, endTimeUs);
        return true;
    }

    public final class EmbeddedSampleStream implements SampleStream {
        private final int index;
        public final ChunkSampleStream<T> parent;
        private final SampleQueue sampleQueue;

        public EmbeddedSampleStream(ChunkSampleStream<T> parent2, SampleQueue sampleQueue2, int index2) {
            this.parent = parent2;
            this.sampleQueue = sampleQueue2;
            this.index = index2;
        }

        public boolean isReady() {
            return ChunkSampleStream.this.loadingFinished || (!ChunkSampleStream.this.isPendingReset() && this.sampleQueue.hasNextSample());
        }

        public int skipData(long positionUs) {
            if (ChunkSampleStream.this.loadingFinished && positionUs > this.sampleQueue.getLargestQueuedTimestampUs()) {
                return this.sampleQueue.advanceToEnd();
            }
            int skipCount = this.sampleQueue.advanceTo(positionUs, true, true);
            if (skipCount == -1) {
                return 0;
            }
            return skipCount;
        }

        public void maybeThrowError() throws IOException {
        }

        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired) {
            if (ChunkSampleStream.this.isPendingReset()) {
                return -3;
            }
            return this.sampleQueue.read(formatHolder, buffer, formatRequired, ChunkSampleStream.this.loadingFinished, ChunkSampleStream.this.lastSeekPositionUs);
        }

        public void release() {
            Assertions.checkState(ChunkSampleStream.this.embeddedTracksSelected[this.index]);
            ChunkSampleStream.this.embeddedTracksSelected[this.index] = false;
        }
    }
}
