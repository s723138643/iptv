package com.orion.iptv.layout.live;

import com.orion.iptv.recycleradapter.ListItem;

public interface SettingValue extends ListItem {
    void onSelected();

    boolean isButton();
}