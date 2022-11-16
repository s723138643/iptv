package com.orion.player.exo;

import android.content.Context;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.orion.player.IExtPlayerFactory;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class ExtExoPlayerFactory implements IExtPlayerFactory<ExtExoPlayer> {
    @Override
    public ExtExoPlayer create(Context context) {
        ExoPlayer.Builder builder = new ExoPlayer.Builder(context);
        // use extension render if possible
        DefaultRenderersFactory renderFactory = new DefaultRenderersFactory(context.getApplicationContext());
        renderFactory = renderFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        builder.setRenderersFactory(renderFactory);

        OkHttpClient client = new OkHttpClient.Builder().build();
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                context,
                new OkHttpDataSource.Factory((Call.Factory) client)
        );
        SimpleTransferMonitor transferMonitor = new SimpleTransferMonitor();
        dataSourceFactory.setTransferListener(transferMonitor);
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(context);
        mediaSourceFactory.setDataSourceFactory(dataSourceFactory);
        builder.setMediaSourceFactory(mediaSourceFactory);

        return new ExtExoPlayer(builder.build(), transferMonitor);
    }
}
