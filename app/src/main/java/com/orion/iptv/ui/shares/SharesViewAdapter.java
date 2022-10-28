package com.orion.iptv.ui.shares;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;

import java.util.ArrayList;
import java.util.List;

public class SharesViewAdapter extends RecyclerView.Adapter<SharesViewHolder> {
    private final Context context;
    private SelectionTracker<Long> tracker;
    private List<Share> shares;
    private View.OnClickListener buttonClickListener;
    private View.OnClickListener itemClickListener;

    public SharesViewAdapter(Context context, List<Share> shares) {
        this.context = context;
        this.shares = shares != null ? shares : new ArrayList<>();
    }

    @NonNull
    @Override
    public SharesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.layout_list_item_with_button, parent, false);
        return new SharesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SharesViewHolder holder, int position) {
        Log.i("Adapter", "binding view " + position);
        Share share = shares.get(position);
        holder.setContent(share.getRoot());
        if (tracker != null) {
            holder.setSelected(tracker.isSelected((long) position));
        }
        if (buttonClickListener != null) {
            holder.setOnButtonClickListener(buttonClickListener);
        }
        if (itemClickListener != null) {
            holder.setOnItemClickListener(itemClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return shares.size();
    }

    public void setButtonClickListener(View.OnClickListener listener) {
        buttonClickListener = listener;
    }

    public void setItemClickListener(View.OnClickListener listener) {
        itemClickListener = listener;
    }

    public void setShares(List<Share> shares) {
        assert shares != null;
        this.shares = shares;
    }

    public void setTracker(SelectionTracker<Long> tracker) {
        this.tracker = tracker;
    }

    public static class KeyProvider extends ItemKeyProvider<Long> {

        /**
         * Creates a new provider with the given scope.
         *
         * @param scope Scope can't be changed at runtime.
         */
        protected KeyProvider(int scope) {
            super(scope);
        }

        @Nullable
        @Override
        public Long getKey(int position) {
            if (position < 0) {
                return null;
            }
            return (long) position;
        }

        @Override
        public int getPosition(@NonNull Long key) {
            return key.intValue();
        }
    }

    public static class DetailsLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView view;

        public DetailsLookup(RecyclerView view) {
            this.view = view;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View v = view.findChildViewUnder(e.getX(), e.getY());
            if (v == null) {
                return null;
            }
            SharesViewHolder viewHolder = (SharesViewHolder) view.getChildViewHolder(v);
            if (viewHolder == null) {
                return null;
            }
            return viewHolder.getItemDetails();
        }
    }
}
