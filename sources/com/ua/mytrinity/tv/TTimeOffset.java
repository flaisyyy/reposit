package com.ua.mytrinity.tv;

public class TTimeOffset implements Comparable<TTimeOffset> {
    private int m_id;
    private int m_offset;

    public TTimeOffset(int id, int offset) {
        this.m_id = id;
        this.m_offset = offset;
    }

    public int id() {
        return this.m_id;
    }

    public int offsetSec() {
        return this.m_offset;
    }

    public int compareTo(TTimeOffset another) {
        int another_offset = another.offsetSec();
        if (this.m_offset < another_offset) {
            return -1;
        }
        if (this.m_offset > another_offset) {
            return 1;
        }
        return 0;
    }
}
