package com.orion.iptv.misc;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class PreferenceStore {
    private final Context context;

    public PreferenceStore(Context context) {
        this.context = context;
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getInt(String key, int defaultInt) {
        return getSharedPreferences().getInt(key, defaultInt);
    }

    public String getString(String key, String defaultString) {
        return getSharedPreferences().getString(key, defaultString);
    }

    public float getFloat(String key, float defaultFloat) {
        return getSharedPreferences().getFloat(key, defaultFloat);
    }

    public long getLong(String key, long defaultLong) {
        return getSharedPreferences().getLong(key, defaultLong);
    }

    public boolean getBoolean(String key, boolean defaultBoolean) {
        return getSharedPreferences().getBoolean(key, defaultBoolean);
    }

    public void setInt(String key, int value) {
        getSharedPreferences().edit().putInt(key, value).apply();
    }

    public void setString(String key, String value) {
        getSharedPreferences().edit().putString(key, value).apply();
    }

    public void setFloat(String key, float value) {
        getSharedPreferences().edit().putFloat(key, value).apply();
    }

    public void setLong(String key, long value) {
        getSharedPreferences().edit().putLong(key, value).apply();
    }

    public void setBoolean(String key, boolean value) {
        getSharedPreferences().edit().putBoolean(key, value).apply();
    }

    public boolean commit() {
        return getSharedPreferences().edit().commit();
    }
}
