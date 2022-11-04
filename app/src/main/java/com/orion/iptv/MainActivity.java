package com.orion.iptv;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.preference.PreferenceManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.video.VideoSize;
import com.orion.iptv.bean.ChannelInfo;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelManager;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.epg.m51zmt.M51ZMT;
import com.orion.iptv.layout.dialog.ChannelSourceDialog;
import com.orion.iptv.layout.livechannelinfo.LiveChannelInfoLayout;
import com.orion.iptv.layout.livechannellist.LiveChannelListLayout;
import com.orion.iptv.layout.liveplayersetting.LivePlayerSettingLayout;
import com.orion.iptv.layout.networkspeed.NetworkSpeed;
import com.orion.iptv.layout.player.PlayerView;
import com.orion.iptv.misc.CancelableRunnable;
import com.orion.iptv.misc.PreferenceStore;
import com.orion.iptv.misc.SourceTypeDetector;
import com.orion.iptv.network.DownloadHelper;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LivePlayer";
    protected PlayerView videoView;
    protected LiveChannelInfoLayout channelInfoLayout;
    protected LiveChannelListLayout channelListLayout;
    protected LivePlayerSettingLayout livePlayerSettingLayout;
    protected NetworkSpeed networkSpeed;
    protected @Nullable ExoPlayer player;
    private Handler mPlayerHandler;
    private Handler mHandler;
    private CancelableRunnable playerDelayedTask;
    private GestureDetectorCompat gestureDetector;
    private float xFlyingThreshold;
    private float yFlyingThreshold;
    private final EpgRefresher epgRefresher = new EpgRefresher();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceStore.setPreferences(PreferenceManager.getDefaultSharedPreferences(this));
        OkHttpClient client = new OkHttpClient.Builder()
                .cache(new Cache(this.getCacheDir(), 50 * 1024 * 1024))
                .followSslRedirects(true)
                .build();
        DownloadHelper.setClient(client);
        videoView = findViewById(R.id.videoView);
        videoView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        videoView.setErrorMessageProvider(new PlayerView.DefaultErrorMessageProvider());
        ChannelManager channelManager = new ChannelManager(this.getString(R.string.default_group_name));
        channelListLayout = new LiveChannelListLayout(this, channelManager);
        channelInfoLayout = new LiveChannelInfoLayout(this);
        livePlayerSettingLayout = new LivePlayerSettingLayout(this);
        networkSpeed = new NetworkSpeed(this);
        gestureDetector = new GestureDetectorCompat(this, new GestureListener());
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        Log.i(TAG, String.format(Locale.ENGLISH, "display size: %dx%d", metrics.widthPixels, metrics.heightPixels));
        // x flying 2cm on screen in pixel unit
        xFlyingThreshold = metrics.xdpi * 0.8f;
        // y flying 2cm on screen in pixel unit
        yFlyingThreshold = metrics.ydpi * 0.8f;
        mHandler = new Handler(this.getMainLooper());
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
        if (player == null) {
            return;
        }
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
        if (player == null) {
            return;
        }
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
                if (player == null) {
                    return;
                }
                player.setMediaItems(items);
                if (player.getPlaybackState() == Player.STATE_IDLE) {
                    player.prepare();
                }
                player.setPlayWhenReady(true);
            }
        });
    }

    private void processChannelList(String channelList) {
        ChannelManager manager = ChannelManager.from(getString(R.string.default_group_name), channelList);
        int savedGroupNumber = PreferenceStore.getInt("selected_group_number", -1);
        int savedChannelNumber = PreferenceStore.getInt("selected_channel_number", -1);
        ChannelItem channel = manager.getChannel(savedGroupNumber, savedChannelNumber)
                .map(item -> {
                    String savedGroupName = PreferenceStore.getString("selected_group_name", "");
                    String savedChannelName = PreferenceStore.getString("selected_channel_name", "");
                    if (savedChannelName.equals(item.info.channelName) && savedGroupName.equals(item.info.groupInfo.groupName)) {
                        return item;
                    }
                    return null;
                })
                .orElseGet(() -> manager.getFirst().orElse(null));
        if (channel == null) {
            return;
        }
        mHandler.post(() -> channelListLayout.resume(manager, channel.info));
        List<MediaItem> items = channel.toMediaItems();
        if (items.size() > 0) {
            setMediaItems(items, 0);
            updateEpgInfo(channel);
        }
    }

    private void fetchChannelList(String url) {
        _fetchChannelList(url, 1);
    }

    private void _fetchChannelList(String url, int depth) {
        if (depth > 3) {
            return;
        }
        CacheControl cacheControl = new CacheControl.Builder()
                .maxAge(30, TimeUnit.MINUTES)
                .build();
        DownloadHelper.get(
                url,
                cacheControl,
                response -> {
                    if (SourceTypeDetector.isJson(response)) {
                        String liveUrl = SourceTypeDetector.getLiveUrl(response);
                        Log.i(TAG, "got live url: " + liveUrl);
                        if (liveUrl.isEmpty()) {
                            return;
                        }
                        _fetchChannelList(liveUrl, depth + 1);
                    } else {
                        processChannelList(response);
                    }
                },
                error -> Log.e(TAG, "got channel list failed, " + error.toString())
        );
    }

    private void getChannelSourceUrl() {
        ChannelSourceDialog dialog = new ChannelSourceDialog(this);
        dialog.setTitle("设置频道源");
        dialog.setDefaultValue(PreferenceStore.getString("channel_source_url", ""));
        dialog.setInputHint("请输入url地址");
        dialog.setOnChannelSourceSubmitListener(url -> {
            Log.i(TAG, String.format(Locale.ENGLISH, "got channel resource url: %s", url));
            mHandler.post(() -> {
                PreferenceStore.setString("channel_source_url", url);
                fetchChannelList(url);
            });
        });
        dialog.show();
    }

    private void updateEpgInfo(ChannelItem channel) {
        // 清除旧信息
        epgRefresher.stop();
        channelInfoLayout.setCurrentEpgProgram(getString(R.string.current_epg_program_default));
        channelInfoLayout.setNextEpgProgram(getString(R.string.next_epg_program_default));
        channelListLayout.setEpgPrograms(new EpgProgram[0], 0);
        Date today = new Date();
        M51ZMT.get(
                channel.name(),
                today,
                programs -> {
                    if (programs.length == 0) {
                        return;
                    }
                    int i = EpgProgram.indexOfCurrentProgram(programs, today);
                    mHandler.post(() -> {
                        assert player != null;
                        MediaItem media = player.getCurrentMediaItem();
                        if (media == null) {
                            return;
                        }
                        assert media.localConfiguration != null && media.localConfiguration.tag != null;
                        ChannelInfo tag = (ChannelInfo) media.localConfiguration.tag;
                        if (!tag.channelName.equals(channel.info.channelName)) {
                            return;
                        }
                        if (i >= 0) {
                            channelListLayout.setEpgPrograms(programs, i);
                            channelInfoLayout.setCurrentEpgProgram(programs[i].name());
                        } else {
                            channelListLayout.setEpgPrograms(programs);
                        }
                        if (i + 1 < programs.length) {
                            channelInfoLayout.setNextEpgProgram(programs[i+1].name());
                        }
                        epgRefresher.start(programs, i);
                    });
                },
                err -> Log.e(TAG, "update epg for " + channel.name() + " failed")
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        initializePlayer();
        assert player != null;

        String url = PreferenceStore.getString("channel_source_url", "");
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
                PreferenceStore.setString("selected_group_name", channel.info.groupInfo.groupName);
                PreferenceStore.setInt("selected_group_number", channel.info.groupInfo.groupNumber);
                PreferenceStore.setString("selected_channel_name", channel.info.channelName);
                PreferenceStore.setInt("selected_channel_number", channel.info.channelNumber);
                updateEpgInfo(channel);
            }
        });

        livePlayerSettingLayout.setOnSettingChangedListener((key, originValue) -> {
            if (key.equals("channel_source_url")) {
                String value = (String) originValue;
                if (value.equals("")) {
                    return;
                }
                mHandler.post(() -> {
                    PreferenceStore.setString(key, value);
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
        releasePlayer();
        PreferenceStore.commit();
    }

    private ExoPlayer newPlayer() {
        ExoPlayer.Builder builder = new ExoPlayer.Builder(this);
        // use extension render if possible
        DefaultRenderersFactory renderFactory = new DefaultRenderersFactory(this.getApplicationContext());
        renderFactory = renderFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        builder.setRenderersFactory(renderFactory);

        OkHttpClient client = new OkHttpClient.Builder().build();
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                this,
                new OkHttpDataSource.Factory((Call.Factory) client)
        );
        dataSourceFactory.setTransferListener(networkSpeed.new SimpleTransferListener());
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);
        mediaSourceFactory.setDataSourceFactory(dataSourceFactory);
        builder.setMediaSourceFactory(mediaSourceFactory);
        return builder.build();
    }

    protected void initializePlayer() {
        releasePlayer();
        player = newPlayer();
        mPlayerHandler = new Handler(player.getApplicationLooper());
        player.addListener(new PlayerEventListener());
        player.addAnalyticsListener(new PlayerAnalyticsListener());
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        videoView.setPlayer(player);
    }

    protected void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

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
                    mHandler.post(() -> channelInfoLayout.displayAsToast(5000));
                } else {
                    mHandler.post(() -> channelInfoLayout.hide());
                }
                return true;
            }
            return false;
        }
    }

    private class PlayerEventListener implements Player.Listener {
        public PlayerEventListener() {
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            Log.e(TAG, error.toString());
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
                    seekToNextMediaItem(15 * 1000);
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
            mHandler.post(() -> {
                channelInfoLayout.setLinkInfo(player.getCurrentMediaItemIndex() + 1, player.getMediaItemCount());
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

        private Optional<Format> getSelectedVideoTrackFormat(Tracks tracks) {
            for (Tracks.Group group : tracks.getGroups()) {
                if (group.isSelected() && group.getType() == C.TRACK_TYPE_VIDEO) {
                    for (int i = 0; i < group.length; i++) {
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
            getSelectedVideoTrackFormat(tracks).map((track) -> {
                Log.i(TAG, String.format(Locale.ENGLISH, "selected track change to codec:%s, bitrate:%d, size: %dx%d", track.codecs, track.bitrate, track.width, track.height));
                mHandler.post(() -> {
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
            mHandler.post(() -> channelInfoLayout.setMediaInfo(String.format(Locale.ENGLISH, "%dx%d", videoSize.width, videoSize.height)));
        }
    }

    private class EpgRefresher implements Runnable {
        private EpgProgram[] epgs;
        private int current = -1;

        private long now() {
            return new Date().getTime();
        }

        @Override
        public void run() {
            int next = current + 1;
            if (next >= epgs.length) {
                return;
            }
            EpgProgram epg = epgs[next];
            if (epg.start > now()) {
                mHandler.postDelayed(this, Math.max(epg.start - now(), 1));
                return;
            }

            current = next;
            channelListLayout.selectEpgProgram(current);
            channelInfoLayout.setCurrentEpgProgram(epg.name());
            next = current + 1;
            if (next >= epgs.length) {
                return;
            }
            epg = epgs[next];
            channelInfoLayout.setNextEpgProgram(epg.name());
            mHandler.postDelayed(this, Math.max(epg.start - now(), 1));
        }

        public void start(EpgProgram[] epgs, int current) {
            this.epgs = epgs;
            this.current = current;
            mHandler.post(this);
        }

        public void stop() {
            mHandler.removeCallbacks(this);
            epgs = new EpgProgram[0];
            current = -1;
        }
    }
}