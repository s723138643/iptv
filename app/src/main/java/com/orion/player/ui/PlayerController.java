package com.orion.player.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.R;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;

import java.util.Locale;

public class PlayerController extends Fragment {
    private static final String TAG = "PlayerController";

    private TextView position;
    private TextView duration;
    private SeekBar seekBar;
    private ImageButton seekToPrevButton;
    private ImageButton playButton;
    private ImageButton pauseButton;
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

            mHandler.postDelayed(this, 30);
        }
    };
    private final Runnable hideMyself = this::hide;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mHandler = new Handler(requireContext().getMainLooper());
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_player_controller, container, false);
        position = view.findViewById(R.id.position);
        duration = view.findViewById(R.id.media_duration);
        seekBar = view.findViewById(R.id.seek_bar);
        seekToPrevButton = view.findViewById(R.id.prev);
        seekToNextButton = view.findViewById(R.id.next);
        playButton = view.findViewById(R.id.play);
        pauseButton = view.findViewById(R.id.pause);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        seekBar.setMin(0);
        playButton.setOnClickListener(button -> {
            if (player != null && !player.isPlaying()) {
                if (player.getPlaybackState() == IExtPlayer.STATE_ENDED) {
                    player.seekTo(0);
                    mHandler.removeCallbacks(updatePosition);
                    mHandler.post(updatePosition);
                }
                player.play();
            }
        });

        pauseButton.setOnClickListener(button -> {
            if (player != null && player.isPlaying()) {
                player.pause();
            }
        });

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
        if (hidden) {
            mHandler.removeCallbacks(updatePosition);
        } else if (player != null && player.getPlaybackState() > IExtPlayer.STATE_IDLE) {
            mHandler.post(updatePosition);
        }
    }

    public void setPlayer(IExtPlayer player) {
        if (this.player != null) {
            this.player.removeListener(componentListener);
            mHandler.removeCallbacks(updatePosition);
        }
        pauseButton.setVisibility(View.GONE);
        playButton.setVisibility(View.VISIBLE);
        this.player = player;
        this.player.addListener(componentListener);
    }

    public void show() {
        mHandler.removeCallbacks(hideMyself);
        if (!isHidden()) {
            return;
        }
        getParentFragmentManager().beginTransaction()
                .show(this)
                .commit();
    }

    public void show(long displayMillis) {
        show();
        mHandler.postDelayed(hideMyself, displayMillis);
    }

    public void hide() {
        if (isHidden()) {
            return;
        }
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
        duration = duration / 60;
        d[0] = (int) duration;
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
            if (isPlaying) {
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
            } else {
                pauseButton.setVisibility(View.GONE);
                playButton.setVisibility(View.VISIBLE);
            }
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
        public void onPlayerError(Exception error) {
            pauseButton.setVisibility(View.GONE);
            playButton.setVisibility(View.VISIBLE);
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