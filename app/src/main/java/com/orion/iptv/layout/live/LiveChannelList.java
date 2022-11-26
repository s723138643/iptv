package com.orion.iptv.layout.live;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
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
import com.orion.player.ui.EnhanceConstraintLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class LiveChannelList extends Fragment {
    private static final String TAG = "LiveChannelList";

    private LivePlayerViewModel mViewModel;

    EnhanceConstraintLayout enhanceConstraintLayout;
    private RecyclerView groupList;
    private RecyclerView channelList;
    private RecyclerView epgList;
    private View channelSpacer1;
    private View channelSpacer3;
    private ToggleButton showEpgButton;

    private static final long AutoHideAfterMillis = 5*1000;
    private long hideMyselfAt = 0;
    private final Runnable hideMyself = new Runnable() {
        @Override
        public void run() {
            long diff = SystemClock.uptimeMillis() - hideMyselfAt;
            if (diff >= 0) {
                hideMyselfAt = 0;
                hide();
            } else {
                enhanceConstraintLayout.postDelayed(this, -diff);
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

        enhanceConstraintLayout = (EnhanceConstraintLayout) view;
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
        enhanceConstraintLayout.addEventListener(new EnhanceConstraintLayout.EventListener() {
            @Override
            public void onMotionEvent(MotionEvent ev) {
                hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            }

            @Override
            public void onKeyEvent(KeyEvent ev) {
                hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            }

            @Override
            public void onVisibilityChanged(@NonNull View changedView, int visibility) {
                if (changedView != enhanceConstraintLayout) {
                    return;
                }
                enhanceConstraintLayout.removeCallbacks(hideMyself);
                if (visibility == View.VISIBLE && hideMyselfAt > 0) {
                    enhanceConstraintLayout.postDelayed(hideMyself, Math.max(hideMyselfAt-SystemClock.uptimeMillis(), 1));
                }
            }
        });
        initGroupList();
        initChannelList();
        initEpgList();
        ViewCompat.setOnApplyWindowInsetsListener(requireView(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin += insets.left;
            mlp.topMargin += insets.top;
            mlp.bottomMargin += insets.bottom;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity());
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
        epgList.setHasFixedSize(true);
    }

    protected void initChannelList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        channelList.setLayoutManager(layoutManager);
        RecyclerAdapter<ViewHolder<ChannelItem>, ChannelItem> channelListViewAdapter = new RecyclerAdapter<>(
                requireActivity(),
                mViewModel.getChannels().map(pair -> pair.second).orElse(new ArrayList<>()),
                new ChannelListViewHolderFactory(requireActivity(), R.layout.layout_list_item_with_number)
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
        channelList.addOnItemTouchListener(channelListViewAdapter.new OnItemTouchListener(requireActivity(), channelList));
        channelList.setHasFixedSize(true);
    }

    protected void initGroupList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        groupList.setLayoutManager(layoutManager);
        RecyclerAdapter<ViewHolder<ChannelGroup>, ChannelGroup> groupListViewAdapter = new RecyclerAdapter<>(
                requireActivity(),
                mViewModel.getGroups().orElse(new ArrayList<>()),
                new GroupListViewHolderFactory(requireActivity(), R.layout.layout_list_item)
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
        groupList.addOnItemTouchListener(groupListViewAdapter.new OnItemTouchListener(requireActivity(), groupList));
        groupList.setHasFixedSize(true);
    }

    public boolean isViewHidden() {
        return enhanceConstraintLayout.getVisibility() == View.GONE;
    }

    public void toggleVisibility() {
        if (enhanceConstraintLayout.getVisibility() == View.GONE) {
            show();
        } else {
            hide();
        }
    }

    public void show() {
        hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
        enhanceConstraintLayout.setAlpha(0f);
        enhanceConstraintLayout.setVisibility(View.VISIBLE);
        enhanceConstraintLayout.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        enhanceConstraintLayout.setAlpha(1f);
                    }
                });
    }

    public void hide() {
        enhanceConstraintLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    /**
                     * {@inheritDoc}
                     *
                     * @param animation
                     */
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        enhanceConstraintLayout.setVisibility(View.GONE);
                    }
                });
    }
}