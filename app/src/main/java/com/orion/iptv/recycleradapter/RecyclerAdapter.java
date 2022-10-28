package com.orion.iptv.recycleradapter;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;

import java.util.List;
import java.util.Locale;

public class RecyclerAdapter<T extends ViewHolder<U>, U extends ListItem> extends RecyclerView.Adapter<T> {
    private final ViewHolderFactory<T, U> factory;
    private final Handler mHandler;

    private OnSelectedListener<U> listener;
    private int lastSelected = RecyclerView.NO_POSITION;
    private int currentSelected = RecyclerView.NO_POSITION;
    private List<U> items;

    public interface OnSelectedListener<K> {
        void onSelected(int position, K item);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final RecyclerView recyclerView;

        public GestureListener(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            View v = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (v == null) { return false; }
            RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(v);
            onSelected(viewHolder.getBindingAdapterPosition());
            return true;
        }
    }

    public class OnItemTouchListener implements RecyclerView.OnItemTouchListener {
        private final GestureDetectorCompat gestureDetector;

        public OnItemTouchListener(Context context, RecyclerView rv) {
            gestureDetector = new GestureDetectorCompat(context, new GestureListener(rv));
        }

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            View v = rv.findChildViewUnder(e.getX(), e.getY());
            if (v == null) { return false; }
            gestureDetector.onTouchEvent(e);
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
    }

    public RecyclerAdapter(Context context, List<U> items, ViewHolderFactory<T, U> factory) {
        this.factory = factory;
        this.items = items;
        this.mHandler = new Handler(context.getMainLooper());
    }

    @NonNull
    @Override
    public T onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return factory.create(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull T holder, int position) {
        holder.setActivated(position == currentSelected);
        holder.setContent(position, items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void onSelected(int position) {
        Log.i("Adapter", String.format(Locale.ENGLISH, "position: %d, selected: %d", position, currentSelected));
        if (position == RecyclerView.NO_POSITION || position==currentSelected) { return; }
        lastSelected = currentSelected;
        notifyItemChanged(lastSelected);
        currentSelected = position;
        notifyItemChanged(currentSelected);
        if (position < items.size() && listener != null) {
            listener.onSelected(position, items.get(position));
        }
    }

    public void setOnSelectedListener(OnSelectedListener<U> listener) {
        this.listener = listener;
    }

    private void _clearSelection() {
        lastSelected = RecyclerView.NO_POSITION;
        currentSelected = RecyclerView.NO_POSITION;
    }

    public void clearSelection() {
        mHandler.post(()-> {
            int selected = currentSelected;
            _clearSelection();
            if (selected != RecyclerView.NO_POSITION) { notifyItemChanged(selected); }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<U> items) {
        mHandler.post(()->{
            _clearSelection();
            this.items = items;
            notifyDataSetChanged();
        });
    }
}