package com.orion.iptv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Tracks;
import com.orion.iptv.network.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelManager;
import com.orion.iptv.layout.livechannelinfo.LiveChannelInfoLayout;
import com.orion.iptv.layout.livechannellist.LiveChannelListLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG  = "LivePlayer";
    protected StyledPlayerView videoView;
    protected LiveChannelInfoLayout channelInfoLayout;
    protected LiveChannelListLayout channelListLayout;
    protected @Nullable ExoPlayer player;
    private RequestQueue reqQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = findViewById(R.id.videoView);
        videoView.setShowBuffering(StyledPlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        channelInfoLayout = new LiveChannelInfoLayout(findViewById(R.id.channelInfoLayout));
        ChannelManager channelManager = new ChannelManager(this.getString(R.string.default_group_name));
        channelListLayout = new LiveChannelListLayout(findViewById(R.id.channelListLayout), channelManager);
        reqQueue = Volley.newRequestQueue(this);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            channelListLayout.setVisibleDelayed(!channelListLayout.getIsVisible(), 0);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float dist = e2.getX() - e1.getX();
            // distance is too short ignore this event
            if (dist < 500.0 && dist > -500.0) {
                return false;
            }
            Log.i(TAG, String.format(Locale.ENGLISH, "scroll event detect, dist: %.2f, direction: %.2f", dist, velocityX));
            if (player != null && player.getMediaItemCount() > 0) {
                if (dist < 0) {
                    // from right to left
                    player.seekToNextMediaItem();
                } else {
                    // from left to right
                    player.seekToPreviousMediaItem();
                }
                if (player.getPlaybackState() == Player.STATE_IDLE) {
                    player.prepare();
                }
                player.setPlayWhenReady(true);
            }
            return true;
        }
    }

    private class PlayerEventListener implements Player.Listener {
        private final ExoPlayer linkedPlayer;
        private final Handler handler;
        private CancelableRunnable delayedTask;

        private abstract class CancelableRunnable implements Runnable {
            private boolean canceled = false;

            @Override
            public void run() {
                if (!canceled) {
                    callback();
                }
            }

            public abstract void callback();

            public void cancel() {
                canceled = true;
            }
        }

        public PlayerEventListener(ExoPlayer player) {
            linkedPlayer = player;
            handler = new Handler(getMainLooper());
        }

        private void runDelayed(int delayMillis, CancelableRunnable task) {
            if (delayedTask != null) {
                delayedTask.cancel();
            }
            delayedTask = task;
            handler.postDelayed(task, delayMillis);
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            Log.e(TAG, error.toString());
            runDelayed(1000, new CancelableRunnable() {
                @Override
                public void callback() {
                    linkedPlayer.seekToNextMediaItem();
                    if (linkedPlayer.getPlaybackState() == Player.STATE_IDLE) {
                        linkedPlayer.prepare();
                    }
                    linkedPlayer.setPlayWhenReady(true);
                }
            });
        }

        @Override
        public void onTracksChanged(Tracks tracks) {
            for (Tracks.Group group : tracks.getGroups()) {
                if (group.getType() == C.TRACK_TYPE_VIDEO) {
                    for (int i=0; i<group.length; i++) {
                        Format vf = group.getTrackFormat(i);
                        if (vf.codecs != null) {
                            channelInfoLayout.setCodecInfo(vf.codecs);
                        }
                        if (vf.width > 0 && vf.height > 0) {
                            channelInfoLayout.setMediaInfo(String.format(Locale.ENGLISH, "%dx%d", vf.width, vf.height));
                        }
                    }
                }
            }
        }

        @Override
        public void onPlaybackStateChanged(@Player.State int state) {
            switch (state) {
                case Player.STATE_READY:
                    Log.w(TAG, "player change state to STATE_READY");
                    if (delayedTask != null) {
                        delayedTask.cancel();
                    }
                    channelInfoLayout.setVisibleDelayed(false, 10*1000);
                    break;
                case Player.STATE_BUFFERING:
                    Log.w(TAG, "player change state to STATE_BUFFERING");
                    runDelayed(10 * 1000, new CancelableRunnable() {
                        @Override
                        public void callback() {
                            linkedPlayer.seekToNextMediaItem();
                            if (linkedPlayer.getPlaybackState() == Player.STATE_IDLE) {
                                linkedPlayer.prepare();
                            }
                            linkedPlayer.setPlayWhenReady(true);
                        }
                    });
                    break;
                case Player.STATE_ENDED:
                    Log.w(TAG, "player change state to STATE_ENDED");
                    break;
                case Player.STATE_IDLE:
                    Log.w(TAG, "player change state to STATE_IDLE");
                    break;
            }
        }

        @Override
        public void onMediaItemTransition(MediaItem mediaItem, @Player.MediaItemTransitionReason int reason) {
            channelInfoLayout.setVisibleDelayed(true, 0);
            channelInfoLayout.setLinkInfo(linkedPlayer.getCurrentMediaItemIndex()+1, linkedPlayer.getMediaItemCount());
            if (mediaItem != null) {
                if (mediaItem.localConfiguration != null && mediaItem.localConfiguration.tag != null) {
                    Log.i(TAG, "start playing " + mediaItem.localConfiguration.uri);
                    ChannelItem.Tag tag = (ChannelItem.Tag) mediaItem.localConfiguration.tag;
                    channelInfoLayout.setChannelName(tag.channelName);
                    channelInfoLayout.setChannelNumber(tag.channelNumber);
                    channelInfoLayout.setCodecInfo(getString(R.string.codec_info_default));
                    channelInfoLayout.setMediaInfo(getString(R.string.media_info_default));
                }
            }
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        String url = this.getString(R.string.channel_list_url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            channelListLayout.setData(ChannelManager.from(getString(R.string.default_group_name), response));
            if (player != null) {
                List<MediaItem> items = channelListLayout.getCurrentChannelSources().orElse(new ArrayList<>());
                if (items.size() > 0 ) {
                    player.setMediaItems(items);
                    if (player.getPlaybackState() == Player.STATE_IDLE) {
                        player.prepare();
                    }
                    player.setPlayWhenReady(true);
                }
            }
        }, error -> Log.e(TAG, "got channel list failed, " + error.toString()));
        reqQueue.add(stringRequest.setTag(TAG));

        GestureDetectorCompat gestureDetector = new GestureDetectorCompat(this, new GestureListener());
        videoView.setOnTouchListener((view, event)->{
            view.performClick();
            return gestureDetector.onTouchEvent(event);
        });
        initializePlayer();
        if (player == null) {
            return;
        }
        channelListLayout.setOnChannelSelectedListener((groupIndex, channelIndex) -> {
           List<MediaItem> items = channelListLayout.getCurrentChannelSources().orElse(new ArrayList<>());
           if (items.size() > 0) {
               player.setMediaItems(items);
               if (player.getPlaybackState() == Player.STATE_IDLE) {
                   player.prepare();
               }
               player.setPlayWhenReady(true);
           }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        channelListLayout.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        channelListLayout.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
        reqQueue.cancelAll(TAG);
        releasePlayer();
    }

    private ExoPlayer newPlayer() {
        ExoPlayer.Builder builder = new ExoPlayer.Builder(this);
        // use extension render if possible
        DefaultRenderersFactory renderFactory = new DefaultRenderersFactory(this.getApplicationContext());
        renderFactory = renderFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        builder.setRenderersFactory(renderFactory);
        return builder.build();
    }
    
    protected void initializePlayer() {
        releasePlayer();
        player = newPlayer();
        player.addListener(new PlayerEventListener(player));
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        videoView.setPlayer(player);
    }

    protected void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}