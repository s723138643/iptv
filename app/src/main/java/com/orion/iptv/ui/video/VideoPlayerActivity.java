package com.orion.iptv.ui.video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.orion.iptv.R;
import com.orion.iptv.ui.live.networkspeed.NetworkSpeed;
import com.orion.iptv.ui.live.player.PlayerView;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.IExtPlayerFactory;
import com.orion.player.ijk.ExtIjkPlayerFactory;

import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";

    private PlayerView playerView;
    private NetworkSpeed networkSpeed;
    private IExtPlayerFactory<? extends IExtPlayer> iExtPlayerFactory;
    private IExtPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        playerView = findViewById(R.id.video_player);
        networkSpeed = new NetworkSpeed(this);
        iExtPlayerFactory = new ExtIjkPlayerFactory();
    }

    @Override
    protected void onStart() {
        super.onStart();
        player = iExtPlayerFactory.create(this);
        playerView.setPlayer(player);
        networkSpeed.setPlayer(player);
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            Log.i("VideoPlayer", uri.toString());
            player.setDataSource(new ExtDataSource(uri.toString()));
        }
        player.prepare();
    }

    @Override
    protected void onResume() {
        super.onResume();
        assert player != null;
        player.play();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.i(TAG, "saving state...");
        super.onSaveInstanceState(outState);
        if (player == null) {
            return;
        }
        long position = player.getCurrentPosition();
        outState.putLong("playbackPosition", position);
        ExtDataSource dataSource = player.getDataSource();
        assert dataSource != null;
        outState.putString("mediaId", dataSource.getUri());
        Log.i(TAG, String.format(Locale.ENGLISH, "save at %s::%d", dataSource.getUri(), position));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.i(TAG, "restoring state...");
        super.onRestoreInstanceState(savedInstanceState);
        long position = savedInstanceState.getLong("playbackPosition");
        String mediaId = savedInstanceState.getString("mediaId");

        if (player != null) {
            ExtDataSource dataSource = player.getDataSource();
            if (!mediaId.equals(dataSource.getUri())) {
                return;
            }
            player.seekTo(position);
            Log.i(TAG, String.format(Locale.ENGLISH, "seek to %s::%d", mediaId, position));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        assert player != null;
        player.release();
    }
}