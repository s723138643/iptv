package com.orion.iptv.layout.liveplayersetting;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.R;
import com.orion.iptv.misc.CancelableRunnable;

import java.util.List;
import java.util.Locale;

public class LivePlayerSettingLayout {
    private final View mLayout;
    private final RecyclerView settingMenuView;
    private final RecyclerView settingValueView;
    private final ListAdapter<SettingMenu> menuViewAdapter;
    private final ListAdapter<SettingValue> valueViewAdapter;
    private final SelectionTracker<Long> valueTracker;
    private final SelectionTracker<Long> menuTracker;
    private final Handler mHandler;
    private CancelableRunnable setVisibleTask;

    private final List<SettingMenu> menus;
    private OnSettingChangedListener listener;

    public LivePlayerSettingLayout(AppCompatActivity activity) {
        menus = List.of(new SetChannelSourceUrl(activity));
        mHandler = new Handler(activity.getMainLooper());
        mLayout = activity.findViewById(R.id.livePlayerSetting);
        settingMenuView = mLayout.findViewById(R.id.livePlayerMenu);
        settingValueView = mLayout.findViewById(R.id.livePlayerValue);

        valueViewAdapter = new ListAdapter<>(mLayout.getContext(), menus.get(0).getValues());
        settingValueView.setAdapter(valueViewAdapter);
        valueTracker = new SelectionTracker.Builder<>(
                "live_player_setting_value",
                settingValueView,
                new KeyProvider(ItemKeyProvider.SCOPE_MAPPED),
                new ItemLookup(settingValueView),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectSingleAnything()).build();
        valueViewAdapter.setTracker(valueTracker);
        valueViewAdapter.setOnSelected(this::onValueSelected);

        menuViewAdapter = new ListAdapter<>(mLayout.getContext(), menus);
        settingMenuView.setAdapter(menuViewAdapter);
        menuTracker = new SelectionTracker.Builder<>(
                "live_player_setting_menu",
                settingMenuView,
                new KeyProvider(ItemKeyProvider.SCOPE_MAPPED),
                new ItemLookup(settingMenuView),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectSingleAnything()).build();
        menuViewAdapter.setTracker(menuTracker);
        menuViewAdapter.setOnSelected(this::onMenuSelected);
    }

    private void onValueSelected(SettingValue value) {
        Log.i("Setting", String.format(Locale.ENGLISH, "value: %s selected", value.Name()));
        if (listener == null) {
            return;
        }
        value.onSelected(listener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void onMenuSelected(SettingMenu menu) {
        Log.i("Setting", String.format(Locale.ENGLISH, "menu %s selected", menu.Name()));
        this.mHandler.post(()->{
            this.valueTracker.clearSelection();
            this.valueViewAdapter.setItems(menu.getValues());
            this.valueViewAdapter.notifyDataSetChanged();
        });
    }

    public void setOnSettingChangedListener(OnSettingChangedListener listener) {
        this.listener = listener;
    }

    private void _setVisible(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (mLayout.getVisibility() != visibility) {
            mLayout.setVisibility(visibility);
        }
    }

    public void setVisible(boolean isVisible) {
        if (setVisibleTask != null) {
            setVisibleTask.cancel();
        }
        setVisibleTask = new CancelableRunnable() {
            @Override
            public void callback() {
                _setVisible(isVisible);
            }
        };
        mHandler.post(setVisibleTask);
    }

    public boolean getIsVisible() {
        return mLayout.getVisibility() == View.VISIBLE;
    }
}