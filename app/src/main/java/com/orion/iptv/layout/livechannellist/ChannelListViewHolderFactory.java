package com.orion.iptv.layout.livechannellist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class ChannelListViewHolderFactory implements ViewHolderFactory<ViewHolder<ChannelItem>, ChannelItem> {
    private final Context context;
    private final int layoutId;

    public ChannelListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }

    @Override
    public ViewHolder<ChannelItem> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new MyViewHolder<>(v, false);
    }
}
