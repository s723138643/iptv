package com.orion.iptv.layout.bandwidth;

import android.view.View;
import android.widget.TextView;

import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.orion.iptv.R;

import java.util.Locale;

public class Bandwidth {
    private final TextView bandwidthView;
    public static final String[] units = {"bps", "kbps", "Mbps", "Gbps"};
    private final Handler mHandler;
    private long newBandwidth;
    private long displayInterval = 300;
    private final Runnable showBandwidth = new Runnable() {
        private long bandwidth = 0;

        @Override
        public void run() {
            if (bandwidthView.getVisibility() == View.GONE) { return; }

            if (newBandwidth != bandwidth) {
                bandwidth = newBandwidth;
                String formattedBandwidth = Bandwidth.format(newBandwidth);
                bandwidthView.setText(formattedBandwidth);
            }
            mHandler.postDelayed(this, displayInterval);
        }
    };

    public Bandwidth(AppCompatActivity activity) {
        bandwidthView = activity.findViewById(R.id.bandwidth);
        mHandler = new Handler(activity.getMainLooper());
        mHandler.post(showBandwidth);
    }

    public static String format(long bandwidth) {
        double base = 1000.0f;
        double band = (double) bandwidth;
        int i = 0;
        for (; i<units.length; i++) {
            if (band < base) {
                break;
            }
            band /= base;
        }
        return String.format(Locale.ENGLISH, "%.2f%s", band, units[i]);
    }

    public void setBandwidth(long bandwidth) {
        // ensure there is no data race
        mHandler.post(()->newBandwidth = bandwidth);
    }

    public void setVisible(boolean isVisible) {
        if (isVisible) {
            mHandler.removeCallbacks(showBandwidth);
            mHandler.post(()->{
                // ensure it is visible before refresh bandwidth data
                bandwidthView.setVisibility(View.VISIBLE);
                mHandler.post(showBandwidth);
            });
        } else {
            // do not need remove showBandwidth, it will auto stop
            mHandler.post(()->bandwidthView.setVisibility(View.GONE));
        }
    }

    public void setDisplayInterval(long milliSec) {
        // ensure there is no data race
        mHandler.post(()->displayInterval = milliSec);
    }
}
