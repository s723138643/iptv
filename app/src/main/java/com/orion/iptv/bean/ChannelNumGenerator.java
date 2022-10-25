package com.orion.iptv.bean;

public class ChannelNumGenerator {
    private int n = 0;

    ChannelNumGenerator(int start) {
        n = start;
    }

    public int next() {
        n += 1;
        return n;
    }
}
