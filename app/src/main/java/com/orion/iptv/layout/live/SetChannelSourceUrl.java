package com.orion.iptv.layout.live;

import android.content.Context;

import com.orion.iptv.layout.dialog.ChannelSourceDialog;

import java.util.List;

public class SetChannelSourceUrl implements SettingMenu {
    private static final String TAG = "SetChannelSourceUrl";
    private final Context context;
    private final LivePlayerViewModel viewModel;

    public SetChannelSourceUrl(Context context, LivePlayerViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
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
        public void onSelected() {
            ChannelSourceDialog dialog = new ChannelSourceDialog(context);
            dialog.setOnChannelSourceSubmitListener(viewModel::updateSettingUrl);
            dialog.setTitle(content());
            dialog.setDefaultValue(viewModel.getSettingUrl());
            dialog.show();
        }
    }
}
