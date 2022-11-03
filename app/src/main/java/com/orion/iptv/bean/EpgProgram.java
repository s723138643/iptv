package com.orion.iptv.bean;

import com.orion.iptv.recycleradapter.ListItem;

import java.util.Date;

public class EpgProgram implements ListItem {
    public final long start;
    public final long end;
    public final String startTime;
    public final String endTime;
    public final String program;

    public EpgProgram(long start, long end, String startTime, String endTime, String program) {
        this.start = start;
        this.end = end;
        this.startTime = startTime;
        this.endTime = endTime;
        this.program = program;
    }

    public static int indexOfCurrentProgram(EpgProgram[] epgPrograms, Date date) {
        long time = date.getTime();
        for (int i=0; i<epgPrograms.length; i++) {
            EpgProgram item = epgPrograms[i];
            if (item.start > time) {
                return i-1;
            }
        }
        int last = epgPrograms.length - 1;
        return epgPrograms[last].end >= time ? last : -1;
    }

    @Override
    public String index() {
        return "";
    }

    @Override
    public String name() {
        return startTime + "-" + endTime + "  " + program;
    }
}
