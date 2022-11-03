package com.orion.iptv.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class PreferenceStore {
    private static final PreferenceStore store = new PreferenceStore();
    private SharedPreferences preferences;

    private PreferenceStore() {}

    public static void setPreferences(SharedPreferences preferences) {
        store.preferences = preferences;
    }

    private static SharedPreferences getSharedPreferences() {
        return store.preferences;
    }

    public static int getInt(String key, int defaultInt) {
        return getSharedPreferences().getInt(key, defaultInt);
    }

    public static String getString(String key, String defaultString) {
        return getSharedPreferences().getString(key, defaultString);
    }

    public static float getFloat(String key, float defaultFloat) {
        return getSharedPreferences().getFloat(key, defaultFloat);
    }

    public static long getLong(String key, long defaultLong) {
        return getSharedPreferences().getLong(key, defaultLong);
    }

    public static boolean getBoolean(String key, boolean defaultBoolean) {
        return getSharedPreferences().getBoolean(key, defaultBoolean);
    }

    public static void setInt(String key, int value) {
        getSharedPreferences().edit().putInt(key, value).apply();
    }

    public static void setString(String key, String value) {
        getSharedPreferences().edit().putString(key, value).apply();
    }

    public static void setFloat(String key, float value) {
        getSharedPreferences().edit().putFloat(key, value).apply();
    }

    public static void setLong(String key, long value) {
        getSharedPreferences().edit().putLong(key, value).apply();
    }

    public static void setBoolean(String key, boolean value) {
        getSharedPreferences().edit().putBoolean(key, value).apply();
    }

    public static boolean commit() {
        return getSharedPreferences().edit().commit();
    }
}
