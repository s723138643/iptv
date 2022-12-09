package com.orion.iptv.bean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orion.iptv.recycleradapter.ListItem;
import com.orion.iptv.recycleradapter.ListItemWithStableId;

import java.util.ArrayList;
import java.util.List;

public class ChannelGroup implements ListItem, ListItemWithStableId {
    public final GroupInfo info;
    public final List<ChannelItem> channels;
    private final NumberGenerator generator;

    public ChannelGroup(int groupNumber, String groupName, NumberGenerator generator) {
        this.info = new GroupInfo(groupNumber, groupName);
        this.generator = generator;
        channels = new ArrayList<>();
    }

    @Override
    public String content() {
        return info.groupName;
    }

    @Override
    public long getId() {
        return info.groupNumber;
    }

    public void appendChannel(String name, String link) {
        ChannelItem ch = getOrCreateChannel(name);
        ch.append(link);
    }

    @NonNull
    private ChannelItem getOrCreateChannel(String channel) {
        ChannelItem channelItem = getByName(channel);
        if (channelItem != null) {
            return channelItem;
        }
        ChannelItem ch = new ChannelItem(generator.next(), channel, info);
        channels.add(ch);
        return ch;
    }

    @Nullable
    private ChannelItem getByName(String channel) {
        for (ChannelItem ch : channels) {
            if (channel.equals(ch.info.channelName)) {
                return ch;
            }
        }
        return null;
    }

    @Nullable
    private ChannelItem getByNumber(int channel) {
        for (ChannelItem ch : channels) {
            if (channel == ch.info.channelNumber) {
                return ch;
            }
        }
        return null;
    }

    @Nullable
    protected ChannelItem getByIndex(int index) {
        return (index < channels.size() && index >= 0) ? channels.get(index) : null;
    }

    public boolean contains(String channel) {
        return indexOf(channel) >= 0;
    }

    public boolean contains(int channel) {
        return indexOf(channel) >= 0;
    }

    public int indexOf(String channel) {
        for (int i = 0; i < channels.size(); i++) {
            ChannelItem ch = channels.get(i);
            if (channel.equals(ch.info.channelName)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(int channelNumber) {
        for (int i = 0; i < channels.size(); i++) {
            ChannelItem ch = channels.get(i);
            if (channelNumber == ch.info.channelNumber) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public ChannelItem getChannel(int channelPos) {
        return getByIndex(channelPos);
    }

    @Nullable
    public List<String> getSources(int channelPos) {
        ChannelItem channel = getByIndex(channelPos);
        if (channel != null) {
            return channel.getSources();
        }
        return null;
    }
}
