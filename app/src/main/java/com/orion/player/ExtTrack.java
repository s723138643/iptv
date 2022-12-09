package com.orion.player;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;

public class ExtTrack {
    public final @C.TrackType int trackType;
    public final int trackId;
    public final Format format;
    public final boolean selected;
    private final String description;

    public ExtTrack(int trackType, Format format, boolean selected) {
        this(trackType, C.INDEX_UNSET, format, selected);
    }

    public ExtTrack(int trackType, int trackId, Format format, boolean selected) {
        this.trackType = trackType;
        this.trackId = trackId;
        this.format = format;
        this.selected = selected;
        this.description = null;
    }

    public ExtTrack(ExtTrack track, String description) {
        this.trackType = track.trackType;
        this.trackId = track.trackId;
        this.format = track.format;
        this.selected = track.selected;
        this.description = description;
    }

    public String description() {
        if (this.description != null) {
            return this.description;
        }
        return makeDescription();
    }

    protected String makeDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append(trackType);
        if (trackId != C.INDEX_UNSET) {
            builder.append(":");
            builder.append(trackId);
        } else if (format.id != null) {
            builder.append(":");
            builder.append(format.id);
        }
        if (trackType == C.TRACK_TYPE_VIDEO) {
            builder.append(" ");
            builder.append(format.width);
            builder.append("x");
            builder.append(format.height);
            if (format.frameRate != Format.NO_VALUE) {
                builder.append(" ");
                builder.append(format.frameRate);
                builder.append("fps");
            }
            if (format.bitrate != Format.NO_VALUE) {
                builder.append(" ");
                builder.append(format.bitrate / 1000);
                builder.append("kbps");
            }
        } else if (trackType == C.TRACK_TYPE_AUDIO) {
            if (format.language != null) {
                builder.append(" ");
                builder.append(format.language);
            }
            if (format.codecs != null) {
                builder.append(" ");
                builder.append(format.codecs);
            }
            if (format.label != null) {
                builder.append(" ");
                builder.append(format.label);
            }
            if (format.sampleRate != Format.NO_VALUE) {
                builder.append(" ");
                builder.append(format.sampleRate / 1000);
                builder.append("KHz");
            }
            if (format.bitrate != Format.NO_VALUE) {
                builder.append(" ");
                builder.append(format.bitrate / 1000);
                builder.append("kbps");
            }
        } else {
            if (format.language != null) {
                builder.append(" ");
                builder.append(format.language);
            }
            if (format.codecs != null) {
                builder.append(" ");
                builder.append(format.codecs);
            }
            if (format.label != null) {
                builder.append(" ");
                builder.append(format.label);
            }
        }
        return builder.toString();
    }
}