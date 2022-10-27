package com.orion.iptv.layout.liveplayersetting;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

public class ItemLookup extends ItemDetailsLookup<Long> {
    private final RecyclerView view;
    public ItemLookup(RecyclerView view) {
        this.view = view;
    }

    @Nullable
    @Override
    public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
        View v = view.findChildViewUnder(e.getX(), e.getY());
        if (v != null) {
            ViewHolder holder = (ViewHolder) view.getChildViewHolder(v);
            return holder.getItemDetails().orElse(null);
        }
        return null;
    }
}
