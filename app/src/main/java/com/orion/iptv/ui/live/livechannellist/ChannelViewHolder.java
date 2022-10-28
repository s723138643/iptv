package com.orion.iptv.ui.live.livechannellist;

import android.view.View;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ListItemWithNumber;
import com.orion.iptv.recycleradapter.ViewHolder;

public class ChannelViewHolder<T extends ListItemWithNumber> extends ViewHolder<T> {
    private final TextView number;
    private final TextView content;

    public ChannelViewHolder(View v) {
        super(v);
        number = v.findViewById(R.id.list_item_number);
        content = v.findViewById(R.id.list_item_content);
        content.setSelected(true);
    }

    @Override
    public void setActivated(boolean isActivated) {
        itemView.setActivated(isActivated);
    }

    @Override
    public void setContent(int position, T content) {
        this.number.setText(content.number());
        this.content.setText(content.content());
    }
}
