package com.orion.iptv.ui.shares;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ViewHolder;

public class FileNodeViewHolder extends ViewHolder<FileNode> {
    private final TextView content;
    public FileNodeViewHolder(@NonNull View itemView) {
        super(itemView);
        content = itemView.findViewById(R.id.list_item_content);
        content.setSelected(true);
    }

    @Override
    public void setActivated(boolean isActivated) {
        itemView.setActivated(isActivated);
    }

    @Override
    public void setContent(int position, FileNode content) {
        this.content.setText(content.getName());
    }
}
