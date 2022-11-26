package com.orion.iptv.ui.live;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelInfo;
import com.orion.iptv.bean.ChannelSource;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.epg.m51zmt.M51ZMT;
import com.orion.iptv.layout.dialog.ChannelSourceDialog;
import com.orion.iptv.layout.live.DataSource;
import com.orion.iptv.layout.live.LiveChannelInfo;
import com.orion.iptv.layout.live.LiveChannelList;
import com.orion.iptv.layout.live.LivePlayerSetting;
import com.orion.iptv.layout.live.LivePlayerViewModel;
import com.orion.iptv.misc.SourceTypeDetector;
import com.orion.iptv.network.DownloadHelper;
import com.orion.iptv.layout.NetworkSpeed;
import com.orion.player.ui.Gesture;
import com.orion.player.ui.VideoView;
import com.orion.iptv.misc.PreferenceStore;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.IExtPlayerFactory;
import com.orion.player.ui.Buffering;
import com.orion.player.ui.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LivePlayerActivity extends AppCompatActivity {
    private static final String TAG = "LivePlayer";

    protected LivePlayerViewModel mViewModel;

    protected VideoView videoView;
    protected LiveChannelInfo channelInfo;
    protected LiveChannelList channelList;
    protected LivePlayerSetting playerSetting;
    protected NetworkSpeed networkSpeed;
    protected Toast toast;
    protected Buffering buffering;

    protected IExtPlayerFactory<? extends IExtPlayer> playerFactory;
    protected IExtPlayer player;

    private Handler mHandler;
    private Handler mPlayerHandler;

    private GestureDetectorCompat gestureDetector;
    private float xFlyingThreshold;
    private float yFlyingThreshold;
    private Gesture.Rect gestureArea;

    private final EpgRefresher epgRefresher = new EpgRefresher();

    private final PlayerEventListener listener = new PlayerEventListener();
    private List<Call> pendingCalls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liveplayer);

        mViewModel = new ViewModelProvider(this).get(LivePlayerViewModel.class);
        gestureDetector = new GestureDetectorCompat(this, new GestureListener());
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        Log.i(TAG, String.format(Locale.ENGLISH, "display size: %dx%d", metrics.widthPixels, metrics.heightPixels));
        // x flying 2cm on screen in pixel unit
        xFlyingThreshold = metrics.xdpi * 0.8f;
        // y flying 2cm on screen in pixel unit
        yFlyingThreshold = metrics.ydpi * 0.8f;

        videoView = findViewById(R.id.video_view);
        buffering = findViewById(R.id.buffering);
        toast = findViewById(R.id.toast);
        networkSpeed = findViewById(R.id.network_speed);
        FragmentManager fg = getSupportFragmentManager();
        channelInfo = (LiveChannelInfo) fg.findFragmentByTag("channel_info");
        channelList = (LiveChannelList) fg.findFragmentByTag("channel_list");
        playerSetting = (LivePlayerSetting) fg.findFragmentByTag("live_player_setting");
        pendingCalls = new ArrayList<>();
    }

    protected void postPlayerAction(long delayMillis, Runnable r) {
        mPlayerHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.postDelayed(r, Math.max(delayMillis, 1L));
    }

    protected void maybeShowSettingUrlDialog() {
        String settingUrl = PreferenceStore.getString(LivePlayerViewModel.SettingUrlKey, "");
        if (!settingUrl.equals("")) {
            return;
        }
        ChannelSourceDialog dialog = new ChannelSourceDialog(this);
        dialog.setTitle("设置频道源");
        dialog.setOnChannelSourceSubmitListener(url -> {
            Log.i(TAG, String.format(Locale.getDefault(), "got setting url: %s", url));
            mViewModel.updateSettingUrl(url);
        });
        dialog.show();
    }

    protected float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    protected void initGestureArea() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        gestureArea = new Gesture.Rect(metrics.widthPixels, metrics.heightPixels);
        gestureArea.inset(dp2px(48), dp2px(48));
    }

    @Override
    public void onStart() {
        super.onStart();

        mHandler = new Handler(this.getMainLooper());
        mPlayerHandler = new Handler(this.getMainLooper());

        buffering.hide();
        toast.hide();
        getSupportFragmentManager().beginTransaction()
                .hide(channelList)
                .hide(channelInfo)
                .hide(playerSetting)
                .commit();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, windowInsets) -> {
            initGestureArea();
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures());
            gestureArea.inset(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        maybeShowSettingUrlDialog();
        mViewModel.observePlayerFactory(this, this::switchPlayer);
        mViewModel.observeLiveSource(this, this::switchDataSource);
        mViewModel.observeNextEpgProgram(this, nextEpgProgram -> {
            epgRefresher.stop();
            if (nextEpgProgram == null) {
                return;
            }
            epgRefresher.start(nextEpgProgram);
        });
        mViewModel.observeSettingUrl(this, this::onSettingUrl);
        mViewModel.observeCurrentChannel(this, this::onCurrentChannel);
    }

    private void switchPlayer(Pair<Integer, IExtPlayerFactory<? extends IExtPlayer>> playerFactory) {
        this.playerFactory = playerFactory.second;
        Pair<Integer, DataSource> dataSource = mViewModel.getCurrentSource();
        if (dataSource != null) {
            switchDataSource(dataSource);
        }
    }

    private void switchDataSource(Pair<Integer, DataSource> dataSource) {
        mPlayerHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
        }
        player = playerFactory.create(this);
        player.addListener(listener);
        videoView.setPlayer(player);
        networkSpeed.setPlayer(player);
        channelInfo.setPlayer(player);
        player.setDataSource(dataSource.second.dataSource);
        player.prepare();
        player.play();
    }

    protected boolean inTouchArea(MotionEvent event) {
        return gestureArea.in(event.getX(), event.getY());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
        hideSystemBars();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        pendingCalls.forEach(Call::cancel);
        pendingCalls.clear();
        mPlayerHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
        }
    }

    protected void hideSystemBars() {
        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController == null) {
            return;
        }
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void processChannelList(String response) {
        ChannelSource source = ChannelSource.from("默认分组", response);
        mHandler.post(() -> {
            buffering.hide();
            mViewModel.updateChannelSource(source);
        });
    }

    protected void fetchSetting(String url, int depth) {
        if (depth > 3) {
            return;
        }
        CacheControl cacheControl = new CacheControl.Builder()
                .maxAge(3, TimeUnit.HOURS)
                .build();
        Call call = DownloadHelper.get(
                url,
                cacheControl,
                new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        pendingCalls.remove(call);
                        Log.e(TAG, "got channel list failed, " + e);
                        mHandler.post(()->{
                            buffering.hide();
                            toast.setMessage(e.toString(), 5*1000);
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        pendingCalls.remove(call);
                        String text = Objects.requireNonNull(response.body()).string();
                        if (SourceTypeDetector.isJson(text)) {
                            String liveUrl = SourceTypeDetector.getLiveUrl(text);
                            Log.i(TAG, "got live url: " + liveUrl);
                            if (liveUrl.isEmpty()) {
                                return;
                            }
                            fetchSetting(liveUrl, depth + 1);
                        } else {
                            processChannelList(text);
                        }
                    }
                }
        );
        pendingCalls.add(call);
    }

    protected void onSettingUrl(String url) {
        if (url == null || url.equals("")) {
            return;
        }
        buffering.show();
        fetchSetting(url, 1);
    }

    protected void onCurrentChannel(Pair<Pair<Integer, Integer>, ChannelInfo> currentChannel) {
        if (currentChannel == null) {
            return;
        }
        ChannelInfo info = currentChannel.second;
        Date today = new Date();
        Call call = M51ZMT.get(
                info.channelName,
                today,
                new M51ZMT.Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull Exception e) {
                        pendingCalls.remove(call);
                        Log.e(TAG, "update epg for " + info.channelName + " failed, " + e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull EpgProgram[] programs) {
                        pendingCalls.remove(call);
                        if (programs.length == 0) {
                            return;
                        }
                        mHandler.post(() -> mViewModel.updateEpgPrograms(info, today, programs));
                    }
                }
        );
        pendingCalls.add(call);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return inTouchArea(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!inTouchArea(e)) {
                return false;
            }
            if (!playerSetting.isViewHidden()) {
                playerSetting.hide();
            } else {
                channelList.toggleVisibility();
            }
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            if (!inTouchArea(e)) {
                return;
            }
            if (!channelList.isViewHidden()) {
                channelList.hide();
            } else {
                playerSetting.toggleVisibility();
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!inTouchArea(e1)) {
                return false;
            }
            float distX = e2.getX() - e1.getX();
            // distance is too short ignore this event
            if (Math.abs(distX) > xFlyingThreshold) {
                Log.i(TAG, String.format(Locale.ENGLISH, "scrollX event detect, dist: %.2f, direction: %.2f", distX, velocityX));
                if (distX < 0) {
                    // from right to left
                    postPlayerAction(0, mViewModel::seekToNextSource);
                } else {
                    // from left to right
                    postPlayerAction(0, mViewModel::seekToPrevSource);
                }
                return true;
            }
            float distY = e2.getY() - e1.getY();
            if (Math.abs(distY) > yFlyingThreshold) {
                Log.i(TAG, String.format(Locale.ENGLISH, "scrollY event detect, dist: %.2f, direction: %.2f", distY, velocityY));
                if (distY < 0) {
                    channelInfo.show(5*1000);
                } else {
                    channelInfo.hide();
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
            buffering.hide();
            toast.setMessage(error.toString(), 5*1000);
            postPlayerAction(5000, mViewModel::seekToNextSource);
        }

        @Override
        public void onDataSourceUsed(ExtDataSource dataSource) {
            toast.hide();
            channelInfo.show();
            buffering.show();
        }

        @Override
        public void onPlaybackStateChanged(@IExtPlayer.State int state) {
            switch (state) {
                case IExtPlayer.STATE_READY:
                    Log.w(TAG, "IExtPlayer change state to STATE_READY");
                    mPlayerHandler.removeCallbacksAndMessages(null);
                    buffering.hide();
                    channelInfo.hide(5*1000);
                    break;
                case IExtPlayer.STATE_BUFFERING:
                    Log.w(TAG, "IExtPlayer change state to STATE_BUFFERING");
                    buffering.show();
                    postPlayerAction(15*1000, mViewModel::seekToNextSource);
                    break;
                case IExtPlayer.STATE_ENDED:
                    Log.w(TAG, "IExtPlayer change state to STATE_ENDED");
                    buffering.hide();
                    postPlayerAction(5*1000, mViewModel::seekToNextSource);
                    break;
                case IExtPlayer.STATE_IDLE:
                    Log.w(TAG, "IExtPlayer change state to STATE_IDLE");
                    buffering.hide();
                    break;
            }
        }
    }

    private class EpgRefresher implements Runnable {
        private int nextPos;
        private ChannelInfo channel;
        private EpgProgram nextProgram;

        private long now() {
            return new Date().getTime();
        }

        @Override
        public void run() {
            if (nextProgram.start > now()) {
                mHandler.postDelayed(this, Math.max(nextProgram.start - now(), 1));
                return;
            }

            mViewModel.selectEpg(nextPos, channel);
        }

        public void start(Pair<Integer, Pair<ChannelInfo, EpgProgram>> nextProgram) {
            this.nextPos = nextProgram.first;
            this.channel = nextProgram.second.first;
            this.nextProgram = nextProgram.second.second;
            mHandler.post(this);
        }

        public void stop() {
            mHandler.removeCallbacks(this);
        }
    }
}