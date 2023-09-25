package com.orion.iptv.recycleradapter;

import android.content.Context;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DefaultSelection<T> implements Selection<T> {
    protected static final String TAG = "Selection";

    protected boolean canRepeatSelect = false;
    protected int oldSelected = RecyclerView.NO_POSITION;
    protected int curSelected = RecyclerView.NO_POSITION;

    protected final RecyclerView recyclerView;
    protected RecyclerAdapter<T> adapter;
    protected final List<OnSelectedListener<T>> listeners;
    protected final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            clearSelection();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            super.onItemRangeChanged(positionStart, itemCount, payload);
            if (TAG != payload) {
                clearSelection();
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            clearSelection();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            clearSelection();
        }
    };

    public DefaultSelection(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.listeners = new ArrayList<>();

        addOnItemTouchListener();
        addOnKeyListener();
    }

    protected void addOnItemTouchListener() {
        recyclerView.addOnItemTouchListener(new OnItemTouchListener(recyclerView.getContext()));
    }

    protected void addOnKeyListener() {
        recyclerView.setOnKeyListener(new OnKeyListener());
        recyclerView.setOnFocusChangeListener((v, hasFocus) -> {
            if (v != recyclerView || adapter == null) {
                return;
            }
            if (hasFocus) {
                int pos = curSelected != RecyclerView.NO_POSITION ? curSelected : 0;
                adapter.notifyItemChanged(pos, TAG);
                recyclerView.scrollToPosition(pos);
            } else {
                if (curSelected != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(curSelected);
                }
            }
        });
    }

    @Override
    public void setAdapter(RecyclerAdapter<T> adapter) {
        setAdapter(adapter, RecyclerView.NO_POSITION);
    }

    @Override
    public void setAdapter(RecyclerAdapter<T> adapter, int selected) {
        if (this.adapter != null) {
            this.adapter.clearSelection();
            this.adapter.unregisterAdapterDataObserver(observer);
        }
        this.adapter = adapter;
        adapter.setSelection(this);
        oldSelected = RecyclerView.NO_POSITION;
        curSelected = isPositionInvalid(selected) ? RecyclerView.NO_POSITION : selected;
        adapter.registerAdapterDataObserver(observer);
    }

    @Override
    public boolean hasSelectedItem() {
        return curSelected != RecyclerView.NO_POSITION;
    }

    @Override
    public boolean hasFocusedItem() {
        return hasSelectedItem();
    }

    @Override
    public void select(int position) {
        if (_select(position)) {
            maybeNotifySelectChanged();
            maybeNotifySelected(position);
        }
    }

    @Override
    public void selectQuiet(int position) {
        if (_select(position)) {
            maybeNotifySelectChanged();
        }
    }

    protected int[] toIntArray(List<Integer> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i<list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    @Override
    public int[] getState(int position) {
        List<Integer> states = new ArrayList<>();
        if (position==curSelected && curSelected!=RecyclerView.NO_POSITION) {
            states.add(android.R.attr.state_activated);
            if (recyclerView.hasFocus()) {
                states.add(android.R.attr.state_focused);
            }
        }
        return toIntArray(states);
    }

    @Override
    public void clearSelection() {
        oldSelected = curSelected;
        curSelected = RecyclerView.NO_POSITION;
        maybeNotifySelectChanged();
    }

    @Override
    public void setCanRepeatSelect(boolean canRepeatSelect) {
        this.canRepeatSelect = canRepeatSelect;
    }

    public boolean isSelected(int position) {
        return position == curSelected && curSelected != RecyclerView.NO_POSITION;
    }

    @Override
    public void addSelectedListener(OnSelectedListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeSelectedListener(OnSelectedListener<T> listener) {
        listeners.remove(listener);
    }

    protected void maybeNotifySelectChanged() {
        if (oldSelected != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(oldSelected, TAG);
        }
        if (curSelected != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(curSelected, TAG);
        }
    }

    protected void maybeNotifySelected(int position) {
        T item = adapter.getItem(position);
        for (OnSelectedListener<T> listener: listeners) {
            listener.onSelected(position, item);
        }
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

    public class OnKeyListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            Log.w(TAG, "handling key event");
            if (event.getAction() != KeyEvent.ACTION_DOWN || adapter == null) {
                return false;
            }
            boolean handled = true;
            int pos = RecyclerView.NO_POSITION;
            switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    pos = curSelected - 1;
                    pos = pos >= 0 ? pos : adapter.getItemCount() - 1;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    pos = curSelected + 1;
                    pos = pos < adapter.getItemCount() ? pos : 0;
                    break;
                default:
                    handled = false;
            }
            if (pos != RecyclerView.NO_POSITION) {
                recyclerView.scrollToPosition(pos);
                select(pos);
            }
            return handled;
        }
    }
}
