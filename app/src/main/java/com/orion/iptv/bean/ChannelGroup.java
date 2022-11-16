package com.orion.iptv.bean;

import com.orion.iptv.recycleradapter.ListItem;
import com.orion.player.ExtDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChannelGroup implements ListItem {
    public final GroupInfo info;
    public final List<ChannelItem> channels;
    private final NumberGenerator generator;

    public ChannelGroup(int groupNumber, String groupName, NumberGenerator generator) {
        this.info = new GroupInfo(groupNumber, groupName);
        this.generator = generator;
        channels = new ArrayList<>();
    }

    public String content() {
        return info.groupName;
    }

    public void appendChannel(String name, String link) {
        ChannelItem ch = getOrCreateChannel(name);
        ch.append(link);
    }

    private ChannelItem getOrCreateChannel(String channel) {
        return getByName(channel).orElseGet(() -> {
            ChannelItem ch = new ChannelItem(generator.next(), channel, info);
            channels.add(ch);
            return ch;
        });
    }

    private Optional<ChannelItem> getByName(String channel) {
        return channels.stream().filter((ch) -> channel.equals(ch.info.channelName)).findFirst();
    }

    private Optional<ChannelItem> getByNumber(int channel) {
        return channels.stream().filter((ch) -> channel == ch.info.channelNumber).findFirst();
    }

    protected Optional<ChannelItem> getByIndex(int index) {
        return index >= channels.size() || index < 0 ? Optional.empty() : Optional.of(channels.get(index));
    }

    public boolean contains(String channel) {
        return indexOf(channel).isPresent();
    }

    public boolean contains(int channel) {
        return indexOf(channel).isPresent();
    }

    public Optional<Integer> indexOf(String channel) {
        for (int i = 0; i < channels.size(); i++) {
            ChannelItem ch = channels.get(i);
            if (channel.equals(ch.info.channelName)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public Optional<Integer> indexOf(int channel) {
        for (int i = 0; i < channels.size(); i++) {
            ChannelItem ch = channels.get(i);
            if (channel == ch.info.channelNumber) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public Optional<ChannelItem> getChannel(int channelNumber) {
        return getByNumber(channelNumber);
    }

    public Optional<List<ExtDataSource>> toMediaItems(int channelIndex) {
        return getByIndex(channelIndex).map(ChannelItem::toMediaItems);
    }
}
