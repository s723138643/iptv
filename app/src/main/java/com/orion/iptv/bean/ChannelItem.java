package com.orion.iptv.bean;

import androidx.annotation.NonNull;

import com.orion.iptv.recycleradapter.ListItemWithNumber;
import com.orion.iptv.recycleradapter.ListItemWithStableId;

import java.util.ArrayList;
import java.util.List;

public class ChannelItem implements ListItemWithNumber, ListItemWithStableId {
    public final ChannelInfo info;
    public List<String> links;

    public ChannelItem(int channelNumber, String channelName, GroupInfo groupInfo) {
        info = new ChannelInfo(channelNumber, channelName, groupInfo);
        links = new ArrayList<>();
    }

    @Override
    public String number() {
        return String.valueOf(info.channelNumber);
    }

    @Override
    public String content() {
        return info.channelName;
    }

    @Override
    public long getId() {
        return info.channelNumber;
    }

    public void append(String link) {
        if (!links.contains(link)) {
            links.add(link);
        }
    }

    public boolean contains(String link) {
        return links.contains(link);
    }

    public int indexOf(String link) {
        return links.indexOf(link);
    }

    @NonNull
    public List<String> getSources() {
        return links;
    }
}
