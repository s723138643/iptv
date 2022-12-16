package com.orion.iptv.layout.live;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class MenuListViewHolderFactory implements ViewHolderFactory<ViewHolder<SettingMenu>> {
    private final Context context;
    private final int layoutId;

    public MenuListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }

    @Override
    public ViewHolder<SettingMenu> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder<>(context, v) {
            private final TextView desc;

            {
                desc = v.findViewById(R.id.list_item_content);
                desc.setEms(10);
            }

            @Override
            public void changeState(int[] states) {
                super.changeState(states);
                int color = getColorForState(states, foreground);
                desc.setTextColor(color);
                desc.setSelected(statesContains(states, android.R.attr.state_activated, android.R.attr.state_focused));
            }

            @Override
            public void setContent(int position, SettingMenu content) {
                desc.setText(content.content());
            }
        };
    }
}
