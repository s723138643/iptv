package com.orion.iptv.layout.live;

import android.util.Pair;

import com.orion.player.IExtPlayer;
import com.orion.player.IExtPlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SetPlayerFactory implements SettingMenu {
    private final LivePlayerViewModel viewModel;
    private final List<PlayerFactoryValue> items;

    public SetPlayerFactory(LivePlayerViewModel viewModel) {
        this.viewModel = viewModel;
        items = new ArrayList<>();
        items.add(new PlayerFactoryValue("ijkplayer(硬解)", LivePlayerViewModel.IJKPLAYER));
        items.add(new PlayerFactoryValue("ijkplayer(软解)", LivePlayerViewModel.IJKPLAYER_SW));
        items.add(new PlayerFactoryValue("exoplayer", LivePlayerViewModel.EXOPLAYER));
    }

    @Override
    public String content() {
        return "切换播放器";
    }

    @Override
    public List<SettingValue> getValues() {
        return items.stream()
                .map(v -> (SettingValue) v)
                .collect(Collectors.toList());
    }

    @Override
    public int getSelectedPosition() {
        Pair<Integer, IExtPlayerFactory<? extends IExtPlayer>> currentPlayerType = viewModel.getPlayerFactory();
        for (int i=0; i<items.size(); i++) {
            if (items.get(i).settingValue == currentPlayerType.first) {
                return i;
            }
        }
        return -1;
    }

    private class PlayerFactoryValue implements SettingValue {
        private final String description;
        private final @LivePlayerViewModel.PlayerType int settingValue;

        public PlayerFactoryValue(String description, @LivePlayerViewModel.PlayerType int value) {
            this.description = description;
            this.settingValue = value;
        }

        @Override
        public String content() {
            return this.description;
        }

        @Override
        public boolean isButton() {
            return false;
        }

        @Override
        public void onSelected() {
            viewModel.setPlayerFactoryType(settingValue);
        }
    }
}
