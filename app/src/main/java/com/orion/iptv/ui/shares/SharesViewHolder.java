package com.orion.iptv.ui.shares;

import android.content.Context;
import android.content.res.ColorStateList;
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
    private final ColorStateList[] backgrounds;
    public SharesViewHolder(@NonNull Context context, @NonNull View itemView) {
        super(itemView);
        backgrounds = new ColorStateList[]{
                context.getColorStateList(R.color.recyclerview_item_even_background),
                context.getColorStateList(R.color.recyclerview_item_odd_background),
        };
        content = itemView.findViewById(R.id.list_item_content);
        content.setSelected(true);
        setting = itemView.findViewById(R.id.list_item_button);
    }

    public void setContent(FileNode node, int position) {
        ColorStateList background = backgrounds[position % 2];
        int color = background.getDefaultColor();
        itemView.setBackgroundColor(color);
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
