package com.orion.iptv.layout.live;

import android.util.Log;
import android.util.Pair;

import com.orion.player.ExtDataSource;

import java.util.List;
import java.util.Locale;

public class DataSourceManager {
    private static final String TAG = "DataSourceManager";
    private final List<ExtDataSource> sources;
    private int cursor = 0;

    public DataSourceManager(List<ExtDataSource> sources) {
        this.sources = sources;
    }

    public int getCursor(ExtDataSource dataSource) {
        ExtDataSource source = sources.get(cursor);
        if (dataSource.getUri().equals(source.getUri())) {
            return cursor;
        }
        Log.w(TAG, String.format(Locale.getDefault(), "mismatch data source cursor %d/%d", cursor+1, sources.size()));
        for (int i=0; i<sources.size(); i++) {
            source = sources.get(i);
            if (source.getUri().equals(dataSource.getUri())) {
                Log.w(TAG, String.format(Locale.getDefault(), "mismatch data source cursor %d/%d, real: %d", cursor+1, sources.size(), i+1));
                return i;
            }
        }
        return -1;
    }

    public Pair<Integer, ExtDataSource> nextDataSource() {
        cursor += 1;
        cursor = cursor < sources.size() ? cursor : 0;
        Log.i(TAG,String.format(Locale.getDefault(), "use source %d/%d", cursor+1, sources.size()));
        return Pair.create(cursor, sources.get(cursor));
    }

    public Pair<Integer, ExtDataSource> prevDataSource() {
        cursor -= 1;
        cursor = cursor >= 0 ? cursor : sources.size() - 1;
        Log.i(TAG,String.format(Locale.getDefault(), "use source %d/%d", cursor+1, sources.size()));
        return Pair.create(cursor, sources.get(cursor));
    }

    public int getDataSourceCount() {
        return sources.size();
    }

    public Pair<Integer, ExtDataSource> getCurrentDataSource() {
        return Pair.create(cursor, sources.get(cursor));
    }
}
