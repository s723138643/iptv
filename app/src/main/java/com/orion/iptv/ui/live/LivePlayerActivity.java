package com.orion.iptv.ui.live;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.DisplayCutoutCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.exoplayer2.PlaybackException;
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
import com.orion.player.ui.NetworkSpeed;
import com.orion.player.ui.Rect;
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
    private static final int GestureInsetXdp = 48;
    private static final int GestureInsetYdp = 48;

    protected LivePlayerViewModel mViewModel;

    protected VideoView videoView;
    protected ConstraintLayout overlay;
    protected LiveChannelInfo channelInfo;
    protected LiveChannelList channelList;
    protected LivePlayerSetting playerSetting;
    protected NetworkSpeed networkSpeed;
    protected Toast toast;
    protected Buffering buffering;

    protected IExtPlayerFactory<? extends IExtPlayer> playerFactory;
    protected int surfaceType;
    protected IExtPlayer player;

    private Handler mHandler;
    private Handler mPlayerHandler;

    private GestureDetectorCompat gestureDetector;
    private float xFlyingThreshold;
    private float yFlyingThreshold;
    private final Rect gestureArea = new Rect(0, 0);
    private Rect gesturePadding = new Rect(0, 0, 0, 0);
    private Rect overlayPadding = new Rect(0, 0, 0, 0);
    private final int[] overlayLocation = new int[2];

    private String epgUrl;
    private final EpgRefresher epgRefresher = new EpgRefresher();

    private final PlayerEventListener listener = new PlayerEventListener();
    private List<Call> pendingCalls;
    private boolean needResume = false;
    private long lastPressed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liveplayer);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        mHandler = new Handler(this.getMainLooper());
        mPlayerHandler = new Handler(this.getMainLooper());
        pendingCalls = new ArrayList<>();
        mViewModel = new ViewModelProvider(this).get(LivePlayerViewModel.class);
        gestureDetector = new GestureDetectorCompat(this, new GestureListener());

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        Log.i(TAG, String.format(Locale.ENGLISH, "display size: %dx%d", metrics.widthPixels, metrics.heightPixels));
        // x flying 2cm on screen in pixel unit
        xFlyingThreshold = metrics.xdpi * 0.8f;
        // y flying 2cm on screen in pixel unit
        yFlyingThreshold = metrics.ydpi * 0.8f;

        videoView = findViewById(R.id.video_view);
        overlay = findViewById(R.id.overlay);
        buffering = findViewById(R.id.buffering);
        toast = findViewById(R.id.toast);
        networkSpeed = findViewById(R.id.network_speed);
        FragmentManager fg = getSupportFragmentManager();
        channelInfo = (LiveChannelInfo) fg.findFragmentByTag("channel_info");
        channelList = (LiveChannelList) fg.findFragmentByTag("channel_list");
        playerSetting = (LivePlayerSetting) fg.findFragmentByTag("live_player_setting");

        ConstraintLayout.LayoutParams original = (ConstraintLayout.LayoutParams) overlay.getLayoutParams();
        ViewCompat.setOnApplyWindowInsetsListener(overlay, (v, insets) -> {
            Log.w(TAG, "apply window insets...");
            Insets gesInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
            gesturePadding = new Rect(gesInsets.left, gesInsets.top, gesInsets.right, gesInsets.bottom);

            Insets sysInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int paddingH = Math.max(sysInsets.left, sysInsets.right);
            int paddingV = Math.max(sysInsets.top, sysInsets.bottom);
            Log.w(TAG, String.format(Locale.getDefault(), "paddingH: %d, paddingV: %d", paddingH, paddingV));
            DisplayCutoutCompat cutout = insets.getDisplayCutout();
            if (cutout != null) {
                paddingH = Math.max(paddingH, Math.max(cutout.getSafeInsetLeft(), cutout.getSafeInsetRight()));
                paddingV = Math.max(paddingV, Math.max(cutout.getSafeInsetTop(), cutout.getSafeInsetBottom()));
            }
            Log.w(TAG, String.format(Locale.getDefault(), "paddingH: %d, paddingV: %d", paddingH, paddingV));
            WindowInsets rootInsets = overlay.getRootWindowInsets();
            if (rootInsets != null) {
                int cornerH = 0;
                int cornerV = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RoundedCorner topLeft = rootInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT);
                    if (topLeft != null) {
                        cornerH = Math.max(cornerH, topLeft.getRadius());
                    }
                    RoundedCorner topRight = rootInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT);
                    if (topRight != null) {
                        cornerH = Math.max(cornerH, topRight.getRadius());
                    }
                    RoundedCorner bottomLeft = rootInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT);
                    if (bottomLeft != null) {
                        cornerV = Math.max(cornerV, bottomLeft.getRadius());
                    }
                    RoundedCorner bottomRight = rootInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT);
                    if (bottomRight != null) {
                        cornerV = Math.max(cornerV, bottomRight.getRadius());
                    }
                }
                int offsetH = cornerH - (int) ((float) cornerH * Math.sin(Math.toRadians(45)));
                int offsetV = cornerV - (int) ((float) cornerV * Math.sin(Math.toRadians(45)));
                paddingH = Math.max(paddingH, offsetH);
                paddingV = Math.max(paddingV, offsetV);
            }
            Log.w(TAG, String.format(Locale.getDefault(), "paddingH: %d, paddingV: %d", paddingH, paddingV));
            overlayPadding = new Rect(paddingH, paddingV, paddingH, paddingV);

            return WindowInsetsCompat.CONSUMED;
        });
        overlay.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)->{
            if (left==oldLeft && top==oldTop && right==oldRight && bottom==oldBottom) {
                return;
            }
            Log.w(TAG, String.format(Locale.getDefault(), "layout changed: (%d, %d, %d, %d)", left, top, right, bottom));
            overlay.getLocationInWindow(overlayLocation);
            Log.w(TAG, String.format(Locale.getDefault(), "overlay location: (%d, %d)", overlayLocation[0], overlayLocation[1]));
            gestureArea.reset(left, top, right, bottom);
            gestureArea.inset(gesturePadding);
            gestureArea.inset(dp2px(GestureInsetXdp), dp2px(GestureInsetYdp));
            Log.w(TAG, String.format(Locale.getDefault(), "gesture area: (%d, %d, %d, %d)", gestureArea.left, gestureArea.top, gestureArea.right, gestureArea.bottom));

            if (overlayLocation[0] <= overlayPadding.left || overlayLocation[1] <= overlayPadding.top) {
                int paddingLeft = Math.max(0, overlayPadding.left - original.leftMargin);
                int paddingTop = Math.max(0, overlayPadding.top - original.topMargin);
                int paddingRight = Math.max(0, overlayPadding.right - original.rightMargin);
                int paddingBottom = Math.max(0, overlayPadding.bottom - original.bottomMargin);
                Log.w(TAG, String.format(Locale.getDefault(), "apply padding: (%d, %d, %d, %d)", paddingLeft, paddingTop, paddingRight, paddingBottom));
                overlay.post(()-> overlay.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom));
            }
        });

        playerFactory = mViewModel.getPlayerFactory().second;
        mViewModel.observePlayerFactory(this, this::switchPlayer);
        surfaceType = mViewModel.getSurfaceType();
        mViewModel.observeSurfaceType(this, this::switchSurfaceType);
        mViewModel.observeLiveSource(this, this::switchDataSource);
        mViewModel.observeNextEpgProgram(this, nextEpgProgram -> {
            epgRefresher.stop();
            if (nextEpgProgram == null) {
                return;
            }
            epgRefresher.start(nextEpgProgram);
        });
        mViewModel.observeSettingUrl(this, this::onSettingUrl);
        epgUrl = mViewModel.getEpgUrl();
        mViewModel.observeEpgUrl(this, url -> epgUrl = url);
        mViewModel.observeCurrentChannel(this, this::onCurrentChannel);
    }

    protected void postPlayerAction(long delayMillis, Runnable r) {
        mPlayerHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.postDelayed(r, Math.max(delayMillis, 1L));
    }

    protected void maybeShowSettingUrlDialog() {
        String settingUrl = PreferenceStore.getString(LivePlayerViewModel.SettingUrlKey, "");
        if (!settingUrl.equals("")) {
            mViewModel.initSettingUrl(settingUrl);
            return;
        }
        ChannelSourceDialog dialog = new ChannelSourceDialog(this);
        dialog.setTitle(getString(R.string.set_live_channel_source));
        dialog.setOnChannelSourceSubmitListener(url -> {
            Log.i(TAG, String.format(Locale.getDefault(), "got setting url: %s", url));
            mViewModel.updateSettingUrl(url);
        });
        dialog.show();
    }

    protected float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        needResume = true;
   }

    @Override
    public void onStart() {
        super.onStart();

        buffering.hide();
        toast.hide();
        getSupportFragmentManager().beginTransaction()
                .hide(channelList)
                .hide(channelInfo)
                .hide(playerSetting)
                .commit();

        maybeShowSettingUrlDialog();
    }

    private void switchPlayer(Pair<Integer, IExtPlayerFactory<? extends IExtPlayer>> playerFactory) {
        this.playerFactory = playerFactory.second;
        Pair<Integer, DataSource> dataSource = mViewModel.getCurrentSource();
        if (dataSource != null) {
            switchDataSource(dataSource);
        }
    }

    private void switchSurfaceType(Integer surfaceType) {
        this.surfaceType = surfaceType;
        Pair<Integer, DataSource> dataSource = mViewModel.getCurrentSource();
        if (dataSource != null) {
            switchDataSource(dataSource);
        }
    }

    private void switchDataSource(Pair<Integer, DataSource> dataSource) {
        mPlayerHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            videoView.setPlayer(null);
        }
        player = playerFactory.create(this);
        player.addListener(listener);
        videoView.setSurfaceType(surfaceType);
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (!playerSetting.isViewVisible()) {
                    channelList.toggleVisibility(true);
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!channelList.isViewVisible() && !playerSetting.isViewVisible()) {
                    postPlayerAction(0, mViewModel::seekToPrevSource);
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!channelList.isViewVisible() && !playerSetting.isViewVisible()) {
                    postPlayerAction(0, mViewModel::seekToNextSource);
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!channelList.isViewVisible() && !playerSetting.isViewVisible()) {
                    postPlayerAction(0, channelList::seekToNextChannel);
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!channelList.isViewVisible() && !playerSetting.isViewVisible()) {
                    postPlayerAction(0, channelList::seekToPrevChannel);
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_MENU:
                if (!channelList.isViewVisible()) {
                    playerSetting.toggleVisibility(true);
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if (channelList.isViewVisible()) {
                    channelList.hide();
                    handled = true;
                } else if (playerSetting.isViewVisible()) {
                    playerSetting.hide();
                    handled = true;
                } else {
                    long now = SystemClock.uptimeMillis();
                    if (now - lastPressed > 1000) {
                        lastPressed = now;
                        toast.setMessage(getString(R.string.back_press_hint), 1000);
                        handled = true;
                    }
                }
                break;
        }
        return handled || super.onKeyDown(keyCode, event);
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
        } else if (needResume) {
            Pair<Integer, DataSource> dataSource = mViewModel.getCurrentSource();
            if (dataSource != null) {
                switchDataSource(dataSource);
            }
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
        for (Call call : pendingCalls) {
            call.cancel();
        }
        pendingCalls.clear();
        mPlayerHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @SuppressWarnings("deprecation")
    protected void hideSystemBars() {
        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController == null) {
            return;
        }
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void processChannelList(String response) {
        ChannelSource source = ChannelSource.from(getString(R.string.default_group_name), response);
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

    protected void onCurrentChannel(LivePlayerViewModel.Channel currentChannel) {
        if (currentChannel == null || epgUrl == null || epgUrl.isEmpty()) {
            return;
        }
        ChannelInfo info = currentChannel.channelInfo;
        Date today = new Date();
        Call call = M51ZMT.get(
                epgUrl,
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
            if (playerSetting.isViewVisible()) {
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
            if (channelList.isViewVisible()) {
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
            if (error instanceof PlaybackException) {
                PlaybackException e = (PlaybackException) error;
                if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    // seek to default position
                    player.seekTo(-1);
                    player.prepare();
                    return;
                }
            }
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
                    postPlayerAction(mViewModel.getSourceTimeout(), mViewModel::seekToNextSource);
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