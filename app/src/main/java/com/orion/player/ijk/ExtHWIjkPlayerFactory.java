package com.orion.player.ijk;

import android.content.Context;

import com.orion.player.IExtPlayerFactory;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class ExtHWIjkPlayerFactory implements IExtPlayerFactory<ExtHWIjkPlayer> {
    @Override
    public ExtHWIjkPlayer create(Context context) {
        IjkMediaPlayer.loadLibrariesOnce(null);
        return new ExtHWIjkPlayer(context);
    }
}
