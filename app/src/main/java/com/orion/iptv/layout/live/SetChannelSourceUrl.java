package com.orion.iptv.layout.live;

import android.content.Context;

import com.orion.iptv.R;
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
        return context.getString(R.string.set_live_sources_title);
    }

    @Override
    public List<SettingValue> getValues() {
        return List.of(
                new SourceUrlValue(
                        context.getString(R.string.set_live_channel_source),
                        viewModel::getSettingUrl,
                        viewModel::updateSettingUrl
                ),
                new SourceUrlValue(
                        context.getString(R.string.set_live_epg_source),
                        viewModel::getEpgUrl,
                        viewModel::updateEpgUrl
                )
        );
    }

    private class SourceUrlValue implements SettingValue {
        private final String content;
        private final ValueGetter valueGetter;
        private final ChannelSourceDialog.OnChannelSourceSubmitListener listener;

        public SourceUrlValue(String content, ValueGetter valueGetter, ChannelSourceDialog.OnChannelSourceSubmitListener listener) {
            this.content = content;
            this.valueGetter = valueGetter;
            this.listener = listener;
        }

        @Override
        public String content() {
            return content;
        }

        @Override
        public boolean isButton() {
            return true;
        }

        @Override
        public void onSelected() {
            ChannelSourceDialog dialog = new ChannelSourceDialog(context);
            dialog.setOnChannelSourceSubmitListener(listener);
            dialog.setTitle(content);
            dialog.setDefaultValue(valueGetter.getValue());
            dialog.show();
        }
    }

    private interface ValueGetter {
        String getValue();
    }
}
