package com.orion.iptv.layout.live;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelGroup;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.player.ui.AutoHide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class LiveChannelList extends Fragment {
    private static final String TAG = "LiveChannelList";

    private LivePlayerViewModel mViewModel;
    private RecyclerView groupList;
    private RecyclerView channelList;
    private RecyclerView epgList;
    private View channelSpacer1;
    private View channelSpacer3;
    private ToggleButton showEpgButton;

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
        return inflater.inflate(R.layout.fragment_live_channel_list, container, false);
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

        groupList = view.findViewById(R.id.channelGroup);
        channelList = view.findViewById(R.id.channelList);
        epgList = view.findViewById(R.id.channelEpgList);
        epgList.setVisibility(View.GONE);
        channelSpacer1 = view.findViewById(R.id.channelSpacer1);
        channelSpacer3 = view.findViewById(R.id.channelSpacer3);
        showEpgButton = view.findViewById(R.id.showEpgButton);
    }

    @Override
    public void onStart() {
        super.onStart();
        mViewModel = new ViewModelProvider(requireActivity()).get(LivePlayerViewModel.class);
        initGroupList();
        initChannelList();
        initEpgList();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
    }

    protected void initEpgList() {
        showEpgButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                channelSpacer3.setVisibility(View.VISIBLE);
                epgList.setVisibility(View.VISIBLE);
            } else {
                channelSpacer3.setVisibility(View.GONE);
                epgList.setVisibility(View.GONE);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        epgList.setLayoutManager(layoutManager);
        RecyclerAdapter<ViewHolder<EpgProgram>, EpgProgram> epgListViewAdapter = new RecyclerAdapter<>(
                requireContext(),
                new ArrayList<>(),
                new EpgListViewHolderFactory(requireContext(), R.layout.layout_list_item)
        );
        mViewModel.observeEpgs(requireActivity(), epgs -> {
            if (epgs != null) {
                epgListViewAdapter.setData(Arrays.stream(epgs.second).collect(Collectors.toList()));
            } else {
                epgListViewAdapter.setData(new ArrayList<>());
            }
        });
        mViewModel.observeCurrentEpgProgram(requireActivity(), pair -> {
            if (pair != null) {
                epgListViewAdapter.selectQuiet(pair.first);
                epgList.smoothScrollToPosition(pair.first);
            }
        });
        epgList.setAdapter(epgListViewAdapter);
    }

    protected void initChannelList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        channelList.setLayoutManager(layoutManager);
        RecyclerAdapter<ViewHolder<ChannelItem>, ChannelItem> channelListViewAdapter = new RecyclerAdapter<>(
                requireContext(),
                mViewModel.getChannels().map(pair -> pair.second).orElse(new ArrayList<>()),
                new ChannelListViewHolderFactory(requireContext(), R.layout.layout_list_item_with_number)
        );
        mViewModel.observeChannels(requireActivity(), channels -> {
            if (channels != null) {
                int pos = mViewModel.getSelectedChannelInGroup(channels.first);
                if (pos >= 0) {
                    channelListViewAdapter.resume(channels.second, pos);
                    channelList.smoothScrollToPosition(pos);
                } else {
                    channelListViewAdapter.setData(channels.second);
                }
            } else {
                channelListViewAdapter.setData(new ArrayList<>());
            }
        });
        channelList.setAdapter(channelListViewAdapter);
        channelListViewAdapter.setOnSelectedListener((position, item) -> {
            Log.i(TAG, String.format(Locale.ENGLISH, "channel item %d::%s selected", position, item.info.channelName));
            mViewModel.selectChannel(position, item);
        });
        channelList.addOnItemTouchListener(channelListViewAdapter.new OnItemTouchListener(requireContext(), channelList));
    }

    protected void initGroupList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        groupList.setLayoutManager(layoutManager);
        RecyclerAdapter<ViewHolder<ChannelGroup>, ChannelGroup> groupListViewAdapter = new RecyclerAdapter<>(
                requireContext(),
                mViewModel.getGroups().orElse(new ArrayList<>()),
                new GroupListViewHolderFactory(requireContext(), R.layout.layout_list_item)
        );
        mViewModel.observeGroups(requireActivity(), groups -> {
            int visibility = (groups == null || groups.size() < 2) ? View.GONE : View.VISIBLE;
            channelSpacer1.setVisibility(visibility);
            groupList.setVisibility(visibility);
            int selectedGroup = mViewModel.getSelectedGroup();
            groupListViewAdapter.resume(groups, selectedGroup);
            groupList.smoothScrollToPosition(selectedGroup);
        });
        groupList.setAdapter(groupListViewAdapter);
        groupListViewAdapter.setOnSelectedListener((position, item) -> mViewModel.selectGroup(position, item));
        groupList.addOnItemTouchListener(groupListViewAdapter.new OnItemTouchListener(requireContext(), groupList));
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