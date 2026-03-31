package com.ua.mytrinity.ui.tv;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.ua.mytrinity.AppConfig;
import com.ua.mytrinity.player.R;
import com.ua.mytrinity.tv.TChannel;
import com.ua.mytrinity.tv.TChannelList;
import com.ua.mytrinity.tv.TEpg;
import com.ua.mytrinity.tv.TEpgFactory;
import com.ua.mytrinity.tv.TEpgItem;
import com.ua.mytrinity.tv.TTimeOffset;

class ChannelListAdapter extends BaseAdapter {
    private static final String TAG = "ChannelListAdapter";
    /* access modifiers changed from: private */
    public TEpg m_epg;
    private LayoutInflater m_inflater;
    /* access modifiers changed from: private */
    public LastOffsetMap m_last_offset_map;
    private TChannelList m_list;
    /* access modifiers changed from: private */
    public CharSequence m_minutes;

    public ChannelListAdapter(Context context, LastOffsetMap last_offset_map) {
        this.m_inflater = LayoutInflater.from(context);
        AppConfig config = AppConfig.getAppConfig(context);
        this.m_list = config.channelList();
        this.m_epg = config.epg();
        this.m_last_offset_map = last_offset_map;
        this.m_minutes = context.getResources().getText(R.string.minutes);
    }

    public void setEpg(TEpg epg) {
        this.m_epg = epg;
    }

    public int getCount() {
        return this.m_list.size();
    }

    public Object getItem(int position) {
        return this.m_list.elementAt(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = this.m_inflater.inflate(R.layout.channel_list_item, (ViewGroup) null);
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
        private ProgressBar epg_progress;
        private TextView epg_start;
        private TextView epg_stop;
        private TextView epg_title;
        /* access modifiers changed from: private */
        public ImageView icon;
        LoadEpgTask load_epg_task;
        LoadImageTask load_icon_task = null;
        private TextView time_offset;
        private ImageView time_shifted;
        private TextView title;

        public ViewHolder(View view) {
            this.title = (TextView) view.findViewById(R.id.title);
            this.epg_title = (TextView) view.findViewById(R.id.epg_title);
            this.epg_start = (TextView) view.findViewById(R.id.epg_start);
            this.epg_stop = (TextView) view.findViewById(R.id.epg_stop);
            this.epg_progress = (ProgressBar) view.findViewById(R.id.epg_progress);
            this.icon = (ImageView) view.findViewById(R.id.icon);
            this.time_offset = (TextView) view.findViewById(R.id.time_offset);
            this.time_shifted = (ImageView) view.findViewById(R.id.time_shifted);
        }

        public void setChannel(TChannel channel2) {
            if (this.channel == null || this.channel.id() != channel2.id()) {
                this.channel = channel2;
                this.title.setText(channel2.index() + ". " + channel2.title());
                this.time_shifted.setVisibility(channel2.offsets().size() > 0 ? 0 : 4);
                updateIcon();
                updateEpg();
                if (this.load_icon_task != null) {
                    this.load_icon_task.cancel(false);
                    this.load_icon_task = null;
                }
                if (channel2.icon() == null) {
                    this.load_icon_task = new LoadImageTask();
                    this.load_icon_task.execute(new ViewHolder[]{this});
                    Log.i(ChannelListAdapter.TAG, "channel " + channel2.id() + ": no icon - start load icon task");
                }
                if (this.load_epg_task != null) {
                    this.load_epg_task.cancel(false);
                    this.load_epg_task = null;
                }
                if (!ChannelListAdapter.this.m_epg.hasTodayEpg(channel2.id())) {
                    this.load_epg_task = new LoadEpgTask();
                    this.load_epg_task.execute(new ViewHolder[]{this});
                    Log.i(ChannelListAdapter.TAG, "channel " + channel2.id() + ": no epg - start load epg task");
                }
            }
        }

        public void updateIcon() {
            this.icon.setImageBitmap(this.channel.icon());
            this.icon.setVisibility(this.channel.icon() == null ? 4 : 0);
        }

        public void updateEpg() {
            TTimeOffset current_offset = ChannelListAdapter.this.m_last_offset_map.get(this.channel);
            TEpgItem item = ChannelListAdapter.this.m_epg.getCurrentForChannel(this.channel.id(), current_offset);
            if (current_offset == null) {
                this.time_offset.setText((CharSequence) null);
            } else {
                this.time_offset.setText(String.valueOf((-current_offset.offsetSec()) / 60) + " " + ChannelListAdapter.this.m_minutes);
            }
            int visible = item == null ? 4 : 0;
            this.epg_title.setVisibility(visible);
            this.epg_start.setVisibility(visible);
            this.epg_stop.setVisibility(visible);
            this.epg_progress.setVisibility(visible);
            if (item != null) {
                this.epg_title.setText(item.title());
                this.epg_start.setText(item.captionStart());
                this.epg_stop.setText(item.captionStop());
                this.epg_progress.setProgress((int) (item.progress(current_offset) * 100.0f));
            }
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
                Animation animation = new AlphaAnimation(0.0f, 1.0f);
                animation.setDuration(100);
                this.m_holder.icon.startAnimation(animation);
            }
        }
    }

    private class LoadEpgTask extends AsyncTask<ViewHolder, Void, TEpg> {
        private ViewHolder m_holder;

        private LoadEpgTask() {
        }

        /* access modifiers changed from: protected */
        public TEpg doInBackground(ViewHolder... params) {
            this.m_holder = params[0];
            return TEpgFactory.load(this.m_holder.channel.id(), true);
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(TEpg result) {
            super.onPostExecute(result);
            ChannelListAdapter.this.m_epg.merge(result);
            if (this.m_holder.load_epg_task == this) {
                this.m_holder.updateEpg();
                this.m_holder.load_epg_task = null;
            }
        }
    }

    public void invalidate() {
        notifyDataSetInvalidated();
    }
}
