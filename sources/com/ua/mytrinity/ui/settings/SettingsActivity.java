package com.ua.mytrinity.ui.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import com.ua.mytrinity.player.R;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object value) {
            CharSequence charSequence = null;
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                if (index >= 0) {
                    charSequence = listPreference.getEntries()[index];
                }
                preference.setSummary(charSequence);
                return true;
            } else if (!(preference instanceof RingtonePreference)) {
                preference.setSummary(stringValue);
                return true;
            } else if (TextUtils.isEmpty(stringValue)) {
                preference.setSummary(R.string.pref_ringtone_silent);
                return true;
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));
                if (ringtone == null) {
                    preference.setSummary((CharSequence) null);
                    return true;
                }
                preference.setSummary(ringtone.getTitle(preference.getContext()));
                return true;
            }
        }
    };

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & 15) >= 4;
    }

    /* access modifiers changed from: private */
    public static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @TargetApi(11)
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /* access modifiers changed from: protected */
    public boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName) || GeneralPreferenceFragment.class.getName().equals(fragmentName) || DataSyncPreferenceFragment.class.getName().equals(fragmentName) || NotificationPreferenceFragment.class.getName().equals(fragmentName) || MediaPortalPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(11)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            SettingsActivity.bindPreferenceSummaryToValue(findPreference("example_text"));
            SettingsActivity.bindPreferenceSummaryToValue(findPreference("example_list"));
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() != 16908332) {
                return super.onOptionsItemSelected(item);
            }
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
    }

    @TargetApi(11)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);
            SettingsActivity.bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() != 16908332) {
                return super.onOptionsItemSelected(item);
            }
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
    }

    @TargetApi(11)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);
            SettingsActivity.bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() != 16908332) {
                return super.onOptionsItemSelected(item);
            }
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
    }

    public static class MediaPortalPreferenceFragment extends PreferenceFragment {
        private static final String TAG = "MediaPortalPref";

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_media_portal);
            setHasOptionsMenu(true);
            ListPreference player_app = (ListPreference) findPreference("player_app");
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setDataAndType(Uri.parse("http://vod1.trkmetro.net/file.mp4"), "application/x-mpegurl");
            PackageManager pm = getActivity().getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 65536);
            ArrayList<CharSequence> entries = new ArrayList<>();
            ArrayList<CharSequence> entryValues = new ArrayList<>();
            if (list != null) {
                for (ResolveInfo resolveInfo : list) {
                    entries.add(resolveInfo.loadLabel(pm));
                    entryValues.add(resolveInfo.activityInfo.packageName);
                }
            }
            player_app.setEntries((CharSequence[]) entries.toArray(new CharSequence[entries.size()]));
            player_app.setEntryValues((CharSequence[]) entryValues.toArray(new CharSequence[entryValues.size()]));
            SettingsActivity.bindPreferenceSummaryToValue(player_app);
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() != 16908332) {
                return super.onOptionsItemSelected(item);
            }
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
    }
}
