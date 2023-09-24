package com.orion.iptv.recycleradapter;

public interface Selection<T> {
    void setAdapter(RecyclerAdapter<T> adapter);
    void setAdapter(RecyclerAdapter<T> adapter, int selected);

    void select(int position);
    void selectQuiet(int position);
    void clearSelection();
    void setCanRepeatSelect(boolean canRepeatSelect);
    int[] getState(int position);

    boolean hasSelectedItem();
    boolean hasFocusedItem();

    void addSelectedListener(OnSelectedListener<T> listener);
    void removeSelectedListener(OnSelectedListener<T> listener);

    interface OnSelectedListener<T> {
        void onSelected(int position, T item);
    }
}