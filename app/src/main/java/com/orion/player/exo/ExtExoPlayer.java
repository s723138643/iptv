package com.orion.player.exo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.text.CueGroup;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.video.VideoSize;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtTrackInfo;
import com.orion.player.ExtVideoSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

public class ExtExoPlayer implements IExtPlayer {
    private static final String TAG = "ExtExoPlayer";
    private final Context context;
    private final List<Listener> listeners;
    private final SimpleTransferMonitor transferMonitor;

    protected List<Runnable> pendingOperations;

    private Handler mHandler;
    private ExoPlayer innerPlayer;
    private float playbackSpeed = 0.0f;
    private ExtDataSource dataSource;

    public ExtExoPlayer(Context context) {
        this.context = context;
        this.transferMonitor = new SimpleTransferMonitor();
        listeners = new ArrayList<>();
        pendingOperations = new ArrayList<>();
    }

    protected void initExoPlayer() {
        ExoPlayer.Builder builder = new ExoPlayer.Builder(context);
        // use extension render if possible
        DefaultRenderersFactory renderFactory = new DefaultRenderersFactory(context.getApplicationContext());
        renderFactory = renderFactory.forceEnableMediaCodecAsynchronousQueueing();
        renderFactory = renderFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        builder.setRenderersFactory(renderFactory);

        OkHttpClient client = new OkHttpClient.Builder().build();
        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory((Call.Factory) client);
        Map<String, String> headers = new ArrayMap<>();
        ExtDataSource.Auth auth = dataSource.getAuth();
        if (!auth.equals(ExtDataSource.NoAuth)) {
            headers.put("Authorization", Credentials.basic(auth.username, auth.password));
        }
        Map<String, String> origHeaders = dataSource.getHeaders();
        if (origHeaders.size() > 0) {
            headers.putAll(origHeaders);
        }
        if (headers.size() > 0) {
            okHttpDataSourceFactory.setDefaultRequestProperties(headers);
        }
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, okHttpDataSourceFactory);
        dataSourceFactory.setTransferListener(transferMonitor);
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(context);
        mediaSourceFactory.setDataSourceFactory(dataSourceFactory);
        builder.setMediaSourceFactory(mediaSourceFactory);

        innerPlayer = builder.build();
        innerPlayer.addListener(new SimpleListener());
        mHandler = new Handler(innerPlayer.getApplicationLooper());
        for (Runnable op : pendingOperations) {
            mHandler.post(op);
        }
        pendingOperations.clear();
    }

    @Override
    public void setDataSource(ExtDataSource dataSource) {
        this.dataSource = dataSource;
        MediaItem item = new MediaItem.Builder()
                .setMediaId(dataSource.getUri())
                .setUri(dataSource.getUri())
                .setRequestMetadata(MediaItem.RequestMetadata.EMPTY)
                .build();
        post(() -> innerPlayer.setMediaItem(item));
    }

    protected void post(Runnable op) {
        if (mHandler != null) {
            mHandler.post(op);
        } else {
            pendingOperations.add(op);
        }
    }

    @Override
    public ExtDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void prepare() {
        initExoPlayer();
        mHandler.post(innerPlayer::prepare);
    }

    @Override
    public void play() {
        post(innerPlayer::play);
    }

    @Override
    public void pause() {
        post(innerPlayer::pause);
    }

    @Override
    public void stop() {
        post(innerPlayer::stop);
    }

    @Override
    public void release() {
        if (mHandler != null) {
            listeners.clear();
            mHandler.removeCallbacksAndMessages(null);
            innerPlayer.release();
        } else {
            listeners.clear();
            pendingOperations.clear();
        }
    }

    @Override
    public void seekTo(long positionMs) {
        post(()-> innerPlayer.seekTo(positionMs));
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        playbackSpeed = speed;
        post(()-> innerPlayer.setPlaybackSpeed(speed));
    }

    @Override
    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    @Nullable
    @Override
    public Looper getApplicationLooper() {
        return innerPlayer != null ? innerPlayer.getApplicationLooper() : null;
    }

    @Override
    public int getPlaybackState() {
        if (innerPlayer == null) {
            return IExtPlayer.STATE_IDLE;
        }
        @State int state = IExtPlayer.STATE_IDLE;
        switch (innerPlayer.getPlaybackState()) {
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                state = IExtPlayer.STATE_BUFFERING;
                break;
            case Player.STATE_READY:
                state = IExtPlayer.STATE_READY;
                break;
            case Player.STATE_ENDED:
                state = IExtPlayer.STATE_ENDED;
                break;
        }
        return state;
    }

    @Override
    public double getNetworkSpeed() {
        return transferMonitor.getNetworkSpeed();
    }

    @Override
    public long getCurrentPosition() {
        return innerPlayer != null ? innerPlayer.getCurrentPosition() : 0;
    }

    @Override
    public long getDuration() {
        return innerPlayer != null ? innerPlayer.getDuration() : 0;
    }

    @Override
    public long getBufferedPosition() {
        return innerPlayer != null ? innerPlayer.getBufferedPosition() : 0;
    }

    @Override
    public boolean isPlaying() {
        return innerPlayer != null && innerPlayer.isPlaying();
    }

    @Override
    public Exception getPlayerError() {
        return innerPlayer != null ? innerPlayer.getPlayerError() : null;
    }

    @Override
    public ExtVideoSize getVideoSize() {
        if (innerPlayer != null) {
            VideoSize videoSize = innerPlayer.getVideoSize();
            return ExtVideoSize.of(videoSize);
        }
        return ExtVideoSize.UNKNOWN;
    }

    @Override
    public void setVideoSurfaceView(SurfaceView surfaceView) {
        post(()-> innerPlayer.setVideoSurfaceView(surfaceView));
    }

    @Override
    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        post(()-> innerPlayer.clearVideoSurfaceView(surfaceView));
    }

    @Override
    public void setVideoTextureView(TextureView textureView) {
        post(()-> innerPlayer.setVideoTextureView(textureView));
    }

    @Override
    public void clearVideoTextureView(TextureView textureView) {
        post(()-> innerPlayer.clearVideoTextureView(textureView));
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    protected @C.TrackType int getExoTrackType(@ExtTrackInfo.TrackType int type) {
        int trackType;
        switch (type) {
            case ExtTrackInfo.TRACK_TYPE_AUDIO:
                trackType = C.TRACK_TYPE_AUDIO;
                break;
            case ExtTrackInfo.TRACK_TYPE_TEXT:
                trackType = C.TRACK_TYPE_TEXT;
                break;
            case ExtTrackInfo.TRACK_TYPE_VIDEO:
                trackType = C.TRACK_TYPE_VIDEO;
                break;
            default:
                trackType = C.TRACK_TYPE_UNKNOWN;
        }
        return trackType;
    }

    protected TrackGroup getGroup(Tracks tracks, @C.TrackType int trackType, int trackId) {
        String id = String.valueOf(trackId);
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != trackType) {
                continue;
            }
            for (int i=0; i<group.length; i++) {
                Format format = group.getTrackFormat(i);
                if (id.equals(format.id)) {
                    return group.getMediaTrackGroup();
                }
            }
        }
        return null;
    }

    @Override
    public void selectTrack(@ExtTrackInfo.TrackType int type, int trackId, int trackIndex) {
        Log.i(TAG, "select track: " + trackId + ":" + trackIndex);
        @C.TrackType int exoTrackType = getExoTrackType(type);
        if (exoTrackType == C.TRACK_TYPE_UNKNOWN) {
            return;
        }
        post(()->{
            Tracks tracks = innerPlayer.getCurrentTracks();
            TrackGroup group = getGroup(tracks, exoTrackType, trackId);
            if (group == null) {
                return;
            }
            innerPlayer.setTrackSelectionParameters(innerPlayer.getTrackSelectionParameters()
                    .buildUpon()
                    .setTrackTypeDisabled(exoTrackType, false)
                    .setOverrideForType(new TrackSelectionOverride(
                            group,
                            trackIndex
                    ))
                    .build());
        });
    }

    @Override
    public void deselectTrack(@ExtTrackInfo.TrackType int type, int trackId, int trackIndex) {
        Log.i(TAG, "deselect track: " + trackId + ":" + trackIndex);
        @C.TrackType int exoTrackType = getExoTrackType(type);
        if (exoTrackType == C.TRACK_TYPE_UNKNOWN) {
            return;
        }
        post(()->{
            innerPlayer.setTrackSelectionParameters(innerPlayer.getTrackSelectionParameters()
                    .buildUpon()
                    .setTrackTypeDisabled(type, true)
                    .build());
        });
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

        @Override
        public void onCues(@NonNull CueGroup cueGroup) {
            listeners.forEach(listener -> listener.onCues(cueGroup));
        }

        @NonNull
        private String makeDesc(@ExtTrackInfo.TrackType int type, int index, Format format) {
            switch (type) {
                case ExtTrackInfo.TRACK_TYPE_VIDEO:
                    return String.format(Locale.getDefault(), "%s:%d %dx%d, %dbps", format.id, index, format.width, format.height, format.bitrate);
                case ExtTrackInfo.TRACK_TYPE_AUDIO:
                    return String.format(Locale.getDefault(), "%s:%d %s, %s, %dbps, %dHz", format.id, index, format.language, format.label, format.bitrate, format.sampleRate);
                case ExtTrackInfo.TRACK_TYPE_TEXT:
                default:
                    return String.format(Locale.getDefault(), "%s:%d %s, %s, %s", format.id, index, format.sampleMimeType, format.language, format.label);
            }
        }

        @NonNull
        private List<ExtTrackInfo> getSelectedTrackInfo(Tracks tracks) {
            List<ExtTrackInfo> tracksInfo = new ArrayList<>();
            for (Tracks.Group group : tracks.getGroups()) {
                for (int i = 0; i < group.length; i++) {
                    Format format = group.getTrackFormat(i);
                    int trackId = -1;
                    try {
                        trackId = Integer.parseInt(format.id != null ? format.id : "-1");
                    } catch (NumberFormatException ignored) {
                    }
                    if (trackId < 0) {
                        continue;
                    }
                    tracksInfo.add(new ExtTrackInfo(
                            trackId,
                            i,
                            group.getType(),
                            format.width,
                            format.height,
                            format.codecs,
                            format.bitrate,
                            format.sampleRate,
                            makeDesc(group.getType(), i, format),
                            group.isTrackSelected(i)
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
            listeners.forEach(listener -> listener.onTracksChanged(tracksInfo));
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