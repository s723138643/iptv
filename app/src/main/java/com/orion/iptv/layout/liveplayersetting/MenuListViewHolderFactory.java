package com.orion.iptv.layout.liveplayersetting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orion.iptv.layout.livechannellist.MyViewHolder;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class MenuListViewHolderFactory implements ViewHolderFactory<ViewHolder<SettingMenu>, SettingMenu> {
    private final Context context;
    private final int layoutId;

    public MenuListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }
    @Override
    public ViewHolder<SettingMenu> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new MyViewHolder<>(v, true);
    }
}
