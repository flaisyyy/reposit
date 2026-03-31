package com.google.android.exoplayer2.metadata.emsg;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;

public final class EventMessage implements Metadata.Entry {
    public static final Parcelable.Creator<EventMessage> CREATOR = new Parcelable.Creator<EventMessage>() {
        public EventMessage createFromParcel(Parcel in) {
            return new EventMessage(in);
        }

        public EventMessage[] newArray(int size) {
            return new EventMessage[size];
        }
    };
    public final long durationMs;
    private int hashCode;
    public final long id;
    public final byte[] messageData;
    public final String schemeIdUri;
    public final String value;

    public EventMessage(String schemeIdUri2, String value2, long durationMs2, long id2, byte[] messageData2) {
        this.schemeIdUri = schemeIdUri2;
        this.value = value2;
        this.durationMs = durationMs2;
        this.id = id2;
        this.messageData = messageData2;
    }

    EventMessage(Parcel in) {
        this.schemeIdUri = in.readString();
        this.value = in.readString();
        this.durationMs = in.readLong();
        this.id = in.readLong();
        this.messageData = in.createByteArray();
    }

    public int hashCode() {
        int i;
        int i2 = 0;
        if (this.hashCode == 0) {
            if (this.schemeIdUri != null) {
                i = this.schemeIdUri.hashCode();
            } else {
                i = 0;
            }
            int i3 = (i + 527) * 31;
            if (this.value != null) {
                i2 = this.value.hashCode();
            }
            this.hashCode = ((((((i3 + i2) * 31) + ((int) (this.durationMs ^ (this.durationMs >>> 32)))) * 31) + ((int) (this.id ^ (this.id >>> 32)))) * 31) + Arrays.hashCode(this.messageData);
        }
        return this.hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EventMessage other = (EventMessage) obj;
        if (this.durationMs != other.durationMs || this.id != other.id || !Util.areEqual(this.schemeIdUri, other.schemeIdUri) || !Util.areEqual(this.value, other.value) || !Arrays.equals(this.messageData, other.messageData)) {
            return false;
        }
        return true;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.schemeIdUri);
        dest.writeString(this.value);
        dest.writeLong(this.durationMs);
        dest.writeLong(this.id);
        dest.writeByteArray(this.messageData);
    }
}
