package com.orion.iptv.ui.video;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.orion.iptv.R;

public class VideoPlayerSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.content, VideoPlayerSettings.class, null)
                .commit();
    }
}