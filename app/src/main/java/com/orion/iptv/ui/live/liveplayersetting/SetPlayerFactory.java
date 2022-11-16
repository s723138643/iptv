package com.orion.iptv.ui.live.liveplayersetting;

import com.orion.iptv.misc.PreferenceStore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SetPlayerFactory implements SettingMenu {
    public static final String settingKey = "live_player_factory";
    private final List<PlayerFactoryValue> items;

    public SetPlayerFactory() {
        items = new ArrayList<>();
        items.add(new PlayerFactoryValue("ijkplayer(硬解)", 0));
        items.add(new PlayerFactoryValue("ijkplayer(软解)", 1));
        items.add(new PlayerFactoryValue("exoplayer", 2));
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
        int storedValue = PreferenceStore.getInt(settingKey, 0);
        for (int i=0; i<items.size(); i++) {
            if (items.get(i).settingValue == storedValue) {
                return i;
            }
        }
        return -1;
    }

    private static class PlayerFactoryValue implements SettingValue {
        private final String description;
        private final int settingValue;

        public PlayerFactoryValue(String description, int value) {
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
        public void onSelected(OnSettingChangedListener listener) {
            listener.onSettingChanged(settingKey, this.settingValue);
        }
    }
}
