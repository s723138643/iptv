package com.orion.player;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExtDataSource {
    public static final Auth NoAuth = new Auth("", "");

    private final String uri;
    private Map<String, String> headers;
    private Auth auth;
    private List<Subtitle> subtitles;

    public ExtDataSource(String uri) {
        this.uri = uri;
        this.headers = new ArrayMap<>();
        this.auth = NoAuth;
        this.subtitles = new ArrayList<>();
    }

    @NonNull
    public String getUri() {
        return uri;
    }

    @NonNull
    public Map<String, String> getHeaders() {
        return headers;
    }

    @NonNull
    public Auth getAuth() {
        return auth;
    }

    @NonNull
    public List<Subtitle> getSubtitles() {
        return this.subtitles;
    }

    public void setHeaders(@NonNull Map<String, String> headers) {
        this.headers = headers;
    }

    public void setAuth(@NonNull Auth auth) {
        this.auth = auth;
    }

    public void setSubtitles(@NonNull List<Subtitle> subtitles) {
        this.subtitles = subtitles;
    }

    public static class Auth {
        public final String username;
        public final String password;

        public Auth(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Auth auth = (Auth) o;
            return username.equals(auth.username) && password.equals(auth.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, password);
        }
    }

    public static class Subtitle {
        public final Uri uri;
        public final String mimeType;

        public Subtitle(Uri uri, String mimeType) {
            this.uri = uri;
            this.mimeType = mimeType;
        }
    }
}