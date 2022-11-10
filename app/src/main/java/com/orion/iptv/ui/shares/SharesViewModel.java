package com.orion.iptv.ui.shares;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public MutableLiveData<Share> getSelectedShare() {
        return selectedShare;
    }

    public void setSelectedShare(int position) {
        Share share = Objects.requireNonNull(shares.getValue()).get(position);
        if (share != null) {
            selectedShare.setValue(share);
        }
    }

    public Optional<Share> getShare(int position) {
        List<Share> mShares = shares.getValue();
        if (mShares == null || position < 0 || position >= mShares.size()) {
            return Optional.empty();
        }
        return Optional.of(mShares.get(position));
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