/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orion.player.ui;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orion.iptv.R;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtVideoSize;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class VideoView extends Fragment {
    private static final String TAG = "VideoView";


    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({METHOD, PARAMETER, TYPE_USE})
    @IntDef({SURFACE_TYPE_SURFACE_VIEW, SURFACE_TYPE_TEXTURE_VIEW})
    public @interface SurfaceType{}
    public static final int SURFACE_TYPE_SURFACE_VIEW = 0;
    public static final int SURFACE_TYPE_TEXTURE_VIEW = 1;

    private ComponentListener componentListener;
    private AspectRatioFrameLayout contentFrame;
    private @Nullable View surfaceView;
    private @Nullable IExtPlayer iExtPlayer;
    private @SurfaceType int surfaceType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_view, container, false);
        contentFrame = (AspectRatioFrameLayout) view;
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        componentListener = new ComponentListener();
        surfaceType = SURFACE_TYPE_SURFACE_VIEW;
        // Content frame.
        contentFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        surfaceView = createSurfaceView(surfaceType);
        contentFrame.addView(surfaceView, 0);
    }

    @NonNull
    private View createSurfaceView(@SurfaceType int surfaceType) {
        View surface;
        switch (surfaceType) {
            case SURFACE_TYPE_TEXTURE_VIEW:
                surface = new TextureView(requireContext());
                break;
            case SURFACE_TYPE_SURFACE_VIEW:
            default:
                surface = new SurfaceView(requireContext());
                break;
        }
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        surface.setLayoutParams(params);
        // We don't want surfaceView to be clickable separately to the StyledPlayerView itself, but we
        // do want to register as an OnClickListener so that surfaceView implementations can propagate
        // click events up to the StyledPlayerView by calling their own performClick method.
        surface.setClickable(false);
        return surface;
    }

    private void maybeDetachViewFromPlayer(View surface) {
        if (iExtPlayer == null) {
            return;
        }
        if (surface instanceof SurfaceView) {
            iExtPlayer.clearVideoSurfaceView((SurfaceView) surface);
        } else if (surface instanceof TextureView) {
            iExtPlayer.clearVideoTextureView((TextureView) surface);
        }
    }

    private void maybeAttachViewToPlayer(View surface) {
        if (iExtPlayer == null) {
            return;
        }
        if (surface instanceof SurfaceView) {
            iExtPlayer.clearVideoSurfaceView((SurfaceView) surface);
        } else if (surface instanceof TextureView) {
            iExtPlayer.clearVideoTextureView((TextureView) surface);
        }
    }

    public void setSurfaceType(@SurfaceType int surfaceType) {
        assert contentFrame != null;
        this.surfaceType = surfaceType;
        View newSurface = createSurfaceView(surfaceType);
        if (surfaceView != null) {
            contentFrame.removeView(surfaceView);
            maybeDetachViewFromPlayer(surfaceView);
        }
        surfaceView = newSurface;
        maybeAttachViewToPlayer(newSurface);
        contentFrame.addView(newSurface, 0);
    }

    public void setResizeMode(@AspectRatioFrameLayout.ResizeMode int resizeMode) {
        assert contentFrame != null;
        contentFrame.setResizeMode(resizeMode);
    }

    public void setVideoSize(ExtVideoSize videoSize) {
        assert contentFrame != null;
        contentFrame.setVideoSize(videoSize);
    }

    /**
     * Returns the IExtPlayer currently set on this view, or null if no IExtPlayer is set.
     */
    @Nullable
    public IExtPlayer getPlayer() {
        return iExtPlayer;
    }

    /**
     * Sets the {@link IExtPlayer} to use.
     */
    public void setPlayer(@Nullable IExtPlayer iExtPlayer) {
        if (this.iExtPlayer == iExtPlayer) {
            return;
        }
        @Nullable IExtPlayer oldIExtPlayer = this.iExtPlayer;
        if (oldIExtPlayer != null) {
            oldIExtPlayer.removeListener(componentListener);
            if (surfaceView instanceof TextureView) {
                oldIExtPlayer.clearVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                oldIExtPlayer.clearVideoSurfaceView((SurfaceView) surfaceView);
            }
            assert surfaceView != null;
            surfaceView.setVisibility(View.GONE);
            surfaceView.setVisibility(View.VISIBLE);
        }
        this.iExtPlayer = iExtPlayer;
        if (iExtPlayer != null) {
            if (surfaceView instanceof TextureView) {
                iExtPlayer.setVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                iExtPlayer.setVideoSurfaceView((SurfaceView) surfaceView);
            }
            updateAspectRatio(iExtPlayer.getVideoSize());
            iExtPlayer.addListener(componentListener);
        }
    }

    /**
     * Returns the {@link AspectRatioFrameLayout.ResizeMode}.
     */
    public @AspectRatioFrameLayout.ResizeMode int getResizeMode() {
        assert contentFrame != null;
        return contentFrame.getResizeMode();
    }

    @Nullable
    public View getVideoSurfaceView() {
        return surfaceView;
    }

    private void updateAspectRatio(ExtVideoSize videoSize) {
        if (iExtPlayer == null || videoSize.equals(ExtVideoSize.UNKNOWN) || surfaceView == null) {
            return;
        }
        assert contentFrame != null;
        contentFrame.setVideoSize(videoSize);
    }

    private final class ComponentListener implements IExtPlayer.Listener {

        // IExtPlayer.Listener implementation
        @Override
        public void onVideoSizeChanged(@NonNull ExtVideoSize videoSize) {
            updateAspectRatio(videoSize);
        }
    }
}
