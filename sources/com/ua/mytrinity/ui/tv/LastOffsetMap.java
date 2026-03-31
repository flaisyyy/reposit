package com.ua.mytrinity.ui.tv;

import com.ua.mytrinity.tv.TChannel;
import com.ua.mytrinity.tv.TTimeOffset;
import java.util.Date;
import java.util.HashMap;

public class LastOffsetMap {
    public static long ExpireTime = 1800000;
    private HashMap<TChannel, TTimeOffset> m_last_offset = new HashMap<>();
    private HashMap<TChannel, Date> m_last_time = new HashMap<>();

    public void put(TChannel channel, TTimeOffset offset) {
        this.m_last_offset.put(channel, offset);
        this.m_last_time.put(channel, new Date());
    }

    public TTimeOffset get(TChannel channel) {
        if (!this.m_last_offset.containsKey(channel) || new Date().getTime() - this.m_last_time.get(channel).getTime() >= ExpireTime) {
            return null;
        }
        return this.m_last_offset.get(channel);
    }
}
