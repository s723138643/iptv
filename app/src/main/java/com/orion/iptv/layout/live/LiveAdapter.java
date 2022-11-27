package com.orion.iptv.layout.live;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.recycleradapter.ListItemWithStableId;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

import java.util.List;

public class LiveAdapter<T extends ListItemWithStableId> extends RecyclerAdapter<T> {

    public LiveAdapter(Context context, List<T> items, ViewHolderFactory<ViewHolder<T>> factory) {
        super(context, items, factory);
        this.setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= getItemCount()) {
            return RecyclerView.NO_ID;
        }
        T item = getItem(position);
        return item.getId();
    }
}
