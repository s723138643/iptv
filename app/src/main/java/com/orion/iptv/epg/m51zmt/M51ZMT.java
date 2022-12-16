package com.orion.iptv.epg.m51zmt;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.network.DownloadHelper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class M51ZMT {
    public static final String TAG = "51zmt";
    public static final String dateFormat = "yyyy-MM-dd";
    public static final String timeFormat = "yyyy-MM-dd HH:mm";

    public static Request.Builder newRequest(String api, String channelName, Date date) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
        Uri uri = Uri.parse(api)
                .buildUpon()
                .appendQueryParameter("ch", channelName)
                .appendQueryParameter("date", format.format(date))
                .build();
        return new Request.Builder().url(uri.toString());
    }

    public static Call get(String api, String channelName, Date date, Callback callback) {
        CacheControl cacheControl = new CacheControl.Builder()
                .maxAge(3, TimeUnit.HOURS)
                .build();
        Request request = newRequest(api, channelName, date)
                .cacheControl(cacheControl)
                .build();
        Log.i(TAG, "request epg url: " + request.url());
        return DownloadHelper.get(request, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String text = Objects.requireNonNull(response.body()).string();
                    EpgProgram[] epgPrograms = M51ZMT.toEpgProgram(text);
                    callback.onResponse(call, epgPrograms);
                } catch (Exception e) {
                    callback.onFailure(call, e);
                }
            }
        });
    }

    public static EpgProgram[] toEpgProgram(String response) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(timeFormat, Locale.ENGLISH);
        EpgMessage message = new Gson().fromJson(response, EpgMessage.class);
        EpgProgram[] epgPrograms = new EpgProgram[message.epg_data.length];
        for (int i=0; i<message.epg_data.length; i++) {
            EpgItem item = message.epg_data[i];
            long start = Objects.requireNonNull(format.parse(message.date + " " + item.start)).getTime();
            long end = Objects.requireNonNull(format.parse(message.date + " " + item.end)).getTime();
            epgPrograms[i] = new EpgProgram(start, end, item.start, item.end, item.title);
        }
        return epgPrograms;
    }

    public interface Callback {
        void onFailure(@NonNull Call call, @NonNull Exception e);
        void onResponse(@NonNull Call call, @NonNull EpgProgram[] programs);
    }
}