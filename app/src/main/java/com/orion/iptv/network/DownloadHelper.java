package com.orion.iptv.network;

import java.io.IOException;

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

    public static Call get(String url, Callback callback) {
        return get(helper.client, url, callback);
    }

    public static Call get(OkHttpClient client, String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return get(client, request, callback);
    }

    public static Call get(String url, CacheControl cacheControl, Callback callback) {
        return get(helper.client, url, cacheControl, callback);
    }

    public static Call get(OkHttpClient client, String url, CacheControl cacheControl, Callback callback) {
         Request request = new Request.Builder()
                .url(url)
                .cacheControl(cacheControl)
                .build();
        return get(client, request, callback);
    }

    public static Call get(OkHttpClient client, Request request, Callback callback) {
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    public static Call get(Request request, Callback callback) {
        return get(helper.client, request, callback);
    }

    public static Response getBlocked(OkHttpClient client, Request request) throws IOException {
        Call call = client.newCall(request);
        return call.execute();
    }

    public static Response getBlocked(Request request) throws IOException {
        return getBlocked(helper.client, request);
    }
}
