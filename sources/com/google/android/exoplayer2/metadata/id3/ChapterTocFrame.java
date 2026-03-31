package com.google.android.exoplayer2.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;

public final class ChapterTocFrame extends Id3Frame {
    public static final Parcelable.Creator<ChapterTocFrame> CREATOR = new Parcelable.Creator<ChapterTocFrame>() {
        public ChapterTocFrame createFromParcel(Parcel in) {
            return new ChapterTocFrame(in);
        }

        public ChapterTocFrame[] newArray(int size) {
            return new ChapterTocFrame[size];
        }
    };
    public static final String ID = "CTOC";
    public final String[] children;
    public final String elementId;
    public final boolean isOrdered;
    public final boolean isRoot;
    private final Id3Frame[] subFrames;

    public ChapterTocFrame(String elementId2, boolean isRoot2, boolean isOrdered2, String[] children2, Id3Frame[] subFrames2) {
        super(ID);
        this.elementId = elementId2;
        this.isRoot = isRoot2;
        this.isOrdered = isOrdered2;
        this.children = children2;
        this.subFrames = subFrames2;
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    ChapterTocFrame(Parcel in) {
        super(ID);
        boolean z;
        boolean z2 = true;
        this.elementId = in.readString();
        if (in.readByte() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.isRoot = z;
        this.isOrdered = in.readByte() == 0 ? false : z2;
        this.children = in.createStringArray();
        int subFrameCount = in.readInt();
        this.subFrames = new Id3Frame[subFrameCount];
        for (int i = 0; i < subFrameCount; i++) {
            this.subFrames[i] = (Id3Frame) in.readParcelable(Id3Frame.class.getClassLoader());
        }
    }

    public int getSubFrameCount() {
        return this.subFrames.length;
    }

    public Id3Frame getSubFrame(int index) {
        return this.subFrames[index];
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ChapterTocFrame other = (ChapterTocFrame) obj;
        if (this.isRoot != other.isRoot || this.isOrdered != other.isOrdered || !Util.areEqual(this.elementId, other.elementId) || !Arrays.equals(this.children, other.children) || !Arrays.equals(this.subFrames, other.subFrames)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int i;
        int i2 = 1;
        int i3 = 0;
        if (this.isRoot) {
            i = 1;
        } else {
            i = 0;
        }
        int i4 = (i + 527) * 31;
        if (!this.isOrdered) {
            i2 = 0;
        }
        int i5 = (i4 + i2) * 31;
        if (this.elementId != null) {
            i3 = this.elementId.hashCode();
        }
        return i5 + i3;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i;
        int i2 = 1;
        dest.writeString(this.elementId);
        if (this.isRoot) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeByte((byte) i);
        if (!this.isOrdered) {
            i2 = 0;
        }
        dest.writeByte((byte) i2);
        dest.writeStringArray(this.children);
        dest.writeInt(this.subFrames.length);
        for (Id3Frame subFrame : this.subFrames) {
            dest.writeParcelable(subFrame, 0);
        }
    }
}
