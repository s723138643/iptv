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
package com.orion.iptv.ui.live.player;

import static com.google.android.exoplayer2.Player.COMMAND_SET_VIDEO_SURFACE;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.video.VideoSize;
import com.orion.iptv.R;
import com.orion.iptv.ui.live.player.AspectRatioFrameLayout.ResizeMode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class PlayerView extends FrameLayout {

    /**
     * The buffering view is never shown.
     */
    public static final int SHOW_BUFFERING_NEVER = 0;
    /**
     * The buffering view is shown when the player is in the {@link Player#STATE_BUFFERING buffering}
     * state and {@link Player#getPlayWhenReady() playWhenReady} is {@code true}.
     */
    public static final int SHOW_BUFFERING_WHEN_PLAYING = 1;
    /**
     * The buffering view is always shown when the player is in the {@link Player#STATE_BUFFERING
     * buffering} state.
     */
    public static final int SHOW_BUFFERING_ALWAYS = 2;
    private static final int SURFACE_TYPE_NONE = 0;
    private static final int SURFACE_TYPE_SURFACE_VIEW = 1;
    private static final int SURFACE_TYPE_TEXTURE_VIEW = 2;
    private static final int SURFACE_TYPE_SPHERICAL_GL_SURFACE_VIEW = 3;
    private static final int SURFACE_TYPE_VIDEO_DECODER_GL_SURFACE_VIEW = 4;
    private final ComponentListener componentListener;
    @Nullable
    private final AspectRatioFrameLayout contentFrame;
    @Nullable
    private final View surfaceView;
    @Nullable
    private final View bufferingView;
    @Nullable
    private final TextView errorMessageView;
    @Nullable
    private Player player;
    private @ShowBuffering int showBuffering;
    private boolean keepContentOnPlayerReset;
    @Nullable
    private ErrorMessageProvider<? super PlaybackException> errorMessageProvider;
    @Nullable
    private CharSequence customErrorMessage;
    private int textureViewRotation;
    public PlayerView(Context context) {
        this(context, /* attrs= */ null);
    }
    public PlayerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, /* defStyleAttr= */ 0);
    }

    @SuppressWarnings({"nullness:argument", "nullness:method.invocation"})
    public PlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        componentListener = new ComponentListener();

        boolean surfaceViewIgnoresVideoAspectRatio1;
        if (isInEditMode()) {
            contentFrame = null;
            surfaceView = null;
            surfaceViewIgnoresVideoAspectRatio1 = false;
            bufferingView = null;
            errorMessageView = null;
            return;
        }

        int playerLayoutId = R.layout.layout_live_player_view;
        int surfaceType = SURFACE_TYPE_SURFACE_VIEW;
        int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
        int showBuffering = SHOW_BUFFERING_NEVER;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PlayerView, defStyleAttr,/* defStyleRes= */0);
            try {
                playerLayoutId = a.getResourceId(R.styleable.PlayerView_player_layout_id, playerLayoutId);
                surfaceType = a.getInt(R.styleable.PlayerView_surface_type, surfaceType);
                resizeMode = a.getInt(R.styleable.PlayerView_resize_mode, resizeMode);
                showBuffering = a.getInteger(R.styleable.PlayerView_show_buffering, showBuffering);
                keepContentOnPlayerReset = a.getBoolean(R.styleable.PlayerView_keep_content_on_player_reset, keepContentOnPlayerReset);
            } finally {
                a.recycle();
            }
        }

        LayoutInflater.from(context).inflate(playerLayoutId, this);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        // Content frame.
        contentFrame = findViewById(R.id.player_content_frame);
        if (contentFrame != null) {
            setResizeModeRaw(contentFrame, resizeMode);
        }

        // Create a surface view and insert it into the content frame, if there is one.
        boolean surfaceViewIgnoresVideoAspectRatio = false;
        if (contentFrame != null && surfaceType != SURFACE_TYPE_NONE) {
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            switch (surfaceType) {
                case SURFACE_TYPE_TEXTURE_VIEW:
                    surfaceView = new TextureView(context);
                    break;
                case SURFACE_TYPE_SPHERICAL_GL_SURFACE_VIEW:
                    try {
                        Class<?> clazz = Class.forName("com.google.android.exoplayer2.video.spherical.SphericalGLSurfaceView");
                        surfaceView = (View) clazz.getConstructor(Context.class).newInstance(context);
                    } catch (Exception e) {
                        throw new IllegalStateException("spherical_gl_surface_view requires an ExoPlayer dependency", e);
                    }
                    surfaceViewIgnoresVideoAspectRatio = true;
                    break;
                case SURFACE_TYPE_VIDEO_DECODER_GL_SURFACE_VIEW:
                    try {
                        Class<?> clazz = Class.forName("com.google.android.exoplayer2.video.VideoDecoderGLSurfaceView");
                        surfaceView = (View) clazz.getConstructor(Context.class).newInstance(context);
                    } catch (Exception e) {
                        throw new IllegalStateException("video_decoder_gl_surface_view requires an ExoPlayer dependency", e);
                    }
                    break;
                default:
                    surfaceView = new SurfaceView(context);
                    break;
            }
            surfaceView.setLayoutParams(params);
            // We don't want surfaceView to be clickable separately to the StyledPlayerView itself, but we
            // do want to register as an OnClickListener so that surfaceView implementations can propagate
            // click events up to the StyledPlayerView by calling their own performClick method.
            surfaceView.setClickable(false);
            contentFrame.addView(surfaceView, 0);
        } else {
            surfaceView = null;
        }
        surfaceViewIgnoresVideoAspectRatio1 = surfaceViewIgnoresVideoAspectRatio;

        // Buffering view.
        bufferingView = findViewById(R.id.player_buffering);
        if (bufferingView != null) {
            bufferingView.setVisibility(View.GONE);
        }
        this.showBuffering = showBuffering;

        // Error message view.
        errorMessageView = findViewById(R.id.player_error_message);
        if (errorMessageView != null) {
            errorMessageView.setVisibility(View.GONE);
        }
    }

    /**
     * Switches the view targeted by a given {@link Player}.
     *
     * @param player        The player whose target view is being switched.
     * @param oldPlayerView The old view to detach from the player.
     * @param newPlayerView The new view to attach to the player.
     */
    public static void switchTargetView(Player player, @Nullable PlayerView oldPlayerView, @Nullable PlayerView newPlayerView) {
        if (oldPlayerView == newPlayerView) {
            return;
        }
        // We attach the new view before detaching the old one because this ordering allows the player
        // to swap directly from one surface to another, without transitioning through a state where no
        // surface is attached. This is significantly more efficient and achieves a more seamless
        // transition when using platform provided video decoders.
        if (newPlayerView != null) {
            newPlayerView.setPlayer(player);
        }
        if (oldPlayerView != null) {
            oldPlayerView.setPlayer(null);
        }
    }

    @SuppressWarnings("ResourceType")
    private static void setResizeModeRaw(AspectRatioFrameLayout aspectRatioFrame, int resizeMode) {
        aspectRatioFrame.setResizeMode(resizeMode);
    }

    /**
     * Applies a texture rotation to a {@link TextureView}.
     */
    private static void applyTextureViewRotation(TextureView textureView, int textureViewRotation) {
        Matrix transformMatrix = new Matrix();
        float textureViewWidth = textureView.getWidth();
        float textureViewHeight = textureView.getHeight();
        if (textureViewWidth != 0 && textureViewHeight != 0 && textureViewRotation != 0) {
            float pivotX = textureViewWidth / 2;
            float pivotY = textureViewHeight / 2;
            transformMatrix.postRotate(textureViewRotation, pivotX, pivotY);

            // After rotation, scale the rotated texture to fit the TextureView size.
            RectF originalTextureRect = new RectF(0, 0, textureViewWidth, textureViewHeight);
            RectF rotatedTextureRect = new RectF();
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect);
            transformMatrix.postScale(
                    textureViewWidth / rotatedTextureRect.width(),
                    textureViewHeight / rotatedTextureRect.height(),
                    pivotX,
                    pivotY);
        }
        textureView.setTransform(transformMatrix);
    }

    /**
     * Returns the player currently set on this view, or null if no player is set.
     */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * Sets the {@link Player} to use.
     */
    public void setPlayer(@Nullable Player player) {
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper());
        Assertions.checkArgument(player == null || player.getApplicationLooper() == Looper.getMainLooper());
        if (this.player == player) {
            return;
        }
        @Nullable Player oldPlayer = this.player;
        if (oldPlayer != null) {
            oldPlayer.removeListener(componentListener);
            if (surfaceView instanceof TextureView) {
                oldPlayer.clearVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                oldPlayer.clearVideoSurfaceView((SurfaceView) surfaceView);
            }
        }
        this.player = player;
        updateBuffering();
        updateErrorMessage();
        if (player != null) {
            if (player.isCommandAvailable(COMMAND_SET_VIDEO_SURFACE)) {
                if (surfaceView instanceof TextureView) {
                    player.setVideoTextureView((TextureView) surfaceView);
                } else if (surfaceView instanceof SurfaceView) {
                    player.setVideoSurfaceView((SurfaceView) surfaceView);
                }
                updateAspectRatio();
            }
            player.addListener(componentListener);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (surfaceView instanceof SurfaceView) {
            // Work around https://github.com/google/ExoPlayer/issues/3160.
            surfaceView.setVisibility(visibility);
        }
    }

    /**
     * Returns the {@link ResizeMode}.
     */
    public @ResizeMode int getResizeMode() {
        Assertions.checkStateNotNull(contentFrame);
        return contentFrame.getResizeMode();
    }

    /**
     * Sets the {@link ResizeMode}.
     *
     * @param resizeMode The {@link ResizeMode}.
     */
    public void setResizeMode(@ResizeMode int resizeMode) {
        Assertions.checkStateNotNull(contentFrame);
        contentFrame.setResizeMode(resizeMode);
    }

    /**
     * Sets whether the currently displayed video frame or media artwork is kept visible when the
     * player is reset. A player reset is defined to mean the player being re-prepared with different
     * media, the player transitioning to unprepared media or an empty list of media items, or the
     * player being replaced or cleared by calling {@link #setPlayer(Player)}.
     *
     * <p>If enabled, the currently displayed video frame or media artwork will be kept visible until
     * the player set on the view has been successfully prepared with new media and loaded enough of
     * it to have determined the available tracks. Hence enabling this option allows transitioning
     * from playing one piece of media to another, or from using one player instance to another,
     * without clearing the view's content.
     *
     * <p>If disabled, the currently displayed video frame or media artwork will be hidden as soon as
     * the player is reset. Note that the video frame is hidden by making {@code exo_shutter} visible.
     * Hence the video frame will not be hidden if using a custom layout that omits this view.
     *
     * @param keepContentOnPlayerReset Whether the currently displayed video frame or media artwork is
     *                                 kept visible when the player is reset.
     */
    public void setKeepContentOnPlayerReset(boolean keepContentOnPlayerReset) {
        if (this.keepContentOnPlayerReset != keepContentOnPlayerReset) {
            this.keepContentOnPlayerReset = keepContentOnPlayerReset;
        }
    }

    /**
     * Sets whether a buffering spinner is displayed when the player is in the buffering state. The
     * buffering spinner is not displayed by default.
     *
     * @param showBuffering The mode that defines when the buffering spinner is displayed. One of
     *                      {@link #SHOW_BUFFERING_NEVER}, {@link #SHOW_BUFFERING_WHEN_PLAYING} and {@link
     *                      #SHOW_BUFFERING_ALWAYS}.
     */
    public void setShowBuffering(@ShowBuffering int showBuffering) {
        if (this.showBuffering != showBuffering) {
            this.showBuffering = showBuffering;
            updateBuffering();
        }
    }

    /**
     * Sets the optional {@link ErrorMessageProvider}.
     *
     * @param errorMessageProvider The error message provider.
     */
    public void setErrorMessageProvider(@Nullable ErrorMessageProvider<? super PlaybackException> errorMessageProvider) {
        if (this.errorMessageProvider != errorMessageProvider) {
            this.errorMessageProvider = errorMessageProvider;
            updateErrorMessage();
        }
    }

    /**
     * Sets a custom error message to be displayed by the view. The error message will be displayed
     * permanently, unless it is cleared by passing {@code null} to this method.
     *
     * @param message The message to display, or {@code null} to clear a previously set message.
     */
    public void setCustomErrorMessage(@Nullable CharSequence message) {
        Assertions.checkState(errorMessageView != null);
        customErrorMessage = message;
        updateErrorMessage();
    }

    /**
     * Sets the {@link AspectRatioFrameLayout.AspectRatioListener}.
     *
     * @param listener The listener to be notified about aspect ratios changes of the video content or
     *                 the content frame.
     */
    public void setAspectRatioListener(@Nullable AspectRatioFrameLayout.AspectRatioListener listener) {
        Assertions.checkStateNotNull(contentFrame);
        contentFrame.setAspectRatioListener(listener);
    }

    /**
     * Gets the view onto which video is rendered. This is a:
     *
     * <ul>
     *   <li>{@link SurfaceView} by default, or if the {@code surface_type} attribute is set to {@code
     *       surface_view}.
     *   <li>{@link TextureView} if {@code surface_type} is {@code texture_view}.
     *   <li>{@code SphericalGLSurfaceView} if {@code surface_type} is {@code
     *       spherical_gl_surface_view}.
     *   <li>{@code VideoDecoderGLSurfaceView} if {@code surface_type} is {@code
     *       video_decoder_gl_surface_view}.
     *   <li>{@code null} if {@code surface_type} is {@code none}.
     * </ul>
     *
     * @return The {@link SurfaceView}, {@link TextureView}, {@code SphericalGLSurfaceView}, {@code
     * VideoDecoderGLSurfaceView} or {@code null}.
     */
    @Nullable
    public View getVideoSurfaceView() {
        return surfaceView;
    }

    /**
     * Should be called when the player is visible to the user, if the {@code surface_type} extends
     * {@link GLSurfaceView}. It is the counterpart to {@link #onPause()}.
     *
     * <p>This method should typically be called in {@code Activity.onStart()}, or {@code
     * Activity.onResume()} for API versions &lt;= 23.
     */
    public void onResume() {
        if (surfaceView instanceof GLSurfaceView) {
            ((GLSurfaceView) surfaceView).onResume();
        }
    }

    /**
     * Should be called when the player is no longer visible to the user, if the {@code surface_type}
     * extends {@link GLSurfaceView}. It is the counterpart to {@link #onResume()}.
     *
     * <p>This method should typically be called in {@code Activity.onStop()}, or {@code
     * Activity.onPause()} for API versions &lt;= 23.
     */
    public void onPause() {
        if (surfaceView instanceof GLSurfaceView) {
            ((GLSurfaceView) surfaceView).onPause();
        }
    }

    /**
     * Called when there's a change in the desired aspect ratio of the content frame. The default
     * implementation sets the aspect ratio of the content frame to the specified value.
     *
     * @param contentFrame The content frame, or {@code null}.
     * @param aspectRatio  The aspect ratio to apply.
     */
    protected void onContentAspectRatioChanged(@Nullable AspectRatioFrameLayout contentFrame, int width, int height, float aspectRatio) {
        if (contentFrame != null) {
            contentFrame.setAspectRatio(width, height, aspectRatio);
        }
    }

    private void updateBuffering() {
        if (bufferingView != null) {
            boolean showBufferingSpinner = player != null;
            showBufferingSpinner = showBufferingSpinner && player.getPlaybackState() == Player.STATE_BUFFERING;
            showBufferingSpinner = showBufferingSpinner && (showBuffering == SHOW_BUFFERING_ALWAYS || (showBuffering == SHOW_BUFFERING_WHEN_PLAYING && player.getPlayWhenReady()));
            bufferingView.setVisibility(showBufferingSpinner ? View.VISIBLE : View.GONE);
        }
    }

    private void updateErrorMessage() {
        if (errorMessageView != null) {
            if (customErrorMessage != null) {
                errorMessageView.setText(customErrorMessage);
                errorMessageView.setVisibility(View.VISIBLE);
                return;
            }
            @Nullable PlaybackException error = player != null ? player.getPlayerError() : null;
            if (error != null && errorMessageProvider != null) {
                CharSequence errorMessage = errorMessageProvider.getErrorMessage(error).second;
                errorMessageView.setText(errorMessage);
                errorMessageView.setVisibility(View.VISIBLE);
            } else {
                errorMessageView.setVisibility(View.GONE);
            }
        }
    }

    private void updateAspectRatio() {
        VideoSize videoSize = player != null ? player.getVideoSize() : VideoSize.UNKNOWN;
        updateAspectRatio(videoSize);
    }

    private void updateAspectRatio(VideoSize videoSize) {
        assert surfaceView != null;
        int width = videoSize.width;
        int height = videoSize.height;
        int unAppliedRotationDegrees = videoSize.unappliedRotationDegrees;
        float videoAspectRatio = (height == 0 || width == 0) ? 0 : (width * videoSize.pixelWidthHeightRatio) / height;

        if (surfaceView instanceof TextureView) {
            // Try to apply rotation transformation when our surface is a TextureView.
            if (videoAspectRatio > 0 && (unAppliedRotationDegrees == 90 || unAppliedRotationDegrees == 270)) {
                // We will apply a rotation 90/270 degree to the output texture of the TextureView.
                // In this case, the output video's width and height will be swapped.
                videoAspectRatio = 1 / videoAspectRatio;
            }
            if (textureViewRotation != 0) {
                surfaceView.removeOnLayoutChangeListener(componentListener);
            }
            textureViewRotation = unAppliedRotationDegrees;
            if (textureViewRotation != 0) {
                // The texture view's dimensions might be changed after layout step.
                // So add an OnLayoutChangeListener to apply rotation after layout step.
                surfaceView.addOnLayoutChangeListener(componentListener);
            }
            applyTextureViewRotation((TextureView) surfaceView, textureViewRotation);
        }

        onContentAspectRatioChanged(contentFrame, width, height, videoAspectRatio);
    }

    /**
     * Determines when the buffering view is shown. One of {@link #SHOW_BUFFERING_NEVER}, {@link
     * #SHOW_BUFFERING_WHEN_PLAYING} or {@link #SHOW_BUFFERING_ALWAYS}.
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({SHOW_BUFFERING_NEVER, SHOW_BUFFERING_WHEN_PLAYING, SHOW_BUFFERING_ALWAYS})
    public @interface ShowBuffering {
    }

    public static class DefaultErrorMessageProvider implements ErrorMessageProvider<PlaybackException> {
        @NonNull
        @Override
        public Pair<Integer, String> getErrorMessage(PlaybackException e) {
            return new Pair<>(e.errorCode, e.getMessage());
        }
    }

    // Implementing the deprecated StyledPlayerControlView.VisibilityListener and
    // StyledPlayerControlView.OnFullScreenModeChangedListener for now.
    private final class ComponentListener implements Player.Listener, OnLayoutChangeListener {

        // Player.Listener implementation
        @Override
        public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
            updateAspectRatio(videoSize);
        }

        @Override
        public void onPlaybackStateChanged(@Player.State int playbackState) {
            updateBuffering();
            updateErrorMessage();
        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason) {
            updateBuffering();
        }

        // OnLayoutChangeListener implementation
        @Override
        public void onLayoutChange(
                View view,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
            applyTextureViewRotation((TextureView) view, textureViewRotation);
        }
    }
}
