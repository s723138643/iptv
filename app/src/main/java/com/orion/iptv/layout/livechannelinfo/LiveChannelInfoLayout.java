package com.orion.iptv.layout.livechannelinfo;

import android.view.View;
import android.widget.TextView;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.orion.iptv.R;
import com.orion.iptv.misc.CancelableRunnable;

import java.util.Locale;

public class LiveChannelInfoLayout {
    protected TextView channelNumber;
    protected TextView channelName;
    protected TextView codecInfo;
    protected TextView mediaInfo;
    protected TextView bitrateInfo;
    protected TextView linkInfo;
    protected TextView currentEpgProgram;
    protected TextView nextEpgProgram;
    protected View mLayout;
    private final String[] units = {"bps", "kbps", "Mbps", "Gbps"};
    private final Handler mHandler;
    private CancelableRunnable setVisibilityDelayedTask;

    public LiveChannelInfoLayout(AppCompatActivity activity) {
        mLayout = activity.findViewById(R.id.channelInfoLayout);
        channelNumber = mLayout.findViewById(R.id.channelNumber);
        channelName = mLayout.findViewById(R.id.channelName);
        channelName.setSelected(true);
        codecInfo = mLayout.findViewById(R.id.codecInfo);
        mediaInfo = mLayout.findViewById(R.id.mediaInfo);
        bitrateInfo = mLayout.findViewById(R.id.bitrateInfo);
        linkInfo = mLayout.findViewById(R.id.linkInfo);
        currentEpgProgram = mLayout.findViewById(R.id.currentEpgProgram);
        currentEpgProgram.setSelected(true);
        nextEpgProgram = mLayout.findViewById(R.id.nextEpgProgram);
        nextEpgProgram.setSelected(true);
        mHandler = new Handler(activity.getMainLooper());
    }

    public void setChannelName(String name) {
        this.channelName.setText(name);
    }

    public void setChannelNumber(int number) {
        this.channelNumber.setText(String.format(Locale.ENGLISH, "%d", number));
    }

    private String formatBitrate(int bitrate) {
        float base = 1000.0f;
        float rate = (float) bitrate;
        int i = 0;
        for (; i<units.length; i++) {
            if (rate < base) {
                break;
            }
            rate /= base;
        }
        return String.format(Locale.ENGLISH, "%.2f%s", rate, units[i]);
    }

    public void setBitrateInfo(int bitrate) {
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
        this.linkInfo.setText(String.format(Locale.ENGLISH, "[%d/%d]", currentIndex, totalLinks));
    }

    public void setCurrentEpgProgram(String program) {
        this.currentEpgProgram.setText(program);
    }

    public void setNextEpgProgram(String program) {
        this.nextEpgProgram.setText(program);
    }

    private void _setVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (mLayout.getVisibility() != visibility) {
            mLayout.setVisibility(visibility);
        }
    }

    public void setVisibleDelayed(boolean isVisible, int delayMillis) {
        if (setVisibilityDelayedTask != null) {
            setVisibilityDelayedTask.cancel();
        }
        setVisibilityDelayedTask = new CancelableRunnable() {
            @Override
            public void callback() {
                _setVisibility(isVisible);
            }
        };
        delayMillis = Math.max(delayMillis, 1);
        mHandler.postDelayed(setVisibilityDelayedTask, delayMillis);
    }

    private boolean isVisible() {
        return mLayout.getVisibility() == View.VISIBLE;
    }

    // do nothing if is already visible,
    // or display it and hide after displayMillis milliseconds
    public void displayAsToast(int displayMillis) {
        mHandler.post(()->{
            if (isVisible()) { return; }
            if (setVisibilityDelayedTask != null) {
                setVisibilityDelayedTask.cancel();
            }
            setVisibilityDelayedTask = new CancelableRunnable() {
                @Override
                public void callback() {
                    _setVisibility(true);
                    setVisibleDelayed(false, displayMillis);
                }
            };
            mHandler.post(setVisibilityDelayedTask);
        });
    }

    public void hide() {
        mHandler.post(()->{
            if (!isVisible()) { return; }
            if (setVisibilityDelayedTask != null) {
                setVisibilityDelayedTask.cancel();
            }
            setVisibilityDelayedTask = new CancelableRunnable() {
                @Override
                public void callback() {
                    _setVisibility(false);
                }
            };
            mHandler.post(setVisibilityDelayedTask);
        });
    }
}
