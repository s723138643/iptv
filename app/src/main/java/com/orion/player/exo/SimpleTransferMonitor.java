package com.orion.player.exo;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;
import androidx.media3.datasource.DataSource;

public class SimpleTransferMonitor implements TransferListener {
    private final Object lock = new Object();
    private long totalTransferred = 0;
    private long lastMeasured = 0;
    private long lastMeasuredTimeMs = -1;

    @Override
    public void onTransferInitializing(@NonNull DataSource source, @NonNull DataSpec dataSpec, boolean isNetwork) {}

    @Override
    public void onTransferStart(@NonNull DataSource source, @NonNull DataSpec dataSpec, boolean isNetwork) {}

    @Override
    public void onBytesTransferred(@NonNull DataSource source, @NonNull DataSpec dataSpec, boolean isNetwork, int bytesTransferred) {
        synchronized (lock) {
            if (lastMeasuredTimeMs < 0) {
                lastMeasuredTimeMs = SystemClock.uptimeMillis();
            }
            totalTransferred += bytesTransferred;
        }
    }

    @Override
    public void onTransferEnd(@NonNull DataSource source, @NonNull DataSpec dataSpec, boolean isNetwork) {}

    public double getNetworkSpeed() {
        long tempMeasured = lastMeasured;
        synchronized (lock) {
            lastMeasured = totalTransferred;
        }
        long tempMeasuredTimeMs = lastMeasuredTimeMs;
        lastMeasuredTimeMs = SystemClock.uptimeMillis();
        return (double) (lastMeasured - tempMeasured) * 1000.0f / (double) (lastMeasuredTimeMs - tempMeasuredTimeMs);
    }
}
