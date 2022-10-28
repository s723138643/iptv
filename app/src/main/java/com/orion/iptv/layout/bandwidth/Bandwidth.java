package com.orion.iptv.layout.bandwidth;

import android.widget.TextView;

import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.orion.iptv.R;

import java.util.Locale;

public class Bandwidth {
    private final TextView bandwidth;
    private final Handler mHandler;
    private final String[] units = {"bps", "kbps", "Mbps", "Gbps"};

    public Bandwidth(AppCompatActivity activity) {
        bandwidth = activity.findViewById(R.id.bandwidth);
        mHandler = new Handler(activity.getMainLooper());
    }

    private String format(long bandwidth) {
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
        String formattedBandwidth = format(bandwidth);
        mHandler.post(()->{
            this.bandwidth.setText(formattedBandwidth);
        });
    }
}
