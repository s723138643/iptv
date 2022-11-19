package com.orion.player.exo;

import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.video.VideoSize;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtTrackInfo;
import com.orion.player.ExtVideoSize;

import java.util.ArrayList;
import java.util.List;

public class ExtExoPlayer implements IExtPlayer {
    private final Handler mHandler;
    private final ExoPlayer innerPlayer;
    private final List<Listener> listeners;
    private final SimpleTransferMonitor transferMonitor;

    private float playbackSpeed = 0.0f;
    private ExtDataSource dataSource;

    public ExtExoPlayer(ExoPlayer player, SimpleTransferMonitor transferMonitor) {
        mHandler = new Handler(player.getApplicationLooper());
        innerPlayer = player;
        this.transferMonitor = transferMonitor;
        listeners = new ArrayList<>();
        innerPlayer.addListener(new SimpleListener());
    }

    @Override
    public void setDataSource(ExtDataSource dataSource) {
        this.dataSource = dataSource;
        MediaItem item = new MediaItem.Builder()
                .setMediaId(dataSource.getUri())
                .setUri(dataSource.getUri())
                .build();
        mHandler.post(()-> innerPlayer.setMediaItem(item));
    }

    @Override
    public ExtDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void prepare() {
        mHandler.post(innerPlayer::prepare);
    }

    @Override
    public void play() {
        mHandler.post(innerPlayer::play);
    }

    @Override
    public void pause() {
        mHandler.post(innerPlayer::pause);
    }

    @Override
    public void stop() {
        mHandler.post(innerPlayer::stop);
    }

    @Override
    public void release() {
        mHandler.post(innerPlayer::release);
    }

    @Override
    public void seekTo(long positionMs) {
        mHandler.post(()-> innerPlayer.seekTo(positionMs));
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        mHandler.post(()->{
            innerPlayer.setPlaybackSpeed(speed);
            playbackSpeed = speed;
        });
    }

    @Override
    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    @Override
    public Looper getApplicationLooper() {
        return innerPlayer.getApplicationLooper();
    }

    @Override
    public int getPlaybackState() {
        switch (innerPlayer.getPlaybackState()) {
            case com.google.android.exoplayer2.Player.STATE_IDLE:
                return IExtPlayer.STATE_IDLE;
            case com.google.android.exoplayer2.Player.STATE_BUFFERING:
                return IExtPlayer.STATE_BUFFERING;
            case com.google.android.exoplayer2.Player.STATE_READY:
                return IExtPlayer.STATE_READY;
            case com.google.android.exoplayer2.Player.STATE_ENDED:
                return IExtPlayer.STATE_ENDED;
        }
        return IExtPlayer.STATE_IDLE;
    }

    @Override
    public double getNetworkSpeed() {
        return transferMonitor.getNetworkSpeed();
    }

    @Override
    public long getCurrentPosition() {
        return innerPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return innerPlayer.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return innerPlayer.getBufferedPosition();
    }

    @Override
    public boolean isPlaying() {
        return innerPlayer.isPlaying();
    }

    @Override
    public Exception getPlayerError() {
        return innerPlayer.getPlayerError();
    }

    @Override
    public ExtVideoSize getVideoSize() {
        com.google.android.exoplayer2.video.VideoSize videoSize = innerPlayer.getVideoSize();
        return ExtVideoSize.of(videoSize);
    }

    @Override
    public void setVideoSurfaceView(SurfaceView surfaceView) {
        mHandler.post(()-> innerPlayer.setVideoSurfaceView(surfaceView));
    }

    @Override
    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        mHandler.post(()-> innerPlayer.clearVideoSurfaceView(surfaceView));
    }

    @Override
    public void setVideoTextureView(TextureView textureView) {
        mHandler.post(()-> innerPlayer.setVideoTextureView(textureView));
    }

    @Override
    public void clearVideoTextureView(TextureView textureView) {
        mHandler.post(()-> innerPlayer.clearVideoTextureView(textureView));
    }

    @Override
    public void addListener(Listener listener) {
            listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
            listeners.remove(listener);
    }

    private class SimpleListener implements com.google.android.exoplayer2.Player.Listener {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            listeners.forEach(listener -> listener.onPlaybackStateChanged(playbackState));
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            listeners.forEach(listener -> listener.onIsPlayingChanged(isPlaying));
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            listeners.forEach(listener -> listener.onPlayerError(error));
        }

        @Override
        public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
            listeners.forEach(listener -> listener.onVideoSizeChanged(ExtVideoSize.of(videoSize)));
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            if (mediaItem == null || !mediaItem.mediaId.equals(dataSource.getUri())) {
                return;
            }
            listeners.forEach(listener -> listener.onDataSourceUsed(dataSource));
        }

        @NonNull
        private List<ExtTrackInfo> getSelectedTrackInfo(Tracks tracks) {
            List<ExtTrackInfo> tracksInfo = new ArrayList<>();
            for (Tracks.Group group : tracks.getGroups()) {
                if (!group.isSelected()) {
                    continue;
                }
                for (int i = 0; i < group.length; i++) {
                    if (!group.isTrackSelected(i)) {
                        continue;
                    }
                    Format format = group.getTrackFormat(i);
                    tracksInfo.add(new ExtTrackInfo(
                            group.getType(), format.width, format.height,
                            format.codecs, format.bitrate
                    ));
                }
            }
            return tracksInfo;
        }

        @Override
        public void onTracksChanged(@NonNull Tracks tracks) {
            List<ExtTrackInfo> tracksInfo = getSelectedTrackInfo(tracks);
            if (tracksInfo.size() == 0) {
                return;
            }
            listeners.forEach(listener -> listener.onTracksSelected(tracksInfo));
        }

        @Override
        public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
            listeners.forEach(listener -> listener.onDurationChanged(innerPlayer.getCurrentPosition(), innerPlayer.getDuration()));
        }

        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
            listeners.forEach(listener -> listener.onDurationChanged(newPosition.positionMs, innerPlayer.getDuration()));
        }
    }
}