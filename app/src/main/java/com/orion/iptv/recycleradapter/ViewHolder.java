package com.orion.iptv.recycleradapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class ViewHolder<T extends ListItem> extends RecyclerView.ViewHolder {
    public ViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public abstract void setActivated(boolean isActivated);

    public abstract void setContent(int position, T content);
}
