package com.ua.mytrinity.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.ua.mytrinity.player.R;
import java.util.ArrayList;
import java.util.List;

public class AppPickerPreference extends ListPreference {
    private static final String TAG = "AppPickerPreference";
    private Context context;
    private String defaultIconFile;
    private ImageView icon;
    private List<IconItem> icons;
    private String key;
    private final PackageManager packageManager;
    private SharedPreferences preferences;
    private Resources resources;
    private IconItem selectedApp;
    private String selectedIconFile;
    private TextView summary;

    private class CustomListPreferenceAdapter extends ArrayAdapter<IconItem> {
        private Context context;
        /* access modifiers changed from: private */
        public List<IconItem> icons;
        private int resource;

        public CustomListPreferenceAdapter(Context context2, int resource2, List<IconItem> objects) {
            super(context2, resource2, objects);
            this.context = context2;
            this.resource = resource2;
            this.icons = objects;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) this.context.getSystemService("layout_inflater")).inflate(this.resource, parent, false);
                holder = new ViewHolder();
                holder.iconName = (TextView) convertView.findViewById(R.id.appName);
                holder.iconImage = (ImageView) convertView.findViewById(R.id.appIcon);
                holder.radioButton = (RadioButton) convertView.findViewById(R.id.appRadio);
                holder.position = position;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            IconItem iconItem = this.icons.get(position);
            holder.iconName.setText(iconItem.name);
            holder.iconImage.setImageDrawable(iconItem.icon);
            holder.radioButton.setChecked(iconItem.isChecked);
            convertView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ViewHolder holder = (ViewHolder) v.getTag();
                    int i = 0;
                    while (i < CustomListPreferenceAdapter.this.icons.size()) {
                        boolean unused = ((IconItem) CustomListPreferenceAdapter.this.icons.get(i)).isChecked = i == holder.position;
                        i++;
                    }
                    AppPickerPreference.this.getDialog().dismiss();
                }
            });
            return convertView;
        }
    }

    private static class IconItem {
        /* access modifiers changed from: private */
        public Drawable icon;
        /* access modifiers changed from: private */
        public boolean isChecked;
        /* access modifiers changed from: private */
        public String name;
        /* access modifiers changed from: private */
        public String packageName;

        IconItem(CharSequence name2, CharSequence packageName2, Drawable icon2, boolean isChecked2) {
            this(name2.toString(), packageName2.toString(), icon2, isChecked2);
        }

        IconItem(String name2, String packageName2, Drawable icon2, boolean isChecked2) {
            this.name = name2;
            this.packageName = packageName2;
            this.icon = icon2;
            this.isChecked = isChecked2;
        }
    }

    private static class ViewHolder {
        ImageView iconImage;
        TextView iconName;
        int position;
        RadioButton radioButton;

        private ViewHolder() {
        }
    }

    public AppPickerPreference(Context context2, AttributeSet attrs) {
        super(context2, attrs);
        this.context = context2;
        Log.d(TAG, "key: " + this.key);
        this.resources = context2.getResources();
        this.packageManager = context2.getPackageManager();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context2);
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        super.onBindView(view);
        this.key = getKey();
        Log.d(TAG, "onBindView key: " + this.key);
        this.selectedIconFile = this.preferences.getString(this.key, this.defaultIconFile);
        enumerateApps();
        this.icon = (ImageView) view.findViewById(R.id.iconSelected);
        this.summary = (TextView) view.findViewById(16908304);
        updateIcon();
    }

    /* access modifiers changed from: protected */
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (this.icons != null) {
            for (IconItem item : this.icons) {
                if (item.isChecked) {
                    this.selectedIconFile = item.packageName;
                    this.selectedApp = item;
                    updateIcon();
                    this.summary.setText(item.name);
                    return;
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNegativeButton("Cancel", (DialogInterface.OnClickListener) null);
        builder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
        this.key = getKey();
        Log.d(TAG, "onPrepareDialogBuilder key: " + this.key);
        builder.setAdapter(new CustomListPreferenceAdapter(this.context, R.layout.pref_list_with_icon, this.icons), (DialogInterface.OnClickListener) null);
    }

    private void enumerateApps() {
        this.icons = new ArrayList();
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(Uri.parse("http://vod1.trkmetro.net/file.mp4"), MimeTypes.VIDEO_MP4);
        List<ResolveInfo> list = this.packageManager.queryIntentActivities(intent, 65536);
        if (list != null) {
            for (ResolveInfo resolveInfo : list) {
                boolean isChecked = resolveInfo.activityInfo.packageName.equals(this.selectedIconFile);
                IconItem item = new IconItem(resolveInfo.loadLabel(this.packageManager), (CharSequence) resolveInfo.activityInfo.packageName, resolveInfo.loadIcon(this.packageManager), isChecked);
                this.icons.add(item);
                if (isChecked) {
                    this.selectedApp = item;
                }
            }
        }
    }

    private void updateIcon() {
        if (this.selectedApp != null) {
            this.icon.setImageDrawable(this.selectedApp.icon);
            this.summary.setText(this.selectedApp.name);
        }
    }
}
