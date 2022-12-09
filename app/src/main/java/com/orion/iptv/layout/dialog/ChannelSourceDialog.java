package com.orion.iptv.layout.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.network.HostIP;
import com.orion.iptv.webserver.WebServer;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import fi.iki.elonen.NanoHTTPD;

public class ChannelSourceDialog {

    private final String TAG = "ChannelSourceDialog";
    private final Handler mHandler;
    private final AlertDialog.Builder builder;
    private final WebServer server;
    private OnChannelSourceSubmitListener listener;
    private String defaultValue;
    private final Context context;

    public ChannelSourceDialog(Context context) {
        this.context = context;
        mHandler = new Handler(context.getMainLooper());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setView(R.layout.dialog_live_channel_source);
        builder = builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            AlertDialog alertDialog = (AlertDialog) dialog;
            EditText text = alertDialog.findViewById(R.id.channel_source_url);
            String input = text.getText().toString();
            if (input.equals("") || listener == null) {
                return;
            }
            listener.onChannelSourceSubmit(input);
        });
        server = new WebServer(context, 9978);
        builder = builder.setOnDismissListener(dialog -> {
            if (server.wasStarted()) {
                server.stop();
                Log.i(TAG, "http server stopped");
            }
        });
        this.builder = builder;
    }

    public void setOnChannelSourceSubmitListener(OnChannelSourceSubmitListener listener) {
        this.listener = listener;
    }

    public void setTitle(String title) {
        this.builder.setTitle(title);
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void show() {
        AlertDialog dialog = builder.create();
        server.setOnPostUrlListener((url) -> mHandler.post(() -> {
            EditText view = dialog.findViewById(R.id.channel_source_url);
            view.setText(url);
        }));
        List<String> addresses = HostIP.getHostIP();
        String address = addresses.size() > 0 ? addresses.get(0) : null;
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT);
            String displayAddress = address != null ? address : "0.0.0.0";
            Log.i(TAG, String.format(Locale.ENGLISH, "start http server at: %s:%d", displayAddress, server.getListeningPort()));
        } catch (IOException eio) {
            Log.i(TAG, String.format(Locale.ENGLISH, "start http server failed, %s", eio));
        }
        dialog.show();
        if (address != null && server.getListeningPort() > 0) {
            TextView v = dialog.findViewById(R.id.ip_address);
            String template = context.getString(R.string.live_channel_web_setting_hint_template);
            v.setText(String.format(Locale.getDefault(), template, address, server.getListeningPort()));
        }
        @SuppressLint("CutPasteId") EditText v = dialog.findViewById(R.id.channel_source_url);
        if (defaultValue != null && !defaultValue.isEmpty()) {
            v.setText(defaultValue);
        }
    }

    public interface OnChannelSourceSubmitListener {
        void onChannelSourceSubmit(String url);
    }
}
