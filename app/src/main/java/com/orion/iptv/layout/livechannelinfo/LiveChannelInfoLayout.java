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
    protected TextView linkInfo;
    protected TextView currentEpgProgram;
    protected TextView nextEpgProgram;
    protected View mLayout;
    private final Handler mHandler;
    private CancelableRunnable setVisibilityDelayedTask;

    public LiveChannelInfoLayout(AppCompatActivity activity) {
        mLayout = activity.findViewById(R.id.channelInfoLayout);
        channelNumber = mLayout.findViewById(R.id.channelNumber);
        channelName = mLayout.findViewById(R.id.channelName);
        codecInfo = mLayout.findViewById(R.id.codecInfo);
        mediaInfo = mLayout.findViewById(R.id.mediaInfo);
        linkInfo = mLayout.findViewById(R.id.linkInfo);
        currentEpgProgram = mLayout.findViewById(R.id.currentEpgProgram);
        nextEpgProgram = mLayout.findViewById(R.id.nextEpgProgram);
        mHandler = new Handler(activity.getMainLooper());
    }

    public void setChannelName(String name) {
        mHandler.post(()->{
            this.channelName.setText(name);
        });
    }

    public void setChannelNumber(int number) {
        mHandler.post(()->{
            this.channelNumber.setText(String.format(Locale.ENGLISH, "%d", number));
        });
    }

    public void setCodecInfo(String info) {
        mHandler.post(()->{
            this.codecInfo.setText(info);
        });
    }

    public void setMediaInfo(String info) {
        mHandler.post(()->{
            this.mediaInfo.setText(info);
        });
    }

    public void setLinkInfo(int currentIndex, int totalLinks) {
        mHandler.post(()->{
            this.linkInfo.setText(String.format(Locale.ENGLISH, "[%d/%d]", currentIndex, totalLinks));
        });
    }

    public void setCurrentEpgProgram(String program) {
        mHandler.post(()->{
            this.currentEpgProgram.setText(program);
        });
    }

    public void setNextEpgProgram(String program) {
        mHandler.post(()->{
            this.nextEpgProgram.setText(program);
        });
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
}
