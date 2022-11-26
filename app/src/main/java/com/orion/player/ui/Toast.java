package com.orion.player.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.orion.iptv.R;

public class Toast extends FrameLayout {
    private TextView toast;
    private long hideMyselfAt = 0;
    private final Runnable hideMyself = new Runnable() {
        @Override
        public void run() {
            if (hideMyselfAt <= 0) {
                return;
            }
            long diff = SystemClock.uptimeMillis() - hideMyselfAt;
            if (diff >= 0) {
                hide();
            } else {
                postDelayed(this, -diff);
            }
        }
    };

    public Toast(@NonNull Context context) {
        this(context, null);
    }

    public Toast(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Toast(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Toast(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // Inflate the layout for this fragment
        LayoutInflater.from(context).inflate(R.layout.fragment_toast, this, true);
        initView();
    }

    public void initView() {
        toast = findViewById(R.id.toast_text);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        removeCallbacks(hideMyself);
        if (visibility == View.VISIBLE && hideMyselfAt > 0) {
            postDelayed(hideMyself, Math.max(hideMyselfAt - SystemClock.uptimeMillis(), 1));
        }
    }

    public void setMessage(String message, long displayMillis) {
        hideMyselfAt = SystemClock.uptimeMillis() + displayMillis;
        toast.setText(message);
        show();
    }

    public void setMessage(String message) {
        hideMyselfAt = 0;
        toast.setText(message);
        show();
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }
}