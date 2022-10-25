package com.orion.iptv.network;

import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;

public class StringRequest extends com.android.volley.toolbox.StringRequest {
    public static final String TAG = "StringRequest";
    private final String[] charsets = {"GB2312", "GBK", "GB18030", "ISO-8859-1"};

    public StringRequest(String url, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
        super(url, listener, errorListener);
    }

    public StringRequest(int method, String url, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    private String tryDecode(byte[] raw) throws UnsupportedEncodingException {
        UnsupportedEncodingException error = new UnsupportedEncodingException();
        for (String charset : charsets) {
            try {
                return new String(raw, charset);
            } catch (UnsupportedEncodingException e) {
                Log.i(TAG, "decode response for " + charset + " failed, " + e);
                error = e;
            }
        }
        throw error;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        String charset = HttpHeaderParser.parseCharset(response.headers);
        if (charset.equals("ISO-8859-1")) {
            charset = "UTF-8";
        }
        try {
            parsed = new String(response.data, charset);
        } catch (UnsupportedEncodingException e) {
            // Since minSdkVersion = 8, we can't call
            // new String(response.data, Charset.defaultCharset())
            // So suppress the warning instead.
            Log.i(TAG, "decode response for default charset failed, " + e);
            try {
                parsed = tryDecode(response.data);
            } catch (UnsupportedEncodingException newError) {
                parsed = new String(response.data);
            }
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }
}
