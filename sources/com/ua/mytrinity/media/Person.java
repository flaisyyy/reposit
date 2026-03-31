package com.ua.mytrinity.media;

public class Person {
    private int m_id;
    private String m_title;

    public Person(int id) {
        this.m_id = id;
    }

    public Person(int id, String title) {
        this.m_id = id;
        this.m_title = title;
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
