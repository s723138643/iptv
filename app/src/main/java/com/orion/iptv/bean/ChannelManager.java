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
    public List<ChannelGroup> groups;
    private final ChannelNumGenerator generator = new ChannelNumGenerator(0);

    public ChannelManager(String defaultGroupName) {
        this.defaultGroupName = defaultGroupName;
        groups = new ArrayList<>();
    }

    public boolean hasGroup() {
        return groups.size() > 1;
    }

    private ChannelGroup getOrCreate(String group) {
        String realGroup = group.equals("") ? defaultGroupName : group;
        return get(realGroup).orElseGet(()->{
            ChannelGroup g = new ChannelGroup(realGroup, generator);
            groups.add(g);
            return g;
        });
    }

    private Optional<ChannelGroup> get(String group) {
        for (int i=0; i<groups.size(); i++) {
            ChannelGroup g = groups.get(i);
            if (group.equals(g.name)) {
                return Optional.of(g);
            }
        }
        return Optional.empty();
    }

    private Optional<ChannelGroup> get(int index) {
        return index >= groups.size() || index < 0 ? Optional.empty() : Optional.of(groups.get(index));
    }

    public void appendChannel(String group, String channel, String link) {
        ChannelGroup g = getOrCreate(group);
        g.appendChannel(channel, link);
    }

    public Optional<List<MediaItem>> toMediaItems(int group, int channel) {
        return get(group).flatMap((g)-> g.toMediaItems(channel));
    }

    public Optional<ChannelItem.PreferredMediaItems> toMediaItems(int group, int channel, String preferredLink) {
        return get(group).flatMap((g)-> g.toMediaItems(channel, preferredLink));
    }

    public Optional<List<ChannelItem>> getChannels(int groupIndex) {
        if (groupIndex>=groups.size() || groupIndex<0) {
            return Optional.empty();
        }
        return Optional.of(groups.get(groupIndex).channels);
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
}
