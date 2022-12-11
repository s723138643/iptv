package com.orion.iptv.layout.live;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.orion.iptv.recycleradapter.DefaultSelection;
import com.orion.iptv.recycleradapter.Selection;
import com.orion.iptv.recycleradapter.SelectionWithFocus;
import com.orion.player.ui.EnhanceConstraintLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LiveChannelList extends Fragment {
    private static final String TAG = "LiveChannelList";

    private LivePlayerViewModel mViewModel;

    EnhanceConstraintLayout enhanceConstraintLayout;
    private RecyclerView groupList;
    private RecyclerView channelList;
    private RecyclerView epgList;
    private Selection<ChannelItem> selection;
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
                if (visibility == View.VISIBLE) {
                    channelList.requestFocus();
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
    }

    protected void initEpgList() {
        epgList.setItemAnimator(null);
        showEpgButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int visibility = isChecked ? View.VISIBLE : View.GONE;
            epgList.setVisibility(visibility);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        // layoutManager.setMeasurementCacheEnabled(false);
        epgList.setLayoutManager(layoutManager);
        Selection<EpgProgram> epgProgramSelection = new SelectionWithFocus<>(epgList);
        mViewModel.observeEpgs(requireActivity(), epgs -> {
            List<EpgProgram> items = epgs != null ? Arrays.asList(epgs.second) : new ArrayList<>();
            RecyclerAdapter<EpgProgram> epgListViewAdapter = new RecyclerAdapter<>(
                    requireContext(),
                    items,
                    new EpgListViewHolderFactory(requireContext(), R.layout.layout_list_item)
            );
            epgProgramSelection.setAdapter(epgListViewAdapter);
            epgList.swapAdapter(epgListViewAdapter, true);
        });
        mViewModel.observeCurrentEpgProgram(requireActivity(), pair -> {
            if (pair != null) {
                epgProgramSelection.selectQuiet(pair.first);
                epgList.scrollToPosition(pair.first);
            }
        });
        // epgList.setHasFixedSize(true);
    }

    protected void initChannelList() {
        channelList.setItemAnimator(null);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        // layoutManager.setMeasurementCacheEnabled(false);
        channelList.setLayoutManager(layoutManager);
        selection = new SelectionWithFocus<>(channelList);
        mViewModel.observeChannels(requireActivity(), channels -> {
            List<ChannelItem> items = channels != null ? channels.second : new ArrayList<>();
            int position = channels != null ? mViewModel.getSelectedChannelInGroup(channels.first) : RecyclerView.NO_POSITION;
            LiveAdapter<ChannelItem> channelListViewAdapter = new LiveAdapter<>(
                    requireActivity(),
                    items,
                    new ChannelListViewHolderFactory(requireActivity(), R.layout.layout_list_item_with_number)
            );
            selection.setAdapter(channelListViewAdapter, position);
            channelList.swapAdapter(channelListViewAdapter, true);
            channelList.scrollToPosition(position);
        });
        selection.addSelectedListener((position, item) -> {
            Log.i(TAG, String.format(Locale.ENGLISH, "channel item %d::%s selected", position, item.info.channelName));
            mViewModel.selectChannel(position, item);
        });
        // channelList.setHasFixedSize(true);
    }

    protected void initGroupList() {
        groupList.setItemAnimator(null);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        // layoutManager.setMeasurementCacheEnabled(false);
        groupList.setLayoutManager(layoutManager);
        DefaultSelection<ChannelGroup> defaultSelection = new DefaultSelection<>(groupList);
        mViewModel.observeGroups(requireActivity(), groups -> {
            int visibility = (groups == null || groups.size() < 2) ? View.GONE : View.VISIBLE;
            channelSpacer1.setVisibility(visibility);
            groupList.setVisibility(visibility);
            LiveAdapter<ChannelGroup> groupListViewAdapter = new LiveAdapter<>(
                    requireActivity(),
                    groups != null ? groups : new ArrayList<>(),
                    new GroupListViewHolderFactory(requireActivity(), R.layout.layout_list_item)
            );
            groupListViewAdapter.setSelection(defaultSelection);
            int selectedGroup = mViewModel.getSelectedGroup();
            defaultSelection.setAdapter(groupListViewAdapter, selectedGroup);
            groupList.swapAdapter(groupListViewAdapter, true);
            groupList.scrollToPosition(selectedGroup);
        });
        defaultSelection.addSelectedListener((position, item) -> mViewModel.selectGroup(position, item));
        // groupList.setHasFixedSize(true);
    }

    public boolean isViewVisible() {
        return enhanceConstraintLayout.getVisibility() == View.VISIBLE;
    }

    public void seekToPrevChannel() {
        selection.selectPrev();
    }

    public void seekToNextChannel() {
        selection.selectNext();
    }

    public void toggleVisibility() {
        if (enhanceConstraintLayout.getVisibility() == View.GONE) {
            show();
        } else {
            hide();
        }
    }

    public void toggleVisibility(boolean needFocus) {
        if (enhanceConstraintLayout.getVisibility() == View.GONE) {
            show(needFocus);
        } else {
            hide();
        }
    }

    public void show(boolean needFocus) {
        show();
        if (needFocus && !enhanceConstraintLayout.hasFocus()) {
            channelList.requestFocus();
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