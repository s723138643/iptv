package com.orion.iptv.misc;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

public class SourceTypeDetector {
    public static final String TAG = "SourceTypeDetector";

    enum SourceType {
        JSON,
        HTML,
        TEXT,
        UNKNOWN,
    }

    // 探测文档类型,忽略文档开头的空字符和注释，不支持嵌套/**/注释检测
    public static SourceType getType(String data) {
        try {
            return _getType(data);
        } catch (IOException e) {
            Log.e(TAG, "parse data type failed, " + e);
        }
        return SourceType.UNKNOWN;
    }

    private static SourceType _getType(String data) throws IOException {
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
                return checkType(remain);
            }
            return checkType(line);
        }
        return SourceType.TEXT;
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

    private static SourceType checkType(String line) {
        if (line.startsWith("{")) {
            return SourceType.JSON;
        }
        if (Pattern.matches("<!DOCTYPE .*?>", line) || Pattern.matches("<html .*?>", line)) {
            return SourceType.HTML;
        }
        return SourceType.TEXT;
    }
}
