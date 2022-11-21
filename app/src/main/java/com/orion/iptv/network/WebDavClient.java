package com.orion.iptv.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orion.iptv.ui.shares.FileNode;
import com.orion.iptv.ui.shares.Share;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebDavClient {
    private final Share share;
    private static final String requestBody =
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                    "<D:propfind xmlns:D=\"DAV:\">" +
                    "<D:prop>" +
                    "<D:resourcetype />" +
                    "<D:displayname />" +
                    "<D:getlastmodified />" +
                    "</D:prop>" +
                    "</D:propfind>";
    private static final MediaType xml = MediaType.get("application/xml; charset=utf-8");
    private final List<Call> calls;

    public WebDavClient(Share share) {
        this.share = share;
        this.calls = new ArrayList<>();
    }

    private Request.Builder setUrl(Request.Builder builder, String path) throws JSONException {
        JSONObject config = share.getConfig();
        HttpUrl url = HttpUrl.parse(config.getString("server"));
        assert url != null;
        HttpUrl.Builder urlBuilder = url.newBuilder().addPathSegments(path);
        builder.url(urlBuilder.build());
        if (config.has("username")) {
            try {
                String username = config.getString("username");
                String password = config.getString("password");
                builder.addHeader("Authorization", Credentials.basic(username, password));
            } catch (Exception ignored) {
            }
        }
        return builder;
    }

    public Request makeRequest(FileNode path) throws JSONException {
        Request.Builder builder = new Request.Builder();
        builder = builder.method("PROPFIND", RequestBody.create(requestBody, xml));
        builder = setUrl(builder, path.getPath());
        builder = builder.addHeader("Depth", "1");
        return builder.build();
    }

    public void list(FileNode path, Callback callback) {
        try {
            Call mCall = DownloadHelper.get(makeRequest(path), new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    calls.remove(call);
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    calls.remove(call);
                    String text = Objects.requireNonNull(response.body()).string();
                    List<FileNode> children = parseResponse(path, text);
                    callback.onResponse(children);
                }
            });
            calls.add(mCall);
        } catch (JSONException e) {
            callback.onFailure(e);
        }
    }

    private List<FileNode> parseResponse(FileNode parent, String responseBody) {
        PropfindParser parser = new PropfindParser(parent);
        return parser.parse(responseBody);
    }

    public void cancel() {
        calls.forEach(Call::cancel);
        calls.clear();
    }

    public interface Callback {
        void onFailure(@NonNull Exception e);
        void onResponse(@Nullable List<FileNode> children);
    }
}
