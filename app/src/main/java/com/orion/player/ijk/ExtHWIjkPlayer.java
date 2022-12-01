package com.orion.player.ijk;

import android.content.Context;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class ExtHWIjkPlayer extends ExtSWIjkPlayer {
    public ExtHWIjkPlayer(Context context) {
        super(context);
    }

    @Override
    protected void setOptions(IjkMediaPlayer ijkMediaPlayer) {
        super.setOptions(ijkMediaPlayer);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-all-videos", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
    }
}
