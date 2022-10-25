package com.orion.iptv.layout.livechannellist;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.MediaItem;
import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelGroup;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelManager;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.misc.CancelableRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import android.util.Log;

public class LiveChannelListLayout {
    private final String TAG = "LiveChannelListLayout";
    private final View mLayout;
    private final RecyclerView groupList;
    private final View groupListSpacer;
    private final GroupListViewAdapter groupListViewAdapter;
    private final SelectionTracker<Long> groupListTracker;
    private final ChannelListViewAdapter channelListViewAdapter;
    private final SelectionTracker<Long> channelListTracker;
    private final View epgSpacer;
    private final RecyclerView epgList;
    private final EpgListViewAdapter epgListViewAdapter;
    private final SelectionTracker<Long> epgListTracker;

    private ChannelManager channelManager;
    private final Handler mHandler;
    private CancelableRunnable setVisibilityDelayedTask;
    private OnChannelSelectedListener channelSelectedListener;

    private int selectedGroup = 0;
    private int selectedChannel = 0;

    public boolean getIsVisible() {
        return mLayout.getVisibility() == View.VISIBLE;
    }

    public interface OnChannelSelectedListener {
        void onChannelSelected(int groupIndex, int channelIndex);
    }

    public LiveChannelListLayout(AppCompatActivity activity, ChannelManager channelManager) {
        this.mLayout = activity.findViewById(R.id.channelListLayout);
        this.channelManager = channelManager;
        this.mHandler = new Handler(activity.getMainLooper());

        ToggleButton showEpg = mLayout.findViewById(R.id.showEpgButton);
        showEpg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mHandler.post(() -> {
                        epgSpacer.setVisibility(View.VISIBLE);
                        epgList.setVisibility(View.VISIBLE);
                    });
                } else {
                    mHandler.post(() -> {
                        epgSpacer.setVisibility(View.GONE);
                        epgList.setVisibility(View.GONE);
                    });
                }
            }
        });
        this.epgSpacer = mLayout.findViewById(R.id.channelSpacer3);
        this.epgList = mLayout.findViewById(R.id.channelEpgList);
        epgListViewAdapter = new EpgListViewAdapter(mLayout.getContext(), new ArrayList<>());
        this.epgList.setAdapter(epgListViewAdapter);
        epgListTracker = new SelectionTracker.Builder<>(
                "epg-list-view",
                epgList,
                epgListViewAdapter.new KeyProvider(ItemKeyProvider.SCOPE_MAPPED),
                epgListViewAdapter.new ItemLookup(epgList),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectSingleAnything()).build();
        epgListViewAdapter.setTracker(epgListTracker);

        RecyclerView channelList = mLayout.findViewById(R.id.channelList);
        channelListViewAdapter = new ChannelListViewAdapter(mLayout.getContext(), channelManager.getChannels(selectedChannel).orElse(new ArrayList<>()));
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

        groupList = mLayout.findViewById(R.id.channelGroup);
        groupListSpacer = mLayout.findViewById(R.id.channelSpacer1);
        if (channelManager.hasGroup()) {
            mHandler.post(() -> {
                groupList.setVisibility(View.VISIBLE);
                groupListSpacer.setVisibility(View.VISIBLE);
            });
        }
        groupListViewAdapter = new GroupListViewAdapter(mLayout.getContext(), channelManager.groups);
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
            setChannels(channels);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setChannels(List<ChannelItem> channels) {
        mHandler.post(()->{
            channelListTracker.clearSelection();
            channelListViewAdapter.setData(channels);
            channelListViewAdapter.notifyDataSetChanged();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setGroups(List<ChannelGroup> groups) {
        mHandler.post(() -> {
            groupListTracker.clearSelection();
            groupListViewAdapter.setData(groups);
            groupListViewAdapter.notifyDataSetChanged();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setEpgs(List<EpgProgram> epgs) {
        mHandler.post(() -> {
            epgListTracker.clearSelection();
            epgListViewAdapter.setData(epgs);
            epgListViewAdapter.notifyDataSetChanged();
        });
    }

    public void setVisibleDelayed(boolean isVisible, int delayMillis) {
        if (setVisibilityDelayedTask != null) {
            setVisibilityDelayedTask.cancel();
        }
        setVisibilityDelayedTask = new CancelableRunnable() {
            @Override
            public void callback() {
                setVisible(isVisible);
            }
        };
        delayMillis = Math.max(delayMillis, 1);
        mHandler.postDelayed(setVisibilityDelayedTask, delayMillis);
    }

    private void setVisible(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (visibility != mLayout.getVisibility()) {
            mLayout.setVisibility(visibility);
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

    @SuppressLint("NotifyDataSetChanged")
    public void setData(ChannelManager m) {
        selectedGroup = 0;
        selectedChannel = 0;
        channelManager = m;
        setGroups(m.groups);
        int visibility = m.hasGroup() ? View.VISIBLE : View.GONE;
        mHandler.post(() -> {
            groupList.setVisibility(visibility);
            groupListSpacer.setVisibility(visibility);
        });
        List<ChannelItem> channels = m.getChannels(selectedGroup).orElse(new ArrayList<>());
        setChannels(channels);
    }
}
