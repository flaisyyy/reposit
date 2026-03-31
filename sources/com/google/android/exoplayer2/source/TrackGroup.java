package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.Assertions;
import java.util.Arrays;

public final class TrackGroup {
    private final Format[] formats;
    private int hashCode;
    public final int length;

    public TrackGroup(Format... formats2) {
        Assertions.checkState(formats2.length > 0);
        this.formats = formats2;
        this.length = formats2.length;
    }

    public Format getFormat(int index) {
        return this.formats[index];
    }

    public int indexOf(Format format) {
        for (int i = 0; i < this.formats.length; i++) {
            if (format == this.formats[i]) {
                return i;
            }
        }
        return -1;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = Arrays.hashCode(this.formats) + 527;
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
        TrackGroup other = (TrackGroup) obj;
        if (this.length != other.length || !Arrays.equals(this.formats, other.formats)) {
            return false;
        }
        return true;
    }
}
