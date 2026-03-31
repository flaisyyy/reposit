package com.ua.mytrinity.media;

import java.util.ArrayList;

public class Genre implements Comparable<Genre> {
    public static final int ID_GENRE_NEW = 31;
    private int m_id;
    private ArrayList<Movie> m_movies = new ArrayList<>();
    private int m_order = 0;
    private String m_title;

    public Genre(int id) {
        this.m_id = id;
    }

    public int id() {
        return this.m_id;
    }

    public String title() {
        return this.m_title;
    }

    public int order() {
        return this.m_order;
    }

    public void setOrder(int order) {
        this.m_order = order;
    }

    public void setTitle(String title) {
        this.m_title = title;
    }

    public ArrayList<Movie> movies() {
        return this.m_movies;
    }

    public int compareTo(Genre another) {
        if (this.m_order == another.order()) {
            return this.m_title.compareTo(another.title());
        }
        if (this.m_order < another.order()) {
            return -1;
        }
        return 1;
    }
}
