package com.google.android.exoplayer2.source.ads;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ForwardingTimeline;
import com.google.android.exoplayer2.util.Assertions;

final class SinglePeriodAdTimeline extends ForwardingTimeline {
    private final int[] adCounts;
    private final long[][] adDurationsUs;
    private final long[] adGroupTimesUs;
    private final long adResumePositionUs;
    private final int[] adsLoadedCounts;
    private final int[] adsPlayedCounts;
    private final long contentDurationUs;

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public SinglePeriodAdTimeline(Timeline contentTimeline, long[] adGroupTimesUs2, int[] adCounts2, int[] adsLoadedCounts2, int[] adsPlayedCounts2, long[][] adDurationsUs2, long adResumePositionUs2, long contentDurationUs2) {
        super(contentTimeline);
        boolean z = true;
        Assertions.checkState(contentTimeline.getPeriodCount() == 1);
        Assertions.checkState(contentTimeline.getWindowCount() != 1 ? false : z);
        this.adGroupTimesUs = adGroupTimesUs2;
        this.adCounts = adCounts2;
        this.adsLoadedCounts = adsLoadedCounts2;
        this.adsPlayedCounts = adsPlayedCounts2;
        this.adDurationsUs = adDurationsUs2;
        this.adResumePositionUs = adResumePositionUs2;
        this.contentDurationUs = contentDurationUs2;
    }

    public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
        this.timeline.getPeriod(periodIndex, period, setIds);
        period.set(period.id, period.uid, period.windowIndex, period.durationUs, period.getPositionInWindowUs(), this.adGroupTimesUs, this.adCounts, this.adsLoadedCounts, this.adsPlayedCounts, this.adDurationsUs, this.adResumePositionUs);
        return period;
    }

    public Timeline.Window getWindow(int windowIndex, Timeline.Window window, boolean setIds, long defaultPositionProjectionUs) {
        Timeline.Window window2 = super.getWindow(windowIndex, window, setIds, defaultPositionProjectionUs);
        if (window2.durationUs == C.TIME_UNSET) {
            window2.durationUs = this.contentDurationUs;
        }
        return window2;
    }
}
