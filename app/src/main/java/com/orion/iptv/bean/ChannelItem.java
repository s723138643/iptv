package com.orion.iptv.bean;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.MediaItem;
import com.orion.iptv.recycleradapter.ListItem;
import com.orion.iptv.recycleradapter.ListItemWithNumber;

import java.util.ArrayList;
import java.util.List;

public class ChannelItem implements ListItemWithNumber {
    public final ChannelInfo info;
    public List<String> links;

    public ChannelItem(int channelNumber, String channelName, GroupInfo groupInfo) {
        info = new ChannelInfo(channelNumber, channelName, groupInfo);
        links = new ArrayList<>();
    }

    public String number() {
        return String.valueOf(info.channelNumber);
    }

    public String content() {
        return info.channelName;
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
    public List<MediaItem> toMediaItems() {
        ArrayList<MediaItem> items = new ArrayList<>();
        for (int i = 0; i < links.size(); i++) {
            String link = links.get(i);
            if (link.isEmpty()) {
                continue;
            }
            MediaItem.Builder builder = new MediaItem.Builder();
            builder.setUri(Uri.parse(link));
            builder.setTag(info);
            items.add(builder.build());
        }
        return items;
    }

    @NonNull
    public PreferredMediaItems toMediaItems(String preferredLink) {
        int position = 0;
        List<MediaItem> mediaItems = new ArrayList<>(links.size());
        for (int i = 0; i < links.size(); i++) {
            String link = links.get(i);
            if (link.isEmpty()) {
                continue;
            }
            if (link.equals(preferredLink)) {
                position = i;
            }
            MediaItem.Builder builder = new MediaItem.Builder();
            builder.setUri(Uri.parse(link));
            builder.setTag(info);
            mediaItems.add(builder.build());
        }
        return new PreferredMediaItems(position, mediaItems);
    }

    public static class PreferredMediaItems {
        public final int preferredPosition;
        public final List<MediaItem> mediaItems;

        public PreferredMediaItems(int preferredPosition, List<MediaItem> mediaItems) {
            this.preferredPosition = preferredPosition;
            this.mediaItems = mediaItems;
        }
    }
}
