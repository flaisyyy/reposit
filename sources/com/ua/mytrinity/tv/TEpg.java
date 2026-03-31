package com.ua.mytrinity.tv;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TEpg {
    private HashMap<Integer, ArrayList<TEpgItem>> m_data = new HashMap<>();
    private String m_hash;

    public void setData(HashMap<Integer, ArrayList<TEpgItem>> data) {
        this.m_data = data;
    }

    public void setHash(String hash) {
        this.m_hash = hash;
    }

    public String hash() {
        return this.m_hash;
    }

    public ArrayList<TEpgItem> getListForChannel(int channel_id) {
        return this.m_data.get(Integer.valueOf(channel_id));
    }

    public boolean hasEpg(int channel_id) {
        return this.m_data.containsKey(Integer.valueOf(channel_id));
    }

    public TEpgItem getCurrentForChannel(int channel_id, TTimeOffset offset) {
        ArrayList<TEpgItem> list = getListForChannel(channel_id);
        if (list != null) {
            Iterator<TEpgItem> i = list.iterator();
            while (i.hasNext()) {
                TEpgItem item = i.next();
                if (item.isNow(offset)) {
                    return item;
                }
            }
        }
        return null;
    }

    public TEpgItem getCurrentForChannel(int channel_id) {
        return getCurrentForChannel(channel_id, (TTimeOffset) null);
    }

    public TEpgItem getNextForChannel(int channel_id, TTimeOffset offset) {
        ArrayList<TEpgItem> list = getListForChannel(channel_id);
        if (list == null) {
            return null;
        }
        Iterator<TEpgItem> i = list.iterator();
        while (i.hasNext()) {
            if (i.next().isNow(offset)) {
                if (i.hasNext()) {
                    return i.next();
                }
                return null;
            }
        }
        return null;
    }

    public TEpgItem getNextForChannel(int channel_id) {
        return getNextForChannel(channel_id);
    }

    public TEpgItem getPrevForChannel(int channel_id, TTimeOffset offset) {
        ArrayList<TEpgItem> list = getListForChannel(channel_id);
        if (list != null) {
            Iterator<TEpgItem> i = list.iterator();
            TEpgItem prew = null;
            while (i.hasNext()) {
                TEpgItem item = i.next();
                if (item.isNow(offset)) {
                    return prew;
                }
                prew = item;
            }
        }
        return null;
    }

    public TEpgItem getPrevForChannel(int channel_id) {
        return getPrevForChannel(channel_id, (TTimeOffset) null);
    }

    public void merge(TEpg other) {
        for (Map.Entry<Integer, ArrayList<TEpgItem>> pair : other.m_data.entrySet()) {
            Integer key = pair.getKey();
            ArrayList<TEpgItem> value = pair.getValue();
            if (this.m_data.containsKey(key)) {
                ArrayList<TEpgItem> data = this.m_data.get(key);
                data.clear();
                data.addAll(value);
            } else {
                this.m_data.put(key, value);
            }
        }
    }

    public boolean hasTodayEpg(int channel_id) {
        ArrayList<TEpgItem> data = getListForChannel(channel_id);
        if (data == null) {
            return false;
        }
        Calendar c_start = Calendar.getInstance();
        c_start.set(11, 0);
        c_start.set(12, 0);
        c_start.set(13, 0);
        c_start.set(14, 0);
        Calendar c_stop = (Calendar) c_start.clone();
        c_stop.add(5, 1);
        Iterator<TEpgItem> it = data.iterator();
        while (it.hasNext()) {
            TEpgItem i = it.next();
            if (c_start.getTimeInMillis() < i.timeStart().getTime() && i.timeStart().getTime() < c_stop.getTimeInMillis()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTomorrowEpg(int channel_id) {
        ArrayList<TEpgItem> data = getListForChannel(channel_id);
        if (data == null) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        c.add(5, 1);
        c.set(11, 0);
        c.set(12, 0);
        c.set(13, 0);
        c.set(14, 0);
        Iterator<TEpgItem> it = data.iterator();
        while (it.hasNext()) {
            if (c.getTimeInMillis() < it.next().timeStart().getTime()) {
                return true;
            }
        }
        return false;
    }
}
