package com.orion.player.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orion.iptv.R;

public class Buffering extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_buffering, container, false);
    }

    public void show() {
        getParentFragmentManager().beginTransaction()
                .show(this)
                .commit();
    }

    public void hide() {
        getParentFragmentManager().beginTransaction()
                .hide(this)
                .commit();
    }
}