package com.google.android.exoplayer2.source;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.upstream.Allocator;
import java.io.IOException;

public interface MediaSource {

    public interface Listener {
        void onSourceInfoRefreshed(MediaSource mediaSource, Timeline timeline, @Nullable Object obj);
    }

    MediaPeriod createPeriod(MediaPeriodId mediaPeriodId, Allocator allocator);

    void maybeThrowSourceInfoRefreshError() throws IOException;

    void prepareSource(ExoPlayer exoPlayer, boolean z, Listener listener);

    void releasePeriod(MediaPeriod mediaPeriod);

    void releaseSource();

    public static final class MediaPeriodId {
        public static final MediaPeriodId UNSET = new MediaPeriodId(-1, -1, -1);
        public final int adGroupIndex;
        public final int adIndexInAdGroup;
        public final int periodIndex;

        public MediaPeriodId(int periodIndex2) {
            this(periodIndex2, -1, -1);
        }

        public MediaPeriodId(int periodIndex2, int adGroupIndex2, int adIndexInAdGroup2) {
            this.periodIndex = periodIndex2;
            this.adGroupIndex = adGroupIndex2;
            this.adIndexInAdGroup = adIndexInAdGroup2;
        }

        /* Debug info: failed to restart local var, previous not found, register: 3 */
        public MediaPeriodId copyWithPeriodIndex(int newPeriodIndex) {
            return this.periodIndex == newPeriodIndex ? this : new MediaPeriodId(newPeriodIndex, this.adGroupIndex, this.adIndexInAdGroup);
        }

        public boolean isAd() {
            return this.adGroupIndex != -1;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MediaPeriodId periodId = (MediaPeriodId) obj;
            if (this.periodIndex == periodId.periodIndex && this.adGroupIndex == periodId.adGroupIndex && this.adIndexInAdGroup == periodId.adIndexInAdGroup) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return ((((this.periodIndex + 527) * 31) + this.adGroupIndex) * 31) + this.adIndexInAdGroup;
        }
    }
}
