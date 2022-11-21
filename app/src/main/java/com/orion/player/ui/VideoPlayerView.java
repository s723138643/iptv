package com.orion.player.ui;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.orion.iptv.R;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtVideoSize;

public class VideoPlayerView extends Fragment {
    @SuppressWarnings("unused")
    private static final String TAG = "VideoPlayerView";

    private ComponentListener componentListener;

    private VideoView videoView;
    private Gesture gesture;
    private PlayerController controller;
    private Buffering buffering;
    private Toast toast;

    IExtPlayer iExtPlayer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_player_view, container, false);
        FragmentManager fg = getChildFragmentManager();
        videoView = (VideoView) fg.findFragmentByTag("video_view");
        controller = (PlayerController) fg.findFragmentByTag("player_controller");
        gesture = (Gesture) fg.findFragmentByTag("gesture");
        buffering = (Buffering) fg.findFragmentByTag("buffering");
        toast = (Toast) fg.findFragmentByTag("toast");
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        toast.hide();
        buffering.hide();
        gesture.hide();
        controller.hide();
        componentListener = new ComponentListener();
        GestureListener gestureListener = new GestureListener();
        GestureDetectorCompat gestureDetector = new GestureDetectorCompat(requireContext(), gestureListener);
        requireView().setOnTouchListener((mView, event)-> {
            mView.performClick();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (gestureListener.isScrolling()) {
                    gestureListener.stopScrolling();
                }
            }
            return gestureDetector.onTouchEvent(event);
        });
    }

    /**
     * Sets the {@link IExtPlayer} to use.
     */
    public void setPlayer(@Nullable IExtPlayer iExtPlayer) {
        if (this.iExtPlayer == iExtPlayer) {
            return;
        }
        videoView.setPlayer(iExtPlayer);
        controller.setPlayer(iExtPlayer);
        @Nullable IExtPlayer oldIExtPlayer = this.iExtPlayer;
        if (oldIExtPlayer != null) {
            oldIExtPlayer.removeListener(componentListener);
        }
        this.iExtPlayer = iExtPlayer;
        if (iExtPlayer != null) {
            iExtPlayer.addListener(componentListener);
        }
    }

    /**
     * Returns the {@link AspectRatioFrameLayout.ResizeMode}.
     */
    @SuppressWarnings("unused")
    public @AspectRatioFrameLayout.ResizeMode int getResizeMode() {
        return videoView.getResizeMode();
    }

    @SuppressWarnings("unused")
    @Nullable
    public View getVideoSurfaceView() {
        return videoView.getVideoSurfaceView();
    }

    private void updateAspectRatio(ExtVideoSize videoSize) {
        videoView.setVideoSize(videoSize);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final float displayWidth;
        // max duration for gesture fast forward or rewind
        @SuppressWarnings("FieldCanBeLocal")
        private final float maxScrollDuration = 30.0f * 60 * 1000;
        @SuppressWarnings("FieldCanBeLocal")
        private final float MIN_SCROLL = 32.0f;
        private boolean scrolling;
        private long position;

        {
            DisplayMetrics metrics = requireContext().getResources().getDisplayMetrics();
            displayWidth = (float) metrics.widthPixels;
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (iExtPlayer == null) {
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
            controller.toggleVisibility();
            return true;
        }

        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            if (iExtPlayer == null) {
                return true;
            }
            float dx = e2.getX() - e1.getX();
            if (Math.abs(dx) < MIN_SCROLL) {
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
