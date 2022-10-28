package com.orion.iptv.layout.livechannellist;

import android.view.View;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ListItem;
import com.orion.iptv.recycleradapter.ViewHolder;

public class MyViewHolder<T extends ListItem> extends ViewHolder<T> {
    private final View container;
    private final TextView index;
    private final TextView desc;

    public MyViewHolder(View v, boolean hideIndex) {
        super(v);
        container = v.findViewById(R.id.live_channel_list_item);
        index = v.findViewById(R.id.list_item_index);
        if (hideIndex) {
            index.setVisibility(View.GONE);
        }
        desc = v.findViewById(R.id.list_item_desc);
    }

    @Override
    public void setActivated(boolean isActivated) {
        container.setActivated(isActivated);
    }

    @Override
    public void setContent(int position, T content) {
        index.setText(content.index());
        desc.setText(content.name());
    }
}
