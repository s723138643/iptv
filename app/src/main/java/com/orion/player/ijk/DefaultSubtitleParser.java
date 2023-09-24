package com.orion.player.ijk;

import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;

import java.util.List;

public class DefaultSubtitleParser implements SubtitleParser {

    @Override
    public CueGroup parse(String text) {
        Cue cue = new Cue.Builder()
                .setText(text)
                .build();
        return new CueGroup(List.of(cue), 0L);
    }
}
