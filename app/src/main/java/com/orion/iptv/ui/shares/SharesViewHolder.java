package com.orion.iptv.ui.shares;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;

public class SharesViewHolder extends RecyclerView.ViewHolder {
    private final TextView content;
    private final ImageButton setting;
    public SharesViewHolder(@NonNull View itemView) {
        super(itemView);
        content = itemView.findViewById(R.id.list_item_content);
        content.setSelected(true);
        setting = itemView.findViewById(R.id.list_item_button);
    }

    public void setContent(FileNode node) {
        content.setText(node.getName());
    }

    public void setSelected(boolean isSelected) {
        itemView.setActivated(isSelected);
    }

    public void setOnButtonClickListener(View.OnClickListener listener) {
        setting.setOnClickListener(listener);
    }

    public void setOnItemClickListener(View.OnClickListener listener) {
        content.setOnClickListener(listener);
    }

    public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
        int position = getBindingAdapterPosition();
        if (position < 0) {
            return null;
        }
        return new ItemDetailsLookup.ItemDetails<>() {
            @Override
            public int getPosition() {
                return position;
            }

            @NonNull
            @Override
            public Long getSelectionKey() {
                return (long) position;
            }
        };
    }
}
