package com.orion.iptv.ui.live.livechannellist;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelGroup;
import com.orion.iptv.bean.ChannelInfo;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelManager;
import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.misc.CancelableRunnable;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.ViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LiveChannelListLayout {
    private final String TAG = "LiveChannelListLayout";
    private final View mLayout;
    private final RecyclerView groupList;
    private final View groupListSpacer;
    private final RecyclerAdapter<ViewHolder<ChannelGroup>, ChannelGroup> groupListViewAdapter;
    private final RecyclerView channelList;
    private final RecyclerAdapter<ViewHolder<ChannelItem>, ChannelItem> channelListViewAdapter;
    private final View epgSpacer;
    private final RecyclerAdapter<ViewHolder<EpgProgram>, EpgProgram> epgListViewAdapter;
    private final RecyclerView epgList;
    private final Handler mHandler;
    private ChannelManager channelManager;
    private CancelableRunnable setVisibilityDelayedTask;
    private OnChannelSelectedListener channelSelectedListener;

    private int selectedGroup = 0;
    private int provisionalSelectedGroup = 0;
    private int selectedChannel = 0;

    public LiveChannelListLayout(AppCompatActivity activity, ChannelManager channelManager) {
        this.mLayout = activity.findViewById(R.id.channelListLayout);
        this.channelManager = channelManager;
        this.mHandler = new Handler(activity.getMainLooper());

        epgSpacer = mLayout.findViewById(R.id.channelSpacer3);
        epgList = mLayout.findViewById(R.id.channelEpgList);
        ToggleButton showEpg = mLayout.findViewById(R.id.showEpgButton);
        showEpg.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                epgSpacer.setVisibility(View.VISIBLE);
                epgList.setVisibility(View.VISIBLE);
            } else {
                epgSpacer.setVisibility(View.GONE);
                epgList.setVisibility(View.GONE);
            }
        });
        epgListViewAdapter = new RecyclerAdapter<>(
                mLayout.getContext(),
                new ArrayList<>(),
                new EpgListViewHolderFactory(mLayout.getContext(), R.layout.layout_list_item)
        );
        epgList.setAdapter(epgListViewAdapter);

        channelList = mLayout.findViewById(R.id.channelList);
        channelListViewAdapter = new RecyclerAdapter<>(
                mLayout.getContext(),
                channelManager.getChannels(selectedChannel).orElse(new ArrayList<>()),
                new ChannelListViewHolderFactory(mLayout.getContext(), R.layout.layout_list_item_with_number)
        );
        channelList.setAdapter(channelListViewAdapter);
        channelListViewAdapter.setOnSelectedListener((position, item) -> {
            Log.i(TAG, String.format(Locale.ENGLISH, "channel item: %d selected", position));
            selectedGroup = provisionalSelectedGroup;
            selectedChannel = position;
            if (channelSelectedListener != null) {
                channelSelectedListener.onChannelSelected(item);
            }
        });
        channelList.addOnItemTouchListener(channelListViewAdapter.new OnItemTouchListener(mLayout.getContext(), channelList));

        groupList = mLayout.findViewById(R.id.channelGroup);
        groupListSpacer = mLayout.findViewById(R.id.channelSpacer1);
        if (channelManager.shouldShowGroup()) {
            groupList.setVisibility(View.VISIBLE);
            groupListSpacer.setVisibility(View.VISIBLE);
        }
        groupListViewAdapter = new RecyclerAdapter<>(
                mLayout.getContext(),
                channelManager.groups,
                new GroupListViewHolderFactory(mLayout.getContext(), R.layout.layout_list_item)
        );
        groupList.setAdapter(groupListViewAdapter);
        groupListViewAdapter.setOnSelectedListener((position, item) -> {
            provisionalSelectedGroup = position;
            boolean needResumeSelection = position == selectedGroup;
            List<ChannelItem> channels = this.channelManager.getChannels(position).orElse(new ArrayList<>());
            Log.i(TAG, String.format("select group: %d, channels: %d", position, channels.size()));
            if (needResumeSelection) {
                channelListViewAdapter.resume(channels, selectedChannel);
                channelList.smoothScrollToPosition(selectedChannel);
            } else {
                channelListViewAdapter.setData(channels);
            }
        });
        groupList.addOnItemTouchListener(groupListViewAdapter.new OnItemTouchListener(mLayout.getContext(), groupList));
    }

    public boolean getIsVisible() {
        return mLayout.getVisibility() == View.VISIBLE;
    }

    public void setVisibleDelayed(boolean isVisible, int delayMillis) {
        if (setVisibilityDelayedTask != null) {
            setVisibilityDelayedTask.cancel();
        }
        setVisibilityDelayedTask = new CancelableRunnable() {
            @Override
            public void callback() {
                setVisible(isVisible);
            }
        };
        delayMillis = Math.max(delayMillis, 1);
        mHandler.postDelayed(setVisibilityDelayedTask, delayMillis);
    }

    private void setVisible(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (visibility != mLayout.getVisibility()) {
            mLayout.setVisibility(visibility);
        }
    }

    public void setOnChannelSelectedListener(OnChannelSelectedListener listener) {
        channelSelectedListener = listener;
    }

    public void setEpgPrograms(EpgProgram[] programs, int current) {
        ViewGroup.LayoutParams params = epgList.getLayoutParams();
        int dimenId = programs.length > 0 ? R.dimen.default_epglist_width : R.dimen.empty_epglist_width ;
        params.width = mLayout.getResources().getDimensionPixelSize(dimenId);
        epgList.setLayoutParams(params);
        epgListViewAdapter.resume(List.of(programs), current);
        epgList.smoothScrollToPosition(current);
    }

    public void setEpgPrograms(EpgProgram[] programs) {
        setEpgPrograms(programs, 0);
    }

    public void selectEpgProgram(int position) {
        epgListViewAdapter.selectQuiet(position);
        epgList.smoothScrollToPosition(position);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void resume(ChannelManager m, ChannelInfo info) {
        selectedGroup = m.indexOf(info.groupInfo.groupNumber).orElse(0);
        provisionalSelectedGroup = selectedGroup;
        selectedChannel = m.indexOfChannel(info.groupInfo.groupNumber, info.channelNumber).orElse(0);

        channelManager = m;
        groupListViewAdapter.resume(m.groups, selectedGroup);
        int visibility = m.shouldShowGroup() ? View.VISIBLE : View.GONE;
        groupList.setVisibility(visibility);
        groupListSpacer.setVisibility(visibility);
        groupList.scrollToPosition(selectedGroup);
        List<ChannelItem> channels = m.getChannels(selectedGroup).orElse(new ArrayList<>());
        channelListViewAdapter.resume(channels, selectedChannel);
        channelList.scrollToPosition(selectedChannel);
    }

    public interface OnChannelSelectedListener {
        void onChannelSelected(ChannelItem channelItem);
    }
}
