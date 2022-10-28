package com.orion.iptv.bean;

public class NumberGenerator {
    private int n = 0;

    NumberGenerator(int start) {
        n = start;
    }

    public int next() {
        n += 1;
        return n;
    }
}
