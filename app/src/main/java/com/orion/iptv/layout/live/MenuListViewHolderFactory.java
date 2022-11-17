package com.orion.iptv.layout.live;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class MenuListViewHolderFactory implements ViewHolderFactory<ViewHolder<SettingMenu>, SettingMenu> {
    private final Context context;
    private final int layoutId;

    public MenuListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }

    @Override
    public ViewHolder<SettingMenu> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder<>(v) {
            private final TextView desc;

            {
                desc = v.findViewById(R.id.list_item_content);
                desc.setSelected(true);
            }

            @Override
            public void setActivated(boolean isActivated) {
                itemView.setActivated(isActivated);
            }

            @Override
            public void setContent(int position, SettingMenu content) {
                desc.setText(content.content());
            }
        };
    }
}
