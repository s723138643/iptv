package com.orion.iptv.bean;

import com.orion.iptv.recycleradapter.ListItem;
import com.orion.iptv.recycleradapter.ListItemWithStableId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        return (index < channels.size() && index >= 0) ? Optional.of(channels.get(index)) : Optional.empty();
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

    public Optional<Integer> indexOf(int channelNumber) {
        for (int i = 0; i < channels.size(); i++) {
            ChannelItem ch = channels.get(i);
            if (channelNumber == ch.info.channelNumber) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public Optional<ChannelItem> getChannel(int channelPos) {
        return getByIndex(channelPos);
    }

    public Optional<List<String>> getSources(int channelPos) {
        return getByIndex(channelPos).map(ChannelItem::getSources);
    }
}
