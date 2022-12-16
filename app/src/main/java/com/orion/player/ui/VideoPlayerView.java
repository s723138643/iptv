package com.orion.player.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.DisplayCutoutCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.orion.iptv.R;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtVideoSize;

import java.util.Locale;

public class VideoPlayerView extends FrameLayout {
    private static final String TAG = "VideoPlayerView";
    private static final int GestureInsetXdp = 48;
    private static final int GestureInsetYdp = 48;

    private ComponentListener componentListener;
    private SettingsButtonCallback settingsCallback;

    private VideoView videoView;
    private ConstraintLayout overlay;
    private Gesture gesture;
    private PlayerController controller;
    private Buffering buffering;
    private Toast toast;
    private TextView title;
    private NetworkSpeed networkSpeed;

    private final Rect gestureArea = new Rect(0, 0);
    private Rect gesturePadding = new Rect(0, 0, 0, 0);
    private Rect overlayPadding = new Rect(0, 0, 0, 0);
    private final int[] overlayLocation = new int[2];
    private ConstraintLayout.LayoutParams original;

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
        overlay = findViewById(R.id.overlay);
        controller = findViewById(R.id.player_controller);
        gesture = findViewById(R.id.gesture);
        buffering = findViewById(R.id.buffering);
        toast = findViewById(R.id.toast);
        ViewGroup header = findViewById(R.id.header);
        title = findViewById(R.id.title);
        ImageButton settings = findViewById(R.id.settings);
        networkSpeed = findViewById(R.id.network_speed);

        header.setVisibility(View.GONE);
        toast.hide();
        buffering.hide();
        gesture.hide();
        controller.hide();

        componentListener = new ComponentListener();

        original = (ConstraintLayout.LayoutParams) overlay.getLayoutParams();
        ViewCompat.setOnApplyWindowInsetsListener(overlay, (v, insets) -> {
            Log.w(TAG, "apply window insets...");
            Insets gesInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
            gesturePadding = new Rect(gesInsets.left, gesInsets.top, gesInsets.right, gesInsets.bottom);

            Insets sysInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int paddingH = Math.max(sysInsets.left, sysInsets.right);
            int paddingV = Math.max(sysInsets.top, sysInsets.bottom);
            Log.w(TAG, String.format(Locale.getDefault(), "paddingH: %d, paddingV: %d", paddingH, paddingV));
            DisplayCutoutCompat cutout = insets.getDisplayCutout();
            if (cutout != null) {
                paddingH = Math.max(paddingH, Math.max(cutout.getSafeInsetLeft(), cutout.getSafeInsetRight()));
                paddingV = Math.max(paddingV, Math.max(cutout.getSafeInsetTop(), cutout.getSafeInsetBottom()));
            }
            Log.w(TAG, String.format(Locale.getDefault(), "paddingH: %d, paddingV: %d", paddingH, paddingV));
            WindowInsets rootInsets = overlay.getRootWindowInsets();
            if (rootInsets != null) {
                int cornerH = 0;
                int cornerV = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RoundedCorner topLeft = rootInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT);
                    if (topLeft != null) {
                        cornerH = Math.max(cornerH, topLeft.getRadius());
                    }
                    RoundedCorner topRight = rootInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT);
                    if (topRight != null) {
                        cornerH = Math.max(cornerH, topRight.getRadius());
                    }
                    RoundedCorner bottomLeft = rootInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT);
                    if (bottomLeft != null) {
                        cornerV = Math.max(cornerV, bottomLeft.getRadius());
                    }
                    RoundedCorner bottomRight = rootInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT);
                    if (bottomRight != null) {
                        cornerV = Math.max(cornerV, bottomRight.getRadius());
                    }
                }
                int offsetH = cornerH - (int) ((float) cornerH * Math.sin(Math.toRadians(45)));
                int offsetV = cornerV - (int) ((float) cornerV * Math.sin(Math.toRadians(45)));
                paddingH = Math.max(paddingH, offsetH);
                paddingV = Math.max(paddingV, offsetV);
            }
            Log.w(TAG, String.format(Locale.getDefault(), "paddingH: %d, paddingV: %d", paddingH, paddingV));
            overlayPadding = new Rect(paddingH, paddingV, paddingH, paddingV);

            return WindowInsetsCompat.CONSUMED;
        });
        overlay.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)->{
            if (left==oldLeft && top==oldTop && right==oldRight && bottom==oldBottom) {
                return;
            }
            Log.w(TAG, String.format(Locale.getDefault(), "layout changed: (%d, %d, %d, %d)", left, top, right, bottom));
            overlay.getLocationInWindow(overlayLocation);
            Log.w(TAG, String.format(Locale.getDefault(), "overlay location: (%d, %d)", overlayLocation[0], overlayLocation[1]));
            gestureArea.reset(left, top, right, bottom);
            gestureArea.inset(gesturePadding);
            gestureArea.inset(dp2px(GestureInsetXdp), dp2px(GestureInsetYdp));
            Log.w(TAG, String.format(Locale.getDefault(), "gesture area: (%d, %d, %d, %d)", gestureArea.left, gestureArea.top, gestureArea.right, gestureArea.bottom));

            if (overlayLocation[0] <= overlayPadding.left || overlayLocation[1] <= overlayPadding.top) {
                int paddingLeft = Math.max(0, overlayPadding.left - original.leftMargin);
                int paddingTop = Math.max(0, overlayPadding.top - original.topMargin);
                int paddingRight = Math.max(0, overlayPadding.right - original.rightMargin);
                int paddingBottom = Math.max(0, overlayPadding.bottom - original.bottomMargin);
                Log.w(TAG, String.format(Locale.getDefault(), "apply padding: (%d, %d, %d, %d)", paddingLeft, paddingTop, paddingRight, paddingBottom));
                overlay.post(()->overlay.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom));
            }
        });

        GestureListener gestureListener = new GestureListener();
        GestureDetectorCompat gestureDetector = new GestureDetectorCompat(getContext(), gestureListener);
        setOnTouchListener((mView, event)-> {
            mView.performClick();
            boolean handled =  gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (gestureListener.isScrolling()) {
                    gestureListener.stopScrolling();
                }
            }
            return handled;
        });
        setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN || controller.isVisible()) {
                return false;
            }
            boolean handled = false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_ENTER:
                    controller.show();
                    handled = true;
                    break;
            }
            return handled;
        });
        controller.setOnVisibilityChangedListener(header::setVisibility);
        settings.setOnClickListener(v -> {
            if (settingsCallback != null) {
                settingsCallback.onClicked(v);
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    protected float dp2px(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
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
        networkSpeed.setPlayer(iExtPlayer);
        if (this.iExtPlayer != null) {
            this.iExtPlayer.removeListener(componentListener);
        }
        this.iExtPlayer = iExtPlayer;
        iExtPlayer.addListener(componentListener);
    }

    public void setOrientationSwitchCallback(PlayerController.OrientationSwitchCallback callback) {
        controller.setOrientationCallback(callback);
    }

    public void setSettingsCallback(SettingsButtonCallback callback) {
        settingsCallback = callback;
    }

    public void showNetworkSpeed(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        networkSpeed.setVisibility(visibility);
    }

    private void updateAspectRatio(ExtVideoSize videoSize) {
        videoView.setVideoSize(videoSize);
    }

    protected void setTitle(ExtDataSource dataSource) {
        Uri uri = Uri.parse(dataSource.getUri());
        String file = uri.getLastPathSegment();
        if (file == null || !file.contains(".")) {
            return;
        }
        title.setText(file);
        title.setSelected(true);
    }

    public interface SettingsButtonCallback {
        void onClicked(View v);
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
            setTitle(dataSource);
            controller.show();
            setKeepScreenOn(true);
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
                    setKeepScreenOn(false);
                    break;
                case IExtPlayer.STATE_BUFFERING:
                    buffering.show();
                    break;
                case IExtPlayer.STATE_IDLE:
                    setKeepScreenOn(false);
                    break;
            }
        }

        @Override
        public void onVideoSizeChanged(@NonNull ExtVideoSize videoSize) {
            updateAspectRatio(videoSize);
        }
    }
}
