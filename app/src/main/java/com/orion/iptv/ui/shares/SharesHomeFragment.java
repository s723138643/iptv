package com.orion.iptv.ui.shares;

import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
//import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.util.Log;

import com.orion.iptv.R;
import com.orion.iptv.layout.dialog.WebDavSettingDialog;

import java.util.ArrayList;
import java.util.List;

public class SharesHomeFragment extends Fragment {
    public static final String TAG = "SharesHome";

    private SharesViewModel mViewModel;
    private RecyclerView sharesView;
    private SharesViewAdapter adapter;

    protected ImageButton addShareButton;
    protected View selectionAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shares_home, container, false);
    }

    @SuppressLint({"NotifyDataSetChanged", "NonConstantResourceId"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addShareButton = view.findViewById(R.id.add_share);
        sharesView = view.findViewById(R.id.shares_body);
        selectionAction = view.findViewById(R.id.selection_actions);

        mViewModel = new ViewModelProvider(requireActivity()).get(SharesViewModel.class);
        addShareButton.setOnClickListener((addRootButton) -> {
            WebDavSettingDialog dialog =  new WebDavSettingDialog(requireContext());
            dialog.setOnSubmitListener(share -> {
                mViewModel.addShare(share);
                Log.i("SharesHomeFragment", "add share: " + share.getRoot().getName());
            });
            dialog.show();
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        sharesView.setLayoutManager(layoutManager);
        /*
        DividerItemDecoration itemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        sharesView.addItemDecoration(itemDecoration);
        */
        adapter = new SharesViewAdapter(requireContext(), mViewModel.getShares().getValue());
        mViewModel.getShares().observe(getViewLifecycleOwner(), shares -> {
            Log.i("SharesHomeFragment", "shares changed, size: " + shares.size());
            adapter.setShares(shares);
            adapter.notifyDataSetChanged();
        });
        adapter.setItemClickListener(itemView -> {
            int position = getBindingAdapterPosition(itemView);
            if (position >= 0) {
                mViewModel.setSelectedShare(position);
                Share share = mViewModel.getSelectedShare();
                if (share == null) {
                    return;
                }
                FileNode root = share.getRoot();
                Log.i("SharesHomeFragment", "share " + root.getName() + " selected");
                Bundle bundle = new Bundle();
                bundle.putSerializable("path", root);
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .addToBackStack(TAG)
                        .replace(R.id.shares_container_view, SharesContentFragment.class, bundle)
                        .commit();
            }
        });
        adapter.setButtonClickListener(buttonView -> {
            int position = getBindingAdapterPosition(buttonView);
            if (position < 0) {
                return;
            }
            Share share = mViewModel.getShare(position);
            if (share == null) {
                return;
            }

            PopupMenu popupMenu = new PopupMenu(requireContext(), buttonView);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.modify:
                        new WebDavSettingDialog(requireContext())
                                .setDefaultValue(share)
                                .setOnSubmitListener(modified -> mViewModel.setShare(position, modified))
                                .show();
                        break;
                    case R.id.delete:
                        List<Share> newShares = new ArrayList<>();
                        List<Share> oldShares = mViewModel.getShares().getValue();
                        assert oldShares != null;
                        for (Share s : oldShares) {
                            if (s != share) {
                                newShares.add(s);
                            }
                        }
                        mViewModel.getShares().setValue(newShares);
                        break;
                }
                return true;
            });
            popupMenu.inflate(R.menu.share_action);
            popupMenu.show();
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

    private int getBindingAdapterPosition(View view) {
        RecyclerView.ViewHolder viewHolder = sharesView.findContainingViewHolder(view);
        if (viewHolder != null) {
            return viewHolder.getBindingAdapterPosition();
        }
        return -1;
    }
}