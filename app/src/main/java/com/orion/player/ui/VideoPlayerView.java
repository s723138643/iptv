package com.orion.player.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.orion.iptv.R;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtVideoSize;

public class VideoPlayerView extends FrameLayout {
    private static final String TAG = "VideoPlayerView";
    private static final int GestureInsetXdp = 48;
    private static final int GestureInsetYdp = 48;

    private ComponentListener componentListener;

    private VideoView videoView;
    private Gesture gesture;
    private PlayerController controller;
    private Buffering buffering;
    private Toast toast;
    private Gesture.Rect gestureArea;
    private int orientation;

    IExtPlayer iExtPlayer;

    public VideoPlayerView(@NonNull Context context) {
        this(context, null);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.fragment_video_player_view, this, true);
        initView();
    }

    protected void initView() {
        videoView = findViewById(R.id.video_view);
        controller = findViewById(R.id.player_controller);
        gesture = findViewById(R.id.gesture);
        buffering = findViewById(R.id.buffering);
        toast = findViewById(R.id.toast);

        toast.hide();
        buffering.hide();
        gesture.hide();
        controller.hide();

        componentListener = new ComponentListener();

        initGestureArea();
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            initGestureArea();
            Insets sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
            gestureArea.inset(sysInsets.left, sysInsets.top, sysInsets.right, sysInsets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        GestureListener gestureListener = new GestureListener();
        GestureDetectorCompat gestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
        setOnTouchListener((mView, event)-> {
            mView.performClick();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (gestureListener.isScrolling()) {
                    gestureListener.stopScrolling();
                }
            }
            return gestureDetector.onTouchEvent(event);
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == orientation) {
            return;
        }
        initGestureArea();
    }

    @SuppressWarnings("SameParameterValue")
    protected float dp2px(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    protected void initGestureArea() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        gestureArea = new Gesture.Rect(metrics.widthPixels, metrics.heightPixels);
        gestureArea.inset(dp2px(GestureInsetXdp), dp2px(GestureInsetYdp));
    }

    public void setSurfaceType(@VideoView.SurfaceType int type) {
        videoView.setSurfaceType(type);
    }

    /**
     * Sets the {@link IExtPlayer} to use.
     */
    public void setPlayer(@NonNull IExtPlayer iExtPlayer) {
        videoView.setPlayer(iExtPlayer);
        controller.setPlayer(iExtPlayer);
        if (this.iExtPlayer != null) {
            this.iExtPlayer.removeListener(componentListener);
        }
        this.iExtPlayer = iExtPlayer;
        iExtPlayer.addListener(componentListener);
    }

    public void setOrientationSwitchCallback(PlayerController.OrientationSwitchCallback callback) {
        controller.setOrientationCallback(callback);
    }

    public void setOnControllerVisibilityChangedListener(PlayerController.OnVisibilityChangedListener listener) {
        controller.setOnVisibilityChangedListener(listener);
    }

    private void updateAspectRatio(ExtVideoSize videoSize) {
        videoView.setVideoSize(videoSize);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final float displayWidth;
        // max duration for gesture fast forward or rewind
        @SuppressWarnings("FieldCanBeLocal")
        private final float maxScrollDuration = 10.0f * 60 * 1000;
        @SuppressWarnings("FieldCanBeLocal")
        private final float MIN_SCROLL = 32.0f;
        private boolean scrolling;
        private long position;

        {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            displayWidth = (float) metrics.widthPixels;
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return gestureArea.in(e.getX(), e.getY());
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (iExtPlayer == null || !gestureArea.in(e.getX(), e.getY())) {
                return true;
            }
            if (iExtPlayer.isPlaying()) {
                iExtPlayer.pause();
            } else {
                iExtPlayer.play();
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!gestureArea.in(e.getX(), e.getY())) {
                return false;
            }
            controller.toggleVisibility();
            return true;
        }

        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            if (iExtPlayer == null || !gestureArea.in(e1.getX(), e1.getY())) {
                return true;
            }
            float dx = e2.getX() - e1.getX();
            float dy = e2.getY() - e1.getY();
            if (Math.abs(dx) < MIN_SCROLL || Math.abs(dx) <= Math.abs(dy) * 1.78) {
                return true;
            }
            // real scroll distance, make sure it start at 0
            dx = dx > 0 ? dx - MIN_SCROLL : dx + MIN_SCROLL;
            int duration = (int) (dx / displayWidth * maxScrollDuration);
            position = duration + iExtPlayer.getCurrentPosition();
            long mediaDuration = iExtPlayer.getDuration();
            if (position < 0) {
                position = 0;
            } else if (position > mediaDuration) {
                position = mediaDuration;
            }
            gesture.setPosition(position, mediaDuration, dx>=0);
            if (!scrolling) {
                scrolling = true;
                gesture.show();
                controller.hide();
            }
            return true;
        }

        private boolean isScrolling() {
            return scrolling;
        }

        private void stopScrolling() {
            scrolling = false;
            if (iExtPlayer != null) {
                iExtPlayer.seekTo(position);
                if (!iExtPlayer.isPlaying()) {
                    iExtPlayer.play();
                }
            }
            position = 0;
            gesture.hide();
        }
    }

    private final class ComponentListener implements IExtPlayer.Listener {
        @Override
        public void onPlayerError(Exception error) {
            buffering.hide();
            toast.setMessage(error.toString());
        }

        @Override
        public void onDataSourceUsed(ExtDataSource dataSource) {
            toast.hide();
            buffering.show();
            controller.show();
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            switch (state) {
                case IExtPlayer.STATE_READY:
                    buffering.hide();
                    break;
                case IExtPlayer.STATE_ENDED:
                    buffering.hide();
                    controller.show();
                    break;
                case IExtPlayer.STATE_BUFFERING:
                    buffering.show();
                    break;
                case IExtPlayer.STATE_IDLE:
                    break;
            }
        }

        @Override
        public void onVideoSizeChanged(@NonNull ExtVideoSize videoSize) {
            updateAspectRatio(videoSize);
        }
    }
}
