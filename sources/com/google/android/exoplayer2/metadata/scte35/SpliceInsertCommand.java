package com.google.android.exoplayer2.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpliceInsertCommand extends SpliceCommand {
    public static final Parcelable.Creator<SpliceInsertCommand> CREATOR = new Parcelable.Creator<SpliceInsertCommand>() {
        public SpliceInsertCommand createFromParcel(Parcel in) {
            return new SpliceInsertCommand(in);
        }

        public SpliceInsertCommand[] newArray(int size) {
            return new SpliceInsertCommand[size];
        }
    };
    public final boolean autoReturn;
    public final int availNum;
    public final int availsExpected;
    public final long breakDurationUs;
    public final List<ComponentSplice> componentSpliceList;
    public final boolean outOfNetworkIndicator;
    public final boolean programSpliceFlag;
    public final long programSplicePlaybackPositionUs;
    public final long programSplicePts;
    public final boolean spliceEventCancelIndicator;
    public final long spliceEventId;
    public final boolean spliceImmediateFlag;
    public final int uniqueProgramId;

    private SpliceInsertCommand(long spliceEventId2, boolean spliceEventCancelIndicator2, boolean outOfNetworkIndicator2, boolean programSpliceFlag2, boolean spliceImmediateFlag2, long programSplicePts2, long programSplicePlaybackPositionUs2, List<ComponentSplice> componentSpliceList2, boolean autoReturn2, long breakDurationUs2, int uniqueProgramId2, int availNum2, int availsExpected2) {
        this.spliceEventId = spliceEventId2;
        this.spliceEventCancelIndicator = spliceEventCancelIndicator2;
        this.outOfNetworkIndicator = outOfNetworkIndicator2;
        this.programSpliceFlag = programSpliceFlag2;
        this.spliceImmediateFlag = spliceImmediateFlag2;
        this.programSplicePts = programSplicePts2;
        this.programSplicePlaybackPositionUs = programSplicePlaybackPositionUs2;
        this.componentSpliceList = Collections.unmodifiableList(componentSpliceList2);
        this.autoReturn = autoReturn2;
        this.breakDurationUs = breakDurationUs2;
        this.uniqueProgramId = uniqueProgramId2;
        this.availNum = availNum2;
        this.availsExpected = availsExpected2;
    }

    private SpliceInsertCommand(Parcel in) {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5 = true;
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
        if (in.readByte() == 1) {
            z4 = true;
        } else {
            z4 = false;
        }
        this.spliceImmediateFlag = z4;
        this.programSplicePts = in.readLong();
        this.programSplicePlaybackPositionUs = in.readLong();
        int componentSpliceListSize = in.readInt();
        List<ComponentSplice> componentSpliceList2 = new ArrayList<>(componentSpliceListSize);
        for (int i = 0; i < componentSpliceListSize; i++) {
            componentSpliceList2.add(ComponentSplice.createFromParcel(in));
        }
        this.componentSpliceList = Collections.unmodifiableList(componentSpliceList2);
        this.autoReturn = in.readByte() != 1 ? false : z5;
        this.breakDurationUs = in.readLong();
        this.uniqueProgramId = in.readInt();
        this.availNum = in.readInt();
        this.availsExpected = in.readInt();
    }

    /* JADX WARNING: type inference failed for: r18v0, types: [java.util.List] */
    /* JADX WARNING: type inference failed for: r18v2 */
    /* JADX WARNING: type inference failed for: r0v7, types: [java.util.ArrayList] */
    /* JADX WARNING: Failed to insert additional move for type inference */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static com.google.android.exoplayer2.metadata.scte35.SpliceInsertCommand parseFromSection(com.google.android.exoplayer2.util.ParsableByteArray r37, long r38, com.google.android.exoplayer2.util.TimestampAdjuster r40) {
        /*
            long r34 = r37.readUnsignedInt()
            int r2 = r37.readUnsignedByte()
            r2 = r2 & 128(0x80, float:1.794E-43)
            if (r2 == 0) goto L_0x0085
            r10 = 1
        L_0x000d:
            r11 = 0
            r12 = 0
            r13 = 0
            r14 = -9223372036854775807(0x8000000000000001, double:-4.9E-324)
            java.util.List r18 = java.util.Collections.emptyList()
            r22 = 0
            r23 = 0
            r24 = 0
            r19 = 0
            r20 = -9223372036854775807(0x8000000000000001, double:-4.9E-324)
            if (r10 != 0) goto L_0x00c6
            int r29 = r37.readUnsignedByte()
            r0 = r29
            r2 = r0 & 128(0x80, float:1.794E-43)
            if (r2 == 0) goto L_0x0087
            r11 = 1
        L_0x0033:
            r2 = r29 & 64
            if (r2 == 0) goto L_0x0089
            r12 = 1
        L_0x0038:
            r2 = r29 & 32
            if (r2 == 0) goto L_0x008b
            r28 = 1
        L_0x003e:
            r2 = r29 & 16
            if (r2 == 0) goto L_0x008e
            r13 = 1
        L_0x0043:
            if (r12 == 0) goto L_0x004b
            if (r13 != 0) goto L_0x004b
            long r14 = com.google.android.exoplayer2.metadata.scte35.TimeSignalCommand.parseSpliceTime(r37, r38)
        L_0x004b:
            if (r12 != 0) goto L_0x0090
            int r25 = r37.readUnsignedByte()
            java.util.ArrayList r18 = new java.util.ArrayList
            r0 = r18
            r1 = r25
            r0.<init>(r1)
            r32 = 0
        L_0x005c:
            r0 = r32
            r1 = r25
            if (r0 >= r1) goto L_0x0090
            int r3 = r37.readUnsignedByte()
            r4 = -9223372036854775807(0x8000000000000001, double:-4.9E-324)
            if (r13 != 0) goto L_0x0071
            long r4 = com.google.android.exoplayer2.metadata.scte35.TimeSignalCommand.parseSpliceTime(r37, r38)
        L_0x0071:
            com.google.android.exoplayer2.metadata.scte35.SpliceInsertCommand$ComponentSplice r2 = new com.google.android.exoplayer2.metadata.scte35.SpliceInsertCommand$ComponentSplice
            r0 = r40
            long r6 = r0.adjustTsTimestamp(r4)
            r8 = 0
            r2.<init>(r3, r4, r6)
            r0 = r18
            r0.add(r2)
            int r32 = r32 + 1
            goto L_0x005c
        L_0x0085:
            r10 = 0
            goto L_0x000d
        L_0x0087:
            r11 = 0
            goto L_0x0033
        L_0x0089:
            r12 = 0
            goto L_0x0038
        L_0x008b:
            r28 = 0
            goto L_0x003e
        L_0x008e:
            r13 = 0
            goto L_0x0043
        L_0x0090:
            if (r28 == 0) goto L_0x00ba
            int r2 = r37.readUnsignedByte()
            long r0 = (long) r2
            r30 = r0
            r6 = 128(0x80, double:6.32E-322)
            long r6 = r6 & r30
            r8 = 0
            int r2 = (r6 > r8 ? 1 : (r6 == r8 ? 0 : -1))
            if (r2 == 0) goto L_0x00d4
            r19 = 1
        L_0x00a5:
            r6 = 1
            long r6 = r6 & r30
            r2 = 32
            long r6 = r6 << r2
            long r8 = r37.readUnsignedInt()
            long r26 = r6 | r8
            r6 = 1000(0x3e8, double:4.94E-321)
            long r6 = r6 * r26
            r8 = 90
            long r20 = r6 / r8
        L_0x00ba:
            int r22 = r37.readUnsignedShort()
            int r23 = r37.readUnsignedByte()
            int r24 = r37.readUnsignedByte()
        L_0x00c6:
            com.google.android.exoplayer2.metadata.scte35.SpliceInsertCommand r7 = new com.google.android.exoplayer2.metadata.scte35.SpliceInsertCommand
            r0 = r40
            long r16 = r0.adjustTsTimestamp(r14)
            r8 = r34
            r7.<init>(r8, r10, r11, r12, r13, r14, r16, r18, r19, r20, r22, r23, r24)
            return r7
        L_0x00d4:
            r19 = 0
            goto L_0x00a5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.exoplayer2.metadata.scte35.SpliceInsertCommand.parseFromSection(com.google.android.exoplayer2.util.ParsableByteArray, long, com.google.android.exoplayer2.util.TimestampAdjuster):com.google.android.exoplayer2.metadata.scte35.SpliceInsertCommand");
    }

    public static final class ComponentSplice {
        public final long componentSplicePlaybackPositionUs;
        public final long componentSplicePts;
        public final int componentTag;

        private ComponentSplice(int componentTag2, long componentSplicePts2, long componentSplicePlaybackPositionUs2) {
            this.componentTag = componentTag2;
            this.componentSplicePts = componentSplicePts2;
            this.componentSplicePlaybackPositionUs = componentSplicePlaybackPositionUs2;
        }

        public void writeToParcel(Parcel dest) {
            dest.writeInt(this.componentTag);
            dest.writeLong(this.componentSplicePts);
            dest.writeLong(this.componentSplicePlaybackPositionUs);
        }

        public static ComponentSplice createFromParcel(Parcel in) {
            return new ComponentSplice(in.readInt(), in.readLong(), in.readLong());
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5 = 1;
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
        if (this.spliceImmediateFlag) {
            i4 = 1;
        } else {
            i4 = 0;
        }
        dest.writeByte((byte) i4);
        dest.writeLong(this.programSplicePts);
        dest.writeLong(this.programSplicePlaybackPositionUs);
        int componentSpliceListSize = this.componentSpliceList.size();
        dest.writeInt(componentSpliceListSize);
        for (int i6 = 0; i6 < componentSpliceListSize; i6++) {
            this.componentSpliceList.get(i6).writeToParcel(dest);
        }
        if (!this.autoReturn) {
            i5 = 0;
        }
        dest.writeByte((byte) i5);
        dest.writeLong(this.breakDurationUs);
        dest.writeInt(this.uniqueProgramId);
        dest.writeInt(this.availNum);
        dest.writeInt(this.availsExpected);
    }
}
