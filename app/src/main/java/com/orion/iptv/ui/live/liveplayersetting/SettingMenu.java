package com.orion.iptv.ui.live.liveplayersetting;

import com.orion.iptv.recycleradapter.ListItem;

import java.util.List;

public interface SettingMenu extends ListItem {
    List<SettingValue> getValues();
    default int getSelectedPosition() {
        return -1;
    }
}
