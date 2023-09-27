package com.orion.iptv.bean.tvbox;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.List;

public class Config {
    @Nullable
    public List<Live> lives;

    public static Config fromString(String text) {
        return new Gson().fromJson(text, Config.class);
    }
}