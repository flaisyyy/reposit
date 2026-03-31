package com.google.android.exoplayer2.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpliceScheduleCommand extends SpliceCommand {
    public static final Parcelable.Creator<SpliceScheduleCommand> CREATOR = new Parcelable.Creator<SpliceScheduleCommand>() {
        public SpliceScheduleCommand createFromParcel(Parcel in) {
            return new SpliceScheduleCommand(in);
        }

        public SpliceScheduleCommand[] newArray(int size) {
            return new SpliceScheduleCommand[size];
        }
    };
    public final List<Event> events;

    public static final class Event {
        public final boolean autoReturn;
        public final int availNum;
        public final int availsExpected;
        public final long breakDurationUs;
        public final List<ComponentSplice> componentSpliceList;
        public final boolean outOfNetworkIndicator;
        public final boolean programSpliceFlag;
        public final boolean spliceEventCancelIndicator;
        public final long spliceEventId;
        public final int uniqueProgramId;
        public final long utcSpliceTime;

        private Event(long spliceEventId2, boolean spliceEventCancelIndicator2, boolean outOfNetworkIndicator2, boolean programSpliceFlag2, List<ComponentSplice> componentSpliceList2, long utcSpliceTime2, boolean autoReturn2, long breakDurationUs2, int uniqueProgramId2, int availNum2, int availsExpected2) {
            this.spliceEventId = spliceEventId2;
            this.spliceEventCancelIndicator = spliceEventCancelIndicator2;
            this.outOfNetworkIndicator = outOfNetworkIndicator2;
            this.programSpliceFlag = programSpliceFlag2;
            this.componentSpliceList = Collections.unmodifiableList(componentSpliceList2);
            this.utcSpliceTime = utcSpliceTime2;
            this.autoReturn = autoReturn2;
            this.breakDurationUs = breakDurationUs2;
            this.uniqueProgramId = uniqueProgramId2;
            this.availNum = availNum2;
            this.availsExpected = availsExpected2;
        }

        private Event(Parcel in) {
            boolean z;
            boolean z2;
            boolean z3;
            boolean z4 = true;
            this.spliceEventId = in.readLong();
            if (in.readByte() == 1) {
                z = true;
            } else {
                z = false;
            }
            this.spliceEventCancelIndicator = z;
            if (in.readByte() == 1) {
                z2 = true;
            } else {
                z2 = false;
            }
            this.outOfNetworkIndicator = z2;
            if (in.readByte() == 1) {
                z3 = true;
            } else {
                z3 = false;
            }
            this.programSpliceFlag = z3;
            int componentSpliceListLength = in.readInt();
            ArrayList<ComponentSplice> componentSpliceList2 = new ArrayList<>(componentSpliceListLength);
            for (int i = 0; i < componentSpliceListLength; i++) {
                componentSpliceList2.add(ComponentSplice.createFromParcel(in));
            }
            this.componentSpliceList = Collections.unmodifiableList(componentSpliceList2);
            this.utcSpliceTime = in.readLong();
            this.autoReturn = in.readByte() != 1 ? false : z4;
            this.breakDurationUs = in.readLong();
            this.uniqueProgramId = in.readInt();
            this.availNum = in.readInt();
            this.availsExpected = in.readInt();
        }

        /* access modifiers changed from: private */
        public static Event parseFromSection(ParsableByteArray sectionData) {
            long spliceEventId2 = sectionData.readUnsignedInt();
            boolean spliceEventCancelIndicator2 = (sectionData.readUnsignedByte() & 128) != 0;
            boolean outOfNetworkIndicator2 = false;
            boolean programSpliceFlag2 = false;
            long utcSpliceTime2 = C.TIME_UNSET;
            ArrayList<ComponentSplice> componentSplices = new ArrayList<>();
            int uniqueProgramId2 = 0;
            int availNum2 = 0;
            int availsExpected2 = 0;
            boolean autoReturn2 = false;
            long breakDurationUs2 = C.TIME_UNSET;
            if (!spliceEventCancelIndicator2) {
                int headerByte = sectionData.readUnsignedByte();
                outOfNetworkIndicator2 = (headerByte & 128) != 0;
                programSpliceFlag2 = (headerByte & 64) != 0;
                boolean durationFlag = (headerByte & 32) != 0;
                if (programSpliceFlag2) {
                    utcSpliceTime2 = sectionData.readUnsignedInt();
                }
                if (!programSpliceFlag2) {
                    int componentCount = sectionData.readUnsignedByte();
                    componentSplices = new ArrayList<>(componentCount);
                    for (int i = 0; i < componentCount; i++) {
                        componentSplices.add(new ComponentSplice(sectionData.readUnsignedByte(), sectionData.readUnsignedInt()));
                    }
                }
                if (durationFlag) {
                    long firstByte = (long) sectionData.readUnsignedByte();
                    autoReturn2 = (128 & firstByte) != 0;
                    breakDurationUs2 = (1000 * (((1 & firstByte) << 32) | sectionData.readUnsignedInt())) / 90;
                }
                uniqueProgramId2 = sectionData.readUnsignedShort();
                availNum2 = sectionData.readUnsignedByte();
                availsExpected2 = sectionData.readUnsignedByte();
            }
            return new Event(spliceEventId2, spliceEventCancelIndicator2, outOfNetworkIndicator2, programSpliceFlag2, componentSplices, utcSpliceTime2, autoReturn2, breakDurationUs2, uniqueProgramId2, availNum2, availsExpected2);
        }

        /* access modifiers changed from: private */
        public void writeToParcel(Parcel dest) {
            int i;
            int i2;
            int i3;
            int i4 = 1;
            dest.writeLong(this.spliceEventId);
            if (this.spliceEventCancelIndicator) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            if (this.outOfNetworkIndicator) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            dest.writeByte((byte) i2);
            if (this.programSpliceFlag) {
                i3 = 1;
            } else {
                i3 = 0;
            }
            dest.writeByte((byte) i3);
            int componentSpliceListSize = this.componentSpliceList.size();
            dest.writeInt(componentSpliceListSize);
            for (int i5 = 0; i5 < componentSpliceListSize; i5++) {
                this.componentSpliceList.get(i5).writeToParcel(dest);
            }
            dest.writeLong(this.utcSpliceTime);
            if (!this.autoReturn) {
                i4 = 0;
            }
            dest.writeByte((byte) i4);
            dest.writeLong(this.breakDurationUs);
            dest.writeInt(this.uniqueProgramId);
            dest.writeInt(this.availNum);
            dest.writeInt(this.availsExpected);
        }

        /* access modifiers changed from: private */
        public static Event createFromParcel(Parcel in) {
            return new Event(in);
        }
    }

    public static final class ComponentSplice {
        public final int componentTag;
        public final long utcSpliceTime;

        private ComponentSplice(int componentTag2, long utcSpliceTime2) {
            this.componentTag = componentTag2;
            this.utcSpliceTime = utcSpliceTime2;
        }

        /* access modifiers changed from: private */
        public static ComponentSplice createFromParcel(Parcel in) {
            return new ComponentSplice(in.readInt(), in.readLong());
        }

        /* access modifiers changed from: private */
        public void writeToParcel(Parcel dest) {
            dest.writeInt(this.componentTag);
            dest.writeLong(this.utcSpliceTime);
        }
    }

    private SpliceScheduleCommand(List<Event> events2) {
        this.events = Collections.unmodifiableList(events2);
    }

    private SpliceScheduleCommand(Parcel in) {
        int eventsSize = in.readInt();
        ArrayList<Event> events2 = new ArrayList<>(eventsSize);
        for (int i = 0; i < eventsSize; i++) {
            events2.add(Event.createFromParcel(in));
        }
        this.events = Collections.unmodifiableList(events2);
    }

    static SpliceScheduleCommand parseFromSection(ParsableByteArray sectionData) {
        int spliceCount = sectionData.readUnsignedByte();
        ArrayList<Event> events2 = new ArrayList<>(spliceCount);
        for (int i = 0; i < spliceCount; i++) {
            events2.add(Event.parseFromSection(sectionData));
        }
        return new SpliceScheduleCommand((List<Event>) events2);
    }

    public void writeToParcel(Parcel dest, int flags) {
        int eventsSize = this.events.size();
        dest.writeInt(eventsSize);
        for (int i = 0; i < eventsSize; i++) {
            this.events.get(i).writeToParcel(dest);
        }
    }
}
