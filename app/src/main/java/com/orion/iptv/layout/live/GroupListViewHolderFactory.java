package com.orion.iptv.layout.live;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelGroup;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class GroupListViewHolderFactory implements ViewHolderFactory<ViewHolder<ChannelGroup>, ChannelGroup> {
    private final Context context;
    private final int layoutId;

    public GroupListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }

    @Override
    public ViewHolder<ChannelGroup> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder<>(v) {
            private final TextView content;

            {
                content = v.findViewById(R.id.list_item_content);
                content.setEms(6);
            }

            @Override
            public void setActivated(boolean isActivated) {
                itemView.setActivated(isActivated);
                content.setSelected(isActivated);
            }

            @Override
            public void setContent(int position, ChannelGroup content) {
                this.content.setText(content.content());
            }
        };
    }
}
