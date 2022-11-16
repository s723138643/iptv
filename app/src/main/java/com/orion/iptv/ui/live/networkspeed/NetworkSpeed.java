package com.orion.iptv.ui.live.networkspeed;

import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orion.iptv.R;
import com.orion.player.IExtPlayer;

import java.util.Locale;

public class NetworkSpeed {
    public static final String[] units = {"B/s", "KiB/s", "MiB/s", "GiB/s"};
    private final TextView networkSpeedView;
    private final Handler mHandler;
    private IExtPlayer iExtPlayer;
    private long updateInterval = 500;
    private final Runnable updateNetworkSpeed = new Runnable() {
        private double speed = -0.01f;

        @Override
        public void run() {
            if (networkSpeedView.getVisibility() == View.GONE) {
                return;
            }
            // unit: Byte/s
            double newSpeed = iExtPlayer.getNetworkSpeed();
            if (speed != newSpeed) {
                speed = newSpeed;
                networkSpeedView.setText(NetworkSpeed.format(newSpeed));
            }
            mHandler.postDelayed(this, updateInterval);
        }
    };

    public NetworkSpeed(AppCompatActivity activity) {
        networkSpeedView = activity.findViewById(R.id.networkSpeed);
        mHandler = new Handler(activity.getMainLooper());
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

    public void setVisible(boolean isVisible) {
        if (isVisible) {
            mHandler.removeCallbacks(updateNetworkSpeed);
            mHandler.post(() -> {
                // ensure it is visible before refresh bandwidth data
                networkSpeedView.setVisibility(View.VISIBLE);
                mHandler.post(updateNetworkSpeed);
            });
        } else {
            // do not need remove showBandwidth, it will auto stop
            mHandler.post(() -> networkSpeedView.setVisibility(View.GONE));
        }
    }

    public void setUpdateInterval(long milliSec) {
        // ensure there is no data race
        mHandler.post(() -> updateInterval = milliSec);
    }

    public void setPlayer(IExtPlayer iExtPlayer) {
        mHandler.removeCallbacks(updateNetworkSpeed);
        this.iExtPlayer = iExtPlayer;
        mHandler.post(updateNetworkSpeed);
    }
}
