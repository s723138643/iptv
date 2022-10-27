package com.orion.iptv.layout.liveplayersetting;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemKeyProvider;

public class KeyProvider extends ItemKeyProvider<Long> {
    public KeyProvider(int scope){
        super(scope);
    }

    @Override
    public Long getKey (int position) {
        return (long)position;
    }

    @Override
    public int getPosition(@NonNull Long key) {
        return key.intValue();
    }
}