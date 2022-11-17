package com.orion.iptv.layout.live;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;
import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.ViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LivePlayerSetting extends Fragment {
    private RecyclerView settingMenuView;
    private RecyclerView settingValueView;
    private RecyclerAdapter<ViewHolder<SettingValue>, SettingValue> valueViewAdapter;
    private Handler mHandler;
    private List<SettingMenu> menus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_live_player_setting, container, false);
        settingMenuView = v.findViewById(R.id.livePlayerMenu);
        settingValueView = v.findViewById(R.id.livePlayerValue);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LivePlayerViewModel viewModel = new ViewModelProvider(requireActivity()).get(LivePlayerViewModel.class);
        mHandler = new Handler(requireContext().getMainLooper());

        menus = new ArrayList<>();
        menus.add(new SetChannelSourceUrl(requireContext(), viewModel));
        menus.add(new SetPlayerFactory(viewModel));

        initMenuList();
        initValueList();
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

    public void setVisible(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (isVisible() != isVisible) {
            requireView().setVisibility(visibility);
        }
    }
}