package com.orion.iptv.bean;

public class EpgProgram {
    public final int start;
    public final int end;
    public final String program;

    public EpgProgram(int start, int end, String program) {
        this.start = start;
        this.end = end;
        this.program = program;
    }
}
