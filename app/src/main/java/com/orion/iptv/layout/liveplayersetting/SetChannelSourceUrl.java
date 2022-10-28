package com.orion.iptv.layout.liveplayersetting;

import android.app.AlertDialog;
import android.widget.EditText;
import android.content.Context;

import com.orion.iptv.R;

import java.util.List;

public class SetChannelSourceUrl implements SettingMenu {
    private final Context context;

    private class SourceUrlValue implements SettingValue {
        private final String settingKey;

        public SourceUrlValue(String key) {
            this.settingKey = key;
        }

        @Override
        public String index() {
            return "";
        }

        @Override
        public String name() {
            return "设置频道源";
        }

        @Override
        public boolean isButton() {
            return true;
        }

        @Override
        public void onSelected(OnSettingChangedListener listener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder = builder.setTitle("setting channel source");
            builder = builder.setView(R.layout.channel_source_dialog);
            builder = builder.setPositiveButton("ok", (dialog, which) -> {
                AlertDialog alertDialog = (AlertDialog)dialog;
                EditText text = alertDialog.findViewById(R.id.channel_source_url);
                if (text == null) {
                    return;
                }
                String input = text.getText().toString();
                if (input.equals("")) {
                    return;
                }
                listener.onSettingChanged(settingKey, input);
            });
            builder.create().show();
        }
    }

    public SetChannelSourceUrl(Context context) {
        this.context = context;
    }

    @Override
    public String index() {
        return "";
    }

    @Override
    public String name() {
        return "设置源";
    }

    @Override
    public List<SettingValue> getValues() {
        return List.of(new SourceUrlValue("channel_source_url"));
    }
}
