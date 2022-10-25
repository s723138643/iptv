package com.orion.iptv.bean;

import com.google.android.exoplayer2.MediaItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChannelGroup {
    public final String name;
    public final List<ChannelItem> channels;
    private final ChannelNumGenerator generator;

    public ChannelGroup(String name, ChannelNumGenerator generator) {
        this.name = name;
        this.generator = generator;
        channels = new ArrayList<>();
    }

    public void appendChannel(String name, String link) {
        ChannelItem ch = getOrCreateChannel(name);
        ch.append(link);
    }

    private ChannelItem getOrCreateChannel(String channel) {
        return get(channel).orElseGet(()->{
            ChannelItem ch = new ChannelItem(generator.next(), channel, name);
            channels.add(ch);
            return ch;
        });
    }

    private Optional<ChannelItem> get(String channel) {
        for (int i=0; i<channels.size(); i++) {
            ChannelItem ch = channels.get(i);
            if (channel.equals(ch.name)) {
                return Optional.of(ch);
            }
        }
        return Optional.empty();
    }

    private Optional<ChannelItem> get(int index) {
        return index >= channels.size() || index < 0 ? Optional.empty() : Optional.of(channels.get(index));
    }

    public boolean contains(String channel) {
        return indexOf(channel) >= 0;
    }

    public int indexOf(String channel) {
        for (int i=0; i<channels.size(); i++) {
            ChannelItem ch = channels.get(i);
            if (channel.equals(ch.name)) {
                return i;
            }
        }
        return -1;
    }

    public Optional<List<MediaItem>> toMediaItems(int channel) {
        return get(channel).map(ChannelItem::toMediaItems);
    }

    public Optional<ChannelItem.PreferredMediaItems> toMediaItems(int channel, String preferredLink) {
        return get(channel).map((ch)-> ch.toMediaItems(preferredLink));
    }
}
