package com.orion.iptv.layout.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;

import com.orion.iptv.R;

public class SearchDialog {

    private final String TAG = "SearchDialog";
    private final AlertDialog.Builder builder;
    private OnSubmitListener listener;
    private String defaultValue;

    public SearchDialog(Context context, String defaultValue) {
        this.defaultValue = defaultValue;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setView(R.layout.dialog_search);
        builder = builder.setTitle(R.string.locate);
        builder = builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            AlertDialog alertDialog = (AlertDialog) dialog;
            EditText text = alertDialog.findViewById(R.id.search_text);
            String input = text.getText().toString();
            if (input.equals("") || listener == null) {
                return;
            }
            this.defaultValue = input;
            listener.onSubmit(input);
        });
        this.builder = builder;
    }

    public void setOnSubmitListener(OnSubmitListener listener) {
        this.listener = listener;
    }

    public void show() {
        AlertDialog dialog = builder.create();
        dialog.show();
        @SuppressLint("CutPasteId") EditText v = dialog.findViewById(R.id.search_text);
        if (defaultValue != null && !defaultValue.isEmpty()) {
            v.setText(defaultValue);
        }
    }

    public void maybeSetLastSearched(String text) {
        if (text != null && text.isEmpty()) {
            return;
        }
        if (defaultValue==null || !defaultValue.equals(text)) {
            defaultValue = text;
        }
    }

    public interface OnSubmitListener {
        void onSubmit(String text);
    }
}