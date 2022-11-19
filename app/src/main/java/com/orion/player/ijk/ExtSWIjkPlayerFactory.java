package com.orion.player.ijk;

import android.content.Context;

import com.orion.player.IExtPlayerFactory;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class ExtSWIjkPlayerFactory implements IExtPlayerFactory<ExtSWIjkPlayer> {
    @Override
    public ExtSWIjkPlayer create(Context context) {
        IjkMediaPlayer.loadLibrariesOnce(null);
        return new ExtSWIjkPlayer(context);
    }
}
