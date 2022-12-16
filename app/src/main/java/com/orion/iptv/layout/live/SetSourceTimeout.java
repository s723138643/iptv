package com.orion.iptv.layout.live;

import android.content.Context;

import com.orion.iptv.R;

import java.util.ArrayList;
import java.util.List;

public class SetSourceTimeout implements SettingMenu {
    private final LivePlayerViewModel viewModel;
    private final String content;
    private final String[] timeoutEntries;
    private final int[] timeoutValues;

    public SetSourceTimeout(Context context, LivePlayerViewModel viewModel) {
        this.viewModel = viewModel;
        this.timeoutEntries = context.getResources().getStringArray(R.array.source_timeout_entries);
        this.timeoutValues = context.getResources().getIntArray(R.array.source_timeout_values);
        this.content = context.getString(R.string.set_source_timeout);
    }

    @Override
    public List<SettingValue> getValues() {
        List<SettingValue> values = new ArrayList<>();
        for (int i=0; i<timeoutEntries.length; i++) {
            values.add(new TimeoutValue(timeoutEntries[i], timeoutValues[i]));
        }
        return values;
    }

    @Override
    public int getSelectedPosition() {
        int v = viewModel.getSourceTimeout();
        for (int i=0; i<timeoutValues.length; i++) {
            if (timeoutValues[i] == v) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String content() {
        return content;
    }

    private class TimeoutValue implements SettingValue {
        private final String content;
        private final int value;

        public TimeoutValue(String content, int value) {
            this.content = content;
            this.value = value;
        }

        @Override
        public void onSelected() {
            viewModel.setSourceTimeout(value);
        }

        @Override
        public boolean isButton() {
            return false;
        }

        @Override
        public String content() {
            return content;
        }
    }
}
