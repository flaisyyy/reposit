package com.ua.mytrinity.ui.tv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.ua.mytrinity.player.R;
import java.util.ArrayList;

public class ActionImageAdapter extends BaseAdapter {
    ArrayList<Integer> m_icons;
    LayoutInflater m_inflater;
    ArrayList<CharSequence> m_titles = new ArrayList<>();

    public ActionImageAdapter(Context context) {
        this.m_inflater = LayoutInflater.from(context);
        for (String act : context.getResources().getStringArray(R.array.act_array)) {
            this.m_titles.add(act);
        }
        this.m_icons = new ArrayList<>();
        this.m_icons.add(Integer.valueOf(R.drawable.act_back_to_real));
        this.m_icons.add(Integer.valueOf(R.drawable.act_navigation));
        this.m_icons.add(Integer.valueOf(R.drawable.act_show_list));
        this.m_icons.add(Integer.valueOf(R.drawable.act_to_program_begin));
        this.m_icons.add(Integer.valueOf(R.drawable.act_media_portal));
    }

    public int getCount() {
        return this.m_titles.size();
    }

    public Object getItem(int position) {
        return Integer.valueOf(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.m_inflater.inflate(R.layout.action_item, parent, false);
        }
        TextView textView = (TextView) convertView.findViewById(R.id.label);
        textView.setCompoundDrawablesWithIntrinsicBounds(0, this.m_icons.get(position).intValue(), 0, 0);
        textView.setText(this.m_titles.get(position));
        return convertView;
    }
}
