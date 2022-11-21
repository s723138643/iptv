package com.orion.player.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orion.iptv.R;

public class Toast extends Fragment {
    private TextView toast;
    private Handler mHandler;
    private final Runnable hideMyself = this::hide;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mHandler = new Handler(requireContext().getMainLooper());
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_toast, container, false);
        toast = view.findViewById(R.id.toast);
        return view;
    }

    public void setMessage(String message, long displayMillis) {
        setMessage(message);
        mHandler.postDelayed(hideMyself, displayMillis);
    }

    public void setMessage(String message) {
        toast.setText(message);
        show();
    }

    public void show() {
        mHandler.removeCallbacks(hideMyself);
        getParentFragmentManager().beginTransaction()
                .show(this)
                .commit();
    }

    public void hide() {
        getParentFragmentManager().beginTransaction()
                .hide(this)
                .commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(null);
    }
}