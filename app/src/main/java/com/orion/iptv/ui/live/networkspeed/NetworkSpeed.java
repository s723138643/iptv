package com.orion.iptv.ui.live.networkspeed;

import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.orion.iptv.R;

import java.util.Locale;

public class NetworkSpeed {
    public static final String[] units = {"B/s", "KiB/s", "MiB/s", "GiB/s"};
    private final TextView networkSpeedView;
    private final Handler mHandler;
    private long totalTransferred = 0;
    private long updateInterval = 500;
    private final Runnable updateNetworkSpeed = new Runnable() {
        private long lastTotalTransferred = 0;
        private long lastTime = NetworkSpeed.now();
        private double speed = -0.01f;

        @Override
        public void run() {
            if (networkSpeedView.getVisibility() == View.GONE) {
                return;
            }
            long totalTransferred = NetworkSpeed.this.getTotalTransferred();
            long now = NetworkSpeed.now();
            // unit: Byte/s
            double newSpeed = (double) ((totalTransferred - lastTotalTransferred) * 1000) / (now - lastTime);
            if (speed != newSpeed) {
                speed = newSpeed;
                networkSpeedView.setText(NetworkSpeed.format(newSpeed));
            }
            lastTotalTransferred = totalTransferred;
            lastTime = now;
            mHandler.postDelayed(this, updateInterval);
        }
    };

    public NetworkSpeed(AppCompatActivity activity) {
        networkSpeedView = activity.findViewById(R.id.networkSpeed);
        mHandler = new Handler(activity.getMainLooper());
        mHandler.post(updateNetworkSpeed);
    }

    public static long now() {
        return SystemClock.uptimeMillis();
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

    private synchronized void onBytesTransferred(int byteTransferred) {
        totalTransferred += byteTransferred;
    }

    public synchronized long getTotalTransferred() {
        return totalTransferred;
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

    public class SimpleTransferListener implements TransferListener {
        @Override
        public void onTransferInitializing(@NonNull DataSource source, @NonNull DataSpec dataSpec, boolean isNetwork) {}

        @Override
        public void onTransferStart(@NonNull DataSource source, @NonNull DataSpec dataSpec, boolean isNetwork) {}

        @Override
        public void onBytesTransferred(@NonNull DataSource source, @NonNull DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {
            NetworkSpeed.this.onBytesTransferred(bytesTransferred);
        }

        @Override
        public void onTransferEnd(@NonNull DataSource source, @NonNull DataSpec dataSpec, boolean isNetwork) {}
    }
}
