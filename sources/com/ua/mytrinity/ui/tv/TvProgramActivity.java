package com.ua.mytrinity.ui.tv;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.ua.mytrinity.AppConfig;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.tv.TChannel;
import com.ua.mytrinity.tv.TChannelList;
import com.ua.mytrinity.tv.TEpg;
import com.ua.mytrinity.tv.TEpgFactory;
import com.ua.mytrinity.tv.TEpgItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

public class TvProgramActivity extends Activity implements AdapterView.OnItemClickListener {
    private ListView m_channel_list;
    private ChannelListAdapter m_channel_list_adapter;
    private AppConfig m_config;
    /* access modifiers changed from: private */
    public TEpg m_epg;
    private ExpandableListView m_epg_list;
    /* access modifiers changed from: private */
    public EpgListAdapter m_epg_list_adapter;
    /* access modifiers changed from: private */
    public LoadEpgTask m_load_epg_task;

    class ChannelListAdapter extends BaseAdapter {
        private LayoutInflater m_inflater;
        private TChannelList m_list;

        public ChannelListAdapter(Context context) {
            this.m_inflater = LayoutInflater.from(context);
            this.m_list = AppConfig.getAppConfig(context).channelList();
        }

        public int getCount() {
            return this.m_list.size();
        }

        public Object getItem(int position) {
            return this.m_list.elementAt(position);
        }

        public long getItemId(int position) {
            return (long) ((TChannel) this.m_list.elementAt(position)).id();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = this.m_inflater.inflate(R.layout.channel_list_item_no_epg, (ViewGroup) null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.setChannel((TChannel) this.m_list.elementAt(position));
            return convertView;
        }

        private class ViewHolder {
            TChannel channel = null;
            private ImageView icon;
            LoadImageTask load_icon_task = null;
            private TextView title;

            public ViewHolder(View view) {
                this.title = (TextView) view.findViewById(R.id.title);
                this.icon = (ImageView) view.findViewById(R.id.icon);
            }

            public void setChannel(TChannel channel2) {
                if (this.channel == null || this.channel.id() != channel2.id()) {
                    this.channel = channel2;
                    this.title.setText(channel2.index() + ". " + channel2.title());
                    updateIcon();
                    if (this.load_icon_task != null) {
                        this.load_icon_task.cancel(false);
                        this.load_icon_task = null;
                    }
                    if (channel2.icon() == null) {
                        this.load_icon_task = new LoadImageTask();
                        this.load_icon_task.execute(new ViewHolder[]{this});
                    }
                }
            }

            public void updateIcon() {
                this.icon.setImageBitmap(this.channel.icon());
                this.icon.setVisibility(this.channel.icon() == null ? 4 : 0);
            }
        }

        private class LoadImageTask extends AsyncTask<ViewHolder, Void, Void> {
            private ViewHolder m_holder;

            private LoadImageTask() {
            }

            /* access modifiers changed from: protected */
            public Void doInBackground(ViewHolder... params) {
                this.m_holder = params[0];
                this.m_holder.channel.loadIcon();
                return null;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Void result) {
                super.onPostExecute(result);
                if (this.m_holder.load_icon_task == this) {
                    this.m_holder.updateIcon();
                    this.m_holder.load_icon_task = null;
                }
            }
        }
    }

    class EpgListAdapter extends BaseExpandableListAdapter {
        private int m_channel_id;
        private SimpleDateFormat m_format = new SimpleDateFormat("EEEEE, d MMMMM");
        private LayoutInflater m_inflater;
        private ArrayList<DayItems> m_list;

        private class DayItems {
            String caption;
            int date;
            ArrayList<TEpgItem> list = new ArrayList<>();

            public DayItems() {
            }
        }

        public EpgListAdapter(Context context) {
            this.m_inflater = LayoutInflater.from(context);
            this.m_list = new ArrayList<>();
        }

        public void update() {
            ArrayList<TEpgItem> list = TvProgramActivity.this.m_epg.getListForChannel(this.m_channel_id);
            this.m_list.clear();
            if (list != null) {
                DayItems day = null;
                Calendar calendar = Calendar.getInstance();
                Iterator<TEpgItem> it = list.iterator();
                while (it.hasNext()) {
                    TEpgItem item = it.next();
                    calendar.setTime(item.timeStart());
                    if (day == null || calendar.get(6) != day.date) {
                        day = new DayItems();
                        day.date = calendar.get(6);
                        day.caption = this.m_format.format(item.timeStart());
                        this.m_list.add(day);
                    }
                    day.list.add(item);
                }
            }
            notifyDataSetChanged();
        }

        public void setChannel(int id) {
            this.m_channel_id = id;
            update();
        }

        public Object getChild(int groupPosition, int childPosition) {
            if (groupPosition < this.m_list.size()) {
                ArrayList<TEpgItem> list = this.m_list.get(groupPosition).list;
                if (childPosition < list.size()) {
                    return list.get(childPosition);
                }
            }
            return null;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return (long) childPosition;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ChildViewHolder holder;
            if (convertView == null) {
                convertView = this.m_inflater.inflate(R.layout.epg_list_item, (ViewGroup) null);
                holder = new ChildViewHolder();
                holder.start = (TextView) convertView.findViewById(R.id.epg_start);
                holder.title = (TextView) convertView.findViewById(R.id.epg_title);
                convertView.setTag(holder);
            } else {
                holder = (ChildViewHolder) convertView.getTag();
            }
            if (this.m_list != null) {
                TEpgItem item = this.m_list.get(groupPosition).list.get(childPosition);
                holder.start.setText(item.captionStart());
                holder.title.setText(item.title());
            }
            return convertView;
        }

        public int getChildrenCount(int groupPosition) {
            if (groupPosition < this.m_list.size()) {
                return this.m_list.get(groupPosition).list.size();
            }
            return 0;
        }

        public Object getGroup(int groupPosition) {
            return this.m_list.get(groupPosition);
        }

        public int getGroupCount() {
            return this.m_list.size();
        }

        public long getGroupId(int groupPosition) {
            return (long) groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            GroupViewHolder holder;
            if (convertView == null) {
                convertView = this.m_inflater.inflate(R.layout.epg_group_item, (ViewGroup) null);
                holder = new GroupViewHolder();
                holder.groupCaption = (TextView) convertView.findViewById(R.id.groupCaption);
                convertView.setTag(holder);
            } else {
                holder = (GroupViewHolder) convertView.getTag();
            }
            if (this.m_list != null && groupPosition < this.m_list.size()) {
                holder.groupCaption.setText(this.m_list.get(groupPosition).caption);
            }
            return convertView;
        }

        public boolean hasStableIds() {
            return false;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        private class ChildViewHolder {
            TextView start;
            TextView title;

            private ChildViewHolder() {
            }
        }

        private class GroupViewHolder {
            TextView groupCaption;

            private GroupViewHolder() {
            }
        }
    }

    private class LoadEpgTask extends AsyncTask<Integer, Void, TEpg> {
        private LoadEpgTask() {
        }

        /* access modifiers changed from: protected */
        public TEpg doInBackground(Integer... params) {
            return TEpgFactory.load(params[0].intValue(), false);
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(TEpg result) {
            TvProgramActivity.this.m_epg.merge(result);
            TvProgramActivity.this.m_epg_list_adapter.update();
            LoadEpgTask unused = TvProgramActivity.this.m_load_epg_task = null;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_program);
        this.m_config = AppConfig.getAppConfig(this);
        this.m_epg = this.m_config.epg();
        this.m_channel_list = (ListView) findViewById(R.id.epg_list_channel);
        this.m_epg_list = (ExpandableListView) findViewById(R.id.epg_list_program);
        ListView listView = this.m_channel_list;
        ChannelListAdapter channelListAdapter = new ChannelListAdapter(this);
        this.m_channel_list_adapter = channelListAdapter;
        listView.setAdapter(channelListAdapter);
        this.m_channel_list.setChoiceMode(1);
        this.m_channel_list.setItemChecked(0, true);
        ExpandableListView expandableListView = this.m_epg_list;
        EpgListAdapter epgListAdapter = new EpgListAdapter(this);
        this.m_epg_list_adapter = epgListAdapter;
        expandableListView.setAdapter(epgListAdapter);
        if (this.m_channel_list_adapter.getCount() > 0) {
            this.m_epg_list_adapter.setChannel(((TChannel) this.m_channel_list_adapter.getItem(0)).id());
        }
        this.m_channel_list.setOnItemClickListener(this);
        this.m_load_epg_task = null;
        onItemClick((AdapterView<?>) null, (View) null, 0, this.m_channel_list_adapter.getItemId(0));
    }

    public void onStart() {
        super.onStart();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
        this.m_channel_list.setItemChecked(pos, true);
        int channel_id = (int) id;
        if (channel_id > 0) {
            this.m_epg_list_adapter.setChannel(channel_id);
            if (!this.m_epg.hasTomorrowEpg(channel_id)) {
                if (this.m_load_epg_task != null) {
                    this.m_load_epg_task.cancel(false);
                }
                this.m_load_epg_task = new LoadEpgTask();
                this.m_load_epg_task.execute(new Integer[]{Integer.valueOf(channel_id)});
            }
        }
    }
}
