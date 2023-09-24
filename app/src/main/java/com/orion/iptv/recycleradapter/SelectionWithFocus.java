package com.orion.iptv.recycleradapter;

import android.view.KeyEvent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SelectionWithFocus<T> extends DefaultSelection<T> {
    protected int lastFocused = RecyclerView.NO_POSITION;
    protected int curFocused = RecyclerView.NO_POSITION;

    public SelectionWithFocus(RecyclerView recyclerView) {
        super(recyclerView);
    }

    @Override
    protected void addOnKeyListener() {
        recyclerView.setOnFocusChangeListener(new OnFocusListener());
        recyclerView.setOnKeyListener(new OnKeyListener());
    }

    @Override
    public void setAdapter(RecyclerAdapter<T> adapter, int selected) {
        super.setAdapter(adapter, selected);
        lastFocused = RecyclerView.NO_POSITION;
        curFocused = isPositionInvalid(selected) ? RecyclerView.NO_POSITION : selected;
    }

    @Override
    public int[] getState(int position) {
        List<Integer> states = new ArrayList<>();
        if (position == curSelected && curSelected != RecyclerView.NO_POSITION) {
            states.add(android.R.attr.state_activated);
        }
        if (recyclerView.hasFocus() && position == curFocused && curFocused != RecyclerView.NO_POSITION) {
            states.add(android.R.attr.state_focused);
        }
        return toIntArray(states);
    }

    @Override
    public boolean hasFocusedItem() {
        return curFocused != RecyclerView.NO_POSITION;
    }

    @Override
    public void select(int position) {
        super.select(position);
    }

    @Override
    public void selectQuiet(int position) {
        super.selectQuiet(position);
    }

    protected boolean _focus(int position) {
        if (position == curFocused) {
            return false;
        }
        lastFocused = curFocused;
        curFocused = position;
        return true;
    }

    public void focus(int position) {
        if (_focus(position)) {
            maybeNotifyFocusChanged();
        }
    }

    protected void maybeNotifyFocusChanged() {
        if (lastFocused != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(lastFocused, TAG);
        }
        if (curFocused != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(curFocused, TAG);
        }
    }

    public class OnFocusListener implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (v != recyclerView || adapter == null) {
                return;
            }

            if (hasFocus) {
                if (curFocused != RecyclerView.NO_POSITION) {
                    recyclerView.scrollToPosition(curFocused);
                    adapter.notifyItemChanged(curFocused, TAG);
                } else {
                    int pos = curSelected != RecyclerView.NO_POSITION ? curSelected : 0;
                    recyclerView.scrollToPosition(pos);
                    focus(pos);
                }
            } else {
                if (curFocused != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(curFocused, TAG);
                }
            }
        }
    }

    public class OnKeyListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN || adapter == null) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (curFocused != RecyclerView.NO_POSITION) {
                    select(curFocused);
                }
                return true;
            }
            boolean handled = true;
            int pos = RecyclerView.NO_POSITION;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    pos = curFocused - 1;
                    pos = pos >= 0 ? pos : adapter.getItemCount() - 1;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    pos = curFocused + 1;
                    pos = pos < adapter.getItemCount() ? pos : 0;
                    break;
                default:
                    handled = false;
            }
            if (pos != RecyclerView.NO_POSITION) {
                recyclerView.scrollToPosition(pos);
                focus(pos);
            }
            return handled;
        }
    }
}
