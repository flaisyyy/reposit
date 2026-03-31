package com.google.android.exoplayer2.source;

import android.util.Pair;
import com.google.android.exoplayer2.Timeline;

abstract class AbstractConcatenatedTimeline extends Timeline {
    private final int childCount;
    private final ShuffleOrder shuffleOrder;

    /* access modifiers changed from: protected */
    public abstract int getChildIndexByChildUid(Object obj);

    /* access modifiers changed from: protected */
    public abstract int getChildIndexByPeriodIndex(int i);

    /* access modifiers changed from: protected */
    public abstract int getChildIndexByWindowIndex(int i);

    /* access modifiers changed from: protected */
    public abstract Object getChildUidByChildIndex(int i);

    /* access modifiers changed from: protected */
    public abstract int getFirstPeriodIndexByChildIndex(int i);

    /* access modifiers changed from: protected */
    public abstract int getFirstWindowIndexByChildIndex(int i);

    /* access modifiers changed from: protected */
    public abstract Timeline getTimelineByChildIndex(int i);

    public AbstractConcatenatedTimeline(ShuffleOrder shuffleOrder2) {
        this.shuffleOrder = shuffleOrder2;
        this.childCount = shuffleOrder2.getLength();
    }

    public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
        int i;
        int childIndex = getChildIndexByWindowIndex(windowIndex);
        int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
        Timeline timelineByChildIndex = getTimelineByChildIndex(childIndex);
        int i2 = windowIndex - firstWindowIndexInChild;
        if (repeatMode == 2) {
            i = 0;
        } else {
            i = repeatMode;
        }
        int nextWindowIndexInChild = timelineByChildIndex.getNextWindowIndex(i2, i, shuffleModeEnabled);
        if (nextWindowIndexInChild != -1) {
            return firstWindowIndexInChild + nextWindowIndexInChild;
        }
        int nextChildIndex = getNextChildIndex(childIndex, shuffleModeEnabled);
        while (nextChildIndex != -1 && getTimelineByChildIndex(nextChildIndex).isEmpty()) {
            nextChildIndex = getNextChildIndex(nextChildIndex, shuffleModeEnabled);
        }
        if (nextChildIndex != -1) {
            return getFirstWindowIndexByChildIndex(nextChildIndex) + getTimelineByChildIndex(nextChildIndex).getFirstWindowIndex(shuffleModeEnabled);
        }
        if (repeatMode == 2) {
            return getFirstWindowIndex(shuffleModeEnabled);
        }
        return -1;
    }

    public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
        int i;
        int childIndex = getChildIndexByWindowIndex(windowIndex);
        int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
        Timeline timelineByChildIndex = getTimelineByChildIndex(childIndex);
        int i2 = windowIndex - firstWindowIndexInChild;
        if (repeatMode == 2) {
            i = 0;
        } else {
            i = repeatMode;
        }
        int previousWindowIndexInChild = timelineByChildIndex.getPreviousWindowIndex(i2, i, shuffleModeEnabled);
        if (previousWindowIndexInChild != -1) {
            return firstWindowIndexInChild + previousWindowIndexInChild;
        }
        int previousChildIndex = getPreviousChildIndex(childIndex, shuffleModeEnabled);
        while (previousChildIndex != -1 && getTimelineByChildIndex(previousChildIndex).isEmpty()) {
            previousChildIndex = getPreviousChildIndex(previousChildIndex, shuffleModeEnabled);
        }
        if (previousChildIndex != -1) {
            return getFirstWindowIndexByChildIndex(previousChildIndex) + getTimelineByChildIndex(previousChildIndex).getLastWindowIndex(shuffleModeEnabled);
        }
        if (repeatMode == 2) {
            return getLastWindowIndex(shuffleModeEnabled);
        }
        return -1;
    }

    public int getLastWindowIndex(boolean shuffleModeEnabled) {
        if (this.childCount == 0) {
            return -1;
        }
        int lastChildIndex = shuffleModeEnabled ? this.shuffleOrder.getLastIndex() : this.childCount - 1;
        while (getTimelineByChildIndex(lastChildIndex).isEmpty()) {
            lastChildIndex = getPreviousChildIndex(lastChildIndex, shuffleModeEnabled);
            if (lastChildIndex == -1) {
                return -1;
            }
        }
        return getFirstWindowIndexByChildIndex(lastChildIndex) + getTimelineByChildIndex(lastChildIndex).getLastWindowIndex(shuffleModeEnabled);
    }

    public int getFirstWindowIndex(boolean shuffleModeEnabled) {
        if (this.childCount == 0) {
            return -1;
        }
        int firstChildIndex = shuffleModeEnabled ? this.shuffleOrder.getFirstIndex() : 0;
        while (getTimelineByChildIndex(firstChildIndex).isEmpty()) {
            firstChildIndex = getNextChildIndex(firstChildIndex, shuffleModeEnabled);
            if (firstChildIndex == -1) {
                return -1;
            }
        }
        return getFirstWindowIndexByChildIndex(firstChildIndex) + getTimelineByChildIndex(firstChildIndex).getFirstWindowIndex(shuffleModeEnabled);
    }

    public final Timeline.Window getWindow(int windowIndex, Timeline.Window window, boolean setIds, long defaultPositionProjectionUs) {
        int childIndex = getChildIndexByWindowIndex(windowIndex);
        int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
        int firstPeriodIndexInChild = getFirstPeriodIndexByChildIndex(childIndex);
        getTimelineByChildIndex(childIndex).getWindow(windowIndex - firstWindowIndexInChild, window, setIds, defaultPositionProjectionUs);
        window.firstPeriodIndex += firstPeriodIndexInChild;
        window.lastPeriodIndex += firstPeriodIndexInChild;
        return window;
    }

    public final Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
        int childIndex = getChildIndexByPeriodIndex(periodIndex);
        int firstWindowIndexInChild = getFirstWindowIndexByChildIndex(childIndex);
        getTimelineByChildIndex(childIndex).getPeriod(periodIndex - getFirstPeriodIndexByChildIndex(childIndex), period, setIds);
        period.windowIndex += firstWindowIndexInChild;
        if (setIds) {
            period.uid = Pair.create(getChildUidByChildIndex(childIndex), period.uid);
        }
        return period;
    }

    public final int getIndexOfPeriod(Object uid) {
        int periodIndexInChild;
        if (!(uid instanceof Pair)) {
            return -1;
        }
        Pair<?, ?> childUidAndPeriodUid = (Pair) uid;
        Object childUid = childUidAndPeriodUid.first;
        Object periodUid = childUidAndPeriodUid.second;
        int childIndex = getChildIndexByChildUid(childUid);
        if (childIndex == -1 || (periodIndexInChild = getTimelineByChildIndex(childIndex).getIndexOfPeriod(periodUid)) == -1) {
            return -1;
        }
        return getFirstPeriodIndexByChildIndex(childIndex) + periodIndexInChild;
    }

    private int getNextChildIndex(int childIndex, boolean shuffleModeEnabled) {
        if (shuffleModeEnabled) {
            return this.shuffleOrder.getNextIndex(childIndex);
        }
        if (childIndex < this.childCount - 1) {
            return childIndex + 1;
        }
        return -1;
    }

    private int getPreviousChildIndex(int childIndex, boolean shuffleModeEnabled) {
        if (shuffleModeEnabled) {
            return this.shuffleOrder.getPreviousIndex(childIndex);
        }
        if (childIndex > 0) {
            return childIndex - 1;
        }
        return -1;
    }
}
