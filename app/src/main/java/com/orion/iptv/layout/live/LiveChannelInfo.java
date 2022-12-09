package com.orion.iptv.layout.live;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.Resources;
import android.os.Bundle;
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

import com.google.android.exoplayer2.C;
import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelInfo;
import com.orion.iptv.bean.EpgProgram;
import com.orion.player.ExtTrack;
import com.orion.player.IExtPlayer;
import com.orion.player.ui.EnhanceConstraintLayout;

import java.util.List;
import java.util.Locale;

public class LiveChannelInfo extends Fragment {
    private final static String TAG = "LiveChannelInfo";
    private final String[] units = {"bps", "kbps", "Mbps", "Gbps"};
    protected LivePlayerViewModel viewModel;
    protected EnhanceConstraintLayout container;
    protected TextView channelNumber;
    protected TextView channelName;
    protected TextView codecInfo;
    protected TextView mediaInfo;
    protected TextView bitrateInfo;
    protected TextView linkInfo;
    protected TextView currentEpgProgram;
    protected TextView nextEpgProgram;
    protected Resources res;

    protected long hideMyselfAt = 0;
    protected final Runnable hideMyself = new Runnable() {
        @Override
        public void run() {
            if (hideMyselfAt <= 0) {
                return;
            }
            long diff = SystemClock.uptimeMillis() - hideMyselfAt;
            if (diff >= 0) {
                hideMyselfAt = 0;
                hide();
            } else {
                container.postDelayed(this, -diff);
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

        container = (EnhanceConstraintLayout) view;
        channelNumber = view.findViewById(R.id.channelNumber);
        channelName = view.findViewById(R.id.channelName);
        codecInfo = view.findViewById(R.id.codecInfo);
        mediaInfo = view.findViewById(R.id.mediaInfo);
        bitrateInfo = view.findViewById(R.id.bitrateInfo);
        linkInfo = view.findViewById(R.id.linkInfo);
        currentEpgProgram = view.findViewById(R.id.currentEpgProgram);
        nextEpgProgram = view.findViewById(R.id.nextEpgProgram);

        res = requireActivity().getResources();
        channelName.setSelected(true);
        currentEpgProgram.setSelected(true);
        nextEpgProgram.setSelected(true);
        viewModel = new ViewModelProvider(requireActivity()).get(LivePlayerViewModel.class);
        viewModel.observeLiveSource(requireActivity(), this::updateChannelInfo);
        viewModel.observeCurrentEpgProgram(requireActivity(), this::setCurrentEpgProgram);
        viewModel.observeNextEpgProgram(requireActivity(), this::setNextEpgProgram);
        container.addEventListener(new EnhanceConstraintLayout.EventListener() {
            @Override
            public void onVisibilityChanged(@NonNull View changedView, int visibility) {
                if (changedView != container) {
                    return;
                }
                container.removeCallbacks(hideMyself);
                if (visibility == View.VISIBLE && hideMyselfAt > 0) {
                    container.postDelayed(hideMyself, Math.max(hideMyselfAt-SystemClock.uptimeMillis(), 1));
                }
            }
        });
    }

    protected void updateChannelInfo(Pair<Integer, DataSource> dataSource) {
        ChannelInfo info = dataSource.second.channelInfo;
        assert info != null;
        setChannelNumber(info.channelNumber);
        setChannelName(info.channelName);
        setCodecInfo(res.getString(R.string.codec_info_default));
        setMediaInfo(res.getString(R.string.media_info_default));
        setBitrateInfo(0);
        setLinkInfo(dataSource.first, viewModel.getSourceCount());
    }

    protected void setChannelName(String name) {
        this.channelName.setText(name);
    }

    protected void setChannelNumber(int number) {
        this.channelNumber.setText(String.format(Locale.ENGLISH, "%d", number));
    }

    protected String formatBitrate(long bitrate) {
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

    protected void setBitrateInfo(long bitrate) {
        if (bitrate <= 0) {
            this.bitrateInfo.setVisibility(View.GONE);
            return;
        }
        String formattedBitrate = formatBitrate(bitrate);
        this.bitrateInfo.setText(formattedBitrate);
        this.bitrateInfo.setVisibility(View.VISIBLE);
    }

    protected void setCodecInfo(String info) {
        this.codecInfo.setText(info);
    }

    protected void setMediaInfo(String info) {
        this.mediaInfo.setText(info);
    }

    protected void setLinkInfo(int currentIndex, int totalLinks) {
        this.linkInfo.setText(String.format(Locale.ENGLISH, "线路[%d/%d]", currentIndex+1, totalLinks));
    }

    protected void setCurrentEpgProgram(Pair<Integer, Pair<ChannelInfo, EpgProgram>> program) {
        if (program != null) {
            currentEpgProgram.setText(program.second.second.content());
        } else {
            currentEpgProgram.setText(res.getString(R.string.current_epg_program_default));
        }
    }

    protected void setNextEpgProgram(Pair<Integer, Pair<ChannelInfo, EpgProgram>> program) {
        if (program != null) {
            nextEpgProgram.setText(program.second.second.content());
        } else {
            nextEpgProgram.setText(res.getString(R.string.next_epg_program_default));
        }
    }

    public void setPlayer(IExtPlayer player) {
        player.addListener(listener);
    }

    public boolean isViewHidden() {
        return container.getVisibility() == View.GONE;
    }

    @SuppressWarnings("unused")
    public void toggleVisibility() {
        if (isViewHidden()) {
            show();
        } else {
            hide();
        }
    }

    public void show(long displayMillis) {
        if (!isViewHidden() && hideMyselfAt <= 0) {
            // we have showed it without auth hide, so ignore this operation
            return;
        }
        hideMyselfAt = SystemClock.uptimeMillis() + displayMillis;
        _show();
    }

    public void show() {
        hideMyselfAt = 0;
        _show();
    }

    protected void _show() {
        container.setAlpha(0);
        container.setVisibility(View.VISIBLE);
        container.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        container.setAlpha(1f);
                    }
                });
    }

    public void hide(long delayMillis) {
        if (isViewHidden()) {
            return;
        }
        container.removeCallbacks(hideMyself);
        hideMyselfAt = SystemClock.uptimeMillis() + delayMillis;
        container.postDelayed(hideMyself, delayMillis);
    }

    public void hide() {
        container.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        container.setVisibility(View.GONE);
                    }
                });
    }

    private class PlayerEventListener implements IExtPlayer.Listener {
        @Override
        public void onTracksChanged(List<ExtTrack> tracks) {
            for (ExtTrack track: tracks) {
                if (track.trackType == C.TRACK_TYPE_VIDEO && track.selected) {
                    if (track.format.width > 0 && track.format.height > 0) {
                        setMediaInfo(track.format.width + "x" + track.format.height);
                    }
                    setCodecInfo(track.format.codecs);
                    setBitrateInfo(track.format.bitrate);
                    break;
                }
            }
        }
    }
}
