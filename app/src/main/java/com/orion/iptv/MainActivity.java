package com.orion.iptv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.video.VideoSize;
import com.orion.iptv.bean.ChannelInfo;
import com.orion.iptv.layout.liveplayersetting.LivePlayerSettingLayout;
import com.orion.iptv.layout.player.PlayerView;
import com.orion.iptv.misc.PreferenceStore;
import com.orion.iptv.network.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelManager;
import com.orion.iptv.layout.livechannelinfo.LiveChannelInfoLayout;
import com.orion.iptv.layout.livechannellist.LiveChannelListLayout;
import com.orion.iptv.layout.bandwidth.Bandwidth;
import com.orion.iptv.misc.CancelableRunnable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MainActivity extends AppCompatActivity {

    private static final String TAG  = "LivePlayer";
    protected PlayerView videoView;
    protected LiveChannelInfoLayout channelInfoLayout;
    protected LiveChannelListLayout channelListLayout;
    protected LivePlayerSettingLayout livePlayerSettingLayout;
    protected Bandwidth bandwidth;
    protected @Nullable ExoPlayer player;
    private RequestQueue reqQueue;
    private Handler mPlayerHandler;
    private CancelableRunnable playerDelayedTask;
    private GestureDetectorCompat gestureDetector;
    private PreferenceStore preferenceStore;
    private float xFlyingThreshold;
    private float yFlyingThreshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        reqQueue = Volley.newRequestQueue(this);
        videoView = findViewById(R.id.videoView);
        videoView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        ChannelManager channelManager = new ChannelManager(this.getString(R.string.default_group_name));
        channelListLayout = new LiveChannelListLayout(this, channelManager);
        channelInfoLayout = new LiveChannelInfoLayout(this);
        livePlayerSettingLayout = new LivePlayerSettingLayout(this);
        bandwidth = new Bandwidth(this);
        gestureDetector = new GestureDetectorCompat(this, new GestureListener());
        preferenceStore = new PreferenceStore(this);
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        // x flying 2cm on screen in pixel unit
        xFlyingThreshold = metrics.xdpi * 0.8f;
        // y flying 2cm on screen in pixel unit
        yFlyingThreshold = metrics.ydpi * 0.8f;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent e) { return true; }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            channelListLayout.setVisibleDelayed(!channelListLayout.getIsVisible(), 0);
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            livePlayerSettingLayout.setVisible(!livePlayerSettingLayout.getIsVisible());
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distX = e2.getX() - e1.getX();
            // distance is too short ignore this event
            if (Math.abs(distX) > xFlyingThreshold) {
                Log.i(TAG, String.format(Locale.ENGLISH, "scrollX event detect, dist: %.2f, direction: %.2f", distX, velocityX));
                if (player != null && player.getMediaItemCount() > 0) {
                    if (distX < 0) {
                        // from right to left
                        seekToNextMediaItem(0);
                    } else {
                        // from left to right
                        seekToPrevMediaItem(0);
                    }
                }
                return true;
            }
            float distY = e2.getY() - e1.getY();
            if (Math.abs(distY) > yFlyingThreshold) {
                Log.i(TAG, String.format(Locale.ENGLISH, "scrollY event detect, dist: %.2f, direction: %.2f", distY, velocityY));
                if (distY < 0) {
                    channelInfoLayout.displayAsToast(5000);
                } else {
                    channelInfoLayout.hide();
                }
                return true;
            }
            return false;
        }
    }

    private void postDelayedPlayerTask(int delayMillis, CancelableRunnable task) {
        if (playerDelayedTask != null) {
            playerDelayedTask.cancel();
        }
        playerDelayedTask = task;
        delayMillis = Math.max(delayMillis, 1);
        mPlayerHandler.postDelayed(task, delayMillis);
    }

    private void _seekToNextMediaItem() {
        if (player == null) { return; }
        player.seekToNextMediaItem();
        if (player.getPlaybackState() == Player.STATE_IDLE) {
            player.prepare();
        }
        player.setPlayWhenReady(true);
    }

    public void seekToNextMediaItem(int delayMillis) {
        postDelayedPlayerTask(delayMillis, new CancelableRunnable() {
            @Override
            public void callback() {
                _seekToNextMediaItem();
            }
        });
    }

    private void _seekToPrevMediaItem() {
        if (player == null) { return; }
        player.seekToPreviousMediaItem();
        if (player.getPlaybackState() == Player.STATE_IDLE) {
            player.prepare();
        }
        player.setPlayWhenReady(true);
    }

    public void seekToPrevMediaItem(int delayMillis) {
        postDelayedPlayerTask(delayMillis, new CancelableRunnable() {
            @Override
            public void callback() {
                _seekToPrevMediaItem();
            }
        });
    }

    public void setMediaItems(List<MediaItem> items, int delayMillis) {
        postDelayedPlayerTask(delayMillis, new CancelableRunnable() {
            @Override
            public void callback() {
                if (player == null) { return; }
                player.setMediaItems(items);
                if (player.getPlaybackState() == Player.STATE_IDLE) {
                    player.prepare();
                }
                player.setPlayWhenReady(true);
            }
        });
    }

    private class PlayerEventListener implements Player.Listener {
        public PlayerEventListener() {}

        @Override
        public void onPlayerError(PlaybackException error) {
            Log.e(TAG, error.toString());
            videoView.setCustomErrorMessage(error.toString());
            seekToNextMediaItem(1000);
        }

        @Override
        public void onPlaybackStateChanged(@Player.State int state) {
            switch (state) {
                case Player.STATE_READY:
                    Log.w(TAG, "player change state to STATE_READY");
                    if (playerDelayedTask != null) {
                        playerDelayedTask.cancel();
                    }
                    channelInfoLayout.setVisibleDelayed(false, 3000);
                    break;
                case Player.STATE_BUFFERING:
                    Log.w(TAG, "player change state to STATE_BUFFERING");
                    /* TODO set timeout value via setting layout */
                    seekToNextMediaItem(15*1000);
                    break;
                case Player.STATE_ENDED:
                    Log.w(TAG, "player change state to STATE_ENDED");
                    break;
                case Player.STATE_IDLE:
                    Log.w(TAG, "player change state to STATE_IDLE");
                    break;
            }
        }
    }

    private class PlayerAnalyticsListener implements AnalyticsListener {
        @Override
        public void onMediaItemTransition(@NonNull EventTime eventTime, @Nullable MediaItem mediaItem, int reason) {
            assert player != null;
            channelInfoLayout.setVisibleDelayed(true, 0);
            mPlayerHandler.post(()->{
                channelInfoLayout.setLinkInfo(player.getCurrentMediaItemIndex()+1, player.getMediaItemCount());
                if (mediaItem == null || mediaItem.localConfiguration == null) {
                    return;
                }
                assert mediaItem.localConfiguration.tag != null;
                Log.i(TAG, "start playing " + mediaItem.localConfiguration.uri);
                ChannelInfo tag = (ChannelInfo) mediaItem.localConfiguration.tag;
                channelInfoLayout.setChannelName(tag.channelName);
                channelInfoLayout.setChannelNumber(tag.channelNumber);
                channelInfoLayout.setBitrateInfo(0);
                channelInfoLayout.setCodecInfo(getString(R.string.codec_info_default));
                channelInfoLayout.setMediaInfo(getString(R.string.media_info_default));
            });
        }

        @Override
        public void onBandwidthEstimate(@NonNull EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
            Log.i(TAG, String.format(Locale.ENGLISH, "bitrate: %d", bitrateEstimate));
            mPlayerHandler.post(()->bandwidth.setBandwidth(bitrateEstimate));
        }

        private Optional<Format> getSelectedVideoTrackFormat(Tracks tracks) {
            for (Tracks.Group group : tracks.getGroups()) {
                if (group.isSelected() && group.getType() == C.TRACK_TYPE_VIDEO) {
                    for (int i=0; i<group.length; i++) {
                        if (group.isTrackSelected(i)) {
                            return Optional.of(group.getTrackFormat(i));
                        }
                    }
                }
            }
            return Optional.empty();
        }

        @Override
        public void onTracksChanged(@NonNull EventTime eventTime, @NonNull Tracks tracks) {
            getSelectedVideoTrackFormat(tracks).map((track)->{
                Log.i(TAG, String.format(Locale.ENGLISH, "selected track change to codec:%s, bitrate:%d, size: %dx%d", track.codecs, track.bitrate, track.width, track.height));
                mPlayerHandler.post(()->{
                    channelInfoLayout.setBitrateInfo(track.bitrate);
                    channelInfoLayout.setCodecInfo(track.codecs);
                    channelInfoLayout.setMediaInfo(String.format(Locale.ENGLISH, "%dx%d", track.width, track.height));
                });
                return null;
            });
        }

        @Override
        public void onVideoSizeChanged(@NonNull EventTime eventTime, @NonNull VideoSize videoSize) {
            Log.i(TAG, String.format(Locale.ENGLISH, "video size change to %dx%d", videoSize.width, videoSize.height));
            mPlayerHandler.post(()-> channelInfoLayout.setMediaInfo(String.format(Locale.ENGLISH, "%dx%d", videoSize.width, videoSize.height)));
        }
    }

    private void fetchChannelList(String url) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            ChannelManager manager = ChannelManager.from(getString(R.string.default_group_name), response);
            int savedGroupNumber = this.preferenceStore.getInt("selected_group_number", -1);
            int savedChannelNumber = this.preferenceStore.getInt("selected_channel_number", -1);
            ChannelItem channel = manager.getChannel(savedGroupNumber, savedChannelNumber)
                    .map((item)->{
                        String savedGroupName = this.preferenceStore.getString("selected_group_name", "");
                        String savedChannelName = this.preferenceStore.getString("selected_channel_name", "");
                        if (savedChannelName.equals(item.info.channelName) && savedGroupName.equals(item.info.groupInfo.groupName)) {
                            return item;
                        }
                        return null;
                    })
                    .orElseGet(() -> manager.getFirst().orElse(null));
            if (channel == null) { return; }
            mPlayerHandler.post(()->channelListLayout.resume(manager, channel.info));
            List<MediaItem> items = channel.toMediaItems();
            if (items.size() > 0 ) {
                setMediaItems(items, 0);
            }
        }, error -> Log.e(TAG, "got channel list failed, " + error.toString()));
        reqQueue.add(stringRequest.setTag(TAG));
    }

    private void getChannelSourceUrl() {
        AlertDialog.Builder builder =  new AlertDialog.Builder(this);
        builder = builder.setTitle("setting channel source");
        builder = builder.setView(R.layout.channel_source_dialog);
        builder = builder.setPositiveButton("ok", (dialog, which) -> {
            AlertDialog alertDialog = (AlertDialog)dialog;
            EditText text = alertDialog.findViewById(R.id.channel_source_url);
            if (text == null) {
                return;
            }
            String input = text.getText().toString();
            Log.i(TAG, String.format(Locale.ENGLISH, "got channel resource url: %s", input));
            if (input.equals("")) {
                return;
            }
            mPlayerHandler.post(()->{
                preferenceStore.setString("channel_source_url", input);
                fetchChannelList(input);
            });
        });
        builder.create().show();
    }

    @Override
    public void onStart() {
        super.onStart();
        initializePlayer();
        assert player != null;

        String url = this.preferenceStore.getString("channel_source_url", "");
        if (url.equals("")) {
            getChannelSourceUrl();
        } else {
            Log.i(TAG, "use saved channel source: " + url);
            fetchChannelList(url);
        }

        channelListLayout.setOnChannelSelectedListener((channel) -> {
            List<MediaItem> items = channel.toMediaItems();
            if (items.size() > 0) {
                setMediaItems(items, 0);
                preferenceStore.setString("selected_group_name", channel.info.groupInfo.groupName);
                preferenceStore.setInt("selected_group_number", channel.info.groupInfo.groupNumber);
                preferenceStore.setString("selected_channel_name", channel.info.channelName);
                preferenceStore.setInt("selected_channel_number", channel.info.channelNumber);
            }
        });

        livePlayerSettingLayout.setOnSettingChangedListener((key, originValue)->{
            if (key.equals("channel_source_url")) {
                String value = (String)originValue;
                if (value.equals("")) { return; }
                mPlayerHandler.post(()->{
                   preferenceStore.setString(key, value);
                   fetchChannelList(value);
                });
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
        reqQueue.cancelAll(TAG);
        releasePlayer();
        preferenceStore.commit();
    }

    private ExoPlayer newPlayer() {
        ExoPlayer.Builder builder = new ExoPlayer.Builder(this);
        // use extension render if possible
        DefaultRenderersFactory renderFactory = new DefaultRenderersFactory(this.getApplicationContext());
        renderFactory = renderFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        builder.setRenderersFactory(renderFactory);
        return builder.build();
    }

    protected void initializePlayer() {
        releasePlayer();
        player = newPlayer();
        mPlayerHandler = new Handler(getMainLooper());
        player.addListener(new PlayerEventListener());
        player.addAnalyticsListener(new PlayerAnalyticsListener());
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        videoView.setPlayer(player);
        videoView.setAspectRatioListener((targetAspectRatio, naturalAspectRatio, aspectRatioMismatch)-> Log.i(TAG, String.format(Locale.ENGLISH, "aspectRatio changed, target: %.2f, natural: %.2f, mismatch: %b", targetAspectRatio, naturalAspectRatio, aspectRatioMismatch)));
    }

    protected void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}