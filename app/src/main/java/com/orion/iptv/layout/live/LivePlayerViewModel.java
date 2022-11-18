package com.orion.iptv.layout.live;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
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
import com.orion.player.ijk.ExtIjkPlayerFactory;
import com.orion.player.ijk.ExtSoftIjkPlayerFactory;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class LivePlayerViewModel extends ViewModel {
    private final static String TAG = "LiveChannelListViewModel";

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
    @IntDef({IJKPLAYER, IJKPLAYER_SW, EXOPLAYER})
    public @interface PlayerType{}
    public static final int IJKPLAYER = 0;
    public static final int IJKPLAYER_SW = 1;
    public static final int EXOPLAYER = 2;

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
    private final MutableLiveData<Pair<Integer, ExtDataSource>> liveSource;
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

        int playerFactoryType = PreferenceStore.getInt(PlayerFactoryKey, IJKPLAYER);
        playerFactory.setValue(Pair.create(playerFactoryType, newPlayerFactory(playerFactoryType)));
        String url = PreferenceStore.getString(SettingUrlKey, "");
        if (!url.isEmpty()) {
            settingUrl.setValue(url);
        }
    }

    @NonNull
    public Optional<Pair<Integer,List<ChannelItem>>> getChannels() {
        Pair<Integer, List<ChannelItem>> list = channels.getValue();
        return list != null ? Optional.of(list) : Optional.empty();
    }

    @NonNull
    public Optional<List<ChannelGroup>> getGroups() {
        List<ChannelGroup> list = groups.getValue();
        return list != null ? Optional.of(list) : Optional.empty();
    }

    @NonNull
    public Optional<Pair<ChannelInfo, EpgProgram[]>> getEpgPrograms() {
        Pair<ChannelInfo, EpgProgram[]> list = epgs.getValue();
        return list != null ? Optional.of(list) : Optional.empty();
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

    public void observeLiveSource(LifecycleOwner owner, Observer<Pair<Integer, ExtDataSource>> observer) {
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

        List<ExtDataSource> sources = item.getSources()
                .stream()
                .filter(link -> link != null && !link.isEmpty())
                .map(link -> new ExtDataSource(link, item.info))
                .collect(Collectors.toList());
        sourceManager = new DataSourceManager(sources);
        liveSource.setValue(sourceManager.getCurrentDataSource());

        currentChannel.setValue(Pair.create(Pair.create(position, items.first), item.info));
        currentEpgProgram.setValue(null);
        nextEpgProgram.setValue(null);
        epgs.setValue(null);
    }

    public int getSelectedGroup() {
        Pair<Pair<Integer, Integer>, ChannelInfo> channelItemPair = currentChannel.getValue();
        if (channelItemPair != null) {
            return channelItemPair.first.second;
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

    public int getSourceCount() {
        return (sourceManager != null) ? sourceManager.getDataSourceCount() : 0;
    }

    public IExtPlayerFactory<? extends IExtPlayer> newPlayerFactory(@PlayerType int playerType) {
        switch (playerType) {
            case IJKPLAYER:
                return new ExtIjkPlayerFactory();
            case IJKPLAYER_SW:
                return new ExtSoftIjkPlayerFactory();
            case EXOPLAYER:
                return new ExtExoPlayerFactory();
        }
        return new ExtIjkPlayerFactory();
    }

    public void setPlayerFactoryType(@PlayerType int playerFactoryType) {
        PreferenceStore.setInt(PlayerFactoryKey, playerFactoryType);
        playerFactory.setValue(Pair.create(playerFactoryType, newPlayerFactory(playerFactoryType)));
    }

    public Pair<Integer, IExtPlayerFactory<? extends IExtPlayer>> getPlayerFactory() {
        return playerFactory.getValue();
    }

    public Pair<Integer, ExtDataSource> getCurrentSource() {
        return liveSource.getValue();
    }

    public int indexOf(ExtDataSource dataSource) {
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

        Optional<ChannelItem> channel = source.getChannel(selectedGroup, selectedChannel);
        if (channel.isPresent()) {
            ChannelItem item = channel.get();
            if (selectedChannelName.equals(item.info.channelName)) {
                Log.i(TAG, String.format(Locale.getDefault(), "use saved group: %d, channel: %d", selectedGroup, selectedChannel));
                selectGroup(selectedGroup, source.groups.get(selectedGroup));
                selectChannel(selectedChannel, item);
                return;
            }
        }
        channel = source.getChannel(0, 0);
        if (channel.isPresent()) {
            Log.i(TAG, String.format(Locale.getDefault(), "use default group: %d, channel: %d", 0, 0));
            selectGroup(0, source.groups.get(0));
            selectChannel(0, channel.get());
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