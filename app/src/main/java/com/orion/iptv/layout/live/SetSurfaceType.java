package com.orion.iptv.layout.live;

import android.content.Context;

import com.orion.iptv.R;

import java.util.ArrayList;
import java.util.List;

public class SetSurfaceType implements SettingMenu {
    private final String name;
    private final String[] surfaceTypeEntries;
    private final int[] surfaceTypeValues;
    private final LivePlayerViewModel viewModel;

    public SetSurfaceType(Context context, LivePlayerViewModel viewModel) {
        name = context.getString(R.string.set_surface_type);
        surfaceTypeEntries = context.getResources().getStringArray(R.array.surface_type_entries);
        surfaceTypeValues = context.getResources().getIntArray(R.array.surface_type_int_values);
        this.viewModel =  viewModel;
    }

    @Override
    public List<SettingValue> getValues() {
        List<SettingValue> values = new ArrayList<>();
        for (int i=0; i<surfaceTypeEntries.length; i++) {
            values.add(new SurfaceValue(surfaceTypeEntries[i], surfaceTypeValues[i]));
        }
        return values;
    }

    @Override
    public int getSelectedPosition() {
        int v = viewModel.getSurfaceType();
        for (int i=0; i<surfaceTypeValues.length; i++) {
            if (surfaceTypeValues[i] == v) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String content() {
        return name;
    }

    private class SurfaceValue implements SettingValue {
        private final String content;
        private final int value;

        public SurfaceValue(String content, int value) {
            this.content = content;
            this.value = value;
        }

        @Override
        public void onSelected() {
            viewModel.setSurfaceType(value);
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
