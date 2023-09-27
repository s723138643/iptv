package com.orion.iptv.misc;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.orion.iptv.bean.ChannelSource;
import com.orion.iptv.bean.tvbox.Config;
import com.orion.iptv.bean.tvbox.Live;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelLoader {
    public static final String TAG = "ChannelLoader";

    @NonNull
    public static List<Live> getChannels(ChannelSource channels, String text) {
        List<Live> lives = new ArrayList<>(0);
        switch (SourceTypeDetector.getType(text)) {
            case TEXT:
                boolean m = mergeTo(channels, text);
                break;
            case JSON:
                lives = parseTVboxConfig(channels, text);
            case HTML:
            case UNKNOWN:
                break;
        }
        return lives;
    }

    @NonNull
    public static List<Live> parseTVboxConfig(ChannelSource channels, String text) {
        List<Live> lives = new ArrayList<>();
        Config conf = new Gson().fromJson(text, Config.class);
        if (conf.lives == null) { return lives; }
        for (Live live: conf.lives) {
            if (live.url != null && !live.url.trim().isEmpty()) {
                live.url = live.url.trim();
                Log.i(TAG, "found live url: " + live.url);
                lives.add(live);
                continue;
            }
            Live proxy = live.fromProxy();
            if (proxy != null) {
                Log.i(TAG, "found proxy url: " + proxy.url);
                lives.add(proxy);
                continue;
            }
        }
        return lives;
    }

    public static boolean mergeTo(ChannelSource channels, String text) {
        return channels.mergeFrom(text);
    }
}
