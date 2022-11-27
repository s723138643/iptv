package com.orion.iptv.recycleradapter;

import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

public interface ViewHolderFactory<T extends RecyclerView.ViewHolder> {
    T create(ViewGroup parent, int viewType);
}