package com.orion.iptv.ui.shares;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.orion.iptv.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class SharesActivity extends AppCompatActivity {
    private static final String TAG = "SharesActivity";
    private static final String storeFile = "shares.json";
    private SharesViewModel sharesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shares);
        sharesViewModel = new ViewModelProvider(this).get(SharesViewModel.class);
        if (savedInstanceState == null) {
            resumeFromFile();
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.shares_container_view, SharesHomeFragment.class, null)
                    .commit();
        }
    }

    protected void resumeFromFile() {
        Log.i(TAG, "resume shares from file");
        try {
            FileInputStream stream = openFileInput(storeFile);
            try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
                Gson gson = new Gson();
                Share[] shares = gson.fromJson(reader, Share[].class);

                ArrayList<Share> muteAbleShares = new ArrayList<>(shares.length);
                muteAbleShares.addAll(Arrays.asList(shares));
                sharesViewModel.getShares().setValue(muteAbleShares);
            }
        } catch (IOException err) {
            Log.e(TAG, "resume shares failed, " + err.toString());
        }
    }

    protected void saveToFile() {
        Log.i(TAG, "saving shares to file");
        Share[] shares = Objects.requireNonNull(sharesViewModel.getShares().getValue()).toArray(new Share[0]);
        try {
            FileOutputStream stream = openFileOutput(storeFile, MODE_PRIVATE);
            try (stream; OutputStreamWriter writer = new OutputStreamWriter(stream)) {
                Gson gson = new Gson();
                gson.toJson(shares, writer);
            }
        } catch (IOException err) {
            Log.e(TAG, "save shares failed, " + err.toString());
        }
    }

    @Override
    protected void onDestroy() {
        saveToFile();
        super.onDestroy();
    }
}