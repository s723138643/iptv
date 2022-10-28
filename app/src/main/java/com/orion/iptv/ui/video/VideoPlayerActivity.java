package com.orion.iptv.ui.video;

import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;
import static com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_ALWAYS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.orion.iptv.R;

import java.util.Locale;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";

    private StyledPlayerView playerView;
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.video_player);
        playerView.setShowBuffering(SHOW_BUFFERING_ALWAYS);
        playerView.setUseController(true);
        playerView.setShowMultiWindowTimeBar(true);
        playerView.setShowPreviousButton(true);
        playerView.setShowNextButton(true);
    }

    protected ExoPlayer newPlayer() {
        ExoPlayer.Builder builder = new ExoPlayer.Builder(this);
        // use extension render if possible
        DefaultRenderersFactory renderFactory = new DefaultRenderersFactory(this.getApplicationContext());
        renderFactory = renderFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        builder.setRenderersFactory(renderFactory);

        OkHttpClient client = new OkHttpClient.Builder().build();
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                this,
                new OkHttpDataSource.Factory((Call.Factory) client)
        );
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this);
        mediaSourceFactory.setDataSourceFactory(dataSourceFactory);
        builder.setMediaSourceFactory(mediaSourceFactory);
        return builder.build();
    }

    protected void initializePlayer() {
        releasePlayer();
        player = newPlayer();
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        playerView.setPlayer(player);
    }

    protected void releasePlayer() {
        if (player != null) {
            player.release();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
        assert player != null;
        player.setRepeatMode(REPEAT_MODE_OFF);
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            MediaItem item = new MediaItem.Builder()
                    .setUri(uri)
                    .build();
            Log.i("VideoPlayer", uri.toString());
            player.setMediaItem(item);
        }
        player.prepare();
    }

    @Override
    protected void onResume() {
        super.onResume();
        assert player != null;
        if (player.getMediaItemCount() > 0) {
            player.setPlayWhenReady(true);
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
        int mediaItemIndex = player.getCurrentMediaItemIndex();
        MediaItem item = player.getCurrentMediaItem();
        outState.putLong("playbackPosition", position);
        outState.putInt("mediaItemIndex", mediaItemIndex);
        assert item != null;
        outState.putString("mediaId", item.mediaId);
        Log.i(TAG, String.format(Locale.ENGLISH, "save at %d::%d", mediaItemIndex, position));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.i(TAG, "restoring state...");
        super.onRestoreInstanceState(savedInstanceState);
        long position = savedInstanceState.getLong("playbackPosition");
        int mediaItemIndex = savedInstanceState.getInt("mediaItemIndex");
        String mediaId = savedInstanceState.getString("mediaId");

        if (player != null) {
            MediaItem item = player.getMediaItemAt(mediaItemIndex);
            if (!item.mediaId.equals(mediaId)) {
                return;
            }
            player.seekTo(mediaItemIndex, position);
            Log.i(TAG, String.format(Locale.ENGLISH, "seek to %d::%d", mediaItemIndex, position));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }
}