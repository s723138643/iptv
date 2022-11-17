package com.orion.iptv.layout;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orion.iptv.R;
import com.orion.player.IExtPlayer;

import java.util.Locale;

public class NetworkSpeed extends Fragment {
    private static final String TAG = "NetworkSpeed";
    public static final String[] units = {"B/s", "KiB/s", "MiB/s", "GiB/s"};
    private TextView view;
    private Handler mHandler;
    private IExtPlayer iExtPlayer;
    private long updateInterval = 500;
    private Runnable updater ;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_network_speed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = (TextView) view;
        mHandler = new Handler(requireContext().getMainLooper());
        updater = new Runnable() {
            private double speed = -0.01f;

            @Override
            public void run() {
                // unit: Byte/s
                double newSpeed = iExtPlayer.getNetworkSpeed();
                if (speed != newSpeed) {
                    speed = newSpeed;
                    NetworkSpeed.this.view.setText(NetworkSpeed.format(newSpeed));
                }
                mHandler.postDelayed(this, updateInterval);
            }
        };
    }

    public static String format(double speed) {
        final double base = 1024.0f;
        int i = 0;
        for (; i < units.length; i++) {
            if (speed < base) {
                break;
            }
            speed /= base;
        }
        i = i >= units.length ? units.length - 1 : i;
        return String.format(Locale.ENGLISH, "%.2f%s", speed, units[i]);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            mHandler.removeCallbacks(updater);
        } else {
            mHandler.post(updater);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(updater);
    }

    public void setUpdateInterval(long milliSec) {
        // ensure there is no data race
        mHandler.post(() -> updateInterval = milliSec);
    }

    public void setPlayer(IExtPlayer iExtPlayer) {
        mHandler.removeCallbacks(updater);
        this.iExtPlayer = iExtPlayer;
        if (!isHidden()) {
            mHandler.post(updater);
        }
    }
}
