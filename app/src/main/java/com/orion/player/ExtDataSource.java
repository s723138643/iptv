package com.orion.player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public class ExtDataSource {
    private final String uri;
    private final Map<String, String> headers;
    private final Object tag;

    public ExtDataSource(String uri) {
        this(uri, null, null);
    }

    public ExtDataSource(String uri, Object tag) {
        this(uri, tag, null);
    }

    public ExtDataSource(String uri, Object tag, Map<String, String> headers) {
        this.uri =uri;
        this.tag = tag;
        this.headers = headers;
    }

    @NonNull
    public String getUri() {
        return uri;
    }

    @Nullable
    public Object getTag() {
        return tag;
    }

    @Nullable
    public Map<String, String> getHeaders() {
        return headers;
    }
}
