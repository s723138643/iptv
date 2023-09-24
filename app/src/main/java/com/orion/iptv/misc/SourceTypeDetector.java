package com.orion.iptv.misc;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceTypeDetector {
    public static final String TAG = "SourceTypeDetector";
    private static final Pattern liveUrlPattern = Pattern.compile("[\"'](proxy://do=live&.*?)[\"']");

    // 探测是否是json字符串，不支持嵌套/**/注释检测
    public static boolean isJson(String data) {
        try {
            return _isJson(data);
        } catch (IOException e) {
            Log.e(TAG, "parse data type failed, " + e);
        }
        return false;
    }

    private static boolean _isJson(String data) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(data));
        String starCommentStart = "/*";
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }
            if (line.startsWith(starCommentStart)) {
                String remain = eatStarComment(line.substring(starCommentStart.length()), reader);
                if (remain.isEmpty()) {
                    continue;
                }
                return remain.startsWith("{");
            }
            return line.startsWith("{");
        }
        return false;
    }

    private static String eatStarComment(String firstLine, BufferedReader reader) throws IOException {
        String starCommentEnd = "*/";
        int e = firstLine.indexOf(starCommentEnd);
        if (e > 0) {
            return firstLine.substring(e + starCommentEnd.length()).trim();
        }
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            e = line.indexOf(starCommentEnd);
            if (e >= 0) {
                return line.substring(e + starCommentEnd.length()).trim();
            }
        }
        return "";
    }

    static String getExt(String params) {
        while (!params.isEmpty()) {
            String[] pair = params.split("&", 2);
            if (pair[0].startsWith("ext")) {
                String[] kv = pair[0].split("=", 2);
                if (kv.length == 2) {
                    return kv[1];
                }
                return "";
            }
            params = pair[1];
        }
        return "";
    }

    public static String getLiveUrl(String data) {
        Matcher matcher = liveUrlPattern.matcher(data);
        if (!matcher.find()) {
            return "";
        }
        String liveUrl = matcher.group(1);
        if (liveUrl == null || liveUrl.trim().isEmpty()) {
            return "";
        }
        Log.i(TAG, "original url: " + liveUrl);
        liveUrl = liveUrl.replace("proxy://", "");
        String ext = getExt(liveUrl);
        if (ext == null || ext.trim().isEmpty()) {
            return "";
        }
        String realUrl = ext;
        try {
            realUrl = new String(Base64.decode(ext, Base64.DEFAULT));
        } catch (IllegalArgumentException ignored) {
        }
        return !realUrl.isEmpty() ? realUrl : "";
    }
}
