package com.orion.iptv.ui.shares;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ViewHolder;

public class FileNodeViewHolder extends ViewHolder<FileNode> {
    private final TextView content;

    public FileNodeViewHolder(@NonNull Context context, @NonNull View itemView) {
        super(context, itemView);
        content = itemView.findViewById(R.id.list_item_content);
    }

    @Override
    public void changeState(int[] states) {
        super.changeState(states);
        int color = getColorForState(states, foreground);
        content.setTextColor(color);
        content.setSelected(statesContains(states, android.R.attr.state_activated, android.R.attr.state_focused));
    }

    @Override
    public void setContent(int position, FileNode content) {
        this.content.setText(content.getName());
    }
}
