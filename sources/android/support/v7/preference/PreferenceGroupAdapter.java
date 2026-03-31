package android.support.v7.preference;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.RestrictTo;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class PreferenceGroupAdapter extends RecyclerView.Adapter<PreferenceViewHolder> implements Preference.OnPreferenceChangeInternalListener, PreferenceGroup.PreferencePositionCallback {
    private static final String TAG = "PreferenceGroupAdapter";
    private Handler mHandler = new Handler();
    private PreferenceGroup mPreferenceGroup;
    private List<PreferenceLayout> mPreferenceLayouts;
    private List<Preference> mPreferenceList;
    private List<Preference> mPreferenceListInternal;
    private Runnable mSyncRunnable = new Runnable() {
        public void run() {
            PreferenceGroupAdapter.this.syncMyPreferences();
        }
    };
    private PreferenceLayout mTempPreferenceLayout = new PreferenceLayout();

    private static class PreferenceLayout {
        /* access modifiers changed from: private */
        public String name;
        /* access modifiers changed from: private */
        public int resId;
        /* access modifiers changed from: private */
        public int widgetResId;

        public PreferenceLayout() {
        }

        public PreferenceLayout(PreferenceLayout other) {
            this.resId = other.resId;
            this.widgetResId = other.widgetResId;
            this.name = other.name;
        }

        public boolean equals(Object o) {
            if (!(o instanceof PreferenceLayout)) {
                return false;
            }
            PreferenceLayout other = (PreferenceLayout) o;
            if (this.resId == other.resId && this.widgetResId == other.widgetResId && TextUtils.equals(this.name, other.name)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return ((((this.resId + 527) * 31) + this.widgetResId) * 31) + this.name.hashCode();
        }
    }

    public PreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
        this.mPreferenceGroup = preferenceGroup;
        this.mPreferenceGroup.setOnPreferenceChangeInternalListener(this);
        this.mPreferenceList = new ArrayList();
        this.mPreferenceListInternal = new ArrayList();
        this.mPreferenceLayouts = new ArrayList();
        if (this.mPreferenceGroup instanceof PreferenceScreen) {
            setHasStableIds(((PreferenceScreen) this.mPreferenceGroup).shouldUseGeneratedIds());
        } else {
            setHasStableIds(true);
        }
        syncMyPreferences();
    }

    /* access modifiers changed from: private */
    public void syncMyPreferences() {
        for (Preference preference : this.mPreferenceListInternal) {
            preference.setOnPreferenceChangeInternalListener((Preference.OnPreferenceChangeInternalListener) null);
        }
        List<Preference> fullPreferenceList = new ArrayList<>(this.mPreferenceListInternal.size());
        flattenPreferenceGroup(fullPreferenceList, this.mPreferenceGroup);
        final List<Preference> visiblePreferenceList = new ArrayList<>(fullPreferenceList.size());
        for (Preference preference2 : fullPreferenceList) {
            if (preference2.isVisible()) {
                visiblePreferenceList.add(preference2);
            }
        }
        final List<Preference> oldVisibleList = this.mPreferenceList;
        this.mPreferenceList = visiblePreferenceList;
        this.mPreferenceListInternal = fullPreferenceList;
        PreferenceManager preferenceManager = this.mPreferenceGroup.getPreferenceManager();
        if (preferenceManager == null || preferenceManager.getPreferenceComparisonCallback() == null) {
            notifyDataSetChanged();
        } else {
            final PreferenceManager.PreferenceComparisonCallback comparisonCallback = preferenceManager.getPreferenceComparisonCallback();
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                public int getOldListSize() {
                    return oldVisibleList.size();
                }

                public int getNewListSize() {
                    return visiblePreferenceList.size();
                }

                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return comparisonCallback.arePreferenceItemsTheSame((Preference) oldVisibleList.get(oldItemPosition), (Preference) visiblePreferenceList.get(newItemPosition));
                }

                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return comparisonCallback.arePreferenceContentsTheSame((Preference) oldVisibleList.get(oldItemPosition), (Preference) visiblePreferenceList.get(newItemPosition));
                }
            }).dispatchUpdatesTo((RecyclerView.Adapter) this);
        }
        for (Preference preference3 : fullPreferenceList) {
            preference3.clearWasDetached();
        }
    }

    private void flattenPreferenceGroup(List<Preference> preferences, PreferenceGroup group) {
        group.sortPreferences();
        int groupSize = group.getPreferenceCount();
        for (int i = 0; i < groupSize; i++) {
            Preference preference = group.getPreference(i);
            preferences.add(preference);
            addPreferenceClassName(preference);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceAsGroup = (PreferenceGroup) preference;
                if (preferenceAsGroup.isOnSameScreenAsChildren()) {
                    flattenPreferenceGroup(preferences, preferenceAsGroup);
                }
            }
            preference.setOnPreferenceChangeInternalListener(this);
        }
    }

    private PreferenceLayout createPreferenceLayout(Preference preference, PreferenceLayout in) {
        PreferenceLayout pl = in != null ? in : new PreferenceLayout();
        String unused = pl.name = preference.getClass().getName();
        int unused2 = pl.resId = preference.getLayoutResource();
        int unused3 = pl.widgetResId = preference.getWidgetLayoutResource();
        return pl;
    }

    private void addPreferenceClassName(Preference preference) {
        PreferenceLayout pl = createPreferenceLayout(preference, (PreferenceLayout) null);
        if (!this.mPreferenceLayouts.contains(pl)) {
            this.mPreferenceLayouts.add(pl);
        }
    }

    public int getItemCount() {
        return this.mPreferenceList.size();
    }

    public Preference getItem(int position) {
        if (position < 0 || position >= getItemCount()) {
            return null;
        }
        return this.mPreferenceList.get(position);
    }

    public long getItemId(int position) {
        if (!hasStableIds()) {
            return -1;
        }
        return getItem(position).getId();
    }

    public void onPreferenceChange(Preference preference) {
        int index = this.mPreferenceList.indexOf(preference);
        if (index != -1) {
            notifyItemChanged(index, preference);
        }
    }

    public void onPreferenceHierarchyChange(Preference preference) {
        this.mHandler.removeCallbacks(this.mSyncRunnable);
        this.mHandler.post(this.mSyncRunnable);
    }

    public void onPreferenceVisibilityChange(Preference preference) {
        if (this.mPreferenceListInternal.contains(preference)) {
            if (preference.isVisible()) {
                int previousVisibleIndex = -1;
                for (Preference pref : this.mPreferenceListInternal) {
                    if (preference.equals(pref)) {
                        break;
                    } else if (pref.isVisible()) {
                        previousVisibleIndex++;
                    }
                }
                this.mPreferenceList.add(previousVisibleIndex + 1, preference);
                notifyItemInserted(previousVisibleIndex + 1);
                return;
            }
            int listSize = this.mPreferenceList.size();
            int removalIndex = 0;
            while (removalIndex < listSize && !preference.equals(this.mPreferenceList.get(removalIndex))) {
                removalIndex++;
            }
            this.mPreferenceList.remove(removalIndex);
            notifyItemRemoved(removalIndex);
        }
    }

    public int getItemViewType(int position) {
        this.mTempPreferenceLayout = createPreferenceLayout(getItem(position), this.mTempPreferenceLayout);
        int viewType = this.mPreferenceLayouts.indexOf(this.mTempPreferenceLayout);
        if (viewType != -1) {
            return viewType;
        }
        int viewType2 = this.mPreferenceLayouts.size();
        this.mPreferenceLayouts.add(new PreferenceLayout(this.mTempPreferenceLayout));
        return viewType2;
    }

    public PreferenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        PreferenceLayout pl = this.mPreferenceLayouts.get(viewType);
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TypedArray a = parent.getContext().obtainStyledAttributes((AttributeSet) null, R.styleable.BackgroundStyle);
        Drawable background = a.getDrawable(R.styleable.BackgroundStyle_android_selectableItemBackground);
        if (background == null) {
            background = ContextCompat.getDrawable(parent.getContext(), 17301602);
        }
        a.recycle();
        View view = inflater.inflate(pl.resId, parent, false);
        if (view.getBackground() == null) {
            ViewCompat.setBackground(view, background);
        }
        ViewGroup widgetFrame = (ViewGroup) view.findViewById(16908312);
        if (widgetFrame != null) {
            if (pl.widgetResId != 0) {
                inflater.inflate(pl.widgetResId, widgetFrame);
            } else {
                widgetFrame.setVisibility(8);
            }
        }
        return new PreferenceViewHolder(view);
    }

    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        getItem(position).onBindViewHolder(holder);
    }

    public int getPreferenceAdapterPosition(String key) {
        int size = this.mPreferenceList.size();
        for (int i = 0; i < size; i++) {
            if (TextUtils.equals(key, this.mPreferenceList.get(i).getKey())) {
                return i;
            }
        }
        return -1;
    }

    public int getPreferenceAdapterPosition(Preference preference) {
        int size = this.mPreferenceList.size();
        for (int i = 0; i < size; i++) {
            Preference candidate = this.mPreferenceList.get(i);
            if (candidate != null && candidate.equals(preference)) {
                return i;
            }
        }
        return -1;
    }
}
