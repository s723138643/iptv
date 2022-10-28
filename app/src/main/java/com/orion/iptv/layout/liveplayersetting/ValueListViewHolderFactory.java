package com.orion.iptv.layout.liveplayersetting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class ValueListViewHolderFactory implements ViewHolderFactory<ViewHolder<SettingValue>, SettingValue> {
    private final Context context;
    private final int layoutId;

    public ValueListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }

    @Override
    public ViewHolder<SettingValue> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new MyViewHolder<>(v);
    }
}
