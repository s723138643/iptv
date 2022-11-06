package com.orion.iptv.layout.liveplayersetting;

import android.view.View;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ListItem;
import com.orion.iptv.recycleradapter.ViewHolder;

public class MyViewHolder<T extends ListItem> extends ViewHolder<T> {
    private final View container;
    private final TextView desc;

    public MyViewHolder(View v) {
        super(v);
        container = v.findViewById(R.id.live_channel_list_item);
        v.findViewById(R.id.list_item_number).setVisibility(View.GONE);
        desc = v.findViewById(R.id.list_item_desc);
    }

    @Override
    public void setActivated(boolean isActivated) {
        container.setActivated(isActivated);
    }

    @Override
    public void setContent(int position, T content) {
        desc.setText(content.describe());
    }
}
