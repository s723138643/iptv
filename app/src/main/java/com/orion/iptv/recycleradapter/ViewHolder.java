package com.orion.iptv.recycleradapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;

public abstract class ViewHolder<T> extends RecyclerView.ViewHolder {
    protected final ColorStateList background;
    protected final ColorStateList foreground;

    public ViewHolder(@NonNull Context context, @NonNull View itemView) {
        super(itemView);
        foreground = context.getColorStateList(R.color.recyclerview_item_foreground);
        background = context.getColorStateList(R.color.recyclerview_item_background);
    }

    protected int getColorForState(int[] states, ColorStateList colorStateList) {
        return colorStateList.getColorForState(states, colorStateList.getDefaultColor());
    }

    protected boolean statesContains(int[] states, int... state) {
        for (int s : states) {
            for (int t : state) {
                if (t == s) {
                    return true;
                }
            }
        }
        return false;
    }

    @CallSuper
    public void changeState(int[] states) {
        itemView.setBackgroundColor(getColorForState(states, background));
    }

    public abstract void setContent(int position, T content);
}
