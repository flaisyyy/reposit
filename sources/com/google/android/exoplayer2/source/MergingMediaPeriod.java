package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;

final class MergingMediaPeriod implements MediaPeriod, MediaPeriod.Callback {
    private MediaPeriod.Callback callback;
    private MediaPeriod[] enabledPeriods;
    private int pendingChildPrepareCount;
    public final MediaPeriod[] periods;
    private SequenceableLoader sequenceableLoader;
    private final IdentityHashMap<SampleStream, Integer> streamPeriodIndices = new IdentityHashMap<>();
    private TrackGroupArray trackGroups;

    public MergingMediaPeriod(MediaPeriod... periods2) {
        this.periods = periods2;
    }

    public void prepare(MediaPeriod.Callback callback2, long positionUs) {
        this.callback = callback2;
        this.pendingChildPrepareCount = this.periods.length;
        for (MediaPeriod period : this.periods) {
            period.prepare(this, positionUs);
        }
    }

    public void maybeThrowPrepareError() throws IOException {
        for (MediaPeriod period : this.periods) {
            period.maybeThrowPrepareError();
        }
    }

    public TrackGroupArray getTrackGroups() {
        return this.trackGroups;
    }

    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        int intValue;
        int[] streamChildIndices = new int[selections.length];
        int[] selectionChildIndices = new int[selections.length];
        for (int i = 0; i < selections.length; i++) {
            if (streams[i] == null) {
                intValue = -1;
            } else {
                intValue = this.streamPeriodIndices.get(streams[i]).intValue();
            }
            streamChildIndices[i] = intValue;
            selectionChildIndices[i] = -1;
            if (selections[i] != null) {
                TrackGroup trackGroup = selections[i].getTrackGroup();
                int j = 0;
                while (true) {
                    if (j >= this.periods.length) {
                        break;
                    } else if (this.periods[j].getTrackGroups().indexOf(trackGroup) != -1) {
                        selectionChildIndices[i] = j;
                        break;
                    } else {
                        j++;
                    }
                }
            }
        }
        this.streamPeriodIndices.clear();
        SampleStream[] newStreams = new SampleStream[selections.length];
        SampleStream[] childStreams = new SampleStream[selections.length];
        TrackSelection[] childSelections = new TrackSelection[selections.length];
        ArrayList<MediaPeriod> enabledPeriodsList = new ArrayList<>(this.periods.length);
        int i2 = 0;
        while (i2 < this.periods.length) {
            for (int j2 = 0; j2 < selections.length; j2++) {
                childStreams[j2] = streamChildIndices[j2] == i2 ? streams[j2] : null;
                childSelections[j2] = selectionChildIndices[j2] == i2 ? selections[j2] : null;
            }
            long selectPositionUs = this.periods[i2].selectTracks(childSelections, mayRetainStreamFlags, childStreams, streamResetFlags, positionUs);
            if (i2 == 0) {
                positionUs = selectPositionUs;
            } else if (selectPositionUs != positionUs) {
                throw new IllegalStateException("Children enabled at different positions");
            }
            boolean periodEnabled = false;
            for (int j3 = 0; j3 < selections.length; j3++) {
                if (selectionChildIndices[j3] == i2) {
                    Assertions.checkState(childStreams[j3] != null);
                    newStreams[j3] = childStreams[j3];
                    periodEnabled = true;
                    this.streamPeriodIndices.put(childStreams[j3], Integer.valueOf(i2));
                } else if (streamChildIndices[j3] == i2) {
                    Assertions.checkState(childStreams[j3] == null);
                }
            }
            if (periodEnabled) {
                enabledPeriodsList.add(this.periods[i2]);
            }
            i2++;
        }
        System.arraycopy(newStreams, 0, streams, 0, newStreams.length);
        this.enabledPeriods = new MediaPeriod[enabledPeriodsList.size()];
        enabledPeriodsList.toArray(this.enabledPeriods);
        this.sequenceableLoader = new CompositeSequenceableLoader(this.enabledPeriods);
        return positionUs;
    }

    public void discardBuffer(long positionUs) {
        for (MediaPeriod period : this.enabledPeriods) {
            period.discardBuffer(positionUs);
        }
    }

    public boolean continueLoading(long positionUs) {
        return this.sequenceableLoader.continueLoading(positionUs);
    }

    public long getNextLoadPositionUs() {
        return this.sequenceableLoader.getNextLoadPositionUs();
    }

    public long readDiscontinuity() {
        long positionUs = this.periods[0].readDiscontinuity();
        for (int i = 1; i < this.periods.length; i++) {
            if (this.periods[i].readDiscontinuity() != C.TIME_UNSET) {
                throw new IllegalStateException("Child reported discontinuity");
            }
        }
        if (positionUs != C.TIME_UNSET) {
            MediaPeriod[] mediaPeriodArr = this.enabledPeriods;
            int length = mediaPeriodArr.length;
            int i2 = 0;
            while (i2 < length) {
                MediaPeriod enabledPeriod = mediaPeriodArr[i2];
                if (enabledPeriod == this.periods[0] || enabledPeriod.seekToUs(positionUs) == positionUs) {
                    i2++;
                } else {
                    throw new IllegalStateException("Children seeked to different positions");
                }
            }
        }
        return positionUs;
    }

    public long getBufferedPositionUs() {
        return this.sequenceableLoader.getBufferedPositionUs();
    }

    public long seekToUs(long positionUs) {
        long positionUs2 = this.enabledPeriods[0].seekToUs(positionUs);
        for (int i = 1; i < this.enabledPeriods.length; i++) {
            if (this.enabledPeriods[i].seekToUs(positionUs2) != positionUs2) {
                throw new IllegalStateException("Children seeked to different positions");
            }
        }
        return positionUs2;
    }

    public void onPrepared(MediaPeriod ignored) {
        int i = 0;
        int i2 = this.pendingChildPrepareCount - 1;
        this.pendingChildPrepareCount = i2;
        if (i2 <= 0) {
            int totalTrackGroupCount = 0;
            for (MediaPeriod period : this.periods) {
                totalTrackGroupCount += period.getTrackGroups().length;
            }
            TrackGroup[] trackGroupArray = new TrackGroup[totalTrackGroupCount];
            int trackGroupIndex = 0;
            MediaPeriod[] mediaPeriodArr = this.periods;
            int length = mediaPeriodArr.length;
            while (i < length) {
                TrackGroupArray periodTrackGroups = mediaPeriodArr[i].getTrackGroups();
                int periodTrackGroupCount = periodTrackGroups.length;
                int j = 0;
                int trackGroupIndex2 = trackGroupIndex;
                while (j < periodTrackGroupCount) {
                    trackGroupArray[trackGroupIndex2] = periodTrackGroups.get(j);
                    j++;
                    trackGroupIndex2++;
                }
                i++;
                trackGroupIndex = trackGroupIndex2;
            }
            this.trackGroups = new TrackGroupArray(trackGroupArray);
            this.callback.onPrepared(this);
        }
    }

    public void onContinueLoadingRequested(MediaPeriod ignored) {
        if (this.trackGroups != null) {
            this.callback.onContinueLoadingRequested(this);
        }
    }
}
