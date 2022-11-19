package com.orion.iptv.layout.live;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
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
    protected final Runnable hideMyself = this::hide;
    protected final IExtPlayer.Listener listener = new PlayerEventListener();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_live_channel_info, container, false);
        channelNumber = v.findViewById(R.id.channelNumber);
        channelName = v.findViewById(R.id.channelName);
        codecInfo = v.findViewById(R.id.codecInfo);
        mediaInfo = v.findViewById(R.id.mediaInfo);
        bitrateInfo = v.findViewById(R.id.bitrateInfo);
        linkInfo = v.findViewById(R.id.linkInfo);
        currentEpgProgram = v.findViewById(R.id.currentEpgProgram);
        nextEpgProgram = v.findViewById(R.id.nextEpgProgram);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler(requireContext().getMainLooper());
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

    public void show(long displayMillis) {
        show();
        mHandler.postDelayed(hideMyself, displayMillis);
    }

    public void toggleVisibility() {
        if (isHidden()) {
            mHandler.removeCallbacks(hideMyself);
            _show();
        } else {
            _hide();
        }
    }

    public void show() {
        mHandler.removeCallbacks(hideMyself);
        if (!isHidden()) {
            return;
        }
        _show();
    }

    private void _show() {
        getParentFragmentManager().beginTransaction()
                .show(this)
                .commit();
    }

    public void hide() {
        if (isHidden()) {
            return;
        }
        _hide();
    }

    private void _hide() {
        getParentFragmentManager().beginTransaction()
                .hide(this)
                .commit();
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
