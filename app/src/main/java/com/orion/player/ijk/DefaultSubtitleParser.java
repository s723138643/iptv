package com.orion.player.ijk;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.CueGroup;

import java.util.List;

public class DefaultSubtitleParser implements SubtitleParser {

    @Override
    public CueGroup parse(String text) {
        Cue cue = new Cue.Builder()
                .setText(text)
                .build();
        return new CueGroup(List.of(cue));
    }
}
