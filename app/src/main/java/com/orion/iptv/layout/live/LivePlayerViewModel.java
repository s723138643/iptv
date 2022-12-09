package com.orion.iptv.layout.live;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.orion.iptv.bean.ChannelGroup;
import com.orion.iptv.bean.ChannelInfo;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelSource;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.misc.PreferenceStore;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.IExtPlayerFactory;
import com.orion.player.exo.ExtExoPlayerFactory;
import com.orion.player.ijk.ExtHWIjkPlayerFactory;
import com.orion.player.ijk.ExtSWIjkPlayerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LivePlayerViewModel extends ViewModel {
    private final static String TAG = "LiveChannelListViewModel";

    public final static String GroupPosKey = "live_player_group_position";
    public final static String ChannelPosKey = "live_player_channel_position";
    public final static String ChannelNameKey = "live_player_channel_name";
    public final static String SettingUrlKey = "live_player_setting_url";
    public final static String PlayerFactoryKey = "live_player_factory";

    private final MutableLiveData<List<ChannelGroup>> groups;
    private final MutableLiveData<Pair<ChannelInfo, EpgProgram[]>> epgs;
    private final MutableLiveData<Pair<Integer, List<ChannelItem>>> channels;
    // currentChannelInfo = Pair<<ChannelPos, GroupPos>, ChannelInfo>
    private final MutableLiveData<Pair<Pair<Integer, Integer>, ChannelInfo>> currentChannel;
    private final MutableLiveData<Pair<Integer, Pair<ChannelInfo, EpgProgram>>> currentEpgProgram;
    private final MutableLiveData<Pair<Integer, Pair<ChannelInfo, EpgProgram>>> nextEpgProgram;
    private final MutableLiveData<Pair<Integer, DataSource>> liveSource;
    private final MutableLiveData<Pair<Integer, IExtPlayerFactory<? extends IExtPlayer>>> playerFactory;
    private final MutableLiveData<String> settingUrl;

    private DataSourceManager sourceManager;

    public LivePlayerViewModel() {
        channels = new MutableLiveData<>();
        groups = new MutableLiveData<>();
        epgs = new MutableLiveData<>();
        currentChannel = new MutableLiveData<>();
        currentEpgProgram = new MutableLiveData<>();
        nextEpgProgram = new MutableLiveData<>();
        liveSource = new MutableLiveData<>();
        playerFactory = new MutableLiveData<>();
        settingUrl = new MutableLiveData<>();

        int playerFactoryType = PreferenceStore.getInt(PlayerFactoryKey, 0);
        playerFactory.setValue(Pair.create(playerFactoryType, newPlayerFactory(playerFactoryType)));
        String url = PreferenceStore.getString(SettingUrlKey, "");
        if (!url.isEmpty()) {
            settingUrl.setValue(url);
        }
    }

    @Nullable
    public Pair<Integer,List<ChannelItem>> getChannels() {
        return channels.getValue();
    }

    @Nullable
    public List<ChannelGroup> getGroups() {
        return groups.getValue();
    }

    @Nullable
    public Pair<ChannelInfo, EpgProgram[]> getEpgPrograms() {
        return epgs.getValue();
    }

    public void observeGroups(LifecycleOwner owner, Observer<List<ChannelGroup>> observer) {
        groups.observe(owner, observer);
    }

    public void observeChannels(LifecycleOwner owner, Observer<Pair<Integer, List<ChannelItem>>> observer) {
        channels.observe(owner, observer);
    }

    public void observeEpgs(LifecycleOwner owner, Observer<Pair<ChannelInfo, EpgProgram[]>> observer) {
        epgs.observe(owner, observer);
    }

    public void observeCurrentEpgProgram(LifecycleOwner owner, Observer<Pair<Integer, Pair<ChannelInfo, EpgProgram>>> observer) {
        currentEpgProgram.observe(owner, observer);
    }

    public void observeNextEpgProgram(LifecycleOwner owner, Observer<Pair<Integer, Pair<ChannelInfo, EpgProgram>>> observer) {
        nextEpgProgram.observe(owner, observer);
    }

    public void observeLiveSource(LifecycleOwner owner, Observer<Pair<Integer, DataSource>> observer) {
        liveSource.observe(owner, observer);
    }

    public void observePlayerFactory(LifecycleOwner owner, Observer<Pair<Integer, IExtPlayerFactory<? extends IExtPlayer>>> observer) {
        playerFactory.observe(owner, observer);
    }

    public void observeCurrentChannel(LifecycleOwner owner, Observer<Pair<Pair<Integer, Integer>, ChannelInfo>> observer) {
        currentChannel.observe(owner, observer);
    }

    public void observeSettingUrl(LifecycleOwner owner, Observer<String> observer) {
        settingUrl.observe(owner, observer);
    }

    private boolean positionInvalid(List<?> list, int position) {
        return list == null || position < 0 || position >= list.size();
    }

    public void selectChannel(int position, ChannelItem item) {
        Pair<Integer, List<ChannelItem>> items = channels.getValue();
        if (items == null || positionInvalid(items.second, position)) {
            return;
        }
        PreferenceStore.setInt(GroupPosKey, items.first);
        PreferenceStore.setInt(ChannelPosKey, position);
        PreferenceStore.setString(ChannelNameKey, item.info.channelName);

        List<DataSource> sources = new ArrayList<>();
        for (String link : item.getSources()) {
            if (link != null && !link.isEmpty()) {
                sources.add(new DataSource(new ExtDataSource(link), item.info));
            }
        }
        sourceManager = new DataSourceManager(sources);
        liveSource.setValue(sourceManager.getCurrentDataSource());

        currentChannel.setValue(Pair.create(Pair.create(position, items.first), item.info));
        currentEpgProgram.setValue(null);
        nextEpgProgram.setValue(null);
        epgs.setValue(null);
    }

    public int getSelectedGroup() {
        Pair<Integer, List<ChannelItem>> channelsPair = channels.getValue();
        if (channelsPair != null) {
            return channelsPair.first;
        }
        return PreferenceStore.getInt(GroupPosKey, 0);
    }

    public int getSelectedChannelInGroup(int groupPos) {
        Pair<Pair<Integer, Integer>, ChannelInfo> channelItemPair = currentChannel.getValue();
        if (channelItemPair != null) {
            if (channelItemPair.first.second == groupPos) {
                return channelItemPair.first.first;
            }
        } else {
            if (groupPos == PreferenceStore.getInt(GroupPosKey, 0)) {
                return PreferenceStore.getInt(ChannelPosKey, 0);
            }
        }
        return -1;
    }

    public void selectGroup(int position, ChannelGroup group) {
        List<ChannelGroup> items = groups.getValue();
        if (positionInvalid(items, position)) {
            return;
        }
        channels.setValue(Pair.create(position, group.channels));
    }

    public void selectEpg(int position, ChannelInfo info) {
        Pair<ChannelInfo, EpgProgram[]> epgItems = epgs.getValue();
        if (epgItems == null) {
            return;
        }
        ChannelInfo channel = epgItems.first;
        EpgProgram[] epgs = epgItems.second;
        if (!info.channelName.equals(channel.channelName) || position >= epgs.length) {
            return;
        }
        currentEpgProgram.setValue(Pair.create(position, Pair.create(channel, epgs[position])));
        if (position + 1 < epgs.length) {
            nextEpgProgram.setValue(Pair.create(position + 1, Pair.create(channel, epgs[position + 1])));
        }
    }

    public void seekToNextSource() {
        if (sourceManager != null) {
            liveSource.setValue(sourceManager.nextDataSource());
        }
    }

    public void seekToPrevSource() {
        if (sourceManager != null) {
            liveSource.setValue(sourceManager.prevDataSource());
        }
    }

    public void seekToPrevChannel() {
        Pair<Pair<Integer, Integer>, ChannelInfo> channel = currentChannel.getValue();
        Pair<Integer, List<ChannelItem>> mChannels = channels.getValue();
        if (channel != null && mChannels != null) {
            int pos = channel.first.first + 1;
            if (pos >= mChannels.second.size()) {
                pos = 0;
            }
            selectChannel(pos, mChannels.second.get(pos));
        }
    }

    public void seekToNextChannel() {
        Pair<Pair<Integer, Integer>, ChannelInfo> channel = currentChannel.getValue();
        Pair<Integer, List<ChannelItem>> mChannels = channels.getValue();
        if (channel != null && mChannels != null) {
            int pos = channel.first.first - 1;
            if (pos < 0) {
                pos = mChannels.second.size() - 1;
            }
            selectChannel(pos, mChannels.second.get(pos));
        }
    }

    public int getSourceCount() {
        return (sourceManager != null) ? sourceManager.getDataSourceCount() : 0;
    }

    public IExtPlayerFactory<? extends IExtPlayer> newPlayerFactory(int playerType) {
        switch (playerType) {
            case 0:
                return new ExtHWIjkPlayerFactory();
            case 1:
                return new ExtSWIjkPlayerFactory();
            case 2:
                return new ExtExoPlayerFactory();
        }
        return new ExtHWIjkPlayerFactory();
    }

    public void setPlayerFactoryType(int playerFactoryType) {
        PreferenceStore.setInt(PlayerFactoryKey, playerFactoryType);
        playerFactory.setValue(Pair.create(playerFactoryType, newPlayerFactory(playerFactoryType)));
    }

    public Pair<Integer, IExtPlayerFactory<? extends IExtPlayer>> getPlayerFactory() {
        return playerFactory.getValue();
    }

    public Pair<Integer, DataSource> getCurrentSource() {
        return liveSource.getValue();
    }

    public int indexOf(DataSource dataSource) {
        return sourceManager.getCursor(dataSource);
    }

    public String getSettingUrl() {
        return settingUrl.getValue();
    }

    public void updateSettingUrl(String url) {
        if (url.isEmpty() || url.equals(settingUrl.getValue())) {
            return;
        }
        PreferenceStore.setString(SettingUrlKey, url);
        settingUrl.setValue(url);
    }

    public void updateChannelSource (ChannelSource source) {
        int selectedGroup;
        int selectedChannel;
        String selectedChannelName;
        Pair<Pair<Integer, Integer>, ChannelInfo> current = currentChannel.getValue();
        if (current != null) {
            selectedGroup = current.first.second;
            selectedChannel = current.first.first;
            selectedChannelName = current.second.channelName;
        } else {
            selectedGroup = PreferenceStore.getInt(GroupPosKey, 0);
            selectedChannel = PreferenceStore.getInt(ChannelPosKey, 0);
            selectedChannelName = PreferenceStore.getString(ChannelNameKey, "");
        }

        Log.i(TAG, String.format(Locale.getDefault(), "has groups: %d", source.groups.size()));
        groups.setValue(source.groups);

        ChannelItem channel = source.getChannel(selectedGroup, selectedChannel);
        if (channel != null) {
            if (selectedChannelName.equals(channel.info.channelName)) {
                Log.i(TAG, String.format(Locale.getDefault(), "use saved group: %d, channel: %d", selectedGroup, selectedChannel));
                selectGroup(selectedGroup, source.groups.get(selectedGroup));
                selectChannel(selectedChannel, channel);
                return;
            }
        }
        channel = source.getChannel(0, 0);
        if (channel != null) {
            Log.i(TAG, String.format(Locale.getDefault(), "use default group: %d, channel: %d", 0, 0));
            selectGroup(0, source.groups.get(0));
            selectChannel(0, channel);
        }
    }

    public void updateEpgPrograms(ChannelInfo info, Date date, EpgProgram[] programs) {
        Pair<Pair<Integer, Integer>, ChannelInfo> channel = currentChannel.getValue();
        if (channel == null) {
            return;
        }
        ChannelInfo currentChannel = channel.second;
        if (!info.channelName.equals(currentChannel.channelName)) {
            return;
        }
        Log.i(TAG, String.format(Locale.getDefault(), "update %d epg programs", programs.length));

        epgs.setValue(Pair.create(currentChannel, programs));
        int i = EpgProgram.indexOfCurrentProgram(programs, date);
        if (i >= 0) {
            currentEpgProgram.setValue(Pair.create(i, Pair.create(currentChannel, programs[i])));
        }
        if (i + 1 < programs.length) {
            nextEpgProgram.setValue(Pair.create(i+1, Pair.create(currentChannel, programs[i+1])));
        }
    }
}