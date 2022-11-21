package com.orion.iptv;

import android.util.Log;

import com.orion.iptv.network.DownloadHelper;
import com.orion.iptv.network.PropfindParser;
import com.orion.iptv.network.WebDavClient;
import com.orion.iptv.ui.shares.FileNode;
import com.orion.iptv.ui.shares.Share;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void request_isCorrect() {
        DownloadHelper.setClient(new OkHttpClient.Builder().build());
        JSONObject config = new JSONObject();
        try {
            config.put("server", "http://orion:5000/");
            Share share = new Share(config, ".config", "/.config/");
            WebDavClient client = new WebDavClient(share);
            Request request = client.makeRequest(share.getRoot());
            Response response = DownloadHelper.getBlocked(request);
            PropfindParser parse = new PropfindParser(share.getRoot());
            String body = Objects.requireNonNull(response.body()).string();
            Log.i("Test", body);
            List<FileNode> children = parse.parse(body);
            for (FileNode child : children) {
                Log.i("Test", String.format(Locale.ENGLISH, "%s::%s %b", child.getName(), child.getPath(), child.isFile()));
            }
        } catch (JSONException | IOException e) {
            Log.e("Test", e.toString());
        }
    }
}