package com.orion.iptv.layout.dialog;

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
import java.util.List;
import java.util.Optional;

import fi.iki.elonen.NanoHTTPD;

public class ChannelSourceDialog {

    public interface OnChannelSourceSubmitListener {
        void onChannelSourceSubmit(String url);
    }

    private final String TAG = "ChannelSourceDialog";
    private final Handler mHandler;
    private final AlertDialog dialog;
    private final WebServer server;
    private OnChannelSourceSubmitListener listener;

    public ChannelSourceDialog(Context context) {
        mHandler = new Handler(context.getMainLooper());
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setTitle("setting channel source");
        builder = builder.setView(R.layout.channel_source_dialog);
        builder = builder.setPositiveButton("ok", (dialog, which) -> {
            AlertDialog alertDialog = (AlertDialog)dialog;
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
        dialog = builder.create();
        server.setOnPostUrlListener((url)->{
            mHandler.post(()->{
                EditText view =  dialog.findViewById(R.id.channel_source_url);
                view.setText(url);
            });
        });
    }

    public void setOnChannelSourceSubmitListener(OnChannelSourceSubmitListener listener) {
        this.listener = listener;
    }

    public void show() {
        Optional<String> address = HostIP.getHostIP().stream().findFirst();
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT);
            Log.i(TAG, String.format(Locale.ENGLISH, "start http server at: %s:%d", address.orElse("0.0.0.0"), server.getListeningPort()));
        } catch (IOException eio) {
            Log.i(TAG, String.format(Locale.ENGLISH, "start http server failed, %s", eio));
        }
        dialog.show();
        if (address.isPresent()) {
            TextView v = dialog.findViewById(R.id.ip_address);
            v.setText(String.format(Locale.ENGLISH, "通过访问 http://%s:%d 设置频道源", address.get(), server.getListeningPort()));
        }
    }
}
