package com.orion.iptv.layout.livechannellist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;
import com.orion.iptv.bean.EpgProgram;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EpgListViewAdapter extends RecyclerView.Adapter<EpgListViewAdapter.ViewHolder> {
    private final String TAG = "EpgListViewAdapter";
    private final Context context;
    private List<EpgProgram> epgs;
    private SelectionTracker<Long> tracker;
    private OnSelectedListener onSelected;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public interface OnSelectedListener {
        void onSelected(int position);
    }

    public class KeyProvider extends ItemKeyProvider<Long> {
        public KeyProvider(int scope){
            super(scope);
        }

        @Override
        public Long getKey (int position) {
            return (long)position;
        }

        @Override
        public int getPosition(@NonNull Long key) {
            return key.intValue();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View item;
        private final TextView index;
        private final TextView desc;

        public ViewHolder(View v) {
            super(v);
            item = v.findViewById(R.id.live_channel_list_item);
            index = v.findViewById(R.id.list_item_index);
            desc = v.findViewById(R.id.list_item_desc);
        }

        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            int position = getBindingAdapterPosition();
            return new ItemDetailsLookup.ItemDetails<Long>() {
                @Override
                public int getPosition() {
                    return position;
                }

                @Override
                public Long getSelectionKey() {
                    return (long)position;
                }

                @Override
                public boolean inSelectionHotspot(@NonNull MotionEvent e) {
                    return true;
                }
            };
        }

        public void setActivated(boolean isActivated) {
            item.setActivated(isActivated);
        }

        public void setContent(final int position, EpgProgram epg) {
            Instant start = Instant.ofEpochSecond(epg.start);
            Instant end = Instant.ofEpochSecond(epg.end);
            index.setText(String.format("%s-%s", Date.from(start), Date.from(end)));
            desc.setText(epg.program);
        }
    }

    public class ItemLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView view;
        public ItemLookup(RecyclerView view) {
            this.view = view;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View v = view.findChildViewUnder(e.getX(), e.getY());
            if (v != null) {
                Log.i(TAG, String.format("got item at (%.2f,%.2f)", e.getX(), e.getY()));
                ViewHolder holder = (ViewHolder) view.getChildViewHolder(v);
                return holder.getItemDetails();
            }
            return null;
        }
    }

    public EpgListViewAdapter(Context context, List<EpgProgram> epgs) {
        this.context = context;
        this.epgs = epgs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.live_channel_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.i(TAG, String.format(Locale.ENGLISH, "item %d bound", position));
        boolean selected = tracker.isSelected((long)position);
        holder.setActivated(selected);
        holder.setContent(position, epgs.get(position));

        if (selected && onSelected != null) {
            onSelected.onSelected(position);
        }
    }

    @Override
    public int getItemCount() {
        return epgs.size();
    }

    public void setTracker(SelectionTracker<Long> tracker) {
        this.tracker = tracker;
    }

    public void setOnSelectedListener(OnSelectedListener listener) {
        onSelected = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<EpgProgram> epgs) {
        this.epgs = epgs;
        tracker.clearSelection();
        // should invoke it on main ui thread
        mHandler.post(this::notifyDataSetChanged);
    }
}
