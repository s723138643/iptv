package com.orion.iptv.layout.liveplayersetting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.R;

import java.util.List;

public class ListAdapter<T extends ListItem> extends RecyclerView.Adapter<ViewHolder> {
    private final Context context;
    private SelectionTracker<Long> tracker;
    private OnSelected<T> onSelected;
    private List<T> items;

    interface OnSelected<T> {
        void onSelected(T item);
    }

    public ListAdapter(Context context, List<T> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.live_channel_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.setActivated(tracker.isSelected((long)position));
        holder.setContent(position, items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setTracker(SelectionTracker<Long> tracker) {
        tracker.addObserver(new SelectionTracker.SelectionObserver<>() {
            @Override
            public void onItemStateChanged(@NonNull Long key, boolean selected) {
                super.onItemStateChanged(key, selected);
                if (!selected || onSelected == null) {
                    return;
                }
                int position = key.intValue();
                if (position >= 0 && position < items.size()) {
                    onSelected.onSelected(items.get(position));
                }
            }
        });
        this.tracker = tracker;
    }

    public void setOnSelected(OnSelected<T> onSelected) {
        this.onSelected = onSelected;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
