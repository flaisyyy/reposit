package com.google.android.exoplayer2.source;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ShuffleOrder;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class DynamicConcatenatingMediaSource implements MediaSource, ExoPlayer.ExoPlayerComponent {
    private static final int MSG_ADD = 0;
    private static final int MSG_ADD_MULTIPLE = 1;
    private static final int MSG_MOVE = 3;
    private static final int MSG_ON_COMPLETION = 4;
    private static final int MSG_REMOVE = 2;
    private final List<DeferredMediaPeriod> deferredMediaPeriods;
    private MediaSource.Listener listener;
    private final Map<MediaPeriod, MediaSource> mediaSourceByMediaPeriod;
    private final List<MediaSourceHolder> mediaSourceHolders;
    private final List<MediaSource> mediaSourcesPublic;
    private int periodCount;
    private ExoPlayer player;
    private boolean preventListenerNotification;
    private final MediaSourceHolder query;
    private ShuffleOrder shuffleOrder;
    private int windowCount;

    public DynamicConcatenatingMediaSource() {
        this(new ShuffleOrder.DefaultShuffleOrder(0));
    }

    public DynamicConcatenatingMediaSource(ShuffleOrder shuffleOrder2) {
        this.shuffleOrder = shuffleOrder2;
        this.mediaSourceByMediaPeriod = new IdentityHashMap();
        this.mediaSourcesPublic = new ArrayList();
        this.mediaSourceHolders = new ArrayList();
        this.deferredMediaPeriods = new ArrayList(1);
        this.query = new MediaSourceHolder((MediaSource) null, (DeferredTimeline) null, -1, -1, -1);
    }

    public synchronized void addMediaSource(MediaSource mediaSource) {
        addMediaSource(this.mediaSourcesPublic.size(), mediaSource, (Runnable) null);
    }

    public synchronized void addMediaSource(MediaSource mediaSource, @Nullable Runnable actionOnCompletion) {
        addMediaSource(this.mediaSourcesPublic.size(), mediaSource, actionOnCompletion);
    }

    public synchronized void addMediaSource(int index, MediaSource mediaSource) {
        addMediaSource(index, mediaSource, (Runnable) null);
    }

    public synchronized void addMediaSource(int index, MediaSource mediaSource, @Nullable Runnable actionOnCompletion) {
        boolean z = true;
        synchronized (this) {
            Assertions.checkNotNull(mediaSource);
            if (this.mediaSourcesPublic.contains(mediaSource)) {
                z = false;
            }
            Assertions.checkArgument(z);
            this.mediaSourcesPublic.add(index, mediaSource);
            if (this.player != null) {
                this.player.sendMessages(new ExoPlayer.ExoPlayerMessage(this, 0, new MessageData(index, mediaSource, actionOnCompletion)));
            } else if (actionOnCompletion != null) {
                actionOnCompletion.run();
            }
        }
    }

    public synchronized void addMediaSources(Collection<MediaSource> mediaSources) {
        addMediaSources(this.mediaSourcesPublic.size(), mediaSources, (Runnable) null);
    }

    public synchronized void addMediaSources(Collection<MediaSource> mediaSources, @Nullable Runnable actionOnCompletion) {
        addMediaSources(this.mediaSourcesPublic.size(), mediaSources, actionOnCompletion);
    }

    public synchronized void addMediaSources(int index, Collection<MediaSource> mediaSources) {
        addMediaSources(index, mediaSources, (Runnable) null);
    }

    public synchronized void addMediaSources(int index, Collection<MediaSource> mediaSources, @Nullable Runnable actionOnCompletion) {
        boolean z;
        for (MediaSource mediaSource : mediaSources) {
            Assertions.checkNotNull(mediaSource);
            if (!this.mediaSourcesPublic.contains(mediaSource)) {
                z = true;
            } else {
                z = false;
            }
            Assertions.checkArgument(z);
        }
        this.mediaSourcesPublic.addAll(index, mediaSources);
        if (this.player != null && !mediaSources.isEmpty()) {
            this.player.sendMessages(new ExoPlayer.ExoPlayerMessage(this, 1, new MessageData(index, mediaSources, actionOnCompletion)));
        } else if (actionOnCompletion != null) {
            actionOnCompletion.run();
        }
    }

    public synchronized void removeMediaSource(int index) {
        removeMediaSource(index, (Runnable) null);
    }

    public synchronized void removeMediaSource(int index, @Nullable Runnable actionOnCompletion) {
        this.mediaSourcesPublic.remove(index);
        if (this.player != null) {
            this.player.sendMessages(new ExoPlayer.ExoPlayerMessage(this, 2, new MessageData(index, null, actionOnCompletion)));
        } else if (actionOnCompletion != null) {
            actionOnCompletion.run();
        }
    }

    public synchronized void moveMediaSource(int currentIndex, int newIndex) {
        moveMediaSource(currentIndex, newIndex, (Runnable) null);
    }

    public synchronized void moveMediaSource(int currentIndex, int newIndex, @Nullable Runnable actionOnCompletion) {
        if (currentIndex != newIndex) {
            this.mediaSourcesPublic.add(newIndex, this.mediaSourcesPublic.remove(currentIndex));
            if (this.player != null) {
                this.player.sendMessages(new ExoPlayer.ExoPlayerMessage(this, 3, new MessageData(currentIndex, Integer.valueOf(newIndex), actionOnCompletion)));
            } else if (actionOnCompletion != null) {
                actionOnCompletion.run();
            }
        }
    }

    public synchronized int getSize() {
        return this.mediaSourcesPublic.size();
    }

    public synchronized MediaSource getMediaSource(int index) {
        return this.mediaSourcesPublic.get(index);
    }

    public synchronized void prepareSource(ExoPlayer player2, boolean isTopLevelSource, MediaSource.Listener listener2) {
        this.player = player2;
        this.listener = listener2;
        this.preventListenerNotification = true;
        this.shuffleOrder = this.shuffleOrder.cloneAndInsert(0, this.mediaSourcesPublic.size());
        addMediaSourcesInternal(0, this.mediaSourcesPublic);
        this.preventListenerNotification = false;
        maybeNotifyListener((EventDispatcher) null);
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
        for (int i = 0; i < this.mediaSourceHolders.size(); i++) {
            this.mediaSourceHolders.get(i).mediaSource.maybeThrowSourceInfoRefreshError();
        }
    }

    public MediaPeriod createPeriod(MediaSource.MediaPeriodId id, Allocator allocator) {
        MediaPeriod mediaPeriod;
        MediaSourceHolder holder = this.mediaSourceHolders.get(findMediaSourceHolderByPeriodIndex(id.periodIndex));
        MediaSource.MediaPeriodId idInSource = id.copyWithPeriodIndex(id.periodIndex - holder.firstPeriodIndexInChild);
        if (!holder.isPrepared) {
            mediaPeriod = new DeferredMediaPeriod(holder.mediaSource, idInSource, allocator);
            this.deferredMediaPeriods.add((DeferredMediaPeriod) mediaPeriod);
        } else {
            mediaPeriod = holder.mediaSource.createPeriod(idInSource, allocator);
        }
        this.mediaSourceByMediaPeriod.put(mediaPeriod, holder.mediaSource);
        return mediaPeriod;
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        MediaSource mediaSource = this.mediaSourceByMediaPeriod.get(mediaPeriod);
        this.mediaSourceByMediaPeriod.remove(mediaPeriod);
        if (mediaPeriod instanceof DeferredMediaPeriod) {
            this.deferredMediaPeriods.remove(mediaPeriod);
            ((DeferredMediaPeriod) mediaPeriod).releasePeriod();
            return;
        }
        mediaSource.releasePeriod(mediaPeriod);
    }

    public void releaseSource() {
        for (int i = 0; i < this.mediaSourceHolders.size(); i++) {
            this.mediaSourceHolders.get(i).mediaSource.releaseSource();
        }
    }

    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        EventDispatcher actionOnCompletion;
        if (messageType == 4) {
            ((EventDispatcher) message).dispatchEvent();
            return;
        }
        this.preventListenerNotification = true;
        switch (messageType) {
            case 0:
                MessageData<MediaSource> messageData = (MessageData) message;
                this.shuffleOrder = this.shuffleOrder.cloneAndInsert(messageData.index, 1);
                addMediaSourceInternal(messageData.index, (MediaSource) messageData.customData);
                actionOnCompletion = messageData.actionOnCompletion;
                break;
            case 1:
                MessageData<Collection<MediaSource>> messageData2 = (MessageData) message;
                this.shuffleOrder = this.shuffleOrder.cloneAndInsert(messageData2.index, ((Collection) messageData2.customData).size());
                addMediaSourcesInternal(messageData2.index, (Collection) messageData2.customData);
                actionOnCompletion = messageData2.actionOnCompletion;
                break;
            case 2:
                MessageData<Void> messageData3 = (MessageData) message;
                this.shuffleOrder = this.shuffleOrder.cloneAndRemove(messageData3.index);
                removeMediaSourceInternal(messageData3.index);
                actionOnCompletion = messageData3.actionOnCompletion;
                break;
            case 3:
                MessageData<Integer> messageData4 = (MessageData) message;
                this.shuffleOrder = this.shuffleOrder.cloneAndRemove(messageData4.index);
                this.shuffleOrder = this.shuffleOrder.cloneAndInsert(((Integer) messageData4.customData).intValue(), 1);
                moveMediaSourceInternal(messageData4.index, ((Integer) messageData4.customData).intValue());
                actionOnCompletion = messageData4.actionOnCompletion;
                break;
            default:
                throw new IllegalStateException();
        }
        this.preventListenerNotification = false;
        maybeNotifyListener(actionOnCompletion);
    }

    private void maybeNotifyListener(@Nullable EventDispatcher actionOnCompletion) {
        if (!this.preventListenerNotification) {
            this.listener.onSourceInfoRefreshed(this, new ConcatenatedTimeline(this.mediaSourceHolders, this.windowCount, this.periodCount, this.shuffleOrder), (Object) null);
            if (actionOnCompletion != null) {
                this.player.sendMessages(new ExoPlayer.ExoPlayerMessage(this, 4, actionOnCompletion));
            }
        }
    }

    private void addMediaSourceInternal(int newIndex, MediaSource newMediaSource) {
        final MediaSourceHolder newMediaSourceHolder;
        Integer newUid = Integer.valueOf(System.identityHashCode(newMediaSource));
        DeferredTimeline newTimeline = new DeferredTimeline();
        if (newIndex > 0) {
            MediaSourceHolder previousHolder = this.mediaSourceHolders.get(newIndex - 1);
            newMediaSourceHolder = new MediaSourceHolder(newMediaSource, newTimeline, previousHolder.timeline.getWindowCount() + previousHolder.firstWindowIndexInChild, previousHolder.timeline.getPeriodCount() + previousHolder.firstPeriodIndexInChild, newUid);
        } else {
            newMediaSourceHolder = new MediaSourceHolder(newMediaSource, newTimeline, 0, 0, newUid);
        }
        correctOffsets(newIndex, newTimeline.getWindowCount(), newTimeline.getPeriodCount());
        this.mediaSourceHolders.add(newIndex, newMediaSourceHolder);
        newMediaSourceHolder.mediaSource.prepareSource(this.player, false, new MediaSource.Listener() {
            public void onSourceInfoRefreshed(MediaSource source, Timeline newTimeline, Object manifest) {
                DynamicConcatenatingMediaSource.this.updateMediaSourceInternal(newMediaSourceHolder, newTimeline);
            }
        });
    }

    private void addMediaSourcesInternal(int index, Collection<MediaSource> mediaSources) {
        for (MediaSource mediaSource : mediaSources) {
            addMediaSourceInternal(index, mediaSource);
            index++;
        }
    }

    /* access modifiers changed from: private */
    public void updateMediaSourceInternal(MediaSourceHolder mediaSourceHolder, Timeline timeline) {
        if (mediaSourceHolder == null) {
            throw new IllegalArgumentException();
        }
        DeferredTimeline deferredTimeline = mediaSourceHolder.timeline;
        if (deferredTimeline.getTimeline() != timeline) {
            int windowOffsetUpdate = timeline.getWindowCount() - deferredTimeline.getWindowCount();
            int periodOffsetUpdate = timeline.getPeriodCount() - deferredTimeline.getPeriodCount();
            if (!(windowOffsetUpdate == 0 && periodOffsetUpdate == 0)) {
                correctOffsets(findMediaSourceHolderByPeriodIndex(mediaSourceHolder.firstPeriodIndexInChild) + 1, windowOffsetUpdate, periodOffsetUpdate);
            }
            mediaSourceHolder.timeline = deferredTimeline.cloneWithNewTimeline(timeline);
            if (!mediaSourceHolder.isPrepared) {
                for (int i = this.deferredMediaPeriods.size() - 1; i >= 0; i--) {
                    if (this.deferredMediaPeriods.get(i).mediaSource == mediaSourceHolder.mediaSource) {
                        this.deferredMediaPeriods.get(i).createPeriod();
                        this.deferredMediaPeriods.remove(i);
                    }
                }
            }
            mediaSourceHolder.isPrepared = true;
            maybeNotifyListener((EventDispatcher) null);
        }
    }

    private void removeMediaSourceInternal(int index) {
        MediaSourceHolder holder = this.mediaSourceHolders.get(index);
        this.mediaSourceHolders.remove(index);
        Timeline oldTimeline = holder.timeline;
        correctOffsets(index, -oldTimeline.getWindowCount(), -oldTimeline.getPeriodCount());
        holder.mediaSource.releaseSource();
    }

    private void moveMediaSourceInternal(int currentIndex, int newIndex) {
        int startIndex = Math.min(currentIndex, newIndex);
        int endIndex = Math.max(currentIndex, newIndex);
        int windowOffset = this.mediaSourceHolders.get(startIndex).firstWindowIndexInChild;
        int periodOffset = this.mediaSourceHolders.get(startIndex).firstPeriodIndexInChild;
        this.mediaSourceHolders.add(newIndex, this.mediaSourceHolders.remove(currentIndex));
        for (int i = startIndex; i <= endIndex; i++) {
            MediaSourceHolder holder = this.mediaSourceHolders.get(i);
            holder.firstWindowIndexInChild = windowOffset;
            holder.firstPeriodIndexInChild = periodOffset;
            windowOffset += holder.timeline.getWindowCount();
            periodOffset += holder.timeline.getPeriodCount();
        }
    }

    private void correctOffsets(int startIndex, int windowOffsetUpdate, int periodOffsetUpdate) {
        this.windowCount += windowOffsetUpdate;
        this.periodCount += periodOffsetUpdate;
        for (int i = startIndex; i < this.mediaSourceHolders.size(); i++) {
            this.mediaSourceHolders.get(i).firstWindowIndexInChild += windowOffsetUpdate;
            this.mediaSourceHolders.get(i).firstPeriodIndexInChild += periodOffsetUpdate;
        }
    }

    private int findMediaSourceHolderByPeriodIndex(int periodIndex) {
        this.query.firstPeriodIndexInChild = periodIndex;
        int index = Collections.binarySearch(this.mediaSourceHolders, this.query);
        if (index < 0) {
            return (-index) - 2;
        }
        while (index < this.mediaSourceHolders.size() - 1 && this.mediaSourceHolders.get(index + 1).firstPeriodIndexInChild == periodIndex) {
            index++;
        }
        return index;
    }

    private static final class MediaSourceHolder implements Comparable<MediaSourceHolder> {
        public int firstPeriodIndexInChild;
        public int firstWindowIndexInChild;
        public boolean isPrepared;
        public final MediaSource mediaSource;
        public DeferredTimeline timeline;
        public final Object uid;

        public MediaSourceHolder(MediaSource mediaSource2, DeferredTimeline timeline2, int window, int period, Object uid2) {
            this.mediaSource = mediaSource2;
            this.timeline = timeline2;
            this.firstWindowIndexInChild = window;
            this.firstPeriodIndexInChild = period;
            this.uid = uid2;
        }

        public int compareTo(@NonNull MediaSourceHolder other) {
            return this.firstPeriodIndexInChild - other.firstPeriodIndexInChild;
        }
    }

    private static final class EventDispatcher {
        public final Handler eventHandler;
        public final Runnable runnable;

        public EventDispatcher(Runnable runnable2) {
            Looper mainLooper;
            this.runnable = runnable2;
            if (Looper.myLooper() != null) {
                mainLooper = Looper.myLooper();
            } else {
                mainLooper = Looper.getMainLooper();
            }
            this.eventHandler = new Handler(mainLooper);
        }

        public void dispatchEvent() {
            this.eventHandler.post(this.runnable);
        }
    }

    private static final class MessageData<CustomType> {
        @Nullable
        public final EventDispatcher actionOnCompletion;
        public final CustomType customData;
        public final int index;

        public MessageData(int index2, CustomType customData2, @Nullable Runnable actionOnCompletion2) {
            this.index = index2;
            this.actionOnCompletion = actionOnCompletion2 != null ? new EventDispatcher(actionOnCompletion2) : null;
            this.customData = customData2;
        }
    }

    private static final class ConcatenatedTimeline extends AbstractConcatenatedTimeline {
        private final SparseIntArray childIndexByUid = new SparseIntArray();
        private final int[] firstPeriodInChildIndices;
        private final int[] firstWindowInChildIndices;
        private final int periodCount;
        private final Timeline[] timelines;
        private final int[] uids;
        private final int windowCount;

        public ConcatenatedTimeline(Collection<MediaSourceHolder> mediaSourceHolders, int windowCount2, int periodCount2, ShuffleOrder shuffleOrder) {
            super(shuffleOrder);
            this.windowCount = windowCount2;
            this.periodCount = periodCount2;
            int childCount = mediaSourceHolders.size();
            this.firstPeriodInChildIndices = new int[childCount];
            this.firstWindowInChildIndices = new int[childCount];
            this.timelines = new Timeline[childCount];
            this.uids = new int[childCount];
            int index = 0;
            for (MediaSourceHolder mediaSourceHolder : mediaSourceHolders) {
                this.timelines[index] = mediaSourceHolder.timeline;
                this.firstPeriodInChildIndices[index] = mediaSourceHolder.firstPeriodIndexInChild;
                this.firstWindowInChildIndices[index] = mediaSourceHolder.firstWindowIndexInChild;
                this.uids[index] = ((Integer) mediaSourceHolder.uid).intValue();
                this.childIndexByUid.put(this.uids[index], index);
                index++;
            }
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByPeriodIndex(int periodIndex) {
            return Util.binarySearchFloor(this.firstPeriodInChildIndices, periodIndex + 1, false, false);
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByWindowIndex(int windowIndex) {
            return Util.binarySearchFloor(this.firstWindowInChildIndices, windowIndex + 1, false, false);
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByChildUid(Object childUid) {
            if (!(childUid instanceof Integer)) {
                return -1;
            }
            int index = this.childIndexByUid.get(((Integer) childUid).intValue(), -1);
            if (index == -1) {
                index = -1;
            }
            return index;
        }

        /* access modifiers changed from: protected */
        public Timeline getTimelineByChildIndex(int childIndex) {
            return this.timelines[childIndex];
        }

        /* access modifiers changed from: protected */
        public int getFirstPeriodIndexByChildIndex(int childIndex) {
            return this.firstPeriodInChildIndices[childIndex];
        }

        /* access modifiers changed from: protected */
        public int getFirstWindowIndexByChildIndex(int childIndex) {
            return this.firstWindowInChildIndices[childIndex];
        }

        /* access modifiers changed from: protected */
        public Object getChildUidByChildIndex(int childIndex) {
            return Integer.valueOf(this.uids[childIndex]);
        }

        public int getWindowCount() {
            return this.windowCount;
        }

        public int getPeriodCount() {
            return this.periodCount;
        }
    }

    private static final class DeferredTimeline extends Timeline {
        private static final Object DUMMY_ID = new Object();
        private static final Timeline.Period period = new Timeline.Period();
        private final Object replacedID;
        private final Timeline timeline;

        public DeferredTimeline() {
            this.timeline = null;
            this.replacedID = null;
        }

        private DeferredTimeline(Timeline timeline2, Object replacedID2) {
            this.timeline = timeline2;
            this.replacedID = replacedID2;
        }

        public DeferredTimeline cloneWithNewTimeline(Timeline timeline2) {
            return new DeferredTimeline(timeline2, (this.replacedID != null || timeline2.getPeriodCount() <= 0) ? this.replacedID : timeline2.getPeriod(0, period, true).uid);
        }

        public Timeline getTimeline() {
            return this.timeline;
        }

        public int getWindowCount() {
            if (this.timeline == null) {
                return 1;
            }
            return this.timeline.getWindowCount();
        }

        public Timeline.Window getWindow(int windowIndex, Timeline.Window window, boolean setIds, long defaultPositionProjectionUs) {
            if (this.timeline != null) {
                return this.timeline.getWindow(windowIndex, window, setIds, defaultPositionProjectionUs);
            }
            return window.set(setIds ? DUMMY_ID : null, C.TIME_UNSET, C.TIME_UNSET, false, true, 0, C.TIME_UNSET, 0, 0, 0);
        }

        public int getPeriodCount() {
            if (this.timeline == null) {
                return 1;
            }
            return this.timeline.getPeriodCount();
        }

        public Timeline.Period getPeriod(int periodIndex, Timeline.Period period2, boolean setIds) {
            Object obj;
            Object obj2 = null;
            if (this.timeline == null) {
                if (setIds) {
                    obj = DUMMY_ID;
                } else {
                    obj = null;
                }
                if (setIds) {
                    obj2 = DUMMY_ID;
                }
                return period2.set(obj, obj2, 0, C.TIME_UNSET, C.TIME_UNSET);
            }
            this.timeline.getPeriod(periodIndex, period2, setIds);
            if (period2.uid != this.replacedID) {
                return period2;
            }
            period2.uid = DUMMY_ID;
            return period2;
        }

        public int getIndexOfPeriod(Object uid) {
            if (this.timeline == null) {
                return uid == DUMMY_ID ? 0 : -1;
            }
            Timeline timeline2 = this.timeline;
            if (uid == DUMMY_ID) {
                uid = this.replacedID;
            }
            return timeline2.getIndexOfPeriod(uid);
        }
    }
}
