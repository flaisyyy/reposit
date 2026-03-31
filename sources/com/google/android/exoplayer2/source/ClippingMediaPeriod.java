package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import java.io.IOException;

public final class ClippingMediaPeriod implements MediaPeriod, MediaPeriod.Callback {
    private MediaPeriod.Callback callback;
    private long endUs = C.TIME_UNSET;
    public final MediaPeriod mediaPeriod;
    private boolean pendingInitialDiscontinuity;
    private ClippingSampleStream[] sampleStreams = new ClippingSampleStream[0];
    private long startUs = C.TIME_UNSET;

    public ClippingMediaPeriod(MediaPeriod mediaPeriod2, boolean enableInitialDiscontinuity) {
        this.mediaPeriod = mediaPeriod2;
        this.pendingInitialDiscontinuity = enableInitialDiscontinuity;
    }

    public void setClipping(long startUs2, long endUs2) {
        this.startUs = startUs2;
        this.endUs = endUs2;
    }

    public void prepare(MediaPeriod.Callback callback2, long positionUs) {
        this.callback = callback2;
        this.mediaPeriod.prepare(this, this.startUs + positionUs);
    }

    public void maybeThrowPrepareError() throws IOException {
        this.mediaPeriod.maybeThrowPrepareError();
    }

    public TrackGroupArray getTrackGroups() {
        return this.mediaPeriod.getTrackGroups();
    }

    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        this.sampleStreams = new ClippingSampleStream[streams.length];
        SampleStream[] internalStreams = new SampleStream[streams.length];
        for (int i = 0; i < streams.length; i++) {
            this.sampleStreams[i] = streams[i];
            internalStreams[i] = this.sampleStreams[i] != null ? this.sampleStreams[i].stream : null;
        }
        long enablePositionUs = this.mediaPeriod.selectTracks(selections, mayRetainStreamFlags, internalStreams, streamResetFlags, positionUs + this.startUs);
        if (this.pendingInitialDiscontinuity) {
            this.pendingInitialDiscontinuity = this.startUs != 0 && shouldKeepInitialDiscontinuity(selections);
        }
        Assertions.checkState(enablePositionUs == this.startUs + positionUs || (enablePositionUs >= this.startUs && (this.endUs == Long.MIN_VALUE || enablePositionUs <= this.endUs)));
        for (int i2 = 0; i2 < streams.length; i2++) {
            if (internalStreams[i2] == null) {
                this.sampleStreams[i2] = null;
            } else if (streams[i2] == null || this.sampleStreams[i2].stream != internalStreams[i2]) {
                this.sampleStreams[i2] = new ClippingSampleStream(internalStreams[i2], this.startUs, this.endUs, this.pendingInitialDiscontinuity);
            }
            streams[i2] = this.sampleStreams[i2];
        }
        return enablePositionUs - this.startUs;
    }

    public void discardBuffer(long positionUs) {
        this.mediaPeriod.discardBuffer(this.startUs + positionUs);
    }

    public long readDiscontinuity() {
        boolean z;
        boolean z2 = false;
        if (this.pendingInitialDiscontinuity) {
            for (ClippingSampleStream sampleStream : this.sampleStreams) {
                if (sampleStream != null) {
                    sampleStream.clearPendingDiscontinuity();
                }
            }
            this.pendingInitialDiscontinuity = false;
            long discontinuityUs = readDiscontinuity();
            if (discontinuityUs != C.TIME_UNSET) {
                return discontinuityUs;
            }
            return 0;
        }
        long discontinuityUs2 = this.mediaPeriod.readDiscontinuity();
        if (discontinuityUs2 == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        if (discontinuityUs2 >= this.startUs) {
            z = true;
        } else {
            z = false;
        }
        Assertions.checkState(z);
        if (this.endUs == Long.MIN_VALUE || discontinuityUs2 <= this.endUs) {
            z2 = true;
        }
        Assertions.checkState(z2);
        return discontinuityUs2 - this.startUs;
    }

    public long getBufferedPositionUs() {
        long bufferedPositionUs = this.mediaPeriod.getBufferedPositionUs();
        if (bufferedPositionUs == Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        if (this.endUs == Long.MIN_VALUE || bufferedPositionUs < this.endUs) {
            return Math.max(0, bufferedPositionUs - this.startUs);
        }
        return Long.MIN_VALUE;
    }

    public long seekToUs(long positionUs) {
        boolean z = false;
        for (ClippingSampleStream sampleStream : this.sampleStreams) {
            if (sampleStream != null) {
                sampleStream.clearSentEos();
            }
        }
        long seekUs = this.mediaPeriod.seekToUs(this.startUs + positionUs);
        if (seekUs == this.startUs + positionUs || (seekUs >= this.startUs && (this.endUs == Long.MIN_VALUE || seekUs <= this.endUs))) {
            z = true;
        }
        Assertions.checkState(z);
        return seekUs - this.startUs;
    }

    public long getNextLoadPositionUs() {
        long nextLoadPositionUs = this.mediaPeriod.getNextLoadPositionUs();
        if (nextLoadPositionUs == Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        if (this.endUs == Long.MIN_VALUE || nextLoadPositionUs < this.endUs) {
            return nextLoadPositionUs - this.startUs;
        }
        return Long.MIN_VALUE;
    }

    public boolean continueLoading(long positionUs) {
        return this.mediaPeriod.continueLoading(this.startUs + positionUs);
    }

    public void onPrepared(MediaPeriod mediaPeriod2) {
        Assertions.checkState((this.startUs == C.TIME_UNSET || this.endUs == C.TIME_UNSET) ? false : true);
        this.callback.onPrepared(this);
    }

    public void onContinueLoadingRequested(MediaPeriod source) {
        this.callback.onContinueLoadingRequested(this);
    }

    private static boolean shouldKeepInitialDiscontinuity(TrackSelection[] selections) {
        for (TrackSelection trackSelection : selections) {
            if (trackSelection != null && !MimeTypes.isAudio(trackSelection.getSelectedFormat().sampleMimeType)) {
                return true;
            }
        }
        return false;
    }

    private final class ClippingSampleStream implements SampleStream {
        private final long endUs;
        private boolean pendingDiscontinuity;
        private boolean sentEos;
        private final long startUs;
        /* access modifiers changed from: private */
        public final SampleStream stream;

        public ClippingSampleStream(SampleStream stream2, long startUs2, long endUs2, boolean pendingDiscontinuity2) {
            this.stream = stream2;
            this.startUs = startUs2;
            this.endUs = endUs2;
            this.pendingDiscontinuity = pendingDiscontinuity2;
        }

        public void clearPendingDiscontinuity() {
            this.pendingDiscontinuity = false;
        }

        public void clearSentEos() {
            this.sentEos = false;
        }

        public boolean isReady() {
            return this.stream.isReady();
        }

        public void maybeThrowError() throws IOException {
            this.stream.maybeThrowError();
        }

        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean requireFormat) {
            if (this.pendingDiscontinuity) {
                return -3;
            }
            if (this.sentEos) {
                buffer.setFlags(4);
                return -4;
            }
            int result = this.stream.readData(formatHolder, buffer, requireFormat);
            if (result == -5) {
                Format format = formatHolder.format;
                formatHolder.format = format.copyWithGaplessInfo(this.startUs != 0 ? 0 : format.encoderDelay, this.endUs != Long.MIN_VALUE ? 0 : format.encoderPadding);
                return -5;
            } else if (this.endUs != Long.MIN_VALUE && ((result == -4 && buffer.timeUs >= this.endUs) || (result == -3 && ClippingMediaPeriod.this.getBufferedPositionUs() == Long.MIN_VALUE))) {
                buffer.clear();
                buffer.setFlags(4);
                this.sentEos = true;
                return -4;
            } else if (result != -4 || buffer.isEndOfStream()) {
                return result;
            } else {
                buffer.timeUs -= this.startUs;
                return result;
            }
        }

        public int skipData(long positionUs) {
            return this.stream.skipData(this.startUs + positionUs);
        }
    }
}
