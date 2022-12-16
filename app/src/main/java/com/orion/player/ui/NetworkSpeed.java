package com.orion.player.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orion.iptv.R;
import com.orion.player.IExtPlayer;

import java.util.Locale;

public class NetworkSpeed extends FrameLayout {
    private static final String TAG = "NetworkSpeed";
    public static final String[] units = {"B/s", "KiB/s", "MiB/s", "GiB/s"};
    private TextView view;
    private IExtPlayer iExtPlayer;
    private long updateInterval = 500;
    private Runnable updater ;

    public NetworkSpeed(@NonNull Context context) {
        this(context, null);
    }

    public NetworkSpeed(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkSpeed(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NetworkSpeed(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.fragment_network_speed, this, true);
        initView();
    }

    public void initView() {
        view = findViewById(R.id.network_text);
        updater = new Runnable() {
            private double speed = -0.01f;

            @Override
            public void run() {
                // unit: Byte/s
                double newSpeed = iExtPlayer.getNetworkSpeed();
                if (speed != newSpeed) {
                    speed = newSpeed;
                    view.setText(NetworkSpeed.format(newSpeed));
                }
                postDelayed(this, updateInterval);
            }
        };
    }

    public static String format(double speed) {
        final double base = 1024.0f;
        int i = 0;
        for (; i < units.length; i++) {
            if (speed < 1000) {
                break;
            }
            speed /= base;
        }
        i = i >= units.length ? units.length - 1 : i;
        return String.format(Locale.ENGLISH, "%.2f%s", speed, units[i]);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        removeCallbacks(updater);
        if (visibility == View.VISIBLE && iExtPlayer != null) {
            post(updater);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(updater);
    }

    public void setUpdateInterval(long milliSec) {
        // ensure there is no data race
        updateInterval = milliSec;
    }

    public void setPlayer(IExtPlayer iExtPlayer) {
        removeCallbacks(updater);
        this.iExtPlayer = iExtPlayer;
        post(updater);
    }
}
