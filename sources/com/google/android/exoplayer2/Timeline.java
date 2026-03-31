package com.google.android.exoplayer2;

import android.util.Pair;
import com.google.android.exoplayer2.util.Assertions;

public abstract class Timeline {
    public static final Timeline EMPTY = new Timeline() {
        public int getWindowCount() {
            return 0;
        }

        public Window getWindow(int windowIndex, Window window, boolean setIds, long defaultPositionProjectionUs) {
            throw new IndexOutOfBoundsException();
        }

        public int getPeriodCount() {
            return 0;
        }

        public Period getPeriod(int periodIndex, Period period, boolean setIds) {
            throw new IndexOutOfBoundsException();
        }

        public int getIndexOfPeriod(Object uid) {
            return -1;
        }
    };

    public abstract int getIndexOfPeriod(Object obj);

    public abstract Period getPeriod(int i, Period period, boolean z);

    public abstract int getPeriodCount();

    public abstract Window getWindow(int i, Window window, boolean z, long j);

    public abstract int getWindowCount();

    public static final class Window {
        public long defaultPositionUs;
        public long durationUs;
        public int firstPeriodIndex;
        public Object id;
        public boolean isDynamic;
        public boolean isSeekable;
        public int lastPeriodIndex;
        public long positionInFirstPeriodUs;
        public long presentationStartTimeMs;
        public long windowStartTimeMs;

        public Window set(Object id2, long presentationStartTimeMs2, long windowStartTimeMs2, boolean isSeekable2, boolean isDynamic2, long defaultPositionUs2, long durationUs2, int firstPeriodIndex2, int lastPeriodIndex2, long positionInFirstPeriodUs2) {
            this.id = id2;
            this.presentationStartTimeMs = presentationStartTimeMs2;
            this.windowStartTimeMs = windowStartTimeMs2;
            this.isSeekable = isSeekable2;
            this.isDynamic = isDynamic2;
            this.defaultPositionUs = defaultPositionUs2;
            this.durationUs = durationUs2;
            this.firstPeriodIndex = firstPeriodIndex2;
            this.lastPeriodIndex = lastPeriodIndex2;
            this.positionInFirstPeriodUs = positionInFirstPeriodUs2;
            return this;
        }

        public long getDefaultPositionMs() {
            return C.usToMs(this.defaultPositionUs);
        }

        public long getDefaultPositionUs() {
            return this.defaultPositionUs;
        }

        public long getDurationMs() {
            return C.usToMs(this.durationUs);
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long getPositionInFirstPeriodMs() {
            return C.usToMs(this.positionInFirstPeriodUs);
        }

        public long getPositionInFirstPeriodUs() {
            return this.positionInFirstPeriodUs;
        }
    }

    public static final class Period {
        private int[] adCounts;
        private long[][] adDurationsUs;
        private long[] adGroupTimesUs;
        private long adResumePositionUs;
        private int[] adsLoadedCounts;
        private int[] adsPlayedCounts;
        public long durationUs;
        public Object id;
        private long positionInWindowUs;
        public Object uid;
        public int windowIndex;

        public Period set(Object id2, Object uid2, int windowIndex2, long durationUs2, long positionInWindowUs2) {
            return set(id2, uid2, windowIndex2, durationUs2, positionInWindowUs2, (long[]) null, (int[]) null, (int[]) null, (int[]) null, (long[][]) null, C.TIME_UNSET);
        }

        public Period set(Object id2, Object uid2, int windowIndex2, long durationUs2, long positionInWindowUs2, long[] adGroupTimesUs2, int[] adCounts2, int[] adsLoadedCounts2, int[] adsPlayedCounts2, long[][] adDurationsUs2, long adResumePositionUs2) {
            this.id = id2;
            this.uid = uid2;
            this.windowIndex = windowIndex2;
            this.durationUs = durationUs2;
            this.positionInWindowUs = positionInWindowUs2;
            this.adGroupTimesUs = adGroupTimesUs2;
            this.adCounts = adCounts2;
            this.adsLoadedCounts = adsLoadedCounts2;
            this.adsPlayedCounts = adsPlayedCounts2;
            this.adDurationsUs = adDurationsUs2;
            this.adResumePositionUs = adResumePositionUs2;
            return this;
        }

        public long getDurationMs() {
            return C.usToMs(this.durationUs);
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long getPositionInWindowMs() {
            return C.usToMs(this.positionInWindowUs);
        }

        public long getPositionInWindowUs() {
            return this.positionInWindowUs;
        }

        public int getAdGroupCount() {
            if (this.adGroupTimesUs == null) {
                return 0;
            }
            return this.adGroupTimesUs.length;
        }

        public long getAdGroupTimeUs(int adGroupIndex) {
            return this.adGroupTimesUs[adGroupIndex];
        }

        public int getPlayedAdCount(int adGroupIndex) {
            return this.adsPlayedCounts[adGroupIndex];
        }

        public boolean hasPlayedAdGroup(int adGroupIndex) {
            return this.adCounts[adGroupIndex] != -1 && this.adsPlayedCounts[adGroupIndex] == this.adCounts[adGroupIndex];
        }

        public int getAdGroupIndexForPositionUs(long positionUs) {
            if (this.adGroupTimesUs == null) {
                return -1;
            }
            int index = this.adGroupTimesUs.length - 1;
            while (index >= 0 && (this.adGroupTimesUs[index] == Long.MIN_VALUE || this.adGroupTimesUs[index] > positionUs)) {
                index--;
            }
            if (index < 0 || hasPlayedAdGroup(index)) {
                index = -1;
            }
            return index;
        }

        public int getAdGroupIndexAfterPositionUs(long positionUs) {
            if (this.adGroupTimesUs == null) {
                return -1;
            }
            int index = 0;
            while (index < this.adGroupTimesUs.length && this.adGroupTimesUs[index] != Long.MIN_VALUE && (positionUs >= this.adGroupTimesUs[index] || hasPlayedAdGroup(index))) {
                index++;
            }
            if (index >= this.adGroupTimesUs.length) {
                index = -1;
            }
            return index;
        }

        public int getAdCountInAdGroup(int adGroupIndex) {
            return this.adCounts[adGroupIndex];
        }

        public boolean isAdAvailable(int adGroupIndex, int adIndexInAdGroup) {
            return adIndexInAdGroup < this.adsLoadedCounts[adGroupIndex];
        }

        public long getAdDurationUs(int adGroupIndex, int adIndexInAdGroup) {
            if (adIndexInAdGroup >= this.adDurationsUs[adGroupIndex].length) {
                return C.TIME_UNSET;
            }
            return this.adDurationsUs[adGroupIndex][adIndexInAdGroup];
        }

        public long getAdResumePositionUs() {
            return this.adResumePositionUs;
        }
    }

    public final boolean isEmpty() {
        return getWindowCount() == 0;
    }

    public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
        switch (repeatMode) {
            case 0:
                if (windowIndex == getLastWindowIndex(shuffleModeEnabled)) {
                    return -1;
                }
                return windowIndex + 1;
            case 1:
                return windowIndex;
            case 2:
                if (windowIndex == getLastWindowIndex(shuffleModeEnabled)) {
                    return getFirstWindowIndex(shuffleModeEnabled);
                }
                return windowIndex + 1;
            default:
                throw new IllegalStateException();
        }
    }

    public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
        switch (repeatMode) {
            case 0:
                if (windowIndex == getFirstWindowIndex(shuffleModeEnabled)) {
                    return -1;
                }
                return windowIndex - 1;
            case 1:
                return windowIndex;
            case 2:
                if (windowIndex == getFirstWindowIndex(shuffleModeEnabled)) {
                    return getLastWindowIndex(shuffleModeEnabled);
                }
                return windowIndex - 1;
            default:
                throw new IllegalStateException();
        }
    }

    public int getLastWindowIndex(boolean shuffleModeEnabled) {
        if (isEmpty()) {
            return -1;
        }
        return getWindowCount() - 1;
    }

    public int getFirstWindowIndex(boolean shuffleModeEnabled) {
        return isEmpty() ? -1 : 0;
    }

    public final Window getWindow(int windowIndex, Window window) {
        return getWindow(windowIndex, window, false);
    }

    public final Window getWindow(int windowIndex, Window window, boolean setIds) {
        return getWindow(windowIndex, window, setIds, 0);
    }

    public final int getNextPeriodIndex(int periodIndex, Period period, Window window, int repeatMode, boolean shuffleModeEnabled) {
        int windowIndex = getPeriod(periodIndex, period).windowIndex;
        if (getWindow(windowIndex, window).lastPeriodIndex != periodIndex) {
            return periodIndex + 1;
        }
        int nextWindowIndex = getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
        if (nextWindowIndex == -1) {
            return -1;
        }
        return getWindow(nextWindowIndex, window).firstPeriodIndex;
    }

    public final boolean isLastPeriod(int periodIndex, Period period, Window window, int repeatMode, boolean shuffleModeEnabled) {
        return getNextPeriodIndex(periodIndex, period, window, repeatMode, shuffleModeEnabled) == -1;
    }

    public final Period getPeriod(int periodIndex, Period period) {
        return getPeriod(periodIndex, period, false);
    }

    public final Pair<Integer, Long> getPeriodPosition(Window window, Period period, int windowIndex, long windowPositionUs) {
        return getPeriodPosition(window, period, windowIndex, windowPositionUs, 0);
    }

    public final Pair<Integer, Long> getPeriodPosition(Window window, Period period, int windowIndex, long windowPositionUs, long defaultPositionProjectionUs) {
        Assertions.checkIndex(windowIndex, 0, getWindowCount());
        getWindow(windowIndex, window, false, defaultPositionProjectionUs);
        if (windowPositionUs == C.TIME_UNSET) {
            windowPositionUs = window.getDefaultPositionUs();
            if (windowPositionUs == C.TIME_UNSET) {
                return null;
            }
        }
        int periodIndex = window.firstPeriodIndex;
        long periodPositionUs = window.getPositionInFirstPeriodUs() + windowPositionUs;
        long periodDurationUs = getPeriod(periodIndex, period).getDurationUs();
        while (periodDurationUs != C.TIME_UNSET && periodPositionUs >= periodDurationUs && periodIndex < window.lastPeriodIndex) {
            periodPositionUs -= periodDurationUs;
            periodIndex++;
            periodDurationUs = getPeriod(periodIndex, period).getDurationUs();
        }
        return Pair.create(Integer.valueOf(periodIndex), Long.valueOf(periodPositionUs));
    }
}
