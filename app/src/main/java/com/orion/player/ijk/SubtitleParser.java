package com.orion.player.ijk;

import androidx.media3.common.text.CueGroup;

public interface SubtitleParser {
    CueGroup parse(String text);
}
