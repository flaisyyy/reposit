package com.google.android.exoplayer2;

import android.util.Pair;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;

final class MediaPeriodInfoSequence {
    private final Timeline.Period period = new Timeline.Period();
    private int repeatMode;
    private boolean shuffleModeEnabled;
    private Timeline timeline;
    private final Timeline.Window window = new Timeline.Window();

    public static final class MediaPeriodInfo {
        public final long contentPositionUs;
        public final long durationUs;
        public final long endPositionUs;
        public final MediaSource.MediaPeriodId id;
        public final boolean isFinal;
        public final boolean isLastInTimelinePeriod;
        public final long startPositionUs;

        private MediaPeriodInfo(MediaSource.MediaPeriodId id2, long startPositionUs2, long endPositionUs2, long contentPositionUs2, long durationUs2, boolean isLastInTimelinePeriod2, boolean isFinal2) {
            this.id = id2;
            this.startPositionUs = startPositionUs2;
            this.endPositionUs = endPositionUs2;
            this.contentPositionUs = contentPositionUs2;
            this.durationUs = durationUs2;
            this.isLastInTimelinePeriod = isLastInTimelinePeriod2;
            this.isFinal = isFinal2;
        }

        public MediaPeriodInfo copyWithPeriodIndex(int periodIndex) {
            return new MediaPeriodInfo(this.id.copyWithPeriodIndex(periodIndex), this.startPositionUs, this.endPositionUs, this.contentPositionUs, this.durationUs, this.isLastInTimelinePeriod, this.isFinal);
        }

        public MediaPeriodInfo copyWithStartPositionUs(long startPositionUs2) {
            return new MediaPeriodInfo(this.id, startPositionUs2, this.endPositionUs, this.contentPositionUs, this.durationUs, this.isLastInTimelinePeriod, this.isFinal);
        }
    }

    public void setTimeline(Timeline timeline2) {
        this.timeline = timeline2;
    }

    public void setRepeatMode(int repeatMode2) {
        this.repeatMode = repeatMode2;
    }

    public void setShuffleModeEnabled(boolean shuffleModeEnabled2) {
        this.shuffleModeEnabled = shuffleModeEnabled2;
    }

    public MediaPeriodInfo getFirstMediaPeriodInfo(PlaybackInfo playbackInfo) {
        return getMediaPeriodInfo(playbackInfo.periodId, playbackInfo.contentPositionUs, playbackInfo.startPositionUs);
    }

    public MediaPeriodInfo getNextMediaPeriodInfo(MediaPeriodInfo currentMediaPeriodInfo, long rendererOffsetUs, long rendererPositionUs) {
        long endUs;
        long startPositionUs;
        if (currentMediaPeriodInfo.isLastInTimelinePeriod) {
            int nextPeriodIndex = this.timeline.getNextPeriodIndex(currentMediaPeriodInfo.id.periodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
            if (nextPeriodIndex == -1) {
                return null;
            }
            int nextWindowIndex = this.timeline.getPeriod(nextPeriodIndex, this.period).windowIndex;
            if (this.timeline.getWindow(nextWindowIndex, this.window).firstPeriodIndex == nextPeriodIndex) {
                Pair<Integer, Long> defaultPosition = this.timeline.getPeriodPosition(this.window, this.period, nextWindowIndex, C.TIME_UNSET, Math.max(0, (currentMediaPeriodInfo.durationUs + rendererOffsetUs) - rendererPositionUs));
                if (defaultPosition == null) {
                    return null;
                }
                nextPeriodIndex = ((Integer) defaultPosition.first).intValue();
                startPositionUs = ((Long) defaultPosition.second).longValue();
            } else {
                startPositionUs = 0;
            }
            return getMediaPeriodInfo(resolvePeriodPositionForAds(nextPeriodIndex, startPositionUs), startPositionUs, startPositionUs);
        }
        MediaSource.MediaPeriodId currentPeriodId = currentMediaPeriodInfo.id;
        if (currentPeriodId.isAd()) {
            int currentAdGroupIndex = currentPeriodId.adGroupIndex;
            this.timeline.getPeriod(currentPeriodId.periodIndex, this.period);
            int adCountInCurrentAdGroup = this.period.getAdCountInAdGroup(currentAdGroupIndex);
            if (adCountInCurrentAdGroup == -1) {
                return null;
            }
            int nextAdIndexInAdGroup = currentPeriodId.adIndexInAdGroup + 1;
            if (nextAdIndexInAdGroup >= adCountInCurrentAdGroup) {
                int nextAdGroupIndex = this.period.getAdGroupIndexAfterPositionUs(currentMediaPeriodInfo.contentPositionUs);
                if (nextAdGroupIndex == -1) {
                    endUs = Long.MIN_VALUE;
                } else {
                    endUs = this.period.getAdGroupTimeUs(nextAdGroupIndex);
                }
                return getMediaPeriodInfoForContent(currentPeriodId.periodIndex, currentMediaPeriodInfo.contentPositionUs, endUs);
            } else if (!this.period.isAdAvailable(currentAdGroupIndex, nextAdIndexInAdGroup)) {
                return null;
            } else {
                return getMediaPeriodInfoForAd(currentPeriodId.periodIndex, currentAdGroupIndex, nextAdIndexInAdGroup, currentMediaPeriodInfo.contentPositionUs);
            }
        } else if (currentMediaPeriodInfo.endPositionUs != Long.MIN_VALUE) {
            int nextAdGroupIndex2 = this.period.getAdGroupIndexForPositionUs(currentMediaPeriodInfo.endPositionUs);
            if (!this.period.isAdAvailable(nextAdGroupIndex2, 0)) {
                return null;
            }
            return getMediaPeriodInfoForAd(currentPeriodId.periodIndex, nextAdGroupIndex2, 0, currentMediaPeriodInfo.endPositionUs);
        } else {
            int adGroupCount = this.period.getAdGroupCount();
            if (adGroupCount == 0 || this.period.getAdGroupTimeUs(adGroupCount - 1) != Long.MIN_VALUE || this.period.hasPlayedAdGroup(adGroupCount - 1) || !this.period.isAdAvailable(adGroupCount - 1, 0)) {
                return null;
            }
            return getMediaPeriodInfoForAd(currentPeriodId.periodIndex, adGroupCount - 1, 0, this.period.getDurationUs());
        }
    }

    public MediaSource.MediaPeriodId resolvePeriodPositionForAds(int periodIndex, long positionUs) {
        this.timeline.getPeriod(periodIndex, this.period);
        int adGroupIndex = this.period.getAdGroupIndexForPositionUs(positionUs);
        if (adGroupIndex == -1) {
            return new MediaSource.MediaPeriodId(periodIndex);
        }
        return new MediaSource.MediaPeriodId(periodIndex, adGroupIndex, this.period.getPlayedAdCount(adGroupIndex));
    }

    public MediaPeriodInfo getUpdatedMediaPeriodInfo(MediaPeriodInfo mediaPeriodInfo) {
        return getUpdatedMediaPeriodInfo(mediaPeriodInfo, mediaPeriodInfo.id);
    }

    public MediaPeriodInfo getUpdatedMediaPeriodInfo(MediaPeriodInfo mediaPeriodInfo, int newPeriodIndex) {
        return getUpdatedMediaPeriodInfo(mediaPeriodInfo, mediaPeriodInfo.id.copyWithPeriodIndex(newPeriodIndex));
    }

    private MediaPeriodInfo getUpdatedMediaPeriodInfo(MediaPeriodInfo info, MediaSource.MediaPeriodId newId) {
        long durationUs;
        long startPositionUs = info.startPositionUs;
        long endPositionUs = info.endPositionUs;
        boolean isLastInPeriod = isLastInPeriod(newId, endPositionUs);
        boolean isLastInTimeline = isLastInTimeline(newId, isLastInPeriod);
        this.timeline.getPeriod(newId.periodIndex, this.period);
        if (newId.isAd()) {
            durationUs = this.period.getAdDurationUs(newId.adGroupIndex, newId.adIndexInAdGroup);
        } else {
            durationUs = endPositionUs == Long.MIN_VALUE ? this.period.getDurationUs() : endPositionUs;
        }
        return new MediaPeriodInfo(newId, startPositionUs, endPositionUs, info.contentPositionUs, durationUs, isLastInPeriod, isLastInTimeline);
    }

    private MediaPeriodInfo getMediaPeriodInfo(MediaSource.MediaPeriodId id, long contentPositionUs, long startPositionUs) {
        long endUs;
        this.timeline.getPeriod(id.periodIndex, this.period);
        if (!id.isAd()) {
            int nextAdGroupIndex = this.period.getAdGroupIndexAfterPositionUs(startPositionUs);
            if (nextAdGroupIndex == -1) {
                endUs = Long.MIN_VALUE;
            } else {
                endUs = this.period.getAdGroupTimeUs(nextAdGroupIndex);
            }
            return getMediaPeriodInfoForContent(id.periodIndex, startPositionUs, endUs);
        } else if (!this.period.isAdAvailable(id.adGroupIndex, id.adIndexInAdGroup)) {
            return null;
        } else {
            return getMediaPeriodInfoForAd(id.periodIndex, id.adGroupIndex, id.adIndexInAdGroup, contentPositionUs);
        }
    }

    private MediaPeriodInfo getMediaPeriodInfoForAd(int periodIndex, int adGroupIndex, int adIndexInAdGroup, long contentPositionUs) {
        MediaSource.MediaPeriodId id = new MediaSource.MediaPeriodId(periodIndex, adGroupIndex, adIndexInAdGroup);
        boolean isLastInPeriod = isLastInPeriod(id, Long.MIN_VALUE);
        boolean isLastInTimeline = isLastInTimeline(id, isLastInPeriod);
        return new MediaPeriodInfo(id, adIndexInAdGroup == this.period.getPlayedAdCount(adGroupIndex) ? this.period.getAdResumePositionUs() : 0, Long.MIN_VALUE, contentPositionUs, this.timeline.getPeriod(id.periodIndex, this.period).getAdDurationUs(id.adGroupIndex, id.adIndexInAdGroup), isLastInPeriod, isLastInTimeline);
    }

    private MediaPeriodInfo getMediaPeriodInfoForContent(int periodIndex, long startPositionUs, long endUs) {
        long durationUs;
        MediaSource.MediaPeriodId id = new MediaSource.MediaPeriodId(periodIndex);
        boolean isLastInPeriod = isLastInPeriod(id, endUs);
        boolean isLastInTimeline = isLastInTimeline(id, isLastInPeriod);
        this.timeline.getPeriod(id.periodIndex, this.period);
        if (endUs == Long.MIN_VALUE) {
            durationUs = this.period.getDurationUs();
        } else {
            durationUs = endUs;
        }
        return new MediaPeriodInfo(id, startPositionUs, endUs, C.TIME_UNSET, durationUs, isLastInPeriod, isLastInTimeline);
    }

    private boolean isLastInPeriod(MediaSource.MediaPeriodId id, long endPositionUs) {
        boolean isLastAd;
        boolean z = false;
        int adGroupCount = this.timeline.getPeriod(id.periodIndex, this.period).getAdGroupCount();
        if (adGroupCount == 0) {
            return true;
        }
        int lastAdGroupIndex = adGroupCount - 1;
        boolean isAd = id.isAd();
        if (this.period.getAdGroupTimeUs(lastAdGroupIndex) == Long.MIN_VALUE) {
            int postrollAdCount = this.period.getAdCountInAdGroup(lastAdGroupIndex);
            if (postrollAdCount == -1) {
                return false;
            }
            if (isAd && id.adGroupIndex == lastAdGroupIndex && id.adIndexInAdGroup == postrollAdCount - 1) {
                isLastAd = true;
            } else {
                isLastAd = false;
            }
            if (isLastAd || (!isAd && this.period.getPlayedAdCount(lastAdGroupIndex) == postrollAdCount)) {
                z = true;
            }
            return z;
        } else if (isAd || endPositionUs != Long.MIN_VALUE) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isLastInTimeline(MediaSource.MediaPeriodId id, boolean isLastMediaPeriodInPeriod) {
        return !this.timeline.getWindow(this.timeline.getPeriod(id.periodIndex, this.period).windowIndex, this.window).isDynamic && this.timeline.isLastPeriod(id.periodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled) && isLastMediaPeriodInPeriod;
    }
}
