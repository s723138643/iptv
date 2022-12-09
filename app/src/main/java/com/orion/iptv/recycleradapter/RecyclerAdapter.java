package com.orion.iptv.recycleradapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class RecyclerAdapter<U> extends RecyclerView.Adapter<ViewHolder<U>> {
    private static final String TAG = "RecyclerAdapter";
    private final ViewHolderFactory<ViewHolder<U>> factory;

    private final List<U> items;
    private Selection<U> selection;

    public RecyclerAdapter(Context context, List<U> items, ViewHolderFactory<ViewHolder<U>> factory) {
        this.factory = factory;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder<U> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return factory.create(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<U> holder, int position) {
        if (selection != null) {
            holder.changeState(selection.getState(position));
        }
        holder.setContent(position, items.get(position));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder<U> holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        if (payloads.size() > 0) {
            for (Object payload : payloads) {
                if (payload.equals(DefaultSelection.TAG)) {
                    holder.changeState(selection.getState(position));
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    protected U getItem(int position) {
        return items.get(position);
    }

    public void setSelection(Selection<U> selection) {
        this.selection = selection;
    }

    public void clearSelection() {
        this.selection = null;
    }
}