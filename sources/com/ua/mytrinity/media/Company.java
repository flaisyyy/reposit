package com.ua.mytrinity.media;

public class Company {
    private int m_id;
    private String m_title;

    public Company(int id) {
        this.m_id = id;
    }

    public int id() {
        return this.m_id;
    }

    public String title() {
        return this.m_title;
    }

    public void setTitle(String title) {
        this.m_title = title;
    }
}
