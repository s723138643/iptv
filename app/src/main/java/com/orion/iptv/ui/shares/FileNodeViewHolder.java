package com.orion.iptv.ui.shares;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ViewHolder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class FileNodeViewHolder extends ViewHolder<FileNode> {
    private final TextView name;
    private final TextView size;
    private final TextView lastModified;
    private final ColorStateList[] backgrounds;
    static private final DateFormat formatter = new SimpleDateFormat("yyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    public FileNodeViewHolder(@NonNull Context context, @NonNull View itemView) {
        super(context, itemView);
        name = itemView.findViewById(R.id.item_file_node_name);
        size = itemView.findViewById(R.id.item_file_node_size);
        lastModified = itemView.findViewById(R.id.item_file_node_last_modified);
        backgrounds = new ColorStateList[]{
                context.getColorStateList(R.color.recyclerview_item_even_background),
                context.getColorStateList(R.color.recyclerview_item_odd_background),
        };
    }

    @Override
    public void changeState(int[] states) {
        super.changeState(states);
        int color = getColorForState(states, foreground);
        name.setTextColor(color);
        size.setTextColor(color);
        lastModified.setTextColor(color);
        name.setSelected(statesContains(states, android.R.attr.state_activated, android.R.attr.state_focused));
        size.setSelected(statesContains(states, android.R.attr.state_activated, android.R.attr.state_focused));
        lastModified.setSelected(statesContains(states, android.R.attr.state_activated, android.R.attr.state_focused));
    }

    @Override
    public void setContent(int position, FileNode content) {
        this.name.setText(content.getName());
        this.size.setText(content.isFile() ? content.getSize() : "-");
        this.lastModified.setText(content.getLastModified(formatter));
        background = backgrounds[position % 2];
    }
}
