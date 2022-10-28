package com.orion.iptv.ui.live.livechannellist;

import android.view.View;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ListItem;
import com.orion.iptv.recycleradapter.ViewHolder;

public class ListViewHolder<T extends ListItem> extends ViewHolder<T> {
    private final TextView content;

    public ListViewHolder(View v) {
        super(v);
        content = v.findViewById(R.id.list_item_content);
        content.setSelected(true);
    }

    @Override
    public void setActivated(boolean isActivated) {
        itemView.setActivated(isActivated);
    }

    @Override
    public void setContent(int position, T content) {
        this.content.setText(content.content());
    }
}
