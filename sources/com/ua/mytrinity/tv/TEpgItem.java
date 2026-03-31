package com.ua.mytrinity.tv;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TEpgItem {
    private static SimpleDateFormat m_caption_format = new SimpleDateFormat("HH:mm");
    private int m_channel_id;
    private Date m_start;
    private Date m_stop;
    private String m_title;

    public TEpgItem(int channelId, String title, Date start, Date stop) {
        this.m_channel_id = channelId;
        this.m_title = title;
        this.m_start = start;
        this.m_stop = stop;
    }

    public int channelId() {
        return this.m_channel_id;
    }

    public String title() {
        return this.m_title;
    }

    public Date timeStart() {
        return this.m_start;
    }

    public Date timeStop() {
        return this.m_stop;
    }

    public boolean isInPast() {
        return this.m_stop.before(new Date());
    }

    public boolean isInFuture() {
        return this.m_start.after(new Date());
    }

    public boolean isNow(TTimeOffset offset) {
        Date now;
        if (offset == null) {
            now = new Date();
        } else {
            now = new Date(System.currentTimeMillis() - ((long) (offset.offsetSec() * 1000)));
        }
        return this.m_start.before(now) && this.m_stop.after(now);
    }

    public boolean isNow() {
        return isNow((TTimeOffset) null);
    }

    public long secsToStart() {
        return (this.m_start.getTime() - new Date().getTime()) / 1000;
    }

    public String captionStart() {
        return m_caption_format.format(this.m_start);
    }

    public String captionStop() {
        return m_caption_format.format(this.m_stop);
    }

    public float progress(TTimeOffset offset) {
        Date now;
        if (offset != null) {
            now = new Date(System.currentTimeMillis() - ((long) (offset.offsetSec() * 1000)));
        } else {
            now = new Date();
        }
        return ((float) (now.getTime() - this.m_start.getTime())) / ((float) (this.m_stop.getTime() - this.m_start.getTime()));
    }

    public float progress() {
        return progress((TTimeOffset) null);
    }
}
