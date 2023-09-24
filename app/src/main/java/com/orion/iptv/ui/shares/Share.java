package com.orion.iptv.ui.shares;

import org.json.JSONObject;

public class Share {
    private final JSONObject config;
    private final String name;
    private final String path;

    public Share(JSONObject config, String name, String path) {
        this.config = config;
        this.name = name;
        this.path = path;
    }

    public JSONObject getConfig() {
        return config;
    }

    public FileNode getRoot() {
        return new FileNode(name, path, false, 0, null);
    }
}
