package com.orion.iptv.bean.tvbox;

import com.google.gson.JsonElement;

import java.util.List;

public class Channel {
    public String name;
    public List<String> urls;
    public String epg;
    public String ua;
    public String referer;
    public JsonElement header;
    public String logo;
}
