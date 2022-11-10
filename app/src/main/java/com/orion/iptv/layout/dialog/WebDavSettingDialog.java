package com.orion.iptv.layout.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.widget.EditText;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.orion.iptv.R;
import com.orion.iptv.ui.shares.FileNode;
import com.orion.iptv.ui.shares.Share;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

public class WebDavSettingDialog {
    private static final String TAG = "WebDavSettingDialog";
    private final AlertDialog.Builder builder;

    private Share defaultValue;
    private OnSubmitListener listener;

    public WebDavSettingDialog(Context context) {
        this.builder = new AlertDialog.Builder(context);
        this.builder.setTitle("添加WebDav");
        this.builder.setView(R.layout.dialog_webdav_setting);
        this.builder.setPositiveButton("ok", new OnClickListener());
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

    private Optional<String> getValue(AlertDialog dialog, int resId) {
        EditText text = dialog.findViewById(resId);
        return text != null ? Optional.of(text.getText().toString()) : Optional.empty();
    }

    public WebDavSettingDialog setOnSubmitListener(OnSubmitListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnSubmitListener {
        void onSubmit(Share share);
    }

    private class OnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            String webdavName = getValue(alertDialog, R.id.webdav_name).orElse("");
            String webdavServer = getValue(alertDialog, R.id.webdav_server).map(address -> {
                Uri uri = Uri.parse(address);
                if (uri.getScheme().equals("webdav")) {
                    return uri.buildUpon().scheme("http").build().toString();
                }
                return uri.toString();
            }).orElse("");
            String webdavPath = getValue(alertDialog, R.id.webdav_path).map(path -> {
                path = path.trim();
                path = path.replaceFirst("^/+", "/");
                path = path.replaceFirst("/+$", "");
                return path;
            }).orElse("");

            String webdavUsername = getValue(alertDialog, R.id.webdav_username).orElse("");
            String webdavPassword = getValue(alertDialog, R.id.webdav_password).orElse("");

            JSONObject config = new JSONObject();
            try {
                config.put("server", webdavServer);
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
