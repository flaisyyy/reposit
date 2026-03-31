package com.google.android.exoplayer2.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.util.Util;

public final class CommentFrame extends Id3Frame {
    public static final Parcelable.Creator<CommentFrame> CREATOR = new Parcelable.Creator<CommentFrame>() {
        public CommentFrame createFromParcel(Parcel in) {
            return new CommentFrame(in);
        }

        public CommentFrame[] newArray(int size) {
            return new CommentFrame[size];
        }
    };
    public static final String ID = "COMM";
    public final String description;
    public final String language;
    public final String text;

    public CommentFrame(String language2, String description2, String text2) {
        super(ID);
        this.language = language2;
        this.description = description2;
        this.text = text2;
    }

    CommentFrame(Parcel in) {
        super(ID);
        this.language = in.readString();
        this.description = in.readString();
        this.text = in.readString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CommentFrame other = (CommentFrame) obj;
        if (!Util.areEqual(this.description, other.description) || !Util.areEqual(this.language, other.language) || !Util.areEqual(this.text, other.text)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int i;
        int i2;
        int i3 = 0;
        if (this.language != null) {
            i = this.language.hashCode();
        } else {
            i = 0;
        }
        int i4 = (i + 527) * 31;
        if (this.description != null) {
            i2 = this.description.hashCode();
        } else {
            i2 = 0;
        }
        int i5 = (i4 + i2) * 31;
        if (this.text != null) {
            i3 = this.text.hashCode();
        }
        return i5 + i3;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.language);
        dest.writeString(this.text);
    }
}
