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
import com.orion.player.ui.VideoView;

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
    public final static String EpgUrlKey = "live_player_epg_url";
    public final static String PlayerFactoryKey = "live_player_factory";
    public final static String SurfaceTypeKey = "live_player_surface_type";
    public final static String SourceTimeoutKey = "live_player_source_timeout";

    public final static String DEFAULT_EPG_URL = "http://epg.51zmt.top:8000/api/diyp/";
    // 10s
    public final static int DEFAULT_SOURCE_TIMEOUT = 10000;

    private final MutableLiveData<List<ChannelGroup>> groups;
    private final MutableLiveData<Pair<ChannelInfo, EpgProgram[]>> epgs;
    private final MutableLiveData<Pair<Integer, List<ChannelItem>>> channels;
    // currentChannelInfo = Pair<<ChannelPos, GroupPos>, ChannelInfo>
    private final MutableLiveData<Channel> currentChannel;
    private final MutableLiveData<Pair<Integer, Pair<ChannelInfo, EpgProgram>>> currentEpgProgram;
    private final MutableLiveData<Pair<Integer, Pair<ChannelInfo, EpgProgram>>> nextEpgProgram;
    private final MutableLiveData<Pair<Integer, DataSource>> liveSource;
    private final MutableLiveData<Pair<Integer, IExtPlayerFactory<? extends IExtPlayer>>> playerFactory;
    private final MutableLiveData<Integer> surfaceType;
    private final MutableLiveData<String> settingUrl;
    private final MutableLiveData<String> epgUrl;
    private final MutableLiveData<Integer> sourceTimeout;

    private DataSourceManager sourceManager;

    public LivePlayerViewModel() {
        channels = new MutableLiveData<>();
        groups = new MutableLiveData<>();
        epgs = new MutableLiveData<>();
        currentChannel = new MutableLiveData<>();
        currentEpgProgram = new MutableLiveData<>();
        nextEpgProgram = new MutableLiveData<>();
        liveSource = new MutableLiveData<>();
        settingUrl = new MutableLiveData<>();

        int playerFactoryType = PreferenceStore.getInt(PlayerFactoryKey, 0);
        playerFactory = new MutableLiveData<>(Pair.create(playerFactoryType, newPlayerFactory(playerFactoryType)));
        int surfaceTypeValue = PreferenceStore.getInt(SurfaceTypeKey, VideoView.SURFACE_TYPE_SURFACE_VIEW);
        surfaceType = new MutableLiveData<>(surfaceTypeValue);
        int timeout = PreferenceStore.getInt(SourceTimeoutKey, DEFAULT_SOURCE_TIMEOUT);
        sourceTimeout = new MutableLiveData<>(timeout);
        String epg = PreferenceStore.getString(EpgUrlKey, DEFAULT_EPG_URL);
        epgUrl = new MutableLiveData<>(epg);
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

    public void observeCurrentChannel(LifecycleOwner owner, Observer<Channel> observer) {
        currentChannel.observe(owner, observer);
    }

    public void observeSettingUrl(LifecycleOwner owner, Observer<String> observer) {
        settingUrl.observe(owner, observer);
    }

    public void observeEpgUrl(LifecycleOwner owner, Observer<String> observer) {
        epgUrl.observe(owner, observer);
    }

    public void observeSurfaceType(LifecycleOwner owner, Observer<Integer> observer) {
        surfaceType.observe(owner, observer);
    }

    public void selectChannel(int position, ChannelItem item) {
        Pair<Integer, List<ChannelItem>> channel = channels.getValue();
        if (channel == null) {
            return;
        }
        PreferenceStore.setInt(GroupPosKey, channel.first);
        selectChannel(position, item, channel);
    }

    protected void selectChannel(int position, ChannelItem item, Pair<Integer, List<ChannelItem>> group) {
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

        currentChannel.setValue(new Channel(position, item.info, group.first, group.second));
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
        Channel channel = currentChannel.getValue();
        if (channel != null) {
            if (channel.groupPos == groupPos) {
                return channel.channelPos;
            }
        } else {
            if (groupPos == PreferenceStore.getInt(GroupPosKey, 0)) {
                return PreferenceStore.getInt(ChannelPosKey, 0);
            }
        }
        return -1;
    }

    public Channel getCurrentChannel() {
        return currentChannel.getValue();
    }

    public void selectGroup(int position, ChannelGroup group) {
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

    public int getSurfaceType() {
        Integer v = surfaceType.getValue();
        assert v != null;
        return v;
    }

    public void setSurfaceType(int value) {
        PreferenceStore.setInt(SurfaceTypeKey, value);
        surfaceType.setValue(value);
    }

    public int getSourceTimeout() {
        Integer v = sourceTimeout.getValue();
        assert v != null;
        return v;
    }

    public void setSourceTimeout(int timeout) {
        PreferenceStore.setInt(SourceTimeoutKey, timeout);
        sourceTimeout.setValue(timeout);
    }

    public Pair<Integer, DataSource> getCurrentSource() {
        return liveSource.getValue();
    }

    public int indexOf(DataSource dataSource) {
        return sourceManager.getCursor(dataSource);
    }

    public String getSettingUrl() {
        String url = settingUrl.getValue();
        return url != null ? url : PreferenceStore.getString(SettingUrlKey, "");
    }

    public void updateSettingUrl(String url) {
        if (url.isEmpty() || url.equals(settingUrl.getValue())) {
            return;
        }
        PreferenceStore.setString(SettingUrlKey, url);
        settingUrl.setValue(url);
    }

    public void initSettingUrl(String url) {
        if (url.isEmpty()) {
            return;
        }
        settingUrl.setValue(url);
    }

    public void updateChannelSource (ChannelSource source) {
        int selectedGroup;
        int selectedChannel;
        String selectedChannelName;
        Channel current = currentChannel.getValue();
        if (current != null) {
            selectedGroup = current.groupPos;
            selectedChannel = current.channelPos;
            selectedChannelName = current.channelInfo.channelName;
        } else {
            selectedGroup = PreferenceStore.getInt(GroupPosKey, 0);
            selectedChannel = PreferenceStore.getInt(ChannelPosKey, 0);
            selectedChannelName = PreferenceStore.getString(ChannelNameKey, "");
        }

        Log.i(TAG, String.format(Locale.getDefault(), "has groups: %d", source.groups.size()));
        ChannelItem channel = source.getChannel(selectedGroup, selectedChannel);
        if (channel != null) {
            if (selectedChannelName.equals(channel.info.channelName)) {
                Log.i(TAG, String.format(Locale.getDefault(), "use saved group: %d, channel: %d", selectedGroup, selectedChannel));
                ChannelGroup group = source.groups.get(selectedGroup);
                selectChannel(selectedChannel, channel, Pair.create(selectedGroup, group.channels));
                selectGroup(selectedGroup, group);
                groups.setValue(source.groups);
                return;
            }
        }
        channel = source.getChannel(0, 0);
        if (channel != null) {
            Log.i(TAG, String.format(Locale.getDefault(), "use default group: %d, channel: %d", 0, 0));
            ChannelGroup group = source.groups.get(0);
            selectChannel(0, channel, Pair.create(0, group.channels));
            selectGroup(0, group);
            groups.setValue(source.groups);
        }
    }

    public String getEpgUrl() {
        String url = epgUrl.getValue();
        return url != null ? url : PreferenceStore.getString(EpgUrlKey, "");
    }

    public void updateEpgUrl(String url) {
        String old = epgUrl.getValue();
        if (url == null || url.equals(old)) {
            return;
        }
        PreferenceStore.setString(EpgUrlKey, url);
        epgUrl.setValue(url);
    }

    public void updateEpgPrograms(ChannelInfo info, Date date, EpgProgram[] programs) {
        Channel channel = currentChannel.getValue();
        if (channel == null) {
            return;
        }
        ChannelInfo currentChannel = channel.channelInfo;
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

    public static class Channel {
        public final int channelPos;
        public final int groupPos;
        public final ChannelInfo channelInfo;
        public final List<ChannelItem> channels;

        public Channel(int channelPos, ChannelInfo channel, int groupPos, List<ChannelItem> channels) {
            this.channelPos = channelPos;
            this.channelInfo = channel;
            this.groupPos = groupPos;
            this.channels = channels;
        }
    }
}