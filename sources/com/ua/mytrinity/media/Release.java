package com.ua.mytrinity.media;

import java.util.ArrayList;
import java.util.Date;

public class Release {
    private String m_audio;
    private int m_id;
    private ArrayList<Link> m_links = new ArrayList<>();
    private Date m_updated;
    private String m_video;

    public int id() {
        return this.m_id;
    }

    public void setId(int id) {
        this.m_id = id;
    }

    public String video() {
        return this.m_video;
    }

    public void setVideo(String video) {
        this.m_video = video;
    }

    public String audio() {
        return this.m_audio;
    }

    public void setAudio(String audio) {
        this.m_audio = audio;
    }

    public void addLink(Link link) {
        this.m_links.add(link);
    }

    public ArrayList<Link> links() {
        return this.m_links;
    }

    public Date updated() {
        return this.m_updated;
    }

    public void setUpdated(Date updated) {
        this.m_updated = updated;
    }
}
