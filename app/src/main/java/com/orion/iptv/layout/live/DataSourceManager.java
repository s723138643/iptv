package com.orion.iptv.layout.live;

import android.util.Log;
import android.util.Pair;

import com.orion.player.ExtDataSource;

import java.util.List;
import java.util.Locale;

public class DataSourceManager {
    private static final String TAG = "DataSourceManager";
    private final List<DataSource> sources;
    private int cursor = 0;

    public DataSourceManager(List<DataSource> sources) {
        this.sources = sources;
    }

    public int getCursor(DataSource dataSource) {
        DataSource source = sources.get(cursor);
        if (dataSource.dataSource.getUri().equals(source.dataSource.getUri())) {
            return cursor;
        }
        Log.w(TAG, String.format(Locale.getDefault(), "mismatch data source cursor %d/%d", cursor+1, sources.size()));
        for (int i=0; i<sources.size(); i++) {
            source = sources.get(i);
            if (source.dataSource.getUri().equals(dataSource.dataSource.getUri())) {
                Log.w(TAG, String.format(Locale.getDefault(), "mismatch data source cursor %d/%d, real: %d", cursor+1, sources.size(), i+1));
                return i;
            }
        }
        return -1;
    }

    public Pair<Integer, DataSource> nextDataSource() {
        cursor += 1;
        cursor = cursor < sources.size() ? cursor : 0;
        Log.i(TAG,String.format(Locale.getDefault(), "use source %d/%d", cursor+1, sources.size()));
        return Pair.create(cursor, sources.get(cursor));
    }

    public Pair<Integer, DataSource> prevDataSource() {
        cursor -= 1;
        cursor = cursor >= 0 ? cursor : sources.size() - 1;
        Log.i(TAG,String.format(Locale.getDefault(), "use source %d/%d", cursor+1, sources.size()));
        return Pair.create(cursor, sources.get(cursor));
    }

    public int getDataSourceCount() {
        return sources.size();
    }

    public Pair<Integer, DataSource> getCurrentDataSource() {
        return Pair.create(cursor, sources.get(cursor));
    }
}
