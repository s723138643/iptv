package com.orion.player.exo;

import android.content.Context;

import com.orion.player.IExtPlayerFactory;

public class ExtExoPlayerFactory implements IExtPlayerFactory<ExtExoPlayer> {
    @Override
    public ExtExoPlayer create(Context context) {
        return new ExtExoPlayer(context);
    }
}
