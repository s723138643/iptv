package com.orion.iptv.recycleradapter;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Selection<T> {
    protected static final String TAG = "Selection";

    protected boolean canRepeatSelect = false;
    protected int oldSelected = RecyclerView.NO_POSITION;
    protected int curSelected = RecyclerView.NO_POSITION;

    private final RecyclerView recyclerView;
    private RecyclerAdapter<T> adapter;
    private final List<OnSelectedListener<T>> listeners;

    public Selection(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.listeners = new ArrayList<>();

        recyclerView.addOnItemTouchListener(new OnItemTouchListener(recyclerView.getContext()));
    }

    public void setAdapter(RecyclerAdapter<T> adapter) {
        setAdapter(adapter, RecyclerView.NO_POSITION);
    }

    public void setAdapter(RecyclerAdapter<T> adapter, int selected) {
        if (this.adapter != null) {
            this.adapter.clearSelection();
        }
        this.adapter = adapter;
        adapter.setSelection(this);
        oldSelected = RecyclerView.NO_POSITION;
        curSelected = isPositionInvalid(selected) ? RecyclerView.NO_POSITION : selected;
    }

    public void select(int position) {
        if (_select(position)) {
            maybeNotifyItemChanged();
            maybeNotifySelected(position);
        }
    }

    public void selectQuiet(int position) {
        if (_select(position)) {
            maybeNotifyItemChanged();
        }
    }

    public void clearSelection() {
        oldSelected = curSelected;
        curSelected = RecyclerView.NO_POSITION;
        maybeNotifyItemChanged();
    }

    public void setCanRepeatSelect(boolean canRepeatSelect) {
        this.canRepeatSelect = canRepeatSelect;
    }

    public boolean isSelected(int position) {
        return position == curSelected && curSelected != RecyclerView.NO_POSITION;
    }

    public void addSelectedListener(OnSelectedListener<T> listener) {
        listeners.add(listener);
    }

    protected void maybeNotifyItemChanged() {
        if (oldSelected != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(oldSelected, TAG);
        }
        if (curSelected != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(curSelected, TAG);
        }
    }

    protected void maybeNotifySelected(int position) {
        T item = adapter.getItem(position);
        listeners.forEach(listener -> listener.onSelected(position, item));
    }

    protected boolean isPositionInvalid(int position) {
        return position < 0 || position >= adapter.getItemCount();
    }

    protected boolean _select(int position) {
        if (isPositionInvalid(position) || (curSelected == position && !canRepeatSelect)) {
            return false;
        }
        oldSelected = curSelected;
        curSelected = position;
        return true;
    }

    public interface OnSelectedListener<T> {
        void onSelected(int position, T item);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            View v = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (v == null) {
                return false;
            }
            RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(v);
            int position = viewHolder.getBindingAdapterPosition();
            if (position < 0 || position >= adapter.getItemCount()) {
                return false;
            }
            select(position);
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

        public OnItemTouchListener(Context context) {
            gestureDetector = new GestureDetectorCompat(context, new GestureListener());
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
