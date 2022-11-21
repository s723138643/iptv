package com.orion.iptv.layout.live;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.player.ui.AutoHide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LivePlayerSetting extends Fragment {
    private RecyclerView settingMenuView;
    private RecyclerView settingValueView;
    private RecyclerAdapter<ViewHolder<SettingValue>, SettingValue> valueViewAdapter;
    private List<SettingMenu> menus;
    private Handler mHandler;
    private static final long AutoHideAfterMillis = 5*1000;
    private long hideMyselfAt = 0;
    private final Runnable hideMyself = new Runnable() {
        @Override
        public void run() {
            if (SystemClock.uptimeMillis() >= hideMyselfAt) {
                hide();
            } else {
                mHandler.postAtTime(this, hideMyselfAt);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_player_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHandler = new Handler(requireContext().getMainLooper());
        AutoHide autoHide = (AutoHide) view;
        autoHide.addEventListener(new AutoHide.EventListener() {
            @Override
            public void onMotionEvent(MotionEvent ev) {
                hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            }

            @Override
            public void onKeyEvent(KeyEvent ev) {
                hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            }
        });

        settingMenuView = view.findViewById(R.id.livePlayerMenu);
        settingValueView = view.findViewById(R.id.livePlayerValue);
    }

    @Override
    public void onStart() {
        super.onStart();
        LivePlayerViewModel viewModel = new ViewModelProvider(requireActivity()).get(LivePlayerViewModel.class);

        menus = new ArrayList<>();
        menus.add(new SetChannelSourceUrl(requireContext(), viewModel));
        menus.add(new SetPlayerFactory(viewModel));

        initMenuList();
        initValueList();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
    }

    protected void initValueList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        settingValueView.setLayoutManager(layoutManager);
        valueViewAdapter = new RecyclerAdapter<>(
                requireContext(),
                menus.get(0).getValues(),
                new ValueListViewHolderFactory(requireContext(), R.layout.layout_list_item)
        );
        valueViewAdapter.setOnSelectedListener(this::onValueSelected);
        settingValueView.setAdapter(valueViewAdapter);
        settingValueView.addOnItemTouchListener(valueViewAdapter.new OnItemTouchListener(requireContext(), settingValueView));
    }

    protected void initMenuList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        settingMenuView.setLayoutManager(layoutManager);
        RecyclerAdapter<ViewHolder<SettingMenu>, SettingMenu> menuViewAdapter = new RecyclerAdapter<>(
                requireContext(),
                menus,
                new MenuListViewHolderFactory(requireContext(), R.layout.layout_list_item)
        );
        settingMenuView.setAdapter(menuViewAdapter);
        settingMenuView.addOnItemTouchListener(menuViewAdapter.new OnItemTouchListener(requireContext(), settingMenuView));
        menuViewAdapter.setOnSelectedListener(this::onMenuSelected);
        menuViewAdapter.selectQuiet(0);
    }

    private void onValueSelected(int position, SettingValue value) {
        value.onSelected();
        if (value.isButton()) {
            Log.i("Setting", "clear selection...");
            mHandler.postDelayed(valueViewAdapter::clearSelection, 800);
        }
    }

    private void onMenuSelected(int position, SettingMenu menu) {
        Log.i("Setting", String.format(Locale.ENGLISH, "menu %s selected", menu.content()));
        int selectedPosition = menu.getSelectedPosition();
        if (selectedPosition >= 0) {
            this.valueViewAdapter.resume(menu.getValues(), selectedPosition);
        } else {
            this.valueViewAdapter.setData(menu.getValues());
        }
    }

    public void toggleVisibility() {
        if (isHidden()) {
            show();
        } else {
            hide();
        }
    }

    public void show() {
        getParentFragmentManager()
                .beginTransaction()
                .show(this)
                .commit();
    }

    public void hide() {
        getParentFragmentManager()
                .beginTransaction()
                .hide(this)
                .commit();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        mHandler.removeCallbacks(hideMyself);
        if (!hidden) {
            hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            mHandler.postAtTime(hideMyself, hideMyselfAt);
        }
    }
}