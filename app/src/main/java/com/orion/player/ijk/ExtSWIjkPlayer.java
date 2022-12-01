package com.orion.player.ijk;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.text.CueGroup;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtTrackInfo;
import com.orion.player.ExtVideoSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.HttpUrl;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaMeta;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class ExtSWIjkPlayer implements IExtPlayer,
        IMediaPlayer.OnErrorListener, IMediaPlayer.OnBufferingUpdateListener,
        IMediaPlayer.OnCompletionListener, IMediaPlayer.OnInfoListener,
        IMediaPlayer.OnPreparedListener, IMediaPlayer.OnVideoSizeChangedListener,
        IMediaPlayer.OnTimedTextListener, IjkMediaPlayer.OnNativeInvokeListener {

    protected static final String TAG = "ExtIjkPlayer";

    protected final IjkMediaPlayer ijkMediaPlayer;
    protected final List<Listener> listeners;
    protected final Context context;
    protected final ComponentListener componentListener;
    protected final SubtitleParser subtitleParser = new AssSubtitleParser();

    protected @State int playbackState = IExtPlayer.STATE_IDLE;
    protected boolean prepared = false;
    protected Exception playerError = null;
    protected long seekToPositionMsWhenReady = 0;
    protected boolean playWhenReady = false;
    protected ExtVideoSize videoSize;
    protected ExtDataSource dataSource;

    protected SurfaceHolder surfaceHolder;
    protected TextureView textureView;
    protected Surface ownedSurface;
    protected Surface videoOutput;

    protected long bufferedPosition = 0;

    public ExtSWIjkPlayer(Context context) {
        this.context = context;
        componentListener = new ComponentListener();
        listeners = new ArrayList<>();
        ijkMediaPlayer = new IjkMediaPlayer();
        setOptions(ijkMediaPlayer);
        ijkMediaPlayer.setOnVideoSizeChangedListener(this);
        ijkMediaPlayer.setOnErrorListener(this);
        ijkMediaPlayer.setOnBufferingUpdateListener(this);
        ijkMediaPlayer.setOnInfoListener(this);
        ijkMediaPlayer.setOnPreparedListener(this);
        ijkMediaPlayer.setOnNativeInvokeListener(this);
        ijkMediaPlayer.setOnCompletionListener(this);
        ijkMediaPlayer.setOnTimedTextListener(this);
    }

    protected void setOptions(IjkMediaPlayer player) {
        // do nothing
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "subtitle", 1);
    }

    private void notifyError(Exception error) {
        playerError = error;
        listeners.forEach(listener -> listener.onPlayerError(error));
        maybeChangePlayerStateTo(STATE_IDLE);
        listeners.forEach(listener -> listener.onIsPlayingChanged(false));
    }

    @Override
    public void setDataSource(ExtDataSource dataSource) {
        this.dataSource = dataSource;
        listeners.forEach(listener -> listener.onDataSourceUsed(dataSource));
        String url = dataSource.getUri();
        ExtDataSource.Auth auth = dataSource.getAuth();
        if (!auth.equals(ExtDataSource.NoAuth) && url.startsWith("http")) {
            HttpUrl origUrl = HttpUrl.parse(dataSource.getUri());
            if (origUrl == null) {
                String msg = String.format(Locale.getDefault(), "unsupported url: %s", dataSource.getUri());
                notifyError(new Exception(msg));
                return;
            }
            HttpUrl.Builder builder = origUrl.newBuilder();
            builder.username(auth.username);
            builder.password(auth.password);
            url = builder.build().toString();
        }
        try {
            Map<String, String> headers = dataSource.getHeaders();
            if (headers.size() > 0) {
                ijkMediaPlayer.setDataSource(url, headers);
            } else {
                ijkMediaPlayer.setDataSource(url);
            }
        } catch (Exception error) {
            notifyError(error);
        }
    }

    @Override
    public ExtDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void prepare() {
        try {
            ijkMediaPlayer.prepareAsync();
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void play() {
        try {
            if (prepared) {
                ijkMediaPlayer.start();
                listeners.forEach(listener -> listener.onIsPlayingChanged(true));
            } else {
                playWhenReady = true;
            }
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void pause() {
        try {
            if (prepared) {
                ijkMediaPlayer.pause();
                listeners.forEach(listener -> listener.onIsPlayingChanged(false));
            } else {
                playWhenReady = false;
            }
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void stop() {
        try {
            ijkMediaPlayer.stop();
            maybeChangePlayerStateTo(IExtPlayer.STATE_IDLE);
            listeners.forEach(listener -> listener.onIsPlayingChanged(false));
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void release() {
        bufferedPosition = 0;
        playWhenReady = false;
        listeners.clear();
        ijkMediaPlayer.reset();
        ijkMediaPlayer.release();
        removeSurfaceCallbacks();
        if (ownedSurface != null) {
            ownedSurface.release();
            ownedSurface = null;
        }
        maybeChangePlayerStateTo(IExtPlayer.STATE_IDLE);
        listeners.forEach(listener -> listener.onIsPlayingChanged(false));
    }

    @Override
    public void seekTo(long positionMs) {
        try {
            if (playbackState == STATE_IDLE) {
                seekToPositionMsWhenReady = positionMs;
            } else {
                ijkMediaPlayer.seekTo(positionMs);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    private void maybeChangePlayerStateTo(@State int state) {
        if (playbackState == state) {
            return;
        }
        playbackState = state;
        listeners.forEach(listener -> listener.onPlaybackStateChanged(state));
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
        return bufferedPosition;
    }

    @Override
    public boolean isPlaying() {
        return ijkMediaPlayer.isPlaying() && playbackState == STATE_READY;
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

    protected void notifyTrackChanged() {
        Bundle meta = ijkMediaPlayer.getMediaMeta();
        if (meta == null) {
            return;
        }
        List<ExtTrackInfo> tracks = getTracks(IjkMediaMeta.parse(meta));
        listeners.forEach(listener -> listener.onTracksChanged(tracks));
    }

    @Override
    public void selectTrack(@ExtTrackInfo.TrackType int trackType, int trackId, int trackIndex) {
        Log.i(TAG, "select track: " + trackId + ":" + trackIndex);
        if (trackType == ExtTrackInfo.TRACK_TYPE_AUDIO || trackType == ExtTrackInfo.TRACK_TYPE_VIDEO) {
            ijkMediaPlayer.pause();
            long position = ijkMediaPlayer.getCurrentPosition();
            ijkMediaPlayer.selectTrack(trackId);
            ijkMediaPlayer.seekTo(position);
            ijkMediaPlayer.start();
        } else {
            ijkMediaPlayer.selectTrack(trackId);
        }
        notifyTrackChanged();
    }

    @Override
    public void deselectTrack(@ExtTrackInfo.TrackType int trackType, int trackId, int trackIndex) {
        Log.i(TAG, "deselect track: " + trackId + ":" + trackIndex);
        ijkMediaPlayer.deselectTrack(trackId);
        notifyTrackChanged();
    }

    protected String makeDesc(@ExtTrackInfo.TrackType int trackType, IjkMediaMeta.IjkStreamMeta streamMeta) {
        String title = streamMeta.getString("label");
        String desc;
        switch (trackType) {
            case ExtTrackInfo.TRACK_TYPE_AUDIO:
                desc = streamMeta.mLanguage + ", " + streamMeta.getBitrateInline() + ", " + streamMeta.getSampleRateInline();
                break;
            case ExtTrackInfo.TRACK_TYPE_VIDEO:
                desc = streamMeta.mLanguage + "," + streamMeta.getResolutionInline() + ", " + streamMeta.getBitrateInline();
                break;
            case ExtTrackInfo.TRACK_TYPE_TEXT:
                desc = streamMeta.mLanguage + ", " + streamMeta.mCodecName;
                break;
            default:
                desc = streamMeta.mType + ", " + streamMeta.mCodecName;
        }
        return desc;
    }

    @NonNull
    protected List<ExtTrackInfo> getTracks(IjkMediaMeta mediaMeta) {
        List<ExtTrackInfo> extTrackInfo = new ArrayList<>();
        List<Integer> selectedTracks = new ArrayList<>();
        selectedTracks.add(ijkMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO));
        selectedTracks.add(ijkMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO));
        selectedTracks.add(ijkMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT));
        for (IjkMediaMeta.IjkStreamMeta streamMeta: mediaMeta.mStreams) {
            @ExtTrackInfo.TrackType int trackType = getTrackType(streamMeta.mType);
            if (trackType == ExtTrackInfo.TRACK_TYPE_UNKNOWN) {
                continue;
            }
            extTrackInfo.add(new ExtTrackInfo(
                    streamMeta.mIndex,
                    0,
                    trackType,
                    streamMeta.mWidth,
                    streamMeta.mHeight,
                    streamMeta.mCodecName,
                    streamMeta.mBitrate,
                    streamMeta.mSampleRate,
                    makeDesc(trackType, streamMeta),
                    selectedTracks.contains(streamMeta.mIndex)
            ));
        }
        return extTrackInfo;
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        prepared = true;
        maybeChangePlayerStateTo(STATE_BUFFERING);
        Bundle bundle = ijkMediaPlayer.getMediaMeta();
        if (bundle != null) {
            IjkMediaMeta mediaMeta = IjkMediaMeta.parse(bundle);
            List<ExtTrackInfo> extTrackInfo = getTracks(mediaMeta);
            if (!extTrackInfo.isEmpty()) {
                listeners.forEach(listener -> listener.onTracksChanged(extTrackInfo));
            }
            listeners.forEach(listener -> listener.onDurationChanged(
                    mediaMeta.mStartUS/1000,
                    mediaMeta.mDurationUS/1000));
        }
        if (seekToPositionMsWhenReady > 0) {
            ijkMediaPlayer.seekTo(seekToPositionMsWhenReady);
            seekToPositionMsWhenReady = 0;
        }
        if (playWhenReady) {
            play();
        } else {
            pause();
        }
    }

    @Override
    public void onTimedText(IMediaPlayer mp, IjkTimedText text) {
        CueGroup cueGroup = subtitleParser.parse(text.getText());
        listeners.forEach(listener -> listener.onCues(cueGroup));
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        maybeChangePlayerStateTo(STATE_ENDED);
        listeners.forEach(listener -> listener.onIsPlayingChanged(false));
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        percent = percent < 95 ? percent : 100;
        bufferedPosition = percent * mp.getDuration() / 100;
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

    protected @ExtTrackInfo.TrackType int getTrackType(String mType) {
        if (mType.equalsIgnoreCase(IjkMediaMeta.IJKM_VAL_TYPE__VIDEO)) {
            return ExtTrackInfo.TRACK_TYPE_VIDEO;
        }
        if (mType.equalsIgnoreCase(IjkMediaMeta.IJKM_VAL_TYPE__AUDIO)) {
            return ExtTrackInfo.TRACK_TYPE_AUDIO;
        }
        if (mType.equalsIgnoreCase(IjkMediaMeta.IJKM_VAL_TYPE__TIMEDTEXT)) {
            return ExtTrackInfo.TRACK_TYPE_TEXT;
        }
        return ExtTrackInfo.TRACK_TYPE_UNKNOWN;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        Log.i(TAG, String.format(Locale.getDefault(), "[info] what: %d, extra: %d", what, extra));
        int state = playbackState;
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                state = STATE_BUFFERING;
                break;
            case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
            case IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START:
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
            case IMediaPlayer.MEDIA_INFO_VIDEO_SEEK_RENDERING_START:
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                state = STATE_READY;
                break;
            default:
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
