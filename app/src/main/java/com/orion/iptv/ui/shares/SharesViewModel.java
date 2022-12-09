package com.orion.iptv.ui.shares;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SharesViewModel extends ViewModel {
    private final MutableLiveData<List<Share>> shares;
    private final MutableLiveData<Share> selectedShare;

    public SharesViewModel() {
        shares = new MutableLiveData<>();
        shares.setValue(new ArrayList<>());
        selectedShare = new MutableLiveData<>();
    }

    public MutableLiveData<List<Share>> getShares() {
        return shares;
    }

    @Nullable
    public Share getSelectedShare() {
        return selectedShare.getValue();
    }

    public void setSelectedShare(int position) {
        Share share = Objects.requireNonNull(shares.getValue()).get(position);
        if (share != null) {
            selectedShare.setValue(share);
        }
    }

    @Nullable
    public Share getShare(int position) {
        List<Share> mShares = shares.getValue();
        if (mShares == null || position < 0 || position >= mShares.size()) {
            return null;
        }
        return mShares.get(position);
    }

    public void addShare(Share share) {
        List<Share> temp = shares.getValue();
        assert temp != null;
        temp.add(share);
        shares.setValue(temp);
    }

    public void setShare(int position, Share share) {
        List<Share> mShares = shares.getValue();
        assert mShares != null;
        if (position < 0 || position >= mShares.size()) {
            return;
        }
        mShares.set(position, share);
        shares.setValue(mShares);
    }
}