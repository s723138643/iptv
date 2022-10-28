package com.orion.iptv.layout.liveplayersetting;

import com.orion.iptv.recycleradapter.ListItem;

import java.util.List;

public interface SettingMenu extends ListItem {
    List<SettingValue> getValues();
}
