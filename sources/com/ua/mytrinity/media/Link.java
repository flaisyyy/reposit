package com.ua.mytrinity.media;

public class Link {
    private int m_id;
    private String m_title;
    private String m_url;

    public Link(int id, String title, String url) {
        this.m_id = id;
        this.m_title = title;
        this.m_url = url;
    }

    public int id() {
        return this.m_id;
    }

    public String title() {
        return this.m_title;
    }

    public String url() {
        return this.m_url;
    }
}
