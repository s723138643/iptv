package com.orion.player.ijk;

import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssSubtitleParser implements SubtitleParser {
    Pattern delete = Pattern.compile("\\{.*?\\}");

    @Override
    public CueGroup parse(String text) {
        Matcher m = delete.matcher(text);
        String clean = m.replaceAll("");
        Cue cue = new Cue.Builder().setText(clean).build();
        return new CueGroup(List.of(cue), 0L);
    }
}
