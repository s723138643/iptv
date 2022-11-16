package com.orion.iptv.ui.live.liveplayersetting;

import android.content.Context;

import com.orion.iptv.layout.dialog.ChannelSourceDialog;
import com.orion.iptv.misc.PreferenceStore;

import java.util.List;

public class SetChannelSourceUrl implements SettingMenu {
    public static final String settingKey = "channel_source_url";

    private static final String TAG = "SetChannelSourceUrl";
    private final Context context;

    public SetChannelSourceUrl(Context context) {
        this.context = context;
    }

    @Override
    public String content() {
        return "设置源";
    }

    @Override
    public List<SettingValue> getValues() {
        return List.of(new SourceUrlValue());
    }

    private class SourceUrlValue implements SettingValue {
        @Override
        public String content() {
            return "设置频道源";
        }

        @Override
        public boolean isButton() {
            return true;
        }

        @Override
        public void onSelected(OnSettingChangedListener listener) {
            ChannelSourceDialog dialog = new ChannelSourceDialog(context);
            dialog.setOnChannelSourceSubmitListener((url) -> listener.onSettingChanged(settingKey, url));
            dialog.setTitle(content());
            dialog.setDefaultValue(PreferenceStore.getString(settingKey, ""));
            dialog.show();
        }
    }
}
