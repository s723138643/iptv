package com.orion.iptv.layout.live;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class ChannelListViewHolderFactory implements ViewHolderFactory<ViewHolder<ChannelItem>> {
    private final Context context;
    private final int layoutId;

    public ChannelListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }

    @Override
    public ViewHolder<ChannelItem> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder<>(context, v) {
            private final TextView number;
            private final TextView content;

            {
                number = v.findViewById(R.id.list_item_number);
                number.setEms(2);
                content = v.findViewById(R.id.list_item_content);
                content.setEms(6);
            }

            @Override
            public void changeState(int[] states) {
                super.changeState(states);
                int color = getColorForState(states, foreground);
                number.setTextColor(color);
                content.setTextColor(color);
                boolean isActivated = statesContains(states, android.R.attr.state_activated, android.R.attr.state_focused);
                number.setSelected(isActivated);
                content.setSelected(isActivated);
            }

            @Override
            public void setContent(int position, ChannelItem content) {
                this.number.setText(content.number());
                this.content.setText(content.content());
            }
        };
    }
}
