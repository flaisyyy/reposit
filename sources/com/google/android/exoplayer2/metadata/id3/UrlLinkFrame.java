package com.google.android.exoplayer2.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.util.Util;

public final class UrlLinkFrame extends Id3Frame {
    public static final Parcelable.Creator<UrlLinkFrame> CREATOR = new Parcelable.Creator<UrlLinkFrame>() {
        public UrlLinkFrame createFromParcel(Parcel in) {
            return new UrlLinkFrame(in);
        }

        public UrlLinkFrame[] newArray(int size) {
            return new UrlLinkFrame[size];
        }
    };
    public final String description;
    public final String url;

    public UrlLinkFrame(String id, String description2, String url2) {
        super(id);
        this.description = description2;
        this.url = url2;
    }

    UrlLinkFrame(Parcel in) {
        super(in.readString());
        this.description = in.readString();
        this.url = in.readString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UrlLinkFrame other = (UrlLinkFrame) obj;
        if (!this.id.equals(other.id) || !Util.areEqual(this.description, other.description) || !Util.areEqual(this.url, other.url)) {
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
        if (this.url != null) {
            i2 = this.url.hashCode();
        }
        return i3 + i2;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.description);
        dest.writeString(this.url);
    }
}
