package com.google.android.exoplayer2.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;

public final class GeobFrame extends Id3Frame {
    public static final Parcelable.Creator<GeobFrame> CREATOR = new Parcelable.Creator<GeobFrame>() {
        public GeobFrame createFromParcel(Parcel in) {
            return new GeobFrame(in);
        }

        public GeobFrame[] newArray(int size) {
            return new GeobFrame[size];
        }
    };
    public static final String ID = "GEOB";
    public final byte[] data;
    public final String description;
    public final String filename;
    public final String mimeType;

    public GeobFrame(String mimeType2, String filename2, String description2, byte[] data2) {
        super(ID);
        this.mimeType = mimeType2;
        this.filename = filename2;
        this.description = description2;
        this.data = data2;
    }

    GeobFrame(Parcel in) {
        super(ID);
        this.mimeType = in.readString();
        this.filename = in.readString();
        this.description = in.readString();
        this.data = in.createByteArray();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GeobFrame other = (GeobFrame) obj;
        if (!Util.areEqual(this.mimeType, other.mimeType) || !Util.areEqual(this.filename, other.filename) || !Util.areEqual(this.description, other.description) || !Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int i;
        int i2;
        int i3 = 0;
        if (this.mimeType != null) {
            i = this.mimeType.hashCode();
        } else {
            i = 0;
        }
        int i4 = (i + 527) * 31;
        if (this.filename != null) {
            i2 = this.filename.hashCode();
        } else {
            i2 = 0;
        }
        int i5 = (i4 + i2) * 31;
        if (this.description != null) {
            i3 = this.description.hashCode();
        }
        return ((i5 + i3) * 31) + Arrays.hashCode(this.data);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mimeType);
        dest.writeString(this.filename);
        dest.writeString(this.description);
        dest.writeByteArray(this.data);
    }
}
