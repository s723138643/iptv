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
        assert epgPrograms != null && epgPrograms.length > 0;
        return binarySearch(epgPrograms, date);
    }

    private static int search(EpgProgram[] epgPrograms, Date date) {
        long time = date.getTime();
        for (int i=0; i<epgPrograms.length; i++) {
            EpgProgram item = epgPrograms[i];
            if (item.start > time) {
                return i-1;
            }
        }
        return epgPrograms.length - 1;
    }

    private static int binarySearch(EpgProgram[] epgPrograms, Date date) {
        long time = date.getTime();
        int low = 0;
        int high = epgPrograms.length;
        int mid = low + (high - low) / 2;
        while (low < high) {
            if (time >= epgPrograms[mid].start) {
                low = mid + 1;
            } else {
                high = mid;
            }
            mid = low + (high - low) / 2;
        }
        return mid - 1;
    }

    @Override
    public String index() {
        return "";
    }

    @Override
    public String name() {
        return String.format("%s-%s %s", startTime, endTime, program);
    }
}
