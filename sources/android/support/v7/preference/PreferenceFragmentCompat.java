package android.support.v7.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.XmlRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.internal.AbstractMultiSelectListPreference;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class PreferenceFragmentCompat extends Fragment implements PreferenceManager.OnPreferenceTreeClickListener, PreferenceManager.OnDisplayPreferenceDialogListener, PreferenceManager.OnNavigateToScreenListener, DialogPreference.TargetFragment {
    public static final String ARG_PREFERENCE_ROOT = "android.support.v7.preference.PreferenceFragmentCompat.PREFERENCE_ROOT";
    private static final String DIALOG_FRAGMENT_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG";
    private static final int MSG_BIND_PREFERENCES = 1;
    private static final String PREFERENCES_TAG = "android:preferences";
    private final DividerDecoration mDividerDecoration = new DividerDecoration();
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PreferenceFragmentCompat.this.bindPreferences();
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mHavePrefs;
    private boolean mInitDone;
    private int mLayoutResId = R.layout.preference_list_fragment;
    /* access modifiers changed from: private */
    public RecyclerView mList;
    private PreferenceManager mPreferenceManager;
    private final Runnable mRequestFocus = new Runnable() {
        public void run() {
            PreferenceFragmentCompat.this.mList.focusableViewAvailable(PreferenceFragmentCompat.this.mList);
        }
    };
    private Runnable mSelectPreferenceRunnable;
    private Context mStyledContext;

    public interface OnPreferenceDisplayDialogCallback {
        boolean onPreferenceDisplayDialog(@NonNull PreferenceFragmentCompat preferenceFragmentCompat, Preference preference);
    }

    public interface OnPreferenceStartFragmentCallback {
        boolean onPreferenceStartFragment(PreferenceFragmentCompat preferenceFragmentCompat, Preference preference);
    }

    public interface OnPreferenceStartScreenCallback {
        boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen);
    }

    public abstract void onCreatePreferences(Bundle bundle, String str);

    public void onCreate(Bundle savedInstanceState) {
        String rootKey;
        super.onCreate(savedInstanceState);
        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.preferenceTheme, tv, true);
        int theme = tv.resourceId;
        if (theme == 0) {
            throw new IllegalStateException("Must specify preferenceTheme in theme");
        }
        this.mStyledContext = new ContextThemeWrapper(getActivity(), theme);
        this.mPreferenceManager = new PreferenceManager(this.mStyledContext);
        this.mPreferenceManager.setOnNavigateToScreenListener(this);
        if (getArguments() != null) {
            rootKey = getArguments().getString(ARG_PREFERENCE_ROOT);
        } else {
            rootKey = null;
        }
        onCreatePreferences(savedInstanceState, rootKey);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TypedArray a = this.mStyledContext.obtainStyledAttributes((AttributeSet) null, R.styleable.PreferenceFragmentCompat, R.attr.preferenceFragmentCompatStyle, 0);
        this.mLayoutResId = a.getResourceId(R.styleable.PreferenceFragmentCompat_android_layout, this.mLayoutResId);
        Drawable divider = a.getDrawable(R.styleable.PreferenceFragmentCompat_android_divider);
        int dividerHeight = a.getDimensionPixelSize(R.styleable.PreferenceFragmentCompat_android_dividerHeight, -1);
        boolean allowDividerAfterLastItem = a.getBoolean(R.styleable.PreferenceFragmentCompat_allowDividerAfterLastItem, true);
        a.recycle();
        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.preferenceTheme, tv, true);
        LayoutInflater layoutInflater = inflater;
        LayoutInflater themedInflater = layoutInflater.cloneInContext(new ContextThemeWrapper(inflater.getContext(), tv.resourceId));
        View view = themedInflater.inflate(this.mLayoutResId, container, false);
        View rawListContainer = view.findViewById(AndroidResources.ANDROID_R_LIST_CONTAINER);
        if (!(rawListContainer instanceof ViewGroup)) {
            throw new RuntimeException("Content has view with id attribute 'android.R.id.list_container' that is not a ViewGroup class");
        }
        ViewGroup listContainer = (ViewGroup) rawListContainer;
        RecyclerView listView = onCreateRecyclerView(themedInflater, listContainer, savedInstanceState);
        if (listView == null) {
            throw new RuntimeException("Could not create RecyclerView");
        }
        this.mList = listView;
        listView.addItemDecoration(this.mDividerDecoration);
        setDivider(divider);
        if (dividerHeight != -1) {
            setDividerHeight(dividerHeight);
        }
        this.mDividerDecoration.setAllowDividerAfterLastItem(allowDividerAfterLastItem);
        listContainer.addView(this.mList);
        this.mHandler.post(this.mRequestFocus);
        return view;
    }

    public void setDivider(Drawable divider) {
        this.mDividerDecoration.setDivider(divider);
    }

    public void setDividerHeight(int height) {
        this.mDividerDecoration.setDividerHeight(height);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.mHavePrefs) {
            bindPreferences();
            if (this.mSelectPreferenceRunnable != null) {
                this.mSelectPreferenceRunnable.run();
                this.mSelectPreferenceRunnable = null;
            }
        }
        this.mInitDone = true;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        Bundle container;
        PreferenceScreen preferenceScreen;
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && (container = savedInstanceState.getBundle(PREFERENCES_TAG)) != null && (preferenceScreen = getPreferenceScreen()) != null) {
            preferenceScreen.restoreHierarchyState(container);
        }
    }

    public void onStart() {
        super.onStart();
        this.mPreferenceManager.setOnPreferenceTreeClickListener(this);
        this.mPreferenceManager.setOnDisplayPreferenceDialogListener(this);
    }

    public void onStop() {
        super.onStop();
        this.mPreferenceManager.setOnPreferenceTreeClickListener((PreferenceManager.OnPreferenceTreeClickListener) null);
        this.mPreferenceManager.setOnDisplayPreferenceDialogListener((PreferenceManager.OnDisplayPreferenceDialogListener) null);
    }

    public void onDestroyView() {
        this.mHandler.removeCallbacks(this.mRequestFocus);
        this.mHandler.removeMessages(1);
        if (this.mHavePrefs) {
            unbindPreferences();
        }
        this.mList = null;
        super.onDestroyView();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            Bundle container = new Bundle();
            preferenceScreen.saveHierarchyState(container);
            outState.putBundle(PREFERENCES_TAG, container);
        }
    }

    public PreferenceManager getPreferenceManager() {
        return this.mPreferenceManager;
    }

    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (this.mPreferenceManager.setPreferences(preferenceScreen) && preferenceScreen != null) {
            onUnbindPreferences();
            this.mHavePrefs = true;
            if (this.mInitDone) {
                postBindPreferences();
            }
        }
    }

    public PreferenceScreen getPreferenceScreen() {
        return this.mPreferenceManager.getPreferenceScreen();
    }

    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromResource(this.mStyledContext, preferencesResId, getPreferenceScreen()));
    }

    public void setPreferencesFromResource(@XmlRes int preferencesResId, @Nullable String key) {
        Preference root;
        requirePreferenceManager();
        PreferenceScreen xmlRoot = this.mPreferenceManager.inflateFromResource(this.mStyledContext, preferencesResId, (PreferenceScreen) null);
        if (key != null) {
            root = xmlRoot.findPreference(key);
            if (!(root instanceof PreferenceScreen)) {
                throw new IllegalArgumentException("Preference object with key " + key + " is not a PreferenceScreen");
            }
        } else {
            root = xmlRoot;
        }
        setPreferenceScreen((PreferenceScreen) root);
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getFragment() == null) {
            return false;
        }
        boolean handled = false;
        if (getCallbackFragment() instanceof OnPreferenceStartFragmentCallback) {
            handled = ((OnPreferenceStartFragmentCallback) getCallbackFragment()).onPreferenceStartFragment(this, preference);
        }
        if (handled || !(getActivity() instanceof OnPreferenceStartFragmentCallback)) {
            return handled;
        }
        return ((OnPreferenceStartFragmentCallback) getActivity()).onPreferenceStartFragment(this, preference);
    }

    public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
        boolean handled = false;
        if (getCallbackFragment() instanceof OnPreferenceStartScreenCallback) {
            handled = ((OnPreferenceStartScreenCallback) getCallbackFragment()).onPreferenceStartScreen(this, preferenceScreen);
        }
        if (!handled && (getActivity() instanceof OnPreferenceStartScreenCallback)) {
            ((OnPreferenceStartScreenCallback) getActivity()).onPreferenceStartScreen(this, preferenceScreen);
        }
    }

    public Preference findPreference(CharSequence key) {
        if (this.mPreferenceManager == null) {
            return null;
        }
        return this.mPreferenceManager.findPreference(key);
    }

    private void requirePreferenceManager() {
        if (this.mPreferenceManager == null) {
            throw new RuntimeException("This should be called after super.onCreate.");
        }
    }

    private void postBindPreferences() {
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.obtainMessage(1).sendToTarget();
        }
    }

    /* access modifiers changed from: private */
    public void bindPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            getListView().setAdapter(onCreateAdapter(preferenceScreen));
            preferenceScreen.onAttached();
        }
        onBindPreferences();
    }

    private void unbindPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.onDetached();
        }
        onUnbindPreferences();
    }

    /* access modifiers changed from: protected */
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public void onBindPreferences() {
    }

    /* access modifiers changed from: protected */
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public void onUnbindPreferences() {
    }

    public final RecyclerView getListView() {
        return this.mList;
    }

    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.preference_recyclerview, parent, false);
        recyclerView.setLayoutManager(onCreateLayoutManager());
        recyclerView.setAccessibilityDelegateCompat(new PreferenceRecyclerViewAccessibilityDelegate(recyclerView));
        return recyclerView;
    }

    public RecyclerView.LayoutManager onCreateLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    /* access modifiers changed from: protected */
    public RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new PreferenceGroupAdapter(preferenceScreen);
    }

    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment f;
        boolean handled = false;
        if (getCallbackFragment() instanceof OnPreferenceDisplayDialogCallback) {
            handled = ((OnPreferenceDisplayDialogCallback) getCallbackFragment()).onPreferenceDisplayDialog(this, preference);
        }
        if (!handled && (getActivity() instanceof OnPreferenceDisplayDialogCallback)) {
            handled = ((OnPreferenceDisplayDialogCallback) getActivity()).onPreferenceDisplayDialog(this, preference);
        }
        if (!handled && getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) == null) {
            if (preference instanceof EditTextPreference) {
                f = EditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            } else if (preference instanceof ListPreference) {
                f = ListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            } else if (preference instanceof AbstractMultiSelectListPreference) {
                f = MultiSelectListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            } else {
                throw new IllegalArgumentException("Tried to display dialog for unknown preference type. Did you forget to override onDisplayPreferenceDialog()?");
            }
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        }
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public Fragment getCallbackFragment() {
        return null;
    }

    public void scrollToPreference(String key) {
        scrollToPreferenceInternal((Preference) null, key);
    }

    public void scrollToPreference(Preference preference) {
        scrollToPreferenceInternal(preference, (String) null);
    }

    private void scrollToPreferenceInternal(final Preference preference, final String key) {
        Runnable r = new Runnable() {
            public void run() {
                int position;
                RecyclerView.Adapter adapter = PreferenceFragmentCompat.this.mList.getAdapter();
                if (adapter instanceof PreferenceGroup.PreferencePositionCallback) {
                    if (preference != null) {
                        position = ((PreferenceGroup.PreferencePositionCallback) adapter).getPreferenceAdapterPosition(preference);
                    } else {
                        position = ((PreferenceGroup.PreferencePositionCallback) adapter).getPreferenceAdapterPosition(key);
                    }
                    if (position != -1) {
                        PreferenceFragmentCompat.this.mList.scrollToPosition(position);
                    } else {
                        adapter.registerAdapterDataObserver(new ScrollToPreferenceObserver(adapter, PreferenceFragmentCompat.this.mList, preference, key));
                    }
                } else if (adapter != null) {
                    throw new IllegalStateException("Adapter must implement PreferencePositionCallback");
                }
            }
        };
        if (this.mList == null) {
            this.mSelectPreferenceRunnable = r;
        } else {
            r.run();
        }
    }

    private static class ScrollToPreferenceObserver extends RecyclerView.AdapterDataObserver {
        private final RecyclerView.Adapter mAdapter;
        private final String mKey;
        private final RecyclerView mList;
        private final Preference mPreference;

        public ScrollToPreferenceObserver(RecyclerView.Adapter adapter, RecyclerView list, Preference preference, String key) {
            this.mAdapter = adapter;
            this.mList = list;
            this.mPreference = preference;
            this.mKey = key;
        }

        private void scrollToPreference() {
            int position;
            this.mAdapter.unregisterAdapterDataObserver(this);
            if (this.mPreference != null) {
                position = ((PreferenceGroup.PreferencePositionCallback) this.mAdapter).getPreferenceAdapterPosition(this.mPreference);
            } else {
                position = ((PreferenceGroup.PreferencePositionCallback) this.mAdapter).getPreferenceAdapterPosition(this.mKey);
            }
            if (position != -1) {
                this.mList.scrollToPosition(position);
            }
        }

        public void onChanged() {
            scrollToPreference();
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            scrollToPreference();
        }

        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            scrollToPreference();
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            scrollToPreference();
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            scrollToPreference();
        }

        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            scrollToPreference();
        }
    }

    private class DividerDecoration extends RecyclerView.ItemDecoration {
        private boolean mAllowDividerAfterLastItem;
        private Drawable mDivider;
        private int mDividerHeight;

        private DividerDecoration() {
            this.mAllowDividerAfterLastItem = true;
        }

        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if (this.mDivider != null) {
                int childCount = parent.getChildCount();
                int width = parent.getWidth();
                for (int childViewIndex = 0; childViewIndex < childCount; childViewIndex++) {
                    View view = parent.getChildAt(childViewIndex);
                    if (shouldDrawDividerBelow(view, parent)) {
                        int top = ((int) view.getY()) + view.getHeight();
                        this.mDivider.setBounds(0, top, width, this.mDividerHeight + top);
                        this.mDivider.draw(c);
                    }
                }
            }
        }

        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (shouldDrawDividerBelow(view, parent)) {
                outRect.bottom = this.mDividerHeight;
            }
        }

        private boolean shouldDrawDividerBelow(View view, RecyclerView parent) {
            boolean dividerAllowedBelow;
            RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
            if (!(holder instanceof PreferenceViewHolder) || !((PreferenceViewHolder) holder).isDividerAllowedBelow()) {
                dividerAllowedBelow = false;
            } else {
                dividerAllowedBelow = true;
            }
            if (!dividerAllowedBelow) {
                return false;
            }
            boolean nextAllowed = this.mAllowDividerAfterLastItem;
            int index = parent.indexOfChild(view);
            if (index < parent.getChildCount() - 1) {
                RecyclerView.ViewHolder nextHolder = parent.getChildViewHolder(parent.getChildAt(index + 1));
                if (!(nextHolder instanceof PreferenceViewHolder) || !((PreferenceViewHolder) nextHolder).isDividerAllowedAbove()) {
                    nextAllowed = false;
                } else {
                    nextAllowed = true;
                }
            }
            return nextAllowed;
        }

        public void setDivider(Drawable divider) {
            if (divider != null) {
                this.mDividerHeight = divider.getIntrinsicHeight();
            } else {
                this.mDividerHeight = 0;
            }
            this.mDivider = divider;
            PreferenceFragmentCompat.this.mList.invalidateItemDecorations();
        }

        public void setDividerHeight(int dividerHeight) {
            this.mDividerHeight = dividerHeight;
            PreferenceFragmentCompat.this.mList.invalidateItemDecorations();
        }

        public void setAllowDividerAfterLastItem(boolean allowDividerAfterLastItem) {
            this.mAllowDividerAfterLastItem = allowDividerAfterLastItem;
        }
    }
}
