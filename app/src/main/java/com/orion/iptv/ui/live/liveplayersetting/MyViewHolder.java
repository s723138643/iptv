package com.orion.iptv.ui.live.liveplayersetting;

import android.view.View;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ListItem;
import com.orion.iptv.recycleradapter.ViewHolder;

public class MyViewHolder<T extends ListItem> extends ViewHolder<T> {
    private final TextView desc;

    public MyViewHolder(View v) {
        super(v);
        desc = v.findViewById(R.id.list_item_content);
    }

    @Override
    public void setActivated(boolean isActivated) {
        itemView.setActivated(isActivated);
    }

    @Override
    public void setContent(int position, T content) {
        desc.setText(content.content());
    }
}
