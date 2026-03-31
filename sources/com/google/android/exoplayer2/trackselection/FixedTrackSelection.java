package com.google.android.exoplayer2.trackselection;

import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.util.Assertions;

public final class FixedTrackSelection extends BaseTrackSelection {
    private final Object data;
    private final int reason;

    public static final class Factory implements TrackSelection.Factory {
        private final Object data;
        private final int reason;

        public Factory() {
            this.reason = 0;
            this.data = null;
        }

        public Factory(int reason2, Object data2) {
            this.reason = reason2;
            this.data = data2;
        }

        public FixedTrackSelection createTrackSelection(TrackGroup group, int... tracks) {
            boolean z = true;
            if (tracks.length != 1) {
                z = false;
            }
            Assertions.checkArgument(z);
            return new FixedTrackSelection(group, tracks[0], this.reason, this.data);
        }
    }

    public FixedTrackSelection(TrackGroup group, int track) {
        this(group, track, 0, (Object) null);
    }

    public FixedTrackSelection(TrackGroup group, int track, int reason2, Object data2) {
        super(group, track);
        this.reason = reason2;
        this.data = data2;
    }

    public void updateSelectedTrack(long playbackPositionUs, long bufferedDurationUs, long availableDurationUs) {
    }

    public int getSelectedIndex() {
        return 0;
    }

    public int getSelectionReason() {
        return this.reason;
    }

    public Object getSelectionData() {
        return this.data;
    }
}
