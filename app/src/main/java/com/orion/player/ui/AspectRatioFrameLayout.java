/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.orion.player.ExtVideoSize;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

public final class AspectRatioFrameLayout extends FrameLayout {
    private static final String TAG = "AspectRatioFrameLayout";

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE, ANNOTATION_TYPE})
    @IntDef({RESIZE_MODE_FIT, RESIZE_MODE_FIXED_WIDTH, RESIZE_MODE_FIXED_HEIGHT, RESIZE_MODE_FILL, RESIZE_MODE_ZOOM})
    public @interface ResizeMode{}
    public static final int RESIZE_MODE_FIT = 0;
    public static final int RESIZE_MODE_FIXED_WIDTH = 1;
    public static final int RESIZE_MODE_FIXED_HEIGHT = 2;
    public static final int RESIZE_MODE_FILL = 3;
    public static final int RESIZE_MODE_ZOOM = 4;

    private ExtVideoSize display;
    private @ResizeMode int resizeMode;
    private Resize resize;
    private ExtVideoSize videoSize;

    public AspectRatioFrameLayout(@NonNull Context context) {
        this(context, /* attrs= */ null, 0, 0);
    }

    public AspectRatioFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public AspectRatioFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AspectRatioFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        resizeMode = RESIZE_MODE_FIT;
        resize = getResize(resizeMode);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        display = new ExtVideoSize(metrics.widthPixels, metrics.heightPixels);
        Log.i(TAG, String.format(Locale.getDefault(), "display size: %dx%d %.2f", display.width, display.height, display.widthHeightRatio));
        videoSize = ExtVideoSize.UNKNOWN;
    }

    public void setVideoSize(ExtVideoSize videoSize) {
        if (this.videoSize.equals(videoSize)) {
            return;
        }
        this.videoSize = videoSize;
        requestLayout();
    }

    /**
     * Returns the {@link ResizeMode}.
     */
    public @ResizeMode int getResizeMode() {
        return resizeMode;
    }

    /**
     * Sets the {@link ResizeMode}
     *
     * @param resizeMode The {@link ResizeMode}.
     */
    public void setResizeMode(@ResizeMode int resizeMode) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode;
            this.resize = getResize(resizeMode);
            requestLayout();
        }
    }

    private int fixSize(int calculated, int displaySize, int videoSize) {
        boolean useVideoSize = (Math.abs(videoSize - calculated) <= 5) && (videoSize <= displaySize);
        return useVideoSize ? videoSize : calculated;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, String.format(Locale.getDefault(), "measureSpec: %dx%d", widthMeasureSpec, heightMeasureSpec));
        if (videoSize.equals(ExtVideoSize.UNKNOWN)) {
            // have no video size yet, set to original display size;
            Log.i(TAG, String.format(Locale.getDefault(), "finalSize: %s", display));
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(display.width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(display.height, MeasureSpec.EXACTLY)
            );
            return;
        }

        Log.i(TAG, String.format(Locale.getDefault(), "video size: %s", videoSize.toString()));
        Pair<Integer, Integer> newSize = resize.resize(videoSize);
        int width = fixSize(newSize.first, display.width, videoSize.width);
        int height = fixSize(newSize.second, display.height, videoSize.height);
        Log.i(TAG, String.format(Locale.ENGLISH, "finalSize: %dx%d", width, height));
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }

    private Resize getResize(@ResizeMode int resizeMode) {
        Resize resize;
        switch (resizeMode) {
            case RESIZE_MODE_FIT:
                resize = new ResizeFit();
                break;
            case RESIZE_MODE_ZOOM:
                resize = new ResizeZoom();
                break;
            case RESIZE_MODE_FIXED_WIDTH:
                resize = new ResizeFixWidth();
                break;
            case RESIZE_MODE_FIXED_HEIGHT:
                resize = new ResizeFixHeight();
                break;
            case RESIZE_MODE_FILL:
                resize = new ResizeFill();
                break;
            default:
                resize = new ResizeDefault();
                break;
        }
        return resize;
    }

    public interface Resize {
        Pair<Integer, Integer> resize(ExtVideoSize videoSize);
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    private class ResizeDefault implements Resize {
        @Override
        public Pair<Integer, Integer> resize(@NonNull ExtVideoSize videoSize) {
            return Pair.create(videoSize.width, videoSize.height);
        }
    }

    private class ResizeFit implements Resize {
        @Override
        public Pair<Integer, Integer> resize(@NonNull ExtVideoSize videoSize) {
            if (Float.compare(display.widthHeightRatio, videoSize.widthHeightRatio) > 0) {
                return Pair.create((int)((float) display.height * videoSize.widthHeightRatio), display.height);
            } else {
                return Pair.create(display.width, (int) ((float) display.width / videoSize.widthHeightRatio));
            }
        }
    }

    private class ResizeZoom implements Resize {
        @Override
        public Pair<Integer, Integer> resize(@NonNull ExtVideoSize videoSize) {
            if (Float.compare(display.widthHeightRatio, videoSize.widthHeightRatio) > 0) {
                return Pair.create(display.width, (int) ((float) display.width / videoSize.widthHeightRatio));
            } else {
                return Pair.create((int)((float) display.height * videoSize.widthHeightRatio), display.height);
            }
        }
    }

    private class ResizeFixWidth implements Resize {
        @Override
        public Pair<Integer, Integer> resize(@NonNull ExtVideoSize videoSize) {
            return Pair.create(display.width, (int)((float) display.width / videoSize.widthHeightRatio));
        }
    }

    private class ResizeFixHeight implements Resize {
        @Override
        public Pair<Integer, Integer> resize(@NonNull ExtVideoSize videoSize) {
            return Pair.create((int)((float) display.height * videoSize.widthHeightRatio), display.height);
        }
    }

    private class ResizeFill implements Resize {
        @Override
        public Pair<Integer, Integer> resize(@NonNull ExtVideoSize videoSize) {
            return Pair.create(display.width, display.height);
        }
    }
}
