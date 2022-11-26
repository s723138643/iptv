package com.orion.player.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;

import java.util.Locale;

public class PlayerController extends FrameLayout {
    private static final String TAG = "PlayerController";
    private static final int playIconRes = com.google.android.exoplayer2.ui.R.drawable.exo_icon_play;
    private static final int pauseIconRes = com.google.android.exoplayer2.ui.R.drawable.exo_icon_pause;
    private static final int fullscreenIconRes = com.google.android.exoplayer2.ui.R.drawable.exo_ic_fullscreen_enter;
    private static final int fullscreenExitIconRes = com.google.android.exoplayer2.ui.R.drawable.exo_ic_fullscreen_exit;

    private TextView title;
    private TextView position;
    private TextView duration;
    private SeekBar seekBar;
    private ImageButton seekToPrevButton;
    private ImageButton playButton;
    private ImageButton seekToNextButton;
    private ImageButton fullscreenButton;

    private @Nullable IExtPlayer player;
    private int orientation;
    private ViewGroup.MarginLayoutParams originMargin;
    private OrientationSwitchCallback orientationSwitchCallback;
    private OnVisibilityChangedListener listener;

    private final IExtPlayer.Listener componentListener = new ComponentListener();
    private final Runnable updatePosition = new Runnable() {
        @Override
        public void run() {
            long buffered = player.getBufferedPosition();
            long current = player.getCurrentPosition();
            buffered = buffered >= 0 ? buffered : 0;
            current = current >= 0 ? current : 0;

            position.setText(formatDuration(current));
            seekBar.setSecondaryProgress((int) (buffered / 1000));
            seekBar.setProgress((int) (current / 1000));

            postDelayed(this, 40);
        }
    };
    private static final long AutoHideAfterMillis = 5*1000;
    private long hideMyselfAt = 0;
    private final Runnable hideMyself = new Runnable() {
        @Override
        public void run() {
            long diff = SystemClock.uptimeMillis() - hideMyselfAt;
            if (diff >= 0) {
                hide();
            } else {
                postDelayed(this, -diff);
            }
        }
    };

    public PlayerController(@NonNull Context context) {
        this(context, null);
    }

    public PlayerController(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PlayerController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.fragment_player_controller, this, true);
        initView();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void initView() {
        position = findViewById(R.id.position);
        duration = findViewById(R.id.media_duration);
        seekBar = findViewById(R.id.seek_bar);
        seekToPrevButton = findViewById(R.id.prev);
        seekToNextButton = findViewById(R.id.next);
        playButton = findViewById(R.id.play_or_pause);
        fullscreenButton = findViewById(R.id.fullscreen);

        orientation = getResources().getConfiguration().orientation;
        int resId = orientation == Configuration.ORIENTATION_PORTRAIT ? fullscreenIconRes : fullscreenExitIconRes;
        fullscreenButton.setImageResource(resId);

        seekBar.setMin(0);
        playButton.setOnClickListener(button -> {
            if (player == null) {
                return;
            }
            if (player.isPlaying() || player.getPlaybackState() == IExtPlayer.STATE_BUFFERING) {
                player.pause();
            } else {
                if (player.getPlaybackState() == IExtPlayer.STATE_ENDED) {
                    player.seekTo(0);
                    removeCallbacks(updatePosition);
                    post(updatePosition);
                    if (!player.isPlaying()) {
                        player.play();
                    }
                }
                player.play();
            }
        });

        seekToPrevButton.setOnClickListener(button -> {});

        seekToNextButton.setOnClickListener(button -> {});

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int progress = -1;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    this.progress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (progress >= 0 && player != null) {
                    player.seekTo(progress * 1000L);
                    if (!player.isPlaying()) {
                        player.play();
                    }
                }
            }
        });

        fullscreenButton.setOnClickListener(button -> {
            if (orientationSwitchCallback == null) {
                return;
            }
            orientationSwitchCallback.switchOrientation();
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == orientation) {
            return;
        }
        orientation = newConfig.orientation;
        int resId = orientation == Configuration.ORIENTATION_PORTRAIT ? fullscreenIconRes : fullscreenExitIconRes;
        fullscreenButton.setImageResource(resId);
    }

    protected void initState(IExtPlayer player) {
        long currentPosition = player.getCurrentPosition();
        long currentDuration = player.getDuration();
        currentPosition = currentPosition >= 0 ? currentPosition : 0;
        currentDuration = currentDuration >= 0 ? currentDuration : 0;
        position.setText(formatDuration(currentPosition));
        duration.setText(formatDuration(currentDuration));
        seekBar.setMax((int) (currentDuration / 1000));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onVisibilityChanged(View view, int visibility) {
        post(() -> {
            if (listener != null) {
                listener.onVisibilityChanged(visibility);
            }
        });
        removeCallbacks(hideMyself);
        if (visibility == View.GONE) {
            removeCallbacks(updatePosition);
            return;
        }
        hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
        postDelayed(hideMyself, AutoHideAfterMillis);
        if (player != null) {
            initState(player);
            post(updatePosition);
        }
    }

    public void setPlayer(IExtPlayer player) {
        removeCallbacks(updatePosition);
        if (this.player != null) {
            this.player.removeListener(componentListener);
        }
        this.player = player;
        player.addListener(componentListener);
        initState(player);
        @IExtPlayer.State int state = player.getPlaybackState();
        if (state == IExtPlayer.STATE_BUFFERING || state == IExtPlayer.STATE_READY) {
            post(updatePosition);
        }
    }

    public void toggleVisibility() {
        if (getVisibility() == View.GONE) {
            show();
        } else {
            hide();
        }
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    public void setOrientationCallback(OrientationSwitchCallback callback) {
        orientationSwitchCallback = callback;
    }

    public void setOnVisibilityChangedListener(OnVisibilityChangedListener listener) {
        this.listener = listener;
    }

    private String formatDuration(long duration) {
        int[] d = new int[]{0, 0, 0};

        duration = duration / 1000;
        d[2] = (int) (duration % 60);
        duration = duration / 60;
        d[1] = (int) (duration % 60);
        d[0] = (int) (duration / 60);
        return String.format(Locale.getDefault(), "%d:%02d:%02d", d[0], d[1], d[2]);
    }

    private class ComponentListener implements IExtPlayer.Listener {
        @Override
        public void onDataSourceUsed(ExtDataSource dataSource) {
            removeCallbacks(updatePosition);
            post(updatePosition);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            assert player != null;
            int res = playIconRes;
            if (isPlaying || player.getPlaybackState() == IExtPlayer.STATE_BUFFERING) {
                res = pauseIconRes;
            }
            playButton.setImageResource(res);
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            switch (state) {
                case IExtPlayer.STATE_IDLE:
                case IExtPlayer.STATE_ENDED:
                    removeCallbacks(updatePosition);
                    break;
                case IExtPlayer.STATE_BUFFERING:
                case IExtPlayer.STATE_READY:
                    break;
            }
        }

        @Override
        public void onDurationChanged(long offsetMs, long durationMs) {
            offsetMs = offsetMs >= 0 ? offsetMs : 0;
            durationMs = durationMs >= 0 ? durationMs : 0;

            seekBar.setMax((int) (durationMs / 1000));
            seekBar.setProgress((int) (offsetMs / 1000));
            position.setText(formatDuration(offsetMs));
            duration.setText(formatDuration(durationMs));
        }
    }

    public interface OrientationSwitchCallback {
        void switchOrientation();
    }

    public interface OnVisibilityChangedListener {
        void onVisibilityChanged(int visibility);
    }
}