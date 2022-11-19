package com.orion.player.ui;

import android.os.Bundle;
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
    private static final String TAG = "VideoPlayerView";

    private ComponentListener componentListener;

    private VideoView videoView;
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
        buffering = (Buffering) fg.findFragmentByTag("buffering");
        toast = (Toast) fg.findFragmentByTag("toast");
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        toast.hide();
        buffering.hide();
        controller.hide();
        componentListener = new ComponentListener();
        GestureDetectorCompat gestureDetector = new GestureDetectorCompat(requireContext(), new GestureListener());
        requireView().setOnTouchListener((mView, event)-> {
            mView.performClick();
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
    public @AspectRatioFrameLayout.ResizeMode int getResizeMode() {
        return videoView.getResizeMode();
    }

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
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (controller.isHidden()) {
                controller.show(5*1000);
            } else {
                controller.hide();
            }
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {}

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    private final class ComponentListener implements IExtPlayer.Listener {
        @Override
        public void onPlayerError(Exception error) {
            toast.setMessage(error.toString());
        }

        @Override
        public void onDataSourceUsed(ExtDataSource dataSource) {
            toast.hide();
            buffering.show();
            controller.show(5*1000);
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            switch (state) {
                case IExtPlayer.STATE_READY:
                case IExtPlayer.STATE_IDLE:
                    buffering.hide();
                    break;
                case IExtPlayer.STATE_ENDED:
                    buffering.hide();
                    controller.show();
                    break;
                case IExtPlayer.STATE_BUFFERING:
                    buffering.show();
                    break;
            }
        }

        @Override
        public void onVideoSizeChanged(@NonNull ExtVideoSize videoSize) {
            updateAspectRatio(videoSize);
        }
    }
}
