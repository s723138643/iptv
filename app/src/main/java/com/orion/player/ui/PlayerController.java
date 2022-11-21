package com.orion.player.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;

import java.util.Locale;

public class PlayerController extends Fragment {
    @SuppressWarnings("unused")
    private static final String TAG = "PlayerController";
    private static final int playIconRes = com.google.android.exoplayer2.ui.R.drawable.exo_icon_play;
    private static final int pauseIconRes = com.google.android.exoplayer2.ui.R.drawable.exo_icon_pause;

    private TextView position;
    private TextView duration;
    private SeekBar seekBar;
    private ImageButton seekToPrevButton;
    private ImageButton playButton;
    private ImageButton seekToNextButton;
    private @Nullable IExtPlayer player;

    private Handler mHandler;

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

            mHandler.postDelayed(this, 40);
        }
    };
    private static final long AutoHideAfterMillis = 5*1000;
    private long hideMyselfAt = 0;
    private final Runnable hideMyself = new Runnable() {
        @Override
        public void run() {
            if (SystemClock.uptimeMillis() >= hideMyselfAt) {
                hide();
            } else {
                mHandler.postAtTime(this, hideMyselfAt);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player_controller, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHandler = new Handler(requireContext().getMainLooper());
        AutoHide autoHide = (AutoHide) view;
        autoHide.addEventListener(new AutoHide.EventListener() {
            @Override
            public void onMotionEvent(MotionEvent ev) {
                hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            }

            @Override
            public void onKeyEvent(KeyEvent ev) {
                hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            }
        });

        position = view.findViewById(R.id.position);
        duration = view.findViewById(R.id.media_duration);
        seekBar = view.findViewById(R.id.seek_bar);
        seekToPrevButton = view.findViewById(R.id.prev);
        seekToNextButton = view.findViewById(R.id.next);
        playButton = view.findViewById(R.id.play_or_pause);
    }

    @Override
    public void onStart() {
        super.onStart();

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
                    mHandler.removeCallbacks(updatePosition);
                    mHandler.post(updatePosition);
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
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        mHandler.removeCallbacks(hideMyself);
        if (hidden) {
            mHandler.removeCallbacks(updatePosition);
        } else{
            hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            mHandler.postAtTime(hideMyself, hideMyselfAt);
            if (player != null && player.getPlaybackState() > IExtPlayer.STATE_IDLE)
            {
                mHandler.post(updatePosition);
            }
        }
    }

    public void setPlayer(IExtPlayer player) {
        if (this.player != null) {
            this.player.removeListener(componentListener);
            mHandler.removeCallbacks(updatePosition);
        }
        this.player = player;
        this.player.addListener(componentListener);
    }

    public void toggleVisibility() {
        if (isHidden()) {
            show();
        } else {
            hide();
        }
    }

    public void show() {
        getParentFragmentManager().beginTransaction()
                .show(this)
                .commit();
    }

    public void hide() {
        getParentFragmentManager().beginTransaction()
                .hide(this)
                .commit();
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
            mHandler.removeCallbacks(updatePosition);
            mHandler.post(updatePosition);
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
                    mHandler.removeCallbacks(updatePosition);
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
}