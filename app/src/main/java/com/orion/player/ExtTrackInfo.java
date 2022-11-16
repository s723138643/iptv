package com.orion.player;

import static java.lang.annotation.ElementType.TYPE_USE;

import androidx.annotation.IntDef;

import java.lang.annotation.Target;

public class ExtTrackInfo {
    @Target(TYPE_USE)
    @IntDef(
            open = true,
            value = {
                    TRACK_TYPE_UNKNOWN,
                    TRACK_TYPE_DEFAULT,
                    TRACK_TYPE_AUDIO,
                    TRACK_TYPE_VIDEO,
                    TRACK_TYPE_TEXT,
                    TRACK_TYPE_IMAGE,
                    TRACK_TYPE_METADATA,
                    TRACK_TYPE_CAMERA_MOTION,
                    TRACK_TYPE_NONE,
            })
    public @interface TrackType {}
    /** A type constant for a fake or empty track. */
    public static final int TRACK_TYPE_NONE = -2;
    /** A type constant for tracks of unknown type. */
    public static final int TRACK_TYPE_UNKNOWN = -1;
    /** A type constant for tracks of some default type, where the type itself is unknown. */
    public static final int TRACK_TYPE_DEFAULT = 0;
    /** A type constant for audio tracks. */
    public static final int TRACK_TYPE_AUDIO = 1;
    /** A type constant for video tracks. */
    public static final int TRACK_TYPE_VIDEO = 2;
    /** A type constant for text tracks. */
    public static final int TRACK_TYPE_TEXT = 3;
    /** A type constant for image tracks. */
    public static final int TRACK_TYPE_IMAGE = 4;
    /** A type constant for metadata tracks. */
    public static final int TRACK_TYPE_METADATA = 5;
    /** A type constant for camera motion tracks. */
    public static final int TRACK_TYPE_CAMERA_MOTION = 6;
    /**
     * Applications or extensions may define custom {@code TRACK_TYPE_*} constants greater than or
     * equal to this value.
     */
    public static final int TRACK_TYPE_CUSTOM_BASE = 10000;

    public final int width;
    public final int height;
    public final String codecs;
    public final long bitrate;
    public final @TrackType int type;

    public ExtTrackInfo(int type, int width, int height, String codecs, long bitrate) {
        this.width = width;
        this.height = height;
        this.codecs = codecs;
        this.bitrate = bitrate;
        this.type = type;
    }
}
