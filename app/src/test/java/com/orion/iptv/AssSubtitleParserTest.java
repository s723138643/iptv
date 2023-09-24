package com.orion.iptv;

import android.util.Log;

import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import com.orion.player.ijk.AssSubtitleParser;

import org.junit.Test;

import java.util.Locale;

public class AssSubtitleParserTest {
    @Test
    public void parseAss() {
        String[] subtitles = new String[] {
                "{\\fade(500,500)\\fn华文楷体\\fs16\\1c&H3CF1F3&\\b0}--==本影片由 {\\1cHFF8000&\\b1}CMCT 团队{\\fn华文楷体\\1c&H3CF1F3&\\b0} 荣誉出品==--更多精彩影视 请访问 {\\fnCronos Pro Subhead\\1c&HFF00FF&\\b1}https://cmct.tv{\\r}",
                "",
                "{}",
                "{\\b0}test",
                "{\\fs16}test",
                "{\\1c&H3cf1f3&}ssssss"
        };
        AssSubtitleParser parser = new AssSubtitleParser();
        for (String subtitle: subtitles) {
            CueGroup cueGroup = parser.parse(subtitle);
            for (Cue cue : cueGroup.cues) {
                Log.i("test", String.format(Locale.getDefault(), "%d, %.2f, %s", cue.windowColor, cue.textSize, cue.text));
            }
        }
    }
}
