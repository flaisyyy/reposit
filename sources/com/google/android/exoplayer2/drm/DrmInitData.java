package com.google.android.exoplayer2.drm;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DrmInitData implements Comparator<SchemeData>, Parcelable {
    public static final Parcelable.Creator<DrmInitData> CREATOR = new Parcelable.Creator<DrmInitData>() {
        public DrmInitData createFromParcel(Parcel in) {
            return new DrmInitData(in);
        }

        public DrmInitData[] newArray(int size) {
            return new DrmInitData[size];
        }
    };
    private int hashCode;
    public final int schemeDataCount;
    private final SchemeData[] schemeDatas;
    @Nullable
    public final String schemeType;

    public DrmInitData(List<SchemeData> schemeDatas2) {
        this((String) null, false, (SchemeData[]) schemeDatas2.toArray(new SchemeData[schemeDatas2.size()]));
    }

    public DrmInitData(String schemeType2, List<SchemeData> schemeDatas2) {
        this(schemeType2, false, (SchemeData[]) schemeDatas2.toArray(new SchemeData[schemeDatas2.size()]));
    }

    public DrmInitData(SchemeData... schemeDatas2) {
        this((String) null, schemeDatas2);
    }

    public DrmInitData(@Nullable String schemeType2, SchemeData... schemeDatas2) {
        this(schemeType2, true, schemeDatas2);
    }

    private DrmInitData(@Nullable String schemeType2, boolean cloneSchemeDatas, SchemeData... schemeDatas2) {
        this.schemeType = schemeType2;
        schemeDatas2 = cloneSchemeDatas ? (SchemeData[]) schemeDatas2.clone() : schemeDatas2;
        Arrays.sort(schemeDatas2, this);
        this.schemeDatas = schemeDatas2;
        this.schemeDataCount = schemeDatas2.length;
    }

    DrmInitData(Parcel in) {
        this.schemeType = in.readString();
        this.schemeDatas = (SchemeData[]) in.createTypedArray(SchemeData.CREATOR);
        this.schemeDataCount = this.schemeDatas.length;
    }

    @Deprecated
    public SchemeData get(UUID uuid) {
        for (SchemeData schemeData : this.schemeDatas) {
            if (schemeData.matches(uuid)) {
                return schemeData;
            }
        }
        return null;
    }

    public SchemeData get(int index) {
        return this.schemeDatas[index];
    }

    /* Debug info: failed to restart local var, previous not found, register: 3 */
    public DrmInitData copyWithSchemeType(@Nullable String schemeType2) {
        return Util.areEqual(this.schemeType, schemeType2) ? this : new DrmInitData(schemeType2, false, this.schemeDatas);
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = ((this.schemeType == null ? 0 : this.schemeType.hashCode()) * 31) + Arrays.hashCode(this.schemeDatas);
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
        DrmInitData other = (DrmInitData) obj;
        if (!Util.areEqual(this.schemeType, other.schemeType) || !Arrays.equals(this.schemeDatas, other.schemeDatas)) {
            return false;
        }
        return true;
    }

    public int compare(SchemeData first, SchemeData second) {
        if (C.UUID_NIL.equals(first.uuid)) {
            return C.UUID_NIL.equals(second.uuid) ? 0 : 1;
        }
        return first.uuid.compareTo(second.uuid);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.schemeType);
        dest.writeTypedArray(this.schemeDatas, 0);
    }

    public static final class SchemeData implements Parcelable {
        public static final Parcelable.Creator<SchemeData> CREATOR = new Parcelable.Creator<SchemeData>() {
            public SchemeData createFromParcel(Parcel in) {
                return new SchemeData(in);
            }

            public SchemeData[] newArray(int size) {
                return new SchemeData[size];
            }
        };
        public final byte[] data;
        private int hashCode;
        public final String mimeType;
        public final boolean requiresSecureDecryption;
        /* access modifiers changed from: private */
        public final UUID uuid;

        public SchemeData(UUID uuid2, String mimeType2, byte[] data2) {
            this(uuid2, mimeType2, data2, false);
        }

        public SchemeData(UUID uuid2, String mimeType2, byte[] data2, boolean requiresSecureDecryption2) {
            this.uuid = (UUID) Assertions.checkNotNull(uuid2);
            this.mimeType = (String) Assertions.checkNotNull(mimeType2);
            this.data = data2;
            this.requiresSecureDecryption = requiresSecureDecryption2;
        }

        SchemeData(Parcel in) {
            this.uuid = new UUID(in.readLong(), in.readLong());
            this.mimeType = in.readString();
            this.data = in.createByteArray();
            this.requiresSecureDecryption = in.readByte() != 0;
        }

        public boolean matches(UUID schemeUuid) {
            return C.UUID_NIL.equals(this.uuid) || schemeUuid.equals(this.uuid);
        }

        public boolean canReplace(SchemeData other) {
            return hasData() && !other.hasData() && matches(other.uuid);
        }

        public boolean hasData() {
            return this.data != null;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SchemeData)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            SchemeData other = (SchemeData) obj;
            if (!this.mimeType.equals(other.mimeType) || !Util.areEqual(this.uuid, other.uuid) || !Arrays.equals(this.data, other.data)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            if (this.hashCode == 0) {
                this.hashCode = (((this.uuid.hashCode() * 31) + this.mimeType.hashCode()) * 31) + Arrays.hashCode(this.data);
            }
            return this.hashCode;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.uuid.getMostSignificantBits());
            dest.writeLong(this.uuid.getLeastSignificantBits());
            dest.writeString(this.mimeType);
            dest.writeByteArray(this.data);
            dest.writeByte((byte) (this.requiresSecureDecryption ? 1 : 0));
        }
    }
}
