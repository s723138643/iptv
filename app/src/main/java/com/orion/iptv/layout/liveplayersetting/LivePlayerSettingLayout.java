package com.orion.iptv.layout.liveplayersetting;

import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.R;
import com.orion.iptv.misc.CancelableRunnable;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.ViewHolder;

import java.util.List;
import java.util.Locale;

public class LivePlayerSettingLayout {
    private final View mLayout;
    private final RecyclerView settingMenuView;
    private final RecyclerView settingValueView;
    private final RecyclerAdapter<ViewHolder<SettingMenu>, SettingMenu> menuViewAdapter;
    private final RecyclerAdapter<ViewHolder<SettingValue>, SettingValue> valueViewAdapter;
    private final Handler mHandler;
    private final List<SettingMenu> menus;
    private CancelableRunnable setVisibleTask;
    private OnSettingChangedListener listener;

    public LivePlayerSettingLayout(AppCompatActivity activity) {
        menus = List.of(new SetChannelSourceUrl(activity));
        mHandler = new Handler(activity.getMainLooper());
        mLayout = activity.findViewById(R.id.livePlayerSetting);
        settingMenuView = mLayout.findViewById(R.id.livePlayerMenu);
        settingValueView = mLayout.findViewById(R.id.livePlayerValue);

        valueViewAdapter = new RecyclerAdapter<>(
                mLayout.getContext(),
                menus.get(0).getValues(),
                new ValueListViewHolderFactory(activity, R.layout.live_channel_list_item)
        );
        valueViewAdapter.setOnSelectedListener(this::onValueSelected);
        settingValueView.setAdapter(valueViewAdapter);
        settingValueView.addOnItemTouchListener(valueViewAdapter.new OnItemTouchListener(activity, settingValueView));

        menuViewAdapter = new RecyclerAdapter<>(
                mLayout.getContext(),
                menus,
                new MenuListViewHolderFactory(activity, R.layout.live_channel_list_item)
        );
        settingMenuView.setAdapter(menuViewAdapter);
        settingMenuView.addOnItemTouchListener(menuViewAdapter.new OnItemTouchListener(activity, settingMenuView));
        menuViewAdapter.setOnSelectedListener(this::onMenuSelected);
        menuViewAdapter.selectQuiet(0);
    }

    private void onValueSelected(int position, SettingValue value) {
        if (listener == null) {
            return;
        }
        value.onSelected(listener);
        if (value.isButton()) {
            Log.i("Setting", "clear selection...");
            mHandler.postDelayed(valueViewAdapter::clearSelection, 800);
        }
    }

    private void onMenuSelected(int position, SettingMenu menu) {
        Log.i("Setting", String.format(Locale.ENGLISH, "menu %s selected", menu.describe()));
        this.valueViewAdapter.setData(menu.getValues());
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