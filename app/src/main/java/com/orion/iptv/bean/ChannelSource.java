package com.orion.iptv.bean;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class ChannelSource {
    public static final String TAG = "ChannelSource";
    private final String defaultGroupName;
    private final NumberGenerator groupNumGenerator = new NumberGenerator(0);
    private final NumberGenerator channelNumGenerator = new NumberGenerator(0);
    public List<ChannelGroup> groups;

    public ChannelSource(String defaultGroupName) {
        this.defaultGroupName = defaultGroupName;
        groups = new ArrayList<>();
    }

    public static ChannelSource from(String defaultGroupName, String channels) {
        ChannelSource m = new ChannelSource(defaultGroupName);
        BufferedReader reader = new BufferedReader(new StringReader(channels));
        String group = defaultGroupName;
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] parts = line.split(",", 2);
                if (parts.length != 2) {
                    if (!line.trim().isEmpty()) {
                        Log.i(TAG, "invalid channel line, " + line);
                    }
                    continue;
                }
                String value = parts[1].trim();
                if (value.equals("#genre#")) {
                    group = parts[0].trim();
                } else {
                    String[] links = value.split("#");
                    for (String link : links) {
                        String trimmed = link.trim();
                        if (!trimmed.isEmpty()) {
                            m.appendChannel(group, parts[0].trim(), trimmed);
                        }
                    }
                }
            }
        } catch (IOException exc) {
            Log.e(TAG, "parse channels failed, " + exc);
        }
        List<ChannelGroup> groups = new ArrayList<>();
        for (ChannelGroup g : m.groups) {
            if (g.channels.size() > 0) {
                groups.add(g);
            }
        }
        m.groups = groups;
        return m;
    }

    @NonNull
    private ChannelGroup getOrCreate(String group) {
        String realGroup = group.equals("") ? defaultGroupName : group;
        ChannelGroup channelGroup = getByName(realGroup);
        if (channelGroup != null) {
            return channelGroup;
        }
        channelGroup = new ChannelGroup(groupNumGenerator.next(), realGroup, channelNumGenerator);
        groups.add(channelGroup);
        return channelGroup;
    }

    @Nullable
    private ChannelGroup getByName(String group) {
        for (ChannelGroup g : groups) {
            if (group.equals(g.info.groupName)) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    private ChannelGroup getByNumber(int group) {
        for (ChannelGroup g : groups) {
            if (group == g.info.groupNumber) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    private ChannelGroup getByIndex(int index) {
        return (index < groups.size() && index >= 0) ? groups.get(index) : null;
    }

    public int indexOf(int group) {
        for (int i = 0; i < groups.size(); i++) {
            ChannelGroup g = groups.get(i);
            if (group == g.info.groupNumber) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(String group) {
        for (int i = 0; i < groups.size(); i++) {
            ChannelGroup g = groups.get(i);
            if (group.equals(g.info.groupName)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfChannel(int groupNumber, int channelNumber) {
        ChannelGroup group = getByNumber(groupNumber);
        if (group != null) {
            return group.indexOf(channelNumber);
        }
        return -1;
    }

    @Nullable
    public ChannelGroup getChannelGroup(int position) {
        return getByIndex(position);
    }

    @Nullable
    public ChannelItem getFirst() {
        return getChannel(0, 0);
    }

    @Nullable
    public ChannelItem getChannel(int groupPos, int channelPos) {
        ChannelGroup group = getByIndex(groupPos);
        if (group != null) {
            return group.getChannel(channelPos);
        }
        return null;
    }

    public void appendChannel(String group, String channel, String link) {
        ChannelGroup g = getOrCreate(group);
        g.appendChannel(channel, link);
    }

    @Nullable
    public List<String> getSources(int groupPos, int channelPos) {
        ChannelItem channel = getChannel(groupPos, channelPos);
        if (channel != null) {
            return channel.getSources();
        }
        return null;
    }

    @Nullable
    public List<ChannelItem> getChannels(int groupPos) {
        ChannelGroup group = getByIndex(groupPos);
        if (group != null) {
            return group.channels;
        }
        return null;
    }
}
