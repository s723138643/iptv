package com.orion.iptv.layout.livechannellist;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.MediaItem;
import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import android.util.Log;

public class LiveChannelListLayout {
    private final String TAG = "LiveChannelListLayout";
    private final View layout;
    private final RecyclerView groupList;
    private final View groupListSpacer;
    private final GroupListViewAdapter groupListViewAdapter;
    private final SelectionTracker<Long> groupListTracker;
    private final RecyclerView channelList;
    private final ChannelListViewAdapter channelListViewAdapter;
    private final SelectionTracker<Long> channelListTracker;
    private final View epgSpacer;
    private final RecyclerView epgList;
    private final SelectionTracker<Long> epgListTracker;

    private ChannelManager channelManager;
    private final Handler handler;
    private setVisibleDelayed flying;
    private OnChannelSelectedListener channelSelectedListener;

    private int selectedGroup = 0;
    private int selectedChannel = 0;

    public boolean getIsVisible() {
        return layout.getVisibility() == View.VISIBLE;
    }

    private class setVisibleDelayed implements Runnable {
        private final boolean isVisible;
        private boolean canceled = false;

        public setVisibleDelayed(boolean isVisible) {
            this.isVisible = isVisible;
        }

        @Override
        public void run() {
            if (!canceled) {
                setVisible(isVisible);
            }
        }

        public void cancel() {
            canceled = true;
        }
    }

    public interface OnChannelSelectedListener {
        void onChannelSelected(int groupIndex, int channelIndex);
    }

    public LiveChannelListLayout(View layout, ChannelManager channelManager) {
        handler = new Handler(Looper.getMainLooper());
        this.channelManager = channelManager;
        this.layout = layout;

        ToggleButton showEpg = layout.findViewById(R.id.showEpgButton);
        showEpg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    epgSpacer.setVisibility(View.VISIBLE);
                    epgList.setVisibility(View.VISIBLE);
                } else {
                    epgSpacer.setVisibility(View.GONE);
                    epgList.setVisibility(View.GONE);
                }
            }
        });
        this.epgSpacer = layout.findViewById(R.id.channelSpacer3);
        this.epgList = layout.findViewById(R.id.channelEpgList);
        EpgListViewAdapter epglistViewAdapter = new EpgListViewAdapter(layout.getContext(), new ArrayList<>());
        this.epgList.setAdapter(epglistViewAdapter);
        epgListTracker = new SelectionTracker.Builder<>(
                "epg-list-view",
                epgList,
                epglistViewAdapter.new KeyProvider(ItemKeyProvider.SCOPE_MAPPED),
                epglistViewAdapter.new ItemLookup(epgList),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectSingleAnything()).build();
        epglistViewAdapter.setTracker(epgListTracker);

        channelList = layout.findViewById(R.id.channelList);
        channelListViewAdapter = new ChannelListViewAdapter(layout.getContext(), channelManager.getChannels(selectedChannel).orElse(new ArrayList<>()));
        channelList.setAdapter(channelListViewAdapter);
        channelListTracker = new SelectionTracker.Builder<>(
                "channel-list-view",
                channelList,
                channelListViewAdapter.new KeyProvider(ItemKeyProvider.SCOPE_MAPPED),
                channelListViewAdapter.new ItemLookup(channelList),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectSingleAnything()).build();
        channelListViewAdapter.setTracker(channelListTracker);
        channelListViewAdapter.setOnSelectedListener(position -> {
            selectedChannel = position;
            if (channelSelectedListener != null) {
                channelSelectedListener.onChannelSelected(selectedGroup, selectedChannel);
            }
        });

        groupList = layout.findViewById(R.id.channelGroup);
        groupListSpacer = layout.findViewById(R.id.channelSpacer1);
        if (channelManager.hasGroup()) {
            groupList.setVisibility(View.VISIBLE);
            groupListSpacer.setVisibility(View.VISIBLE);
        }
        groupListViewAdapter = new GroupListViewAdapter(layout.getContext(), channelManager.groups);
        groupList.setAdapter(groupListViewAdapter);
        groupListTracker = new SelectionTracker.Builder<>(
                "group-list-view",
                groupList,
                groupListViewAdapter.new KeyProvider(ItemKeyProvider.SCOPE_MAPPED),
                groupListViewAdapter.new ItemLookup(groupList),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectSingleAnything()).build();
        groupListViewAdapter.setTracker(groupListTracker);
        groupListViewAdapter.setOnSelectedListener(position -> {
            selectedGroup = position;
            List<ChannelItem> channels = this.channelManager.getChannels(position).orElse(new ArrayList<>());
            Log.i(TAG, String.format("select group: %d, channels: %d", position, channels.size()));
            channelListViewAdapter.setData(channels);
        });
    }

    public void setVisibleDelayed(boolean isVisible, int delayMillis) {
        if (flying != null) {
            flying.cancel();
        }
        if (delayMillis <= 0) {
           setVisible(isVisible);
        } else {
            flying = new setVisibleDelayed(isVisible);
            handler.postDelayed(flying, delayMillis);
        }
    }

    private void setVisible(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (visibility != layout.getVisibility()) {
            layout.setVisibility(visibility);
        }
    }

    public void setOnChannelSelectedListener(OnChannelSelectedListener listener) {
        channelSelectedListener = listener;
    }

    public Optional<List<MediaItem>> getCurrentChannelSources() {
        return channelManager.toMediaItems(selectedGroup, selectedChannel);
    }

    public void onSaveInstanceState(@NonNull Bundle outSate) {
        groupListTracker.onSaveInstanceState(outSate);
        channelListTracker.onSaveInstanceState(outSate);
        epgListTracker.onSaveInstanceState(outSate);
    }

    public void onRestoreInstanceState(@NonNull Bundle outSate) {
        groupListTracker.onRestoreInstanceState(outSate);
        channelListTracker.onRestoreInstanceState(outSate);
        epgListTracker.onRestoreInstanceState(outSate);
    }

    public void setData(ChannelManager m) {
        channelManager = m;
        selectedGroup = 0;
        selectedChannel = 0;
        groupListViewAdapter.setData(m.groups);
        int visibility = m.hasGroup() ? View.VISIBLE : View.GONE;
        groupList.setVisibility(visibility);
        groupListSpacer.setVisibility(visibility);
        List<ChannelItem> channels = m.getChannels(selectedGroup).orElse(new ArrayList<>());
        channelListViewAdapter.setData(channels);
    }
}
