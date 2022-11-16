package com.orion.player.ijk;

import android.content.Context;

import com.orion.player.IExtPlayerFactory;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class ExtSoftIjkPlayerFactory implements IExtPlayerFactory<ExtIjkPlayer> {
    @Override
    public ExtIjkPlayer create(Context context) {
        IjkMediaPlayer.loadLibrariesOnce(null);
        return new ExtIjkPlayer(context, false);
    }
}
