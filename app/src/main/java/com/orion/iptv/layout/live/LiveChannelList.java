package com.orion.iptv.layout.live;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelGroup;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.ViewHolder;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_channel_list, container, false);
        groupList = view.findViewById(R.id.channelGroup);
        channelList = view.findViewById(R.id.channelList);
        epgList = view.findViewById(R.id.channelEpgList);
        epgList.setVisibility(View.GONE);
        channelSpacer1 = view.findViewById(R.id.channelSpacer1);
        channelSpacer3 = view.findViewById(R.id.channelSpacer3);
        showEpgButton = view.findViewById(R.id.showEpgButton);
        return view;
    }

    private int getTextPixelSize(DisplayMetrics metrics) {
        float sp = new Paint().getTextSize();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics);
        return (int) px;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(LivePlayerViewModel.class);
        initGroupList();
        initChannelList();
        initEpgList();
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
}