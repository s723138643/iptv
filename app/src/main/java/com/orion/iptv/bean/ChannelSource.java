package com.orion.iptv.bean;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                    Log.i(TAG, "invalid channel line, " + line);
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
        m.groups = m.groups.stream()
                .filter(channelGroup -> channelGroup.channels.size() > 0)
                .collect(Collectors.toList());
        return m;
    }

    public boolean shouldShowGroup() {
        return groups.size() > 1;
    }

    private ChannelGroup getOrCreate(String group) {
        String realGroup = group.equals("") ? defaultGroupName : group;
        return getByName(realGroup).orElseGet(() -> {
            ChannelGroup g = new ChannelGroup(groupNumGenerator.next(), realGroup, channelNumGenerator);
            groups.add(g);
            return g;
        });
    }

    private Optional<ChannelGroup> getByName(String group) {
        return groups.stream().filter((g) -> group.equals(g.info.groupName)).findFirst();
    }

    private Optional<ChannelGroup> getByNumber(int group) {
        return groups.stream().filter((g) -> group == g.info.groupNumber).findFirst();
    }

    private Optional<ChannelGroup> getByIndex(int index) {
        return (index < groups.size() && index >= 0) ? Optional.of(groups.get(index)) : Optional.empty();
    }

    public Optional<Integer> indexOf(int group) {
        for (int i = 0; i < groups.size(); i++) {
            ChannelGroup g = groups.get(i);
            if (group == g.info.groupNumber) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public Optional<Integer> indexOf(String group) {
        for (int i = 0; i < groups.size(); i++) {
            ChannelGroup g = groups.get(i);
            if (group.equals(g.info.groupName)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public Optional<Integer> indexOfChannel(int groupNumber, int channelNumber) {
        return getByNumber(groupNumber).flatMap((g) -> g.indexOf(channelNumber));
    }

    public Optional<ChannelGroup> getChannelGroup(int position) {
        return getByIndex(position);
    }

    public Optional<ChannelItem> getFirst() {
        return getChannel(0, 0);
    }

    public Optional<ChannelItem> getChannel(int groupPos, int channelPos) {
        return getByIndex(groupPos).flatMap(group -> group.getChannel(channelPos));
    }

    public void appendChannel(String group, String channel, String link) {
        ChannelGroup g = getOrCreate(group);
        g.appendChannel(channel, link);
    }

    public Optional<List<String>> getSources(int groupPos, int channelPos) {
        return getChannel(groupPos, channelPos).map(ChannelItem::getSources);
    }

    public Optional<List<ChannelItem>> getChannels(int groupPos) {
        return getByIndex(groupPos).map((item) -> item.channels);
    }
}
