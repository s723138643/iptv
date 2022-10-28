package com.orion.iptv.network;

import android.util.Log;

import com.orion.iptv.ui.shares.FileNode;
import com.orion.iptv.ui.shares.Share;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class WebDavClient {
    private final Share share;
    private static final String requestBody =
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
            "<D:propfind xmlns:D=\"DAV:\">" +
            "<D:prop>" +
            "<D:displayname />" +
            "<D:getlastmodified />" +
            "</D:prop>" +
            "</D:propfind>";
    private static final MediaType xml = MediaType.get("application/xml; charset=utf-8");

    public WebDavClient(Share share) {
        this.share = share;
    }

    private Request.Builder maybeAddAuth(Request.Builder builder) {
        JSONObject config = share.getConfig();
        try {
            String username = config.getString("username");
            String password = config.getString("password");
            return builder.addHeader("Authorization", Credentials.basic(username, password));
        } catch (JSONException ignored) {
            return builder;
        }
    }

    private Request.Builder setUrl(Request.Builder builder, String path) throws JSONException {
        JSONObject config = share.getConfig();
        HttpUrl url = HttpUrl.parse(config.getString("server"));
        assert url != null;
        url = url.newBuilder()
                .addPathSegments(path)
                .build();
        Log.i("WebDavClient", url.toString());
        return builder.url(url);
    }

    public Request makeRequest(FileNode path) throws JSONException {
        Request.Builder builder = new Request.Builder();
        builder = builder.method("PROPFIND", RequestBody.create(requestBody, xml));
        builder = setUrl(builder, path.getPath());
        builder = maybeAddAuth(builder);
        builder = builder.addHeader("Depth", "1");
        return builder.build();
    }

    public void list(FileNode path, OnResponseListener listener, DownloadHelper.OnErrorListener errorListener) {
        try {
            Request request = makeRequest(path);
            DownloadHelper.get(request, response -> {
                List<FileNode> children = parseResponse(path, response);
                listener.onResponse(children);
            }, errorListener);
        } catch (JSONException err) {
            errorListener.onError(err);
        }
    }

    private List<FileNode> parseResponse(FileNode parent, String responseBody) {
        PropfindParser parser = new PropfindParser(parent);
        return parser.parse(responseBody);
    }

    public interface OnResponseListener {
        void onResponse(List<FileNode> children);
    }
}
