package com.orion.iptv.bean;

import android.util.Log;

import com.google.android.exoplayer2.MediaItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChannelManager {
    public static final String TAG = "ChannelManager";
    private final String defaultGroupName;
    private final NumberGenerator groupNumGenerator = new NumberGenerator(0);
    private final NumberGenerator channelNumGenerator = new NumberGenerator(0);
    public List<ChannelGroup> groups;

    public ChannelManager(String defaultGroupName) {
        this.defaultGroupName = defaultGroupName;
        groups = new ArrayList<>();
    }

    public static ChannelManager from(String defaultGroupName, String channels) {
        ChannelManager m = new ChannelManager(defaultGroupName);
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
        return index >= groups.size() || index < 0 ? Optional.empty() : Optional.of(groups.get(index));
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

    public Optional<ChannelGroup> getChannelGroup(int groupNumber) {
        return getByNumber(groupNumber);
    }

    public Optional<ChannelItem> getFirst() {
        return getByIndex(0).flatMap((group) -> group.getByIndex(0));
    }

    public Optional<ChannelItem> getChannel(int groupNumber, int channelNumber) {
        return getChannelGroup(groupNumber).flatMap((group) -> group.getChannel(channelNumber));
    }

    public void appendChannel(String group, String channel, String link) {
        ChannelGroup g = getOrCreate(group);
        g.appendChannel(channel, link);
    }

    public Optional<List<MediaItem>> toMediaItems(int groupIndex, int channelIndex) {
        return getByIndex(groupIndex).flatMap((g) -> g.toMediaItems(channelIndex));
    }

    public Optional<ChannelItem.PreferredMediaItems> toMediaItems(int groupIndex, int channelIndex, String preferredLink) {
        return getByIndex(groupIndex).flatMap((g) -> g.toMediaItems(channelIndex, preferredLink));
    }

    public Optional<List<ChannelItem>> getChannels(int groupIndex) {
        return getByIndex(groupIndex).map((item) -> item.channels);
    }
}
