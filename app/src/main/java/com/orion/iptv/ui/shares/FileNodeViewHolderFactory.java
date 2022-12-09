package com.orion.iptv.ui.shares;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class FileNodeViewHolderFactory implements ViewHolderFactory<ViewHolder<FileNode>> {
    private final Context context;
    private final int layout;

    public FileNodeViewHolderFactory(Context context, int layout) {
        this.context = context;
        this.layout = layout;
    }

    @Override
    public FileNodeViewHolder create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layout, parent, false);
        return new FileNodeViewHolder(context, v);
    }
}
