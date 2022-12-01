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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.CueGroup;
import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.video.VideoDecoderGLSurfaceView;
import com.orion.iptv.R;
import com.orion.player.IExtPlayer;
import com.orion.player.ExtVideoSize;
import com.orion.player.exo.ExtExoPlayer;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

public class VideoView extends FrameLayout {
    @SuppressWarnings("unused")
    private static final String TAG = "VideoView";

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({METHOD, PARAMETER, TYPE_USE})
    @IntDef({SURFACE_TYPE_SURFACE_VIEW, SURFACE_TYPE_TEXTURE_VIEW, SURFACE_TYPE_GL_SURFACE_VIEW})
    public @interface SurfaceType{}
    public static final int SURFACE_TYPE_SURFACE_VIEW = 0;
    public static final int SURFACE_TYPE_TEXTURE_VIEW = 1;
    public static final int SURFACE_TYPE_GL_SURFACE_VIEW = 2;

    private final ComponentListener componentListener;
    private final AspectRatioFrameLayout contentFrame;
    private final SubtitleView subtitle;
    private @Nullable View surfaceView;
    private @Nullable IExtPlayer iExtPlayer;
    private @SurfaceType int surfaceType;

    public VideoView(@NonNull Context context) {
        this(context, null);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.fragment_video_view, this, true);
        subtitle = findViewById(R.id.subtitle);
        subtitle.setStyle(new CaptionStyleCompat(
                Color.WHITE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_NONE,
                Color.TRANSPARENT,
                Typeface.DEFAULT));
        contentFrame = findViewById(R.id.content_frame);
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
                surface = new TextureView(getContext());
                break;
            case SURFACE_TYPE_GL_SURFACE_VIEW:
                surface = new VideoDecoderGLSurfaceView(getContext());
                break;
            case SURFACE_TYPE_SURFACE_VIEW:
            default:
                surface = new SurfaceView(getContext());
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
            iExtPlayer.setVideoSurfaceView((SurfaceView) surface);
        } else if (surface instanceof TextureView) {
            iExtPlayer.setVideoTextureView((TextureView) surface);
        }
    }

    @SuppressWarnings("unused")
    public void setSurfaceType(@SurfaceType int surfaceType) {
        if (this.surfaceType == surfaceType) {
            return;
        }
        assert contentFrame != null;
        this.surfaceType = surfaceType;
        View newSurface = createSurfaceView(surfaceType);
        if (surfaceView != null) {
            maybeDetachViewFromPlayer(surfaceView);
            contentFrame.removeView(surfaceView);
        }
        surfaceView = newSurface;
        contentFrame.addView(newSurface, 0);
        maybeAttachViewToPlayer(newSurface);
    }

    @SuppressWarnings("unused")
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
            subtitle.setCues(List.of(Cue.EMPTY));
            if (surfaceView instanceof TextureView) {
                oldIExtPlayer.clearVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                oldIExtPlayer.clearVideoSurfaceView((SurfaceView) surfaceView);
            }
            assert surfaceView != null;
            if (!(oldIExtPlayer instanceof ExtExoPlayer && iExtPlayer instanceof ExtExoPlayer)) {
                surfaceView.setVisibility(View.GONE);
                surfaceView.setVisibility(View.VISIBLE);
            }
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

        @Override
        public void onCues(CueGroup cueGroup) {
            subtitle.setCues(cueGroup.cues);
        }

        // IExtPlayer.Listener implementation
        @Override
        public void onVideoSizeChanged(@NonNull ExtVideoSize videoSize) {
            updateAspectRatio(videoSize);
        }
    }
}
