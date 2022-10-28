package com.orion.iptv.recycleradapter;

import android.view.ViewGroup;

public interface ViewHolderFactory<T extends ViewHolder<U>, U extends ListItem> {
    T  create(ViewGroup parent, int viewType);
}
