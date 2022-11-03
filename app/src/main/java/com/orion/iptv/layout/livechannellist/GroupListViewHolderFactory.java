package com.orion.iptv.layout.livechannellist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orion.iptv.bean.ChannelGroup;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class GroupListViewHolderFactory implements ViewHolderFactory<ViewHolder<ChannelGroup>, ChannelGroup> {
    private final Context context;
    private final int layoutId;

    public GroupListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }

    @Override
    public ViewHolder<ChannelGroup> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new MyViewHolder<>(v, true);
    }
}
