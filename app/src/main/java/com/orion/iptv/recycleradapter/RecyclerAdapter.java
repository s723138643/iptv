package com.orion.iptv.recycleradapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;
import java.util.Locale;

public class RecyclerAdapter<T extends ViewHolder<U>, U> extends RecyclerView.Adapter<T> {
    private final ViewHolderFactory<T, U> factory;

    private boolean repeatClickEnabled = false;
    private OnSelectedListener<U> listener;
    private int lastSelected = RecyclerView.NO_POSITION;
    private int currentSelected = RecyclerView.NO_POSITION;
    private List<U> items;

    public RecyclerAdapter(Context context, List<U> items, ViewHolderFactory<T, U> factory) {
        this.factory = factory;
        this.items = items;
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

    private void changeState(@NonNull RecyclerView.ViewHolder holder, int position) {
        onBindViewHolder((T) holder, position);
    }

    private void onSelected(@NonNull RecyclerView.ViewHolder holder, int position) {
        Log.i("Adapter", String.format(Locale.ENGLISH, "position: %d, selected: %d", position, currentSelected));
        if (position < 0 || (!repeatClickEnabled && position == currentSelected)) {
            return;
        }
        if (position != currentSelected) {
            lastSelected = currentSelected;
            currentSelected = position;
            changeState(holder, position);
            if (lastSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(lastSelected);
            }
        }
        if (listener != null) {
            listener.onSelected(position, items.get(position));
        }
    }

    public void setOnSelectedListener(OnSelectedListener<U> listener) {
        this.listener = listener;
    }

    private void _selectQuiet(int position) {
        lastSelected = currentSelected;
        currentSelected = position;
        if (lastSelected != RecyclerView.NO_POSITION) {
            notifyItemChanged(lastSelected);
        }
        notifyItemChanged(currentSelected);
    }

    public void select(int position) {
        if (position < 0 || position >= items.size() || position == currentSelected) {
            return;
        }
        _selectQuiet(position);
        if (listener != null) {
            listener.onSelected(position, items.get(position));
        }
    }

    public void selectQuiet(int position) {
        Log.i("Adapter", String.format(Locale.ENGLISH, "position: %d, selected: %d", position, currentSelected));
        if (position < 0 || position >= items.size() || position == currentSelected) {
            return;
        }

        _selectQuiet(position);
    }

    public void setRepeatClickEnabled(boolean isEnable) {
        repeatClickEnabled = isEnable;
    }

    public void deselect(int position) {
        if (position < 0 || position >= items.size() || position != currentSelected) {
            return;
        }
        currentSelected = RecyclerView.NO_POSITION;
        lastSelected = RecyclerView.NO_POSITION;
        notifyItemChanged(position);
    }

    private void _clearSelection() {
        lastSelected = RecyclerView.NO_POSITION;
        currentSelected = RecyclerView.NO_POSITION;
    }

    public void clearSelection() {
        int selected = currentSelected;
        _clearSelection();
        if (selected != RecyclerView.NO_POSITION) {
            notifyItemChanged(selected);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<U> items) {
        _clearSelection();
        this.items = items;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void resume(List<U> items, int position) {
        lastSelected = currentSelected;
        currentSelected = position;
        this.items = items;
        notifyDataSetChanged();
    }

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
            if (v == null) {
                return false;
            }
            RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(v);
            onSelected(viewHolder, viewHolder.getBindingAdapterPosition());
            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            // translate double tap as single tap
            return e.getActionMasked() == MotionEvent.ACTION_UP && onSingleTapUp(e);
        }
    }

    public class OnItemTouchListener implements RecyclerView.OnItemTouchListener {
        private final GestureDetectorCompat gestureDetector;
        private boolean mDisallowIntercept = false;

        public OnItemTouchListener(Context context, RecyclerView rv) {
            gestureDetector = new GestureDetectorCompat(context, new GestureListener(rv));
        }

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            if (mDisallowIntercept && e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mDisallowIntercept = false;
            }
            return !mDisallowIntercept && gestureDetector.onTouchEvent(e);
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            if (!mDisallowIntercept) {
                gestureDetector.onTouchEvent(e);
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (!disallowIntercept) {
                return;
            }
            mDisallowIntercept = true;
            gestureDetector.onTouchEvent(MotionEvent.obtain(
                    0,
                    1,
                    MotionEvent.ACTION_CANCEL,
                    0,
                    0,
                    0
            ));
        }
    }
}