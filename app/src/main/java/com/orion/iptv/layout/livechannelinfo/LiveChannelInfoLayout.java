package com.orion.iptv.layout.livechannelinfo;

import android.view.View;
import android.widget.TextView;
import android.os.Looper;
import android.os.Handler;

import com.orion.iptv.R;

import java.util.Locale;

public class LiveChannelInfoLayout {
    protected TextView channelNumber;
    protected TextView channelName;
    protected TextView codecInfo;
    protected TextView mediaInfo;
    protected TextView linkInfo;
    protected TextView currentEpgProgram;
    protected TextView nextEpgProgram;
    protected View myView;
    private final Handler handler;
    private DelayTask delayTask;

    public LiveChannelInfoLayout(View view) {
        myView = view;
        channelNumber = view.findViewById(R.id.channelNumber);
        channelName = view.findViewById(R.id.channelName);
        codecInfo = view.findViewById(R.id.codecInfo);
        mediaInfo = view.findViewById(R.id.mediaInfo);
        linkInfo = view.findViewById(R.id.linkInfo);
        currentEpgProgram = view.findViewById(R.id.currentEpgProgram);
        nextEpgProgram = view.findViewById(R.id.nextEpgProgram);
        handler = new Handler(Looper.getMainLooper());
    }

    public void setChannelName(String name) {
        this.channelName.setText(name);
    }

    public void setChannelNumber(int number) {
        this.channelNumber.setText(String.format(Locale.ENGLISH, "%d", number));
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

    protected class DelayTask implements Runnable {
        private final boolean isVisible;
        private boolean canceled = false;

        public DelayTask(boolean isVisible) {
            this.isVisible = isVisible;
        }

        public void cancel() {
            canceled = true;
        }

        @Override
        public void run() {
            if (!canceled) {
                _setVisibility(isVisible);
            }
        }
    }

    private void _setVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (myView.getVisibility() != visibility) {
            myView.setVisibility(visibility);
        }
    }

    public void setVisibleDelayed(boolean isVisible, int delay) {
        if (delayTask != null) {
            delayTask.cancel();
        }
        if (delay <= 0) {
            _setVisibility(isVisible);
        } else {
            delayTask = new DelayTask(isVisible);
            handler.postDelayed(delayTask, delay);
        }
    }
}
