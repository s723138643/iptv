package com.orion.player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public class ExtDataSource {
    private final String uri;
    private final Object tag;
    private Map<String, String> headers;
    private Auth auth;

    public ExtDataSource(String uri) {
        this(uri, null);
    }

    public ExtDataSource(String uri, Object tag) {
        this.uri = uri;
        this.tag = tag;
        this.headers = null;
        this.auth = null;
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

    @Nullable
    public Auth getAuth() {
        return auth;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public static class Auth {
        public final String username;
        public final String password;

        public Auth(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
