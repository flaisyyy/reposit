package android.support.v7.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

public class EditTextPreference extends DialogPreference {
    private String mText;

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.editTextPreferenceStyle, AndroidResources.ANDROID_R_EDITTEXT_PREFERENCE_STYLE));
    }

    public EditTextPreference(Context context) {
        this(context, (AttributeSet) null);
    }

    public void setText(String text) {
        boolean wasBlocking = shouldDisableDependents();
        this.mText = text;
        persistString(text);
        boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    public String getText() {
        return this.mText;
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    /* access modifiers changed from: protected */
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setText(restoreValue ? getPersistedString(this.mText) : (String) defaultValue);
    }

    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(this.mText) || super.shouldDisableDependents();
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }
        SavedState myState = new SavedState(superState);
        myState.text = getText();
        return myState;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setText(myState.text);
    }

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        String text;

        public SavedState(Parcel source) {
            super(source);
            this.text = source.readString();
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(this.text);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }
    }
}
