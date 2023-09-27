package com.orion.iptv.bean.tvbox;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;

import java.util.List;

public class Live {
    public static String TAG = "Live";
    public int type;
    public String name;
    public String group;
    public String url;
    public String logo;
    public String epg;
    public String ua;
    public String referer;
    public JsonElement header;
    public List<Channel> channels;
    public List<Group> groups;

    @Nullable
    public Live fromProxy() {
        if (channels == null || channels.size() == 0) {
            return null;
        }
        Channel channel = channels.get(0);
        if (channel.urls == null || channel.urls.size() == 0) {
            return null;
        }
        String url = channel.urls.get(0);
        String proxy = url.trim().startsWith("proxy") ? parseProxy(url) : "";
        if (proxy.isEmpty()) {
            return null;
        }
        Live live = new Live();
        live.name = channel.name;
        live.url = proxy;
        live.ua = channel.ua;
        live.epg = channel.epg;
        live.logo = channel.logo;
        live.referer = channel.referer;
        live.header = channel.header;
        return live;
    }

    private static String getExt(String params) {
        while (!params.isEmpty()) {
            String[] pair = params.split("&", 2);
            if (pair[0].startsWith("ext")) {
                String[] kv = pair[0].split("=", 2);
                if (kv.length == 2) {
                    return kv[1];
                }
                return "";
            }
            params = pair[1];
        }
        return "";
    }

    public static String parseProxy(String proxy) {
        Log.i(TAG, "original proxy url: " + proxy);
        proxy = proxy.replace("proxy://", "");
        String ext = getExt(proxy);
        if (ext == null || ext.trim().isEmpty()) {
            return "";
        }
        String realUrl = ext;
        try {
            realUrl = new String(Base64.decode(ext, Base64.DEFAULT));
        } catch (IllegalArgumentException ignored) {
        }
        return realUrl.trim();
    }
}