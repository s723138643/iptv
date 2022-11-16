package com.orion.iptv.ui.live;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelInfo;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelManager;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.epg.m51zmt.M51ZMT;
import com.orion.iptv.layout.dialog.ChannelSourceDialog;
import com.orion.iptv.ui.live.livechannelinfo.LiveChannelInfoLayout;
import com.orion.iptv.ui.live.livechannellist.LiveChannelListLayout;
import com.orion.iptv.ui.live.liveplayersetting.LivePlayerSettingLayout;
import com.orion.iptv.ui.live.liveplayersetting.SetChannelSourceUrl;
import com.orion.iptv.ui.live.liveplayersetting.SetPlayerFactory;
import com.orion.iptv.ui.live.networkspeed.NetworkSpeed;
import com.orion.iptv.ui.live.player.PlayerView;
import com.orion.iptv.misc.CancelableRunnable;
import com.orion.iptv.misc.PreferenceStore;
import com.orion.iptv.misc.SourceTypeDetector;
import com.orion.iptv.network.DownloadHelper;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.IExtPlayerFactory;
import com.orion.player.ExtTrackInfo;
import com.orion.player.ExtVideoSize;
import com.orion.player.exo.ExtExoPlayerFactory;
import com.orion.player.ijk.ExtIjkPlayerFactory;
import com.orion.player.ijk.ExtSoftIjkPlayerFactory;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;

public class LivePlayerActivity extends AppCompatActivity {

    private static final String TAG = "LivePlayer";
    protected PlayerView videoView;
    protected LiveChannelInfoLayout channelInfoLayout;
    protected LiveChannelListLayout channelListLayout;
    protected LivePlayerSettingLayout livePlayerSettingLayout;
    protected NetworkSpeed networkSpeed;
    protected IExtPlayerFactory<? extends IExtPlayer> playerFactory;
    protected IExtPlayer iExtPlayer;
    private Handler mHandler;
    private CancelableRunnable playerDelayedTask;
    private GestureDetectorCompat gestureDetector;
    private float xFlyingThreshold;
    private float yFlyingThreshold;
    private final EpgRefresher epgRefresher = new EpgRefresher();
    private DataSourceManager dataSourceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liveplayer);
        PreferenceStore.setPreferences(PreferenceManager.getDefaultSharedPreferences(this));
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

    private IExtPlayer createPlayer() {
        IExtPlayer extPlayer = playerFactory.create(this);
        extPlayer.addListener(new PlayerEventListener());
        videoView.setPlayer(extPlayer);
        networkSpeed.setPlayer(extPlayer);
        return extPlayer;
    }

    private void destroyPlayer(IExtPlayer iExtPlayer) {
        if (iExtPlayer != null) {
            iExtPlayer.release();
        }
    }

    private void postDelayedPlayerTask(int delayMillis, CancelableRunnable task) {
        if (playerDelayedTask != null) {
            playerDelayedTask.cancel();
        }
        playerDelayedTask = task;
        delayMillis = Math.max(delayMillis, 1);
        mHandler.postDelayed(task, delayMillis);
    }

    public void seekToNextMediaItem(int delayMillis) {
        if (dataSourceManager == null) {
            return;
        }
        postDelayedPlayerTask(delayMillis, new CancelableRunnable() {
            @Override
            public void callback() {
                _setMediaItem(dataSourceManager.nextDataSource());
            }
        });
    }

    public void seekToPrevMediaItem(int delayMillis) {
        if (dataSourceManager == null) {
            return;
        }
        postDelayedPlayerTask(delayMillis, new CancelableRunnable() {
            @Override
            public void callback() {
                _setMediaItem(dataSourceManager.prevDataSource());
            }
        });
    }

    private void _setMediaItem(ExtDataSource dataSource) {
        destroyPlayer(iExtPlayer);
        iExtPlayer = createPlayer();
        iExtPlayer.setDataSource(dataSource);
        iExtPlayer.prepare();
        iExtPlayer.play();
    }

    public void setMediaItem(ExtDataSource dataSource, int delayMillis) {
        postDelayedPlayerTask(delayMillis, new CancelableRunnable() {
            @Override
            public void callback() {
                _setMediaItem(dataSource);
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
        List<ExtDataSource> items = channel.toMediaItems();
        if (items.size() > 0) {
            dataSourceManager = new DataSourceManager(items);
            setMediaItem(items.get(0), 0);
            updateEpgInfo(channel.info);
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
                        mHandler.post(() -> processChannelList(response));
                    }
                },
                error -> Log.e(TAG, "got channel list failed, " + error.toString())
        );
    }

    private void getChannelSourceUrl() {
        ChannelSourceDialog dialog = new ChannelSourceDialog(this);
        dialog.setTitle("设置频道源");
        dialog.setDefaultValue(PreferenceStore.getString(SetChannelSourceUrl.settingKey, ""));
        dialog.setOnChannelSourceSubmitListener(url -> {
            Log.i(TAG, String.format(Locale.ENGLISH, "got channel resource url: %s", url));
            mHandler.post(() -> {
                PreferenceStore.setString(SetChannelSourceUrl.settingKey, url);
                fetchChannelList(url);
            });
        });
        dialog.show();
    }

    private void updateEpgInfo(ChannelInfo channel) {
        // 清除旧信息
        epgRefresher.stop();
        channelInfoLayout.setCurrentEpgProgram(getString(R.string.current_epg_program_default));
        channelInfoLayout.setNextEpgProgram(getString(R.string.next_epg_program_default));
        channelListLayout.setEpgPrograms(new EpgProgram[0], 0);
        Date today = new Date();
        M51ZMT.get(
                channel.channelName,
                today,
                programs -> {
                    if (programs.length == 0) {
                        return;
                    }
                    int i = EpgProgram.indexOfCurrentProgram(programs, today);
                    mHandler.post(() -> {
                        assert dataSourceManager != null;
                        ExtDataSource dataSource = dataSourceManager.getCurrentDataSource();
                        ChannelInfo tag = (ChannelInfo) dataSource.getTag();
                        assert tag != null;
                        if (!tag.channelName.equals(channel.channelName)) {
                            return;
                        }
                        if (i >= 0) {
                            channelListLayout.setEpgPrograms(programs, i);
                            channelInfoLayout.setCurrentEpgProgram(programs[i].content());
                        } else {
                            channelListLayout.setEpgPrograms(programs);
                        }
                        if (i + 1 < programs.length) {
                            channelInfoLayout.setNextEpgProgram(programs[i+1].content());
                        }
                        epgRefresher.start(programs, i);
                    });
                },
                err -> Log.e(TAG, "update epg for " + channel.channelName + " failed")
        );
    }

    private IExtPlayerFactory<? extends IExtPlayer> getPlayerFactory(int category) {
        if (category == 1) {
            return new ExtSoftIjkPlayerFactory();
        } else if (category == 2) {
            return new ExtExoPlayerFactory();
        } else {
            return new ExtIjkPlayerFactory();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        int playerFactoryNum = PreferenceStore.getInt(SetPlayerFactory.settingKey, 0);
        this.playerFactory = getPlayerFactory(playerFactoryNum);
        String url = PreferenceStore.getString(SetChannelSourceUrl.settingKey, "");
        if (url.equals("")) {
            getChannelSourceUrl();
        } else {
            Log.i(TAG, "use saved channel source: " + url);
            fetchChannelList(url);
        }

        channelListLayout.setOnChannelSelectedListener((channel) -> {
            List<ExtDataSource> items = channel.toMediaItems();
            if (items.size() > 0) {
                dataSourceManager = new DataSourceManager(items);
                setMediaItem(items.get(0), 0);
                PreferenceStore.setString("selected_group_name", channel.info.groupInfo.groupName);
                PreferenceStore.setInt("selected_group_number", channel.info.groupInfo.groupNumber);
                PreferenceStore.setString("selected_channel_name", channel.info.channelName);
                PreferenceStore.setInt("selected_channel_number", channel.info.channelNumber);
                updateEpgInfo(channel.info);
            }
        });

        livePlayerSettingLayout.setOnSettingChangedListener((key, originValue) -> {
            if (key.equals(SetChannelSourceUrl.settingKey)) {
                String value = (String) originValue;
                if (value.equals("")) {
                    return;
                }
                mHandler.post(() -> {
                    PreferenceStore.setString(key, value);
                    fetchChannelList(value);
                });
            } else if (key.equals(SetPlayerFactory.settingKey)) {
                this.playerFactory = getPlayerFactory((int) originValue);
                mHandler.post(()->{
                    PreferenceStore.setInt(key, (int) originValue);
                    if (iExtPlayer != null && iExtPlayer.isPlaying()) {
                        ExtDataSource dataSource = iExtPlayer.getDataSource();
                        setMediaItem(dataSource, 0);
                    }
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
    protected void onResume() {
        super.onResume();
        if (iExtPlayer != null) {
            iExtPlayer.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (iExtPlayer != null) {
            iExtPlayer.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(playerDelayedTask);
        destroyPlayer(iExtPlayer);
        PreferenceStore.commit();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (livePlayerSettingLayout.getIsVisible()) {
                livePlayerSettingLayout.setVisible(false);
            } else {
                channelListLayout.setVisibleDelayed(!channelListLayout.getIsVisible(), 0);
            }
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            channelListLayout.setVisibleDelayed(false, 0);
            livePlayerSettingLayout.setVisible(!livePlayerSettingLayout.getIsVisible());
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distX = e2.getX() - e1.getX();
            // distance is too short ignore this event
            if (Math.abs(distX) > xFlyingThreshold) {
                Log.i(TAG, String.format(Locale.ENGLISH, "scrollX event detect, dist: %.2f, direction: %.2f", distX, velocityX));
                if (distX < 0) {
                    // from right to left
                    seekToNextMediaItem(0);
                } else {
                    // from left to right
                    seekToPrevMediaItem(0);
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

    private class PlayerEventListener implements IExtPlayer.Listener {
        @Override
        public void onPlayerError(Exception error) {
            Log.e(TAG, error.toString());
            seekToNextMediaItem(1000);
        }

        @Override
        public void onPlaybackStateChanged(@IExtPlayer.State int state) {
            switch (state) {
                case IExtPlayer.STATE_READY:
                    Log.w(TAG, "IExtPlayer change state to STATE_READY");
                    if (playerDelayedTask != null) {
                        playerDelayedTask.cancel();
                    }
                    channelInfoLayout.setVisibleDelayed(false, 3000);
                    break;
                case IExtPlayer.STATE_BUFFERING:
                    Log.w(TAG, "IExtPlayer change state to STATE_BUFFERING");
                    /* TODO set timeout value via setting layout */
                    seekToNextMediaItem(15 * 1000);
                    break;
                case IExtPlayer.STATE_ENDED:
                    Log.w(TAG, "IExtPlayer change state to STATE_ENDED");
                    break;
                case IExtPlayer.STATE_IDLE:
                    Log.w(TAG, "IExtPlayer change state to STATE_IDLE");
                    break;
            }
        }

        @Override
        public void onDataSourceUsed(ExtDataSource dataSource) {
            channelInfoLayout.setVisibleDelayed(true, 0);
            mHandler.post(() -> {
                channelInfoLayout.setLinkInfo(dataSourceManager.getCursor(dataSource) + 1, dataSourceManager.getDataSourceCount());
                Log.i(TAG, "start playing " + dataSource.getUri());
                ChannelInfo tag = (ChannelInfo) dataSource.getTag();
                assert tag != null;
                channelInfoLayout.setChannelName(tag.channelName);
                channelInfoLayout.setChannelNumber(tag.channelNumber);
                channelInfoLayout.setBitrateInfo(0);
                channelInfoLayout.setCodecInfo(getString(R.string.codec_info_default));
                channelInfoLayout.setMediaInfo(getString(R.string.media_info_default));
            });
        }

        @Override
        public void onTracksSelected(List<ExtTrackInfo> tracks) {
            for (ExtTrackInfo track : tracks) {
                if (track.type != ExtTrackInfo.TRACK_TYPE_VIDEO) {
                    continue;
                }
                Log.i(TAG, String.format(Locale.ENGLISH, "selected track change to codec:%s, bitrate:%d, size: %dx%d", track.codecs, track.bitrate, track.width, track.height));
                String sizeInfo = getString(R.string.media_info_default);
                if (track.width > 0 && track.height > 0) {
                    sizeInfo = String.format(Locale.ENGLISH, "%dx%d", track.width, track.height);
                }
                final String finalSizeInfo = sizeInfo;
                mHandler.post(() -> {
                    channelInfoLayout.setBitrateInfo(track.bitrate);
                    channelInfoLayout.setCodecInfo(track.codecs);
                    channelInfoLayout.setMediaInfo(finalSizeInfo);
                });
            }
        }

        @Override
        public void onVideoSizeChanged(@NonNull ExtVideoSize videoSize) {
            Log.i(TAG, String.format(Locale.ENGLISH, "video size change to %dx%d", videoSize.width, videoSize.height));
            String sizeInfo = getString(R.string.media_info_default);
            if (videoSize.width > 0 && videoSize.height > 0) {
                sizeInfo = String.format(Locale.ENGLISH, "%dx%d", videoSize.width, videoSize.height);
            }
            final String finalSizeInfo = sizeInfo;
            mHandler.post(() -> channelInfoLayout.setMediaInfo(finalSizeInfo));
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
            channelInfoLayout.setCurrentEpgProgram(epg.content());
            next = current + 1;
            if (next >= epgs.length) {
                return;
            }
            epg = epgs[next];
            channelInfoLayout.setNextEpgProgram(epg.content());
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