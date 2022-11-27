package com.orion.iptv.ui.video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.player.ExtDataSource;
import com.orion.player.IExtPlayer;
import com.orion.player.IExtPlayerFactory;
import com.orion.player.exo.ExtExoPlayerFactory;
import com.orion.player.ijk.ExtHWIjkPlayerFactory;
import com.orion.player.ijk.ExtSWIjkPlayerFactory;
import com.orion.player.ui.VideoPlayerView;
import com.orion.player.ui.VideoView;

import java.util.Map;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";
    private static final long NoPosition = -1;

    private VideoPlayerView playerView;
    private View header;
    private TextView title;
    private ImageButton settings;
    private IExtPlayer player;
    private long currentPosition = NoPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.video_player);
        header = findViewById(R.id.header);
        title = findViewById(R.id.title);
        settings = findViewById(R.id.settings);
    }

    protected ExtDataSource getDataSource() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            return null;
        }
        Log.i(TAG, uri.toString());
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
        return dataSource;
    }

    @Override
    protected void onStart() {
        super.onStart();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        header.setVisibility(View.GONE);
        settings.setOnClickListener(v -> {
            startActivity(new Intent(this, VideoPlayerSettingsActivity.class));
        });

        ExtDataSource dataSource = getDataSource();
        if (dataSource == null) {
            finish();
            return;
        }
        setTitle(dataSource);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String playerType = preferences.getString("player_type", "ijkplayer");
        String surfaceType = preferences.getString("surface_type", "surface_view");

        playerView.setSurfaceType(toVideoViewSurfaceType(surfaceType));
        playerView.setOrientationSwitchCallback(this::switchOrientation);
        playerView.setOnControllerVisibilityChangedListener(this::changeHeaderVisibility);

        player = createPlayer(playerType);
        playerView.setPlayer(player);

        player.setDataSource(dataSource);
        player.prepare();
    }

    protected void switchOrientation() {
        int orientation = getRequestedOrientation();
        int newOrientation = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        setRequestedOrientation(newOrientation);
    }

    protected void changeHeaderVisibility(int visibility) {
        header.setVisibility(visibility);
    }

    @NonNull
    private IExtPlayer createPlayer(String playerType) {
        IExtPlayerFactory<? extends IExtPlayer> factory;
        switch (playerType) {
            case "ijkplayer":
                factory = new ExtHWIjkPlayerFactory();
                break;
            case "ijkplayer_sw":
                factory = new ExtSWIjkPlayerFactory();
                break;
            case "exoplayer":
            default:
                factory = new ExtExoPlayerFactory();
        }
        return factory.create(this);
    }

    private @VideoView.SurfaceType int toVideoViewSurfaceType(String surfaceType) {
        @VideoView.SurfaceType int type;
        switch (surfaceType) {
            case "texture_view":
                type = VideoView.SURFACE_TYPE_TEXTURE_VIEW;
                break;
            case "surface_view":
            default:
                type = VideoView.SURFACE_TYPE_SURFACE_VIEW;
        }
        return type;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
            currentPosition = player.getCurrentPosition();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        if (player != null) {
            if (currentPosition != NoPosition) {
                player.seekTo(currentPosition);
            }
            player.play();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
        }
    }

    protected void setTitle(ExtDataSource dataSource) {
        Uri uri = Uri.parse(dataSource.getUri());
        String file = uri.getLastPathSegment();
        if (file == null || !file.contains(".")) {
            return;
        }
        title.setText(file);
    }

    protected void hideSystemBars() {
        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController == null) {
            return;
        }
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }
}