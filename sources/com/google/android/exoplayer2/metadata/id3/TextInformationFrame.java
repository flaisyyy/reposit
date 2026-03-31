package com.google.android.exoplayer2.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.util.Util;

public final class TextInformationFrame extends Id3Frame {
    public static final Parcelable.Creator<TextInformationFrame> CREATOR = new Parcelable.Creator<TextInformationFrame>() {
        public TextInformationFrame createFromParcel(Parcel in) {
            return new TextInformationFrame(in);
        }

        public TextInformationFrame[] newArray(int size) {
            return new TextInformationFrame[size];
        }
    };
    public final String description;
    public final String value;

    public TextInformationFrame(String id, String description2, String value2) {
        super(id);
        this.description = description2;
        this.value = value2;
    }

    TextInformationFrame(Parcel in) {
        super(in.readString());
        this.description = in.readString();
        this.value = in.readString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TextInformationFrame other = (TextInformationFrame) obj;
        if (!this.id.equals(other.id) || !Util.areEqual(this.description, other.description) || !Util.areEqual(this.value, other.value)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int i;
        int i2 = 0;
        int hashCode = (this.id.hashCode() + 527) * 31;
        if (this.description != null) {
            i = this.description.hashCode();
        } else {
            i = 0;
        }
        int i3 = (hashCode + i) * 31;
        if (this.value != null) {
            i2 = this.value.hashCode();
        }
        return i3 + i2;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.description);
        dest.writeString(this.value);
    }
}
