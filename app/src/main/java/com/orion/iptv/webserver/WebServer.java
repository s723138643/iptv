package com.orion.iptv.webserver;

import android.util.Log;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    private static final String TAG = "WebServer";
    private static final String MIME_HTML = "text/html; charset=UTF-8";
    private static final String MIME_PLAIN = "text/plain; charset=UTF-8";
    private OnPostUrlListener listener;
    private final Context context;

    public interface OnPostUrlListener {
        void onPostUrl(String url);
    }

    public WebServer(Context context, int port) {
        super(port);
        this.context = context;
    }

    public void setOnPostUrlListener(OnPostUrlListener listener) {
        this.listener = listener;
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            return _serve(session);
        } catch (ResponseException | IOException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAIN, "bad request");
        }
    }

    private Response _serve(IHTTPSession session) throws ResponseException, IOException {
        if (session.getMethod().equals(Method.GET)) {
            return serveGet(session);
        } else if (session.getMethod().equals(Method.POST)) {
            return servePost(session);
        } else {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAIN, "unsupported request");
        }
    }

    private Response serveGet(IHTTPSession session) throws IOException {
        InputStream stream = context.getAssets().open("web/channel_source.html");
        return newChunkedResponse(Response.Status.ACCEPTED, MIME_HTML, stream);
    }

    private Response servePost(IHTTPSession session) throws ResponseException, IOException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        Map<String, List<String>> parameters = session.getParameters();
        List<String> entry = parameters.get("url");
        if (entry == null || entry.size() != 1) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAIN, "unexpected post data");
        }
        String url = entry.get(0);
        Log.i(TAG, String.format(Locale.ENGLISH, "got url: %s", entry.get(0)));
        if (listener != null) { listener.onPostUrl(url); }
        return newFixedLengthResponse(Response.Status.ACCEPTED, MIME_PLAIN, "ok");
    }
}
