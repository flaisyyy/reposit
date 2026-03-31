package com.google.android.exoplayer2.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class BinaryFrame extends Id3Frame {
    public static final Parcelable.Creator<BinaryFrame> CREATOR = new Parcelable.Creator<BinaryFrame>() {
        public BinaryFrame createFromParcel(Parcel in) {
            return new BinaryFrame(in);
        }

        public BinaryFrame[] newArray(int size) {
            return new BinaryFrame[size];
        }
    };
    public final byte[] data;

    public BinaryFrame(String id, byte[] data2) {
        super(id);
        this.data = data2;
    }

    BinaryFrame(Parcel in) {
        super(in.readString());
        this.data = in.createByteArray();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BinaryFrame other = (BinaryFrame) obj;
        if (!this.id.equals(other.id) || !Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return ((this.id.hashCode() + 527) * 31) + Arrays.hashCode(this.data);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeByteArray(this.data);
    }
}
