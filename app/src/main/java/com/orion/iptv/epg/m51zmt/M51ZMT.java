package com.orion.iptv.epg.m51zmt;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.network.DownloadHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Request;

public class M51ZMT {
    public static final String TAG = "51zmt";
    public static final String api = "http://epg.51zmt.top:8000/api/diyp/";
    public static final String dateFormat = "yyyy-MM-dd";
    public static final String timeFormat = "yyyy-MM-dd HH:mm";

    public static Request.Builder newRequest(String channelName, Date date) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
        Uri uri = Uri.parse(api)
                .buildUpon()
                .appendQueryParameter("ch", channelName)
                .appendQueryParameter("date", format.format(date))
                .build();
        return new Request.Builder().url(uri.toString());
    }

    public static void get(
            String channelName,
            Date date,
            OnResponseListener listener,
            DownloadHelper.OnErrorListener errorListener) {
        CacheControl cacheControl = new CacheControl.Builder()
                .maxAge(6, TimeUnit.HOURS)
                .build();
        Request request = newRequest(channelName, date).cacheControl(cacheControl).build();
        Log.i(TAG, "request epg url: " + request.url());
        DownloadHelper.get(
                request,
                response -> {
                    try {
                        EpgProgram[] epgPrograms = toEpgProgram(response);
                        Log.i(TAG, "got programs " + String.valueOf(epgPrograms.length));
                        listener.onResponse(epgPrograms);
                    } catch (Exception e) {
                        errorListener.onError(e);
                    }
                },
                errorListener
        );
    }

    private static EpgProgram[] toEpgProgram(String response) throws ParseException {
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

    public interface OnResponseListener {
        void onResponse(EpgProgram[] epgs);
    }
}