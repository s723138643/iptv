package com.orion.player;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.os.Looper;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.IntDef;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;


public interface IExtPlayer {
    interface Listener {
        default void onDurationChanged(long offsetMs, long durationMs) {}
        default void onPlaybackStateChanged(@State int state) {}
        default void onPlayerError(Exception error) {}
        default void onVideoSizeChanged(ExtVideoSize videoSize) {}
        default void onDataSourceUsed(ExtDataSource dataSource) {}
        default void onTracksSelected(List<ExtTrackInfo> tracks) {}
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
    @IntDef({STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_ENDED})
    @interface State {}
    int STATE_IDLE = 1;
    int STATE_BUFFERING = 2;
    int STATE_READY = 3;
    int STATE_ENDED = 4;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
    @IntDef({REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL})
    @interface RepeatMode {}
    int REPEAT_MODE_OFF = 0;
    int REPEAT_MODE_ONE = 1;
    int REPEAT_MODE_ALL = 2;

    void setDataSource(ExtDataSource dataSource);
    ExtDataSource getDataSource();

    void prepare();
    void play();
    void pause();
    void stop();
    void release();

    void seekTo(long positionMs);

    void setPlaybackSpeed(float speed);
    float getPlaybackSpeed();

    @State
    int getPlaybackState();

    Looper getApplicationLooper();

    double getNetworkSpeed();

    long getCurrentPosition();
    long getDuration();
    long getBufferedPosition();

    boolean isPlaying();

    Exception getPlayerError();
    ExtVideoSize getVideoSize();
    void setVideoSurfaceView(SurfaceView surfaceView);
    void clearVideoSurfaceView(SurfaceView surfaceView);
    void setVideoTextureView(TextureView textureView);
    void clearVideoTextureView(TextureView textureView);
    void addListener(Listener listener);
    void removeListener(Listener listener);
}

