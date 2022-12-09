package com.orion.iptv.layout.live;

import android.content.Context;
import android.util.Pair;

import com.orion.iptv.R;
import com.orion.player.IExtPlayer;
import com.orion.player.IExtPlayerFactory;

import java.util.ArrayList;
import java.util.List;

public class SetPlayerFactory implements SettingMenu {
    private final Context context;
    private final LivePlayerViewModel viewModel;
    private final List<PlayerFactoryValue> items;

    public SetPlayerFactory(Context context, LivePlayerViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
        items = new ArrayList<>();
        String[] playerTypes = context.getResources().getStringArray(R.array.player_type_entries);
        int[] playerValues = context.getResources().getIntArray(R.array.player_type_int_values);
        for (int i=0; i<playerTypes.length; i++) {
            items.add(new PlayerFactoryValue(playerTypes[i], playerValues[i]));
        }
    }

    @Override
    public String content() {
        return context.getString(R.string.set_player_type);
    }

    @Override
    public List<SettingValue> getValues() {
        List<SettingValue> values = new ArrayList<>();
        for (PlayerFactoryValue v : items) {
            values.add((SettingValue) v);
        }
        return values;
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
        public void onSelected() {
            viewModel.setPlayerFactoryType(settingValue);
        }
    }
}
