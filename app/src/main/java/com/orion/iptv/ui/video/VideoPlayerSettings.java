package com.orion.iptv.ui.video;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.orion.iptv.R;

public class VideoPlayerSettings extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}