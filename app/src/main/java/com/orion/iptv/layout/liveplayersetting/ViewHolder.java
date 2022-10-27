package com.orion.iptv.layout.liveplayersetting;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;

import java.util.Optional;

public class ViewHolder extends RecyclerView.ViewHolder {
    private final View itemView;
    private final TextView desc;

    public ViewHolder(View v) {
        super(v);
        itemView = v;
        v.findViewById(R.id.list_item_index).setVisibility(View.GONE);
        desc = v.findViewById(R.id.list_item_desc);
    }

    public void setActivated(boolean isActivated) {
        itemView.setActivated(isActivated);
    }

    public void setContent(int position, ListItem item) {
        desc.setText(item.Name());
    }

    public Optional<ItemDetailsLookup.ItemDetails<Long>> getItemDetails() {
        int position = getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return Optional.empty();
        }
        return Optional.of(new ItemDetailsLookup.ItemDetails<>() {
            @Override
            public int getPosition() {
                return position;
            }

            @Override
            public Long getSelectionKey() {
                return (long)position;
            }

            @Override
            public boolean inSelectionHotspot(@NonNull MotionEvent e) {
                return true;
            }
        });
    }
}
