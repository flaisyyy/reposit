package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.util.Assertions;

public final class SinglePeriodTimeline extends Timeline {
    private static final Object ID = new Object();
    private final boolean isDynamic;
    private final boolean isSeekable;
    private final long periodDurationUs;
    private final long presentationStartTimeMs;
    private final long windowDefaultStartPositionUs;
    private final long windowDurationUs;
    private final long windowPositionInPeriodUs;
    private final long windowStartTimeMs;

    public SinglePeriodTimeline(long durationUs, boolean isSeekable2) {
        this(durationUs, durationUs, 0, 0, isSeekable2, false);
    }

    public SinglePeriodTimeline(long periodDurationUs2, long windowDurationUs2, long windowPositionInPeriodUs2, long windowDefaultStartPositionUs2, boolean isSeekable2, boolean isDynamic2) {
        this(C.TIME_UNSET, C.TIME_UNSET, periodDurationUs2, windowDurationUs2, windowPositionInPeriodUs2, windowDefaultStartPositionUs2, isSeekable2, isDynamic2);
    }

    public SinglePeriodTimeline(long presentationStartTimeMs2, long windowStartTimeMs2, long periodDurationUs2, long windowDurationUs2, long windowPositionInPeriodUs2, long windowDefaultStartPositionUs2, boolean isSeekable2, boolean isDynamic2) {
        this.presentationStartTimeMs = presentationStartTimeMs2;
        this.windowStartTimeMs = windowStartTimeMs2;
        this.periodDurationUs = periodDurationUs2;
        this.windowDurationUs = windowDurationUs2;
        this.windowPositionInPeriodUs = windowPositionInPeriodUs2;
        this.windowDefaultStartPositionUs = windowDefaultStartPositionUs2;
        this.isSeekable = isSeekable2;
        this.isDynamic = isDynamic2;
    }

    public int getWindowCount() {
        return 1;
    }

    public Timeline.Window getWindow(int windowIndex, Timeline.Window window, boolean setIds, long defaultPositionProjectionUs) {
        Assertions.checkIndex(windowIndex, 0, 1);
        Object id = setIds ? ID : null;
        long windowDefaultStartPositionUs2 = this.windowDefaultStartPositionUs;
        if (this.isDynamic) {
            windowDefaultStartPositionUs2 += defaultPositionProjectionUs;
            if (windowDefaultStartPositionUs2 > this.windowDurationUs) {
                windowDefaultStartPositionUs2 = C.TIME_UNSET;
            }
        }
        return window.set(id, this.presentationStartTimeMs, this.windowStartTimeMs, this.isSeekable, this.isDynamic, windowDefaultStartPositionUs2, this.windowDurationUs, 0, 0, this.windowPositionInPeriodUs);
    }

    public int getPeriodCount() {
        return 1;
    }

    public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
        Assertions.checkIndex(periodIndex, 0, 1);
        Object id = setIds ? ID : null;
        return period.set(id, id, 0, this.periodDurationUs, -this.windowPositionInPeriodUs);
    }

    public int getIndexOfPeriod(Object uid) {
        return ID.equals(uid) ? 0 : -1;
    }
}
