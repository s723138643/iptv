package com.orion.iptv.network;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadHelper {
    private static final String TAG = "DownloadHelper";
    private static final DownloadHelper helper = new DownloadHelper();

    private OkHttpClient client;

    public static void setClient(OkHttpClient client) {
        helper.client = client;
    }

    public static void get(String url, OnResponseListener listener, OnErrorListener errorListener) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        get(request, listener, errorListener);
    }

    public static void get(String url, CacheControl cacheControl, OnResponseListener listener, OnErrorListener errorListener) {
        Request request = new Request.Builder()
                .url(url)
                .cacheControl(cacheControl)
                .build();
        get(request, listener, errorListener);
    }

    public static void get(Request request, OnResponseListener listener, OnErrorListener errorListener) {
        Call call = helper.client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                errorListener.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = Objects.requireNonNull(response.body()).string();
                listener.onResponse(body);
            }
        });
    }

    public interface OnResponseListener {
        void onResponse(String response);
    }

    public interface OnErrorListener {
        void onError(Exception err);
    }
}
