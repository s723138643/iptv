package com.orion.iptv.layout.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Pair;
import android.widget.EditText;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.orion.iptv.R;
import com.orion.iptv.ui.shares.FileNode;
import com.orion.iptv.ui.shares.Share;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.HttpUrl;

public class WebDavSettingDialog {
    private static final String TAG = "WebDavSettingDialog";
    private final AlertDialog.Builder builder;

    private Share defaultValue;
    private OnSubmitListener listener;

    public WebDavSettingDialog(Context context) {
        this.builder = new AlertDialog.Builder(context);
        this.builder.setTitle(R.string.add_webdav_storage);
        this.builder.setView(R.layout.dialog_webdav_setting);
        this.builder.setPositiveButton(R.string.add, new OnClickListener());
    }

    public WebDavSettingDialog setDefaultValue(Share share) {
        defaultValue = share;
        return this;
    }

    public void show() {
        AlertDialog dialog = builder.create();
        dialog.show();
        if (defaultValue != null) {
            feedDefaultValue(dialog, defaultValue);
        }
    }

    private void feedDefaultValue(AlertDialog dialog, Share defaultValue) {
        JSONObject config = defaultValue.getConfig();
        FileNode root = defaultValue.getRoot();

        maybeFeed(dialog, R.id.webdav_name, root.getName());
        maybeFeed(dialog, R.id.webdav_server, config.optString("server", ""));
        maybeFeed(dialog, R.id.webdav_path, root.getAbsolutePath());
        maybeFeed(dialog, R.id.webdav_username, config.optString("username", ""));
        maybeFeed(dialog, R.id.webdav_password, config.optString("password", ""));
    }

    private void maybeFeed(AlertDialog dialog, int resId, String value) {
        if (value == null || value.equals("")) {
            return;
        }
        EditText text = dialog.findViewById(resId);
        if (text != null) {
            text.setText(value);
        }
    }

    @Nullable
    private String getValue(AlertDialog dialog, int resId) {
        EditText text = dialog.findViewById(resId);
        return text != null ? text.getText().toString() : null;
    }

    public WebDavSettingDialog setOnSubmitListener(OnSubmitListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnSubmitListener {
        void onSubmit(Share share);
    }

    private class OnClickListener implements DialogInterface.OnClickListener {
        private List<String> concat(List<String> first, List<String> second) {
            List<String> path = new ArrayList<>();
            for (String p : first) {
                if (!p.isEmpty()) {
                    path.add(p);
                }
            }
            for (String p : second) {
                if (!p.isEmpty()) {
                    path.add(p);
                }
            }
            return path;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            String webdavName = getValue(alertDialog, R.id.webdav_name);
            webdavName = webdavName != null ? webdavName : "";
            // parse server address
            String address = getValue(alertDialog, R.id.webdav_server);
            Pair<String, List<String>> webdavServer = null;
            if (address != null) {
                HttpUrl uri = HttpUrl.parse(address);
                if (uri != null) {
                    List<String> path = uri.pathSegments();
                    HttpUrl.Builder builder = new HttpUrl.Builder();
                    builder.scheme(uri.scheme());
                    builder.host(uri.host());
                    builder.port(uri.port());
                    webdavServer = Pair.create(builder.build().toString(), path);
                }
            }
            if (webdavServer == null) {
                webdavServer = Pair.create("", new ArrayList<>());
            }

            // parse initial path
            String path = getValue(alertDialog, R.id.webdav_path);
            List<String> initPath = path != null ? Arrays.asList(path.split("/")) : new ArrayList<>();
            // join all path, remove empty segment
            List<String> finalPath = concat(webdavServer.second, initPath);
            // make relative path
            String webdavPath = String.join("/", finalPath);
            // translate to absolute path
            webdavPath = webdavPath.isEmpty() ? "/" : "/" + webdavPath + "/";

            String webdavUsername = getValue(alertDialog, R.id.webdav_username);
            webdavUsername = webdavUsername != null ? webdavUsername : "";
            String webdavPassword = getValue(alertDialog, R.id.webdav_password);
            webdavPassword = webdavPassword != null ? webdavPassword : "";

            JSONObject config = new JSONObject();
            try {
                config.put("server", webdavServer.first);
                if (!webdavUsername.equals("")) {
                    config.put("username", webdavUsername);
                    config.put("password", webdavPassword);
                }
            } catch(JSONException err) {
                Log.e(TAG, "create config failed, " + err);
            }
            if (listener != null) {
                listener.onSubmit(new Share(config, webdavName, webdavPath));
            }
        }
    }
}
