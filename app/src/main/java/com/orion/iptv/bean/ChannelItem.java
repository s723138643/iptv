package com.orion.iptv.bean;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class ChannelItem {
    public final String group;
    public final String name;
    public final int number;
    public List<String> links;

    public ChannelItem(int number, String name, String group) {
        this.number = number;
        this.name = name;
        this.group = group;
        links = new ArrayList<>();
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

    public static class Tag {
        public final String channelGroup;
        public final String channelName;
        public final int channelNumber;

        public Tag(int number, String name, String group) {
            channelName = name;
            channelGroup= group;
            channelNumber = number;
        }
    }

    @NonNull
    public List<MediaItem> toMediaItems() {
        ArrayList<MediaItem> items = new ArrayList<>();
        for (int i=0; i<links.size(); i++) {
            String link = links.get(i);
            if (link.isEmpty()) {
                continue;
            }
            MediaItem.Builder builder = new MediaItem.Builder();
            builder.setUri(Uri.parse(link));
            builder.setTag(new Tag(number, name, group));
            items.add(builder.build());
        }
        return items;
    }

    public static class PreferredMediaItems {
        public final int preferredPosition;
        public final List<MediaItem> mediaItems;

        public PreferredMediaItems(int preferredPosition, List<MediaItem> mediaItems) {
            this.preferredPosition = preferredPosition;
            this.mediaItems = mediaItems;
        }
    }

    @NonNull
    public PreferredMediaItems toMediaItems(String preferredLink) {
        int position = 0;
        List<MediaItem> mediaItems = new ArrayList<>(links.size());
        for (int i=0; i<links.size(); i++) {
            String link = links.get(i);
            if (link.isEmpty()) {
                continue;
            }
            if (link.equals(preferredLink)) {
                position = i;
            }
            MediaItem.Builder builder = new MediaItem.Builder();
            builder.setUri(Uri.parse(link));
            builder.setTag(new Tag(number, name, group));
            mediaItems.add(builder.build());
        }
        return new PreferredMediaItems(position, mediaItems);
    }
}
