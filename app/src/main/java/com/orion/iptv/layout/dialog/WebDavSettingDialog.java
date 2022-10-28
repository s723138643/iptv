package com.orion.iptv.layout.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.orion.iptv.R;
import com.orion.iptv.ui.shares.Share;

import org.json.JSONException;
import org.json.JSONObject;

public class WebDavSettingDialog extends DialogFragment {
    private OnSubmitListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle("添加WebDav")
                .setPositiveButton("ok", (dialog, which) -> {
                    AlertDialog alertDialog = (AlertDialog) dialog;
                    EditText name = alertDialog.findViewById(R.id.webdav_name);
                    assert name != null;
                    String webdavName = name.getText().toString();

                    EditText server = alertDialog.findViewById(R.id.webdav_server);
                    assert server != null;
                    String webdavServer = server.getText().toString();

                    EditText path = alertDialog.findViewById(R.id.webdav_path);
                    assert path != null;
                    String webdavPath = path.getText().toString();

                    EditText username = alertDialog.findViewById(R.id.webdav_username);
                    assert username != null;
                    String webdavUsername = username.getText().toString();

                    EditText password = alertDialog.findViewById(R.id.webdav_password);
                    assert password != null;
                    String webdavPassword = password.getText().toString();

                    JSONObject webdavConfig = new JSONObject();
                    try {
                        webdavConfig.put("server", webdavServer);
                        if (!webdavUsername.equals("")) {
                            webdavConfig.put("username", webdavUsername);
                            webdavConfig.put("password", webdavPassword);
                        }
                    } catch(JSONException ignored) {
                    }
                    if (listener != null) {
                        listener.onSubmit(new Share(webdavConfig, webdavName, webdavPath));
                    }
                })
                .setView(R.layout.dialog_webdav_setting)
                .create();
    }

    public void setOnSubmitListener(OnSubmitListener listener) {
        this.listener = listener;
    }

    public interface OnSubmitListener {
        void onSubmit(Share share);
    }
}
