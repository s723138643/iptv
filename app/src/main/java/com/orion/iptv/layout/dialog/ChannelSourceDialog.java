package com.orion.iptv.layout.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.R;
import com.orion.iptv.network.HostIP;
import com.orion.iptv.webserver.WebServer;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import fi.iki.elonen.NanoHTTPD;

public class ChannelSourceDialog {

    private final String TAG = "ChannelSourceDialog";
    private final Handler mHandler;
    private final AlertDialog.Builder builder;
    private final WebServer server;
    private OnChannelSourceSubmitListener listener;
    private String inputHint;
    private String defaultValue;
    public ChannelSourceDialog(Context context) {
        mHandler = new Handler(context.getMainLooper());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setView(R.layout.dialog_live_channel_source);
        builder = builder.setPositiveButton("ok", (dialog, which) -> {
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

    public void setInputHint(String hint) {
        this.inputHint = hint;
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
        Optional<String> address = HostIP.getHostIP().stream().findFirst();
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT);
            Log.i(TAG, String.format(Locale.ENGLISH, "start http server at: %s:%d", address.orElse("0.0.0.0"), server.getListeningPort()));
        } catch (IOException eio) {
            Log.i(TAG, String.format(Locale.ENGLISH, "start http server failed, %s", eio));
        }
        dialog.show();
        if (address.isPresent() && server.getListeningPort() > 0) {
            TextView v = dialog.findViewById(R.id.ip_address);
            v.setText(String.format(Locale.ENGLISH, "通过访问 http://%s:%d 设置", address.get(), server.getListeningPort()));
        }
        @SuppressLint("CutPasteId") EditText v = dialog.findViewById(R.id.channel_source_url);
        if (!defaultValue.isEmpty()) {
            v.setText(defaultValue);
        } else if (!inputHint.isEmpty()) {
            v.setAutofillHints(inputHint);
        }
    }

    public interface OnChannelSourceSubmitListener {
        void onChannelSourceSubmit(String url);
    }
}
