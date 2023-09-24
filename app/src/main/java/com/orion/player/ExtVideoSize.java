package com.orion.player;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Objects;

public class ExtVideoSize {
    public static final ExtVideoSize UNKNOWN = new ExtVideoSize(0, 0);
    public final int width;
    public final int height;
    public final float widthHeightRatio;
    @IntRange(from = 0, to = 359)
    public final int unAppliedRotationDegrees;

    public ExtVideoSize(int width, int height) {
        this(width, height, (float) width / (float) height, 0);
    }

    public ExtVideoSize(int width, int height, float widthHeightRatio, int unAppliedRotationDegrees) {
        this.width = width;
        this.height = height;
        this.widthHeightRatio = widthHeightRatio;
        this.unAppliedRotationDegrees = unAppliedRotationDegrees;
    }

    public static ExtVideoSize of(androidx.media3.common.VideoSize videoSize) {
        if (videoSize.width == 0 || videoSize.height == 0) {
            return ExtVideoSize.UNKNOWN;
        }
        int width = videoSize.width;
        int height = videoSize.height;
        float ratio = (float) width * videoSize.pixelWidthHeightRatio / (float) height;
        return new ExtVideoSize(width, height, ratio, videoSize.unappliedRotationDegrees);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%dx%d::%.2f", width, height, widthHeightRatio);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtVideoSize that = (ExtVideoSize) o;
        return width == that.width && height == that.height && Float.compare(that.widthHeightRatio, widthHeightRatio) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, widthHeightRatio);
    }
}