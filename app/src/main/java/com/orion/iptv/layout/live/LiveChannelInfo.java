package com.orion.iptv.layout.live;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelInfo;
import com.orion.iptv.bean.EpgProgram;
import com.orion.player.ExtDataSource;
import com.orion.player.ExtTrackInfo;
import com.orion.player.IExtPlayer;

import java.util.List;
import java.util.Locale;

public class LiveChannelInfo extends Fragment {
    private final String[] units = {"bps", "kbps", "Mbps", "Gbps"};
    protected LivePlayerViewModel viewModel;
    protected TextView channelNumber;
    protected TextView channelName;
    protected TextView codecInfo;
    protected TextView mediaInfo;
    protected TextView bitrateInfo;
    protected TextView linkInfo;
    protected TextView currentEpgProgram;
    protected TextView nextEpgProgram;
    protected Resources res;

    protected Handler mHandler;
    protected long hideMyselfAt = 0;
    protected final Runnable hideMyself = new Runnable() {
        @Override
        public void run() {
            if (hideMyselfAt <= 0) {
                return;
            }
            if (SystemClock.uptimeMillis() >= hideMyselfAt) {
                hideMyselfAt = 0;
                hide();
            } else {
                mHandler.postAtTime(this, hideMyselfAt);
            }
        }
    };
    protected final IExtPlayer.Listener listener = new PlayerEventListener();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_channel_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHandler = new Handler(requireContext().getMainLooper());
        channelNumber = view.findViewById(R.id.channelNumber);
        channelName = view.findViewById(R.id.channelName);
        codecInfo = view.findViewById(R.id.codecInfo);
        mediaInfo = view.findViewById(R.id.mediaInfo);
        bitrateInfo = view.findViewById(R.id.bitrateInfo);
        linkInfo = view.findViewById(R.id.linkInfo);
        currentEpgProgram = view.findViewById(R.id.currentEpgProgram);
        nextEpgProgram = view.findViewById(R.id.nextEpgProgram);
    }

    @Override
    public void onStart() {
        super.onStart();
        res = requireContext().getResources();
        channelName.setSelected(true);
        currentEpgProgram.setSelected(true);
        nextEpgProgram.setSelected(true);
        viewModel = new ViewModelProvider(requireActivity()).get(LivePlayerViewModel.class);
        viewModel.observeLiveSource(requireActivity(), this::updateChannelInfo);
        viewModel.observeCurrentEpgProgram(requireActivity(), this::setCurrentEpgProgram);
        viewModel.observeNextEpgProgram(requireActivity(), this::setNextEpgProgram);
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
    }

    protected void updateChannelInfo(Pair<Integer, ExtDataSource> dataSource) {
        ChannelInfo info = (ChannelInfo) dataSource.second.getTag();
        assert info != null;
        setChannelNumber(info.channelNumber);
        setChannelName(info.channelName);
        setCodecInfo(res.getString(R.string.codec_info_default));
        setMediaInfo(res.getString(R.string.media_info_default));
        setBitrateInfo(0);
        setLinkInfo(dataSource.first, viewModel.getSourceCount());
    }

    public void setChannelName(String name) {
        this.channelName.setText(name);
    }

    public void setChannelNumber(int number) {
        this.channelNumber.setText(String.format(Locale.ENGLISH, "%d", number));
    }

    private String formatBitrate(long bitrate) {
        float base = 1000.0f;
        float rate = (float) bitrate;
        int i = 0;
        for (; i < units.length; i++) {
            if (rate < base) {
                break;
            }
            rate /= base;
        }
        return String.format(Locale.ENGLISH, "%.2f%s", rate, units[i]);
    }

    public void setBitrateInfo(long bitrate) {
        if (bitrate <= 0) {
            this.bitrateInfo.setVisibility(View.GONE);
            return;
        }
        String formattedBitrate = formatBitrate(bitrate);
        this.bitrateInfo.setText(formattedBitrate);
        this.bitrateInfo.setVisibility(View.VISIBLE);
    }

    public void setCodecInfo(String info) {
        this.codecInfo.setText(info);
    }

    public void setMediaInfo(String info) {
        this.mediaInfo.setText(info);
    }

    public void setLinkInfo(int currentIndex, int totalLinks) {
        this.linkInfo.setText(String.format(Locale.ENGLISH, "[%d/%d]", currentIndex+1, totalLinks));
    }

    public void setCurrentEpgProgram(Pair<Integer, Pair<ChannelInfo, EpgProgram>> program) {
        if (program != null) {
            currentEpgProgram.setText(program.second.second.content());
        } else {
            currentEpgProgram.setText(res.getString(R.string.current_epg_program_default));
        }
    }

    public void setNextEpgProgram(Pair<Integer, Pair<ChannelInfo, EpgProgram>> program) {
        if (program != null) {
            nextEpgProgram.setText(program.second.second.content());
        } else {
            nextEpgProgram.setText(res.getString(R.string.next_epg_program_default));
        }
    }

    public void setPlayer(IExtPlayer player) {
        player.addListener(listener);
    }

    @SuppressWarnings("unused")
    public void toggleVisibility() {
        if (isHidden()) {
            show();
        } else {
            hide();
        }
    }

    public void show(long displayMillis) {
        if (!isHidden() && hideMyselfAt <= 0) {
            // we have showed it without auth hide, so ignore this operation
            return;
        }
        show();
        hideMyselfAt = SystemClock.uptimeMillis() + displayMillis;
    }

    public void show() {
        hideMyselfAt = 0;
        getParentFragmentManager().beginTransaction()
                .show(this)
                .commit();
    }

    public void hide(long delayMillis) {
        mHandler.removeCallbacks(hideMyself);
        hideMyselfAt = SystemClock.uptimeMillis() + delayMillis;
        mHandler.postAtTime(hideMyself, hideMyselfAt);
    }

    public void hide() {
        getParentFragmentManager().beginTransaction()
                .hide(this)
                .commit();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        mHandler.removeCallbacks(hideMyself);
        if (!hidden && hideMyselfAt > 0) {
            mHandler.postAtTime(hideMyself, hideMyselfAt);
        }
    }

    private class PlayerEventListener implements IExtPlayer.Listener {
        @Override
        public void onTracksSelected(List<ExtTrackInfo> tracks) {
            for (int i=0; i<tracks.size(); i++) {
                ExtTrackInfo track = tracks.get(i);
                if (track.type == ExtTrackInfo.TRACK_TYPE_VIDEO) {
                    setMediaInfo(track.width + "x" + track.height);
                    setCodecInfo(track.codecs);
                    setBitrateInfo(track.bitrate);
                }
            }
        }
    }
}
