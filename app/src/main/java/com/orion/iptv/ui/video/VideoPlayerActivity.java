package com.orion.iptv.ui.video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import com.orion.iptv.R;
import com.orion.iptv.layout.NetworkSpeed;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.IExtPlayerFactory;
import com.orion.player.exo.ExtExoPlayerFactory;
import com.orion.player.ijk.ExtHWIjkPlayerFactory;
import com.orion.player.ui.VideoPlayerView;

import java.util.Locale;
import java.util.Map;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";

    private VideoPlayerView videoPlayerView;
    private NetworkSpeed networkSpeed;

    private IExtPlayerFactory<? extends IExtPlayer> iExtPlayerFactory;
    private IExtPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        FragmentManager fg = getSupportFragmentManager();
        videoPlayerView = (VideoPlayerView) fg.findFragmentByTag("video_player");
        networkSpeed = (NetworkSpeed) fg.findFragmentByTag("network_speed");
        iExtPlayerFactory = new ExtHWIjkPlayerFactory();
    }

    @Override
    protected void onStart() {
        super.onStart();
        player = iExtPlayerFactory.create(this);
        videoPlayerView.setPlayer(player);
        networkSpeed.setPlayer(player);
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            Log.i("VideoPlayer", uri.toString());
            ExtDataSource dataSource = new ExtDataSource(uri.toString());
            if (intent.hasExtra("headers")) {
                Bundle bundle = intent.getBundleExtra("headers");
                Map<String, String> headers = new ArrayMap<>();
                headers.keySet().forEach(key -> headers.put(key, bundle.getString(key)));
                dataSource.setHeaders(headers);
            }
            if (intent.hasExtra("auth")) {
                Bundle bundle = intent.getBundleExtra("auth");
                ExtDataSource.Auth auth = new ExtDataSource.Auth(
                        bundle.getString("username"),
                        bundle.getString("password")
                );
                dataSource.setAuth(auth);
            }
            player.setDataSource(dataSource);
        }
        player.prepare();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
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
        if (player != null) {
            player.release();
        }
    }
}