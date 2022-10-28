package com.orion.iptv.layout.liveplayersetting;

import com.orion.iptv.recycleradapter.ListItem;

public interface SettingValue extends ListItem {
    void onSelected(OnSettingChangedListener listener);
    boolean isButton();
}