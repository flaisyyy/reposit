package com.google.android.exoplayer2.trackselection;

import android.os.SystemClock;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.chunk.MediaChunk;
import com.google.android.exoplayer2.util.Assertions;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public abstract class BaseTrackSelection implements TrackSelection {
    private final long[] blacklistUntilTimes;
    private final Format[] formats;
    protected final TrackGroup group;
    private int hashCode;
    protected final int length;
    protected final int[] tracks;

    public BaseTrackSelection(TrackGroup group2, int... tracks2) {
        Assertions.checkState(tracks2.length > 0);
        this.group = (TrackGroup) Assertions.checkNotNull(group2);
        this.length = tracks2.length;
        this.formats = new Format[this.length];
        for (int i = 0; i < tracks2.length; i++) {
            this.formats[i] = group2.getFormat(tracks2[i]);
        }
        Arrays.sort(this.formats, new DecreasingBandwidthComparator());
        this.tracks = new int[this.length];
        for (int i2 = 0; i2 < this.length; i2++) {
            this.tracks[i2] = group2.indexOf(this.formats[i2]);
        }
        this.blacklistUntilTimes = new long[this.length];
    }

    public void enable() {
    }

    public void disable() {
    }

    public final TrackGroup getTrackGroup() {
        return this.group;
    }

    public final int length() {
        return this.tracks.length;
    }

    public final Format getFormat(int index) {
        return this.formats[index];
    }

    public final int getIndexInTrackGroup(int index) {
        return this.tracks[index];
    }

    public final int indexOf(Format format) {
        for (int i = 0; i < this.length; i++) {
            if (this.formats[i] == format) {
                return i;
            }
        }
        return -1;
    }

    public final int indexOf(int indexInTrackGroup) {
        for (int i = 0; i < this.length; i++) {
            if (this.tracks[i] == indexInTrackGroup) {
                return i;
            }
        }
        return -1;
    }

    public final Format getSelectedFormat() {
        return this.formats[getSelectedIndex()];
    }

    public final int getSelectedIndexInTrackGroup() {
        return this.tracks[getSelectedIndex()];
    }

    public int evaluateQueueSize(long playbackPositionUs, List<? extends MediaChunk> queue) {
        return queue.size();
    }

    public final boolean blacklist(int index, long blacklistDurationMs) {
        long nowMs = SystemClock.elapsedRealtime();
        boolean canBlacklist = isBlacklisted(index, nowMs);
        for (int i = 0; i < this.length && !canBlacklist; i++) {
            if (i == index || isBlacklisted(i, nowMs)) {
                canBlacklist = false;
            } else {
                canBlacklist = true;
            }
        }
        if (!canBlacklist) {
            return false;
        }
        this.blacklistUntilTimes[index] = Math.max(this.blacklistUntilTimes[index], nowMs + blacklistDurationMs);
        return true;
    }

    /* access modifiers changed from: protected */
    public final boolean isBlacklisted(int index, long nowMs) {
        return this.blacklistUntilTimes[index] > nowMs;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = (System.identityHashCode(this.group) * 31) + Arrays.hashCode(this.tracks);
        }
        return this.hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseTrackSelection other = (BaseTrackSelection) obj;
        if (this.group != other.group || !Arrays.equals(this.tracks, other.tracks)) {
            return false;
        }
        return true;
    }

    private static final class DecreasingBandwidthComparator implements Comparator<Format> {
        private DecreasingBandwidthComparator() {
        }

        public int compare(Format a, Format b) {
            return b.bitrate - a.bitrate;
        }
    }
}
