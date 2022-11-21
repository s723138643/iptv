package com.orion.player.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.orion.iptv.R;

import java.util.Arrays;
import java.util.Locale;

public class Gesture extends Fragment {

    private ImageView indicator;
    private TextView position;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gesture, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        indicator = view.findViewById(R.id.direction_indicator);
        position = view.findViewById(R.id.position);
    }

    protected String formatDuration(long duration) {
        long[] d = new long[]{0, 0, 0};
        duration /= 1000;
        d[2] = duration % 60;
        duration /= 60;
        d[1] = duration % 60;
        d[0] = duration / 60;
        return String.format(Locale.getDefault(), "%d:%02d:%02d", d[0], d[1], d[2]);
    }

    @SuppressLint("SetTextI18n")
    public void setPosition(long position, long duration, boolean fastForward) {
        int res = fastForward ? com.google.android.exoplayer2.ui.R.drawable.exo_icon_fastforward : com.google.android.exoplayer2.ui.R.drawable.exo_icon_rewind;
        indicator.setImageResource(res);
        this.position.setText(formatDuration(position) + "/" + formatDuration(duration));
    }

    public void show() {
        if (!isHidden()) {
            return;
        }
        getParentFragmentManager().beginTransaction()
                .show(this)
                .commit();
    }

    public void hide() {
        if (isHidden()) {
            return;
        }
        getParentFragmentManager().beginTransaction()
                .hide(this)
                .commit();
    }
}