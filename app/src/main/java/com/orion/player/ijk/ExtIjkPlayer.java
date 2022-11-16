package com.orion.player.ijk;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.android.exoplayer2.util.Log;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtTrackInfo;
import com.orion.player.ExtVideoSize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaMeta;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class ExtIjkPlayer implements IExtPlayer,
        IMediaPlayer.OnErrorListener, IMediaPlayer.OnBufferingUpdateListener,
        IMediaPlayer.OnCompletionListener, IMediaPlayer.OnInfoListener,
        IMediaPlayer.OnPreparedListener, IMediaPlayer.OnVideoSizeChangedListener,
        IjkMediaPlayer.OnNativeInvokeListener {

    private static final String TAG = "ExtIjkPlayer";

    @Retention(RetentionPolicy.SOURCE)
    @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
    @IntDef({STATE_IDLE, STATE_PREPARING, STATE_PREPARED, STATE_BUFFERING, STATE_STARTED, STATE_PAUSED, STATE_COMPLETED, STATE_STOPPED, STATE_ENDED})
    @interface PlayerState {}
    static final int STATE_ENDED = 0;
    static final int STATE_IDLE = 1;
    static final int STATE_PREPARING = 2;
    static final int STATE_PREPARED = 3;
    static final int STATE_BUFFERING = 4;
    static final int STATE_STARTED= 5;
    static final int STATE_PAUSED = 6;
    static final int STATE_COMPLETED = 7;
    static final int STATE_STOPPED = 8;

    private final IjkMediaPlayer ijkMediaPlayer;
    private final List<Listener> listeners;
    private final Context context;
    private final ComponentListener componentListener;

    private @PlayerState int playerState = STATE_IDLE;
    private @State int playbackState = IExtPlayer.STATE_IDLE;
    private Exception playerError = null;
    private long seekToPositionMsWhenReady = 0;
    private boolean playWhenReady = false;
    private ExtVideoSize videoSize;
    private ExtDataSource dataSource;

    private SurfaceHolder surfaceHolder;
    private TextureView textureView;
    private Surface ownedSurface;
    private Surface videoOutput;

    public ExtIjkPlayer(Context context, boolean useMediacodec) {
        this.context = context;
        componentListener = new ComponentListener();
        listeners = new ArrayList<>();
        ijkMediaPlayer = new IjkMediaPlayer();
        if (useMediacodec) {
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        }
        ijkMediaPlayer.setOnVideoSizeChangedListener(this);
        ijkMediaPlayer.setOnErrorListener(this);
        ijkMediaPlayer.setOnBufferingUpdateListener(this);
        ijkMediaPlayer.setOnInfoListener(this);
        ijkMediaPlayer.setOnPreparedListener(this);
        ijkMediaPlayer.setOnNativeInvokeListener(this);
        ijkMediaPlayer.setOnCompletionListener(this);
    }

    private void notifyError(Exception error) {
        playerError = error;
        listeners.forEach(listener -> listener.onPlayerError(error));
        maybeChangePlayerStateTo(STATE_ENDED);
    }

    @Override
    public void setDataSource(ExtDataSource dataSource) {
        this.dataSource = dataSource;
        listeners.forEach(listener -> listener.onDataSourceUsed(dataSource));
        try {
            ijkMediaPlayer.setDataSource(dataSource.getUri());
        } catch (Exception error) {
            notifyError(error);
            return;
        }
        maybeChangePlayerStateTo(STATE_PREPARING);
    }

    @Override
    public ExtDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void prepare() {
        try {
            ijkMediaPlayer.prepareAsync();
        } catch (Exception error) {
            notifyError(error);
        }
    }

    @Override
    public void play() {
        if (playerState >= STATE_PREPARED) {
            ijkMediaPlayer.start();
            maybeChangePlayerStateTo(STATE_STARTED);
        } else {
            playWhenReady = true;
        }
    }

    @Override
    public void pause() {
        ijkMediaPlayer.pause();
        maybeChangePlayerStateTo(STATE_PAUSED);
    }

    @Override
    public void stop() {
        ijkMediaPlayer.stop();
        maybeChangePlayerStateTo(STATE_STOPPED);
    }

    @Override
    public void release() {
        listeners.clear();
        ijkMediaPlayer.reset();
        ijkMediaPlayer.release();
        removeSurfaceCallbacks();
        if (ownedSurface != null) {
            ownedSurface.release();
            ownedSurface = null;
        }
        maybeChangePlayerStateTo(STATE_ENDED);
    }

    @Override
    public void seekTo(long positionMs) {
        if (playerState >= STATE_PREPARED) {
            ijkMediaPlayer.seekTo(positionMs);
        } else {
            seekToPositionMsWhenReady = positionMs;
        }
    }

    private @State int getPlaybackState(@PlayerState int state) {
        switch (state) {
            case STATE_IDLE:
            case STATE_STOPPED:
            case STATE_ENDED:
                return IExtPlayer.STATE_IDLE;
            case STATE_PREPARING:
            case STATE_PREPARED:
            case STATE_BUFFERING:
                return IExtPlayer.STATE_BUFFERING;
            case STATE_STARTED:
            case STATE_PAUSED:
                return IExtPlayer.STATE_READY;
            case STATE_COMPLETED:
                return IExtPlayer.STATE_ENDED;
        }
        return IExtPlayer.STATE_ENDED;
    }

    private void maybeChangePlayerStateTo(@PlayerState int state) {
        if (playerState == state) {
            return;
        }
        playerState = state;
        int newPlaybackState = getPlaybackState(state);
        if (newPlaybackState == playbackState) {
            return;
        }
        playbackState = newPlaybackState;
        listeners.forEach(listener -> listener.onPlaybackStateChanged(newPlaybackState));
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        ijkMediaPlayer.setSpeed(speed);
    }

    @Override
    public float getPlaybackSpeed() {
        return ijkMediaPlayer.getSpeed(.0f);
    }

    @Override
    public int getPlaybackState() {
        return playbackState;
    }

    @Override
    public Looper getApplicationLooper() {
        return context.getMainLooper();
    }

    @Override
    public double getNetworkSpeed() {
        return ijkMediaPlayer.getTcpSpeed();
    }

    @Override
    public long getCurrentPosition() {
        return ijkMediaPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return ijkMediaPlayer.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return ijkMediaPlayer.getAsyncStatisticBufForwards();
    }

    @Override
    public boolean isPlaying() {
        return ijkMediaPlayer.isPlaying();
    }

    @Override
    public Exception getPlayerError() {
        return playerError;
    }

    @Override
    public ExtVideoSize getVideoSize() {
        return new ExtVideoSize(ijkMediaPlayer.getVideoWidth(), ijkMediaPlayer.getVideoHeight());
    }

    private void setVideoSurface(Surface surface) {
        if (surface == videoOutput) {
            return;
        }
        ijkMediaPlayer.setSurface(surface);
        if (videoOutput != null && videoOutput == ownedSurface) {
            ownedSurface.release();
            ownedSurface = null;
        }
        videoOutput = surface;
    }

    private void clearVideoSurface() {
        removeSurfaceCallbacks();
        setVideoSurface(null);
    }

    @Override
    public void setVideoSurfaceView(SurfaceView surfaceView) {
        if (surfaceView == null) {
            clearVideoSurface();
            return;
        }

        removeSurfaceCallbacks();
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(componentListener);
        Surface surface = surfaceHolder.getSurface();
        if (surface != null && surface.isValid()) {
            setVideoSurface(surface);
        } else {
            setVideoSurface(null);
        }
    }

    @Override
    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        if (surfaceView == null) {
            return;
        }
        if (surfaceView.getHolder() == surfaceHolder && surfaceHolder != null) {
            clearVideoSurface();
        }
    }

    private void removeSurfaceCallbacks() {
        if (surfaceHolder != null) {
            surfaceHolder.removeCallback(componentListener);
            surfaceHolder = null;
        }
        if (textureView != null) {
            if (textureView.getSurfaceTextureListener() == componentListener) {
               textureView.setSurfaceTextureListener(null);
            }
            textureView = null;
        }
    }

    @Override
    public void setVideoTextureView(TextureView textureView) {
        if (textureView == null) {
            clearVideoSurface();
            return;
        }
        removeSurfaceCallbacks();
        this.textureView = textureView;
        textureView.setSurfaceTextureListener(componentListener);
        SurfaceTexture surfaceTexture = textureView.isAvailable() ? textureView.getSurfaceTexture() : null;
        if (surfaceTexture != null) {
            ownedSurface = new Surface(surfaceTexture);
            setVideoSurface(ownedSurface);
        } else {
           setVideoSurface(null);
        }
    }

    @Override
    public void clearVideoTextureView(TextureView textureView) {
        if (textureView != null && this.textureView == textureView) {
            clearVideoSurface();
        }
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        maybeChangePlayerStateTo(STATE_PREPARED);
        if (seekToPositionMsWhenReady > 0) {
            ijkMediaPlayer.seekTo(seekToPositionMsWhenReady);
        }
        if (playWhenReady) {
            ijkMediaPlayer.start();
            maybeChangePlayerStateTo(STATE_STARTED);
        } else {
            ijkMediaPlayer.pause();
            maybeChangePlayerStateTo(STATE_PAUSED);
        }
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        maybeChangePlayerStateTo(STATE_ENDED);
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        if (width == 0 || height == 0) {
            return;
        }
        ExtVideoSize videoSize = new ExtVideoSize(width, height);
        if (videoSize.equals(this.videoSize)) {
            return;
        }
        this.videoSize = videoSize;
        listeners.forEach(listener -> listener.onVideoSizeChanged(videoSize));
    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        notifyError(new Exception(String.format(Locale.getDefault(), "[error] what: %d, extra: %d", what, extra)));
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        Log.i("IjkMediaIExtPlayer", String.format(Locale.getDefault(), "[info] what: %d, extra: %d", what, extra));
        int state = playerState;
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                state = STATE_BUFFERING;
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                state = STATE_STARTED;
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_DECODED_START:
                IjkMediaMeta.IjkStreamMeta meta = ijkMediaPlayer.getMediaInfo().mMeta.mVideoStream;
                List<ExtTrackInfo> tracksInfo = new ArrayList<>();
                tracksInfo.add(new ExtTrackInfo(
                        ExtTrackInfo.TRACK_TYPE_VIDEO,
                        meta.mWidth,
                        meta.mHeight,
                        meta.mCodecName,
                        meta.mBitrate
                ));
                listeners.forEach(listener -> listener.onTracksSelected(tracksInfo));
                break;
        }
        maybeChangePlayerStateTo(state);
        return true;
    }

    @Override
    public boolean onNativeInvoke(int what, Bundle args) {
        return true;
    }

    private class ComponentListener implements SurfaceHolder.Callback,
            TextureView.SurfaceTextureListener{
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            setVideoSurface(holder.getSurface());
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            // Do nothing
            Log.i(TAG, String.format(Locale.getDefault(), "player: %s, surface changed, %dx%d format %d", ijkMediaPlayer, width, height, format));
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            setVideoSurface(null);
        }

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            ownedSurface = new Surface(surfaceTexture);
            setVideoSurface(ownedSurface);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            // Do nothing
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            setVideoSurface(null);
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            // Do nothing
        }
    }
}
