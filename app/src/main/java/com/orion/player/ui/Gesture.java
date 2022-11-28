package com.orion.player.ui;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.orion.iptv.R;

import java.util.Locale;

public class Gesture extends FrameLayout {


    private final int fastForwardIconRes = com.google.android.exoplayer2.ui.R.drawable.exo_icon_fastforward;
    private final int rewindIconRes = com.google.android.exoplayer2.ui.R.drawable.exo_icon_rewind;
    private ImageView indicator;
    private TextView position;

    public Gesture(@NonNull Context context) {
        this(context, null);
    }

    public Gesture(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Gesture(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Gesture(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.fragment_gesture, this, true);
        initView();
    }

    public void initView() {
        indicator = findViewById(R.id.direction_indicator);
        position = findViewById(R.id.position);
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
        int res = fastForward ? fastForwardIconRes : rewindIconRes;
        indicator.setImageResource(res);
        this.position.setText(formatDuration(position) + "/" + formatDuration(duration));
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }
}