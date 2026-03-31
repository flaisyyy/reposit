package com.ua.mytrinity.verticalslidevar;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class DefaultSavedState extends View.BaseSavedState {
    public static final Parcelable.Creator<DefaultSavedState> CREATOR = new Parcelable.Creator<DefaultSavedState>() {
        public DefaultSavedState createFromParcel(Parcel in) {
            return new DefaultSavedState(in);
        }

        public DefaultSavedState[] newArray(int size) {
            return new DefaultSavedState[size];
        }
    };
    private int m_progress;
    private int m_secondary_progress;

    public DefaultSavedState(Parcelable superState) {
        super(superState);
    }

    public DefaultSavedState(Parcel in) {
        super(in);
        this.m_progress = in.readInt();
        this.m_secondary_progress = in.readInt();
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(this.m_progress);
        out.writeInt(this.m_secondary_progress);
    }

    public int getProgress() {
        return this.m_progress;
    }

    public void setProgress(int progress) {
        this.m_progress = progress;
    }

    public int getSecondaryProgress() {
        return this.m_secondary_progress;
    }

    public void setSecondaryProgress(int secondaryProgress) {
        this.m_secondary_progress = secondaryProgress;
    }
}
