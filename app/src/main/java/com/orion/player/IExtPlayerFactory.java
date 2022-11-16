package com.orion.player;

import android.content.Context;

public interface IExtPlayerFactory<T extends IExtPlayer> {
    T create(Context context);
}