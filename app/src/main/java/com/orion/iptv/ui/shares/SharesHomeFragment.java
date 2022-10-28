package com.orion.iptv.ui.shares;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.util.Log;

import com.orion.iptv.R;
import com.orion.iptv.layout.dialog.WebDavSettingDialog;

import java.util.ArrayList;
import java.util.List;

public class SharesHomeFragment extends Fragment {
    private SharesViewModel mViewModel;
    private RecyclerView sharesView;
    private SharesViewAdapter adapter;
    private Handler mHandler;

    public SharesHomeFragment() {
        super(R.layout.fragment_shares_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(SharesViewModel.class);
        mHandler = new Handler(requireContext().getMainLooper());

        ImageButton addShareButton = view.findViewById(R.id.add_share);
        addShareButton.setOnClickListener((addRootButton) -> {
            WebDavSettingDialog dialog =  new WebDavSettingDialog();
            dialog.setOnSubmitListener(share -> {
                mViewModel.addShare(share);
                Log.i("SharesHomeFragment", "add share: " + share.getRoot().getName());
            });
            dialog.show(getChildFragmentManager(), "webdav");
        });

        sharesView = view.findViewById(R.id.shares_body);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        sharesView.setLayoutManager(layoutManager);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        sharesView.addItemDecoration(itemDecoration);
        adapter = new SharesViewAdapter(requireContext(), mViewModel.getShares().getValue());
        mViewModel.getShares().observe(getViewLifecycleOwner(), shares -> {
            Log.i("SharesHomeFragment", "shares changed, size: " + shares.size());
            adapter.setShares(shares);
            mHandler.post(adapter::notifyDataSetChanged);
        });
        adapter.setItemClickListener((itemView) -> {
            Log.i("ShareHomeFragment",  "clicking...");
            RecyclerView.ViewHolder viewHolder = sharesView.findContainingViewHolder(itemView);
            if (viewHolder == null) {
                return;
            }
            int position = viewHolder.getBindingAdapterPosition();
            if (position < 0) {
                return;
            }
            mViewModel.setSelectedShare(position);
            Share share = mViewModel.getSelectedShare().getValue();
            if (share == null) {
                return;
            }
            Log.i("SharesHomeFragment", "share " + share.getRoot().getName() + " selected");
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.shares_container_view, SharesContentFragment.class, null)
                    .addToBackStack("SharesHomeFragment")
                    .commit();
        });
        sharesView.setAdapter(adapter);
        SelectionTracker<Long> tracker = new SelectionTracker.Builder<>(
                "shares_selection",
                sharesView,
                new SharesViewAdapter.KeyProvider(ItemKeyProvider.SCOPE_MAPPED),
                new SharesViewAdapter.DetailsLookup(sharesView),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything()).build();
        adapter.setTracker(tracker);

        View selectionAction = view.findViewById(R.id.selection_actions);
        selectionAction.setVisibility(View.GONE);
        ImageButton cancelButton = selectionAction.findViewById(R.id.delete_cancel);
        cancelButton.setOnClickListener(cancelView -> tracker.clearSelection());
        ImageButton confirmButton = selectionAction.findViewById(R.id.delete_confirm);
        confirmButton.setOnClickListener(confirmView -> {
            List<Share> newShares = new ArrayList<>();
            List<Share> oldShares = mViewModel.getShares().getValue();
            assert oldShares != null;
            for (int i=0; i<oldShares.size(); i++) {
                if (tracker.isSelected((long) i)) {
                    continue;
                }
                newShares.add(oldShares.get(i));
            }
            mViewModel.getShares().setValue(newShares);
            tracker.clearSelection();
        });
        tracker.addObserver(new SelectionTracker.SelectionObserver<>() {
            @Override
            public void onSelectionChanged() {
                int visibility = tracker.hasSelection() ? View.VISIBLE : View.GONE;
                selectionAction.setVisibility(visibility);
            }
        });
    }
}