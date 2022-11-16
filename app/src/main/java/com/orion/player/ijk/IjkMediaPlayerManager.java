package com.orion.player.ijk;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkMediaPlayerManager {
    private boolean videoViewAttached = false;
    private SurfaceHolder holder;
    private IjkMediaPlayer player;
    private final SurfaceHolder.Callback callback = new SurfaceViewCallback();

    synchronized public IjkMediaPlayer get(ExtIjkPlayer ijkMediaPlayer) {
        if (player != null) {
            return player;
        }
        player = new IjkMediaPlayer();
        player.setOnCompletionListener(ijkMediaPlayer);
        player.setOnPreparedListener(ijkMediaPlayer);
        player.setOnNativeInvokeListener(ijkMediaPlayer);
        player.setOnInfoListener(ijkMediaPlayer);
        player.setOnErrorListener(ijkMediaPlayer);
        player.setOnBufferingUpdateListener(ijkMediaPlayer);
        player.setOnVideoSizeChangedListener(ijkMediaPlayer);
        if (holder != null) {
            Surface surface = holder.getSurface();
            if (surface != null && surface.isValid()) {
                player.setSurface(surface);
            }
        }
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        return player;
    }

    synchronized public void setVideoView(View videoView) {
        if (videoView instanceof SurfaceView) {
            if (holder != null) {
                holder.removeCallback(callback);
            }
            holder = ((SurfaceView) videoView).getHolder();
            holder.addCallback(callback);
            if (player != null) {
                Surface surface = holder.getSurface();
                if (surface != null && surface.isValid()) {
                    player.setSurface(surface);
                } else {
                    player.setSurface(null);
                }
            }
        } else if (videoView instanceof TextureView) {
            if (player != null) {
                if (videoViewAttached) {
                    player.setSurface(null);
                }
                player.setSurface(new Surface(((TextureView) videoView).getSurfaceTexture()));
                videoViewAttached = true;
            }
        }
    }

    synchronized public void destroy() {
        if (player == null) {
            return;
        }
        player.reset();
        player.release();
        player = null;
    }

    private class SurfaceViewCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            if (player == null) {
                return;
            }
            player.setSurface(holder.getSurface());
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            if (player != null) {
                player.setSurface(null);
            }
        }
    }
}
