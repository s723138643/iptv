package com.orion.iptv.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;

import com.orion.iptv.R;
import com.orion.iptv.misc.PreferenceStore;
import com.orion.iptv.network.DownloadHelper;
import com.orion.iptv.ui.live.LivePlayerActivity;
import com.orion.iptv.ui.shares.SharesActivity;

import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceStore.setPreferences(PreferenceManager.getDefaultSharedPreferences(this));
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .cache(new Cache(this.getCacheDir(), 50 * 1024 * 1024))
                .followSslRedirects(true)
                .build();
        DownloadHelper.setClient(client);
        Button live_page = findViewById(R.id.live_page);
        live_page.setOnClickListener((view) -> {
            Intent intent = new Intent(this, LivePlayerActivity.class);
            startActivity(intent);
        });

        Button shares_page = findViewById(R.id.shares_page);
        shares_page.setOnClickListener((view) -> {
            Intent intent = new Intent(this, SharesActivity.class);
            startActivity(intent);
        });
    }
}