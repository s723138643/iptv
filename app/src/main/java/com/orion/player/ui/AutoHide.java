package com.orion.player.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AutoHide extends FrameLayout {
    private final List<EventListener> listeners;

    public AutoHide(@NonNull Context context) {
        this(context, null);
    }

    public AutoHide(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoHide(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AutoHide(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        listeners = new ArrayList<>();
    }

    public void addEventListener(EventListener listener) {
        listeners.add(listener);
    }

    public void remoteEventListener(EventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        listeners.forEach(listener -> listener.onMotionEvent(ev));
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        listeners.forEach(listener -> listener.onKeyEvent(event));
        return super.dispatchKeyEvent(event);
    }

    public interface EventListener {
        default void onMotionEvent(MotionEvent ev) {}
        default void onKeyEvent(KeyEvent ev) {}
    }
}
