package com.orion.iptv.misc;

public abstract class CancelableRunnable implements Runnable {
    private boolean canceled = false;

    public abstract void callback();

    @Override
    public void run() {
        if (!canceled) {
            callback();
        }
    }

    public void cancel() {
        canceled = true;
    }
}
