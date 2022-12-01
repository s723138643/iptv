package com.orion.player.ijk;

import com.google.android.exoplayer2.text.CueGroup;

public interface SubtitleParser {
    CueGroup parse(String text);
}
