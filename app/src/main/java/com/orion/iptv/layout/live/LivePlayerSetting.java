package com.orion.iptv.layout.live;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.DefaultSelection;
import com.orion.iptv.recycleradapter.Selection;
import com.orion.iptv.recycleradapter.SelectionWithFocus;
import com.orion.player.ui.EnhanceConstraintLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LivePlayerSetting extends Fragment {
    private RecyclerView settingMenuView;
    private RecyclerView settingValueView;
    private Selection<SettingValue> valueSelection;
    private List<SettingMenu> menus;
    EnhanceConstraintLayout enhanceConstraintLayout;
    private static final long AutoHideAfterMillis = 5 * 1000;
    private long hideMyselfAt = 0;
    private final Runnable hideMyself = new Runnable() {
        @Override
        public void run() {
            long diff = SystemClock.uptimeMillis() - hideMyselfAt;
            if (diff >= 0) {
                hideMyselfAt = 0;
                hide();
            } else {
                enhanceConstraintLayout.postDelayed(this, -diff);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_player_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LivePlayerViewModel viewModel = new ViewModelProvider(requireActivity()).get(LivePlayerViewModel.class);

        enhanceConstraintLayout = (EnhanceConstraintLayout) view;
        settingMenuView = view.findViewById(R.id.livePlayerMenu);
        settingValueView = view.findViewById(R.id.livePlayerValue);

        menus = new ArrayList<>();
        menus.add(new SetChannelSourceUrl(requireActivity(), viewModel));
        menus.add(new SetPlayerFactory(requireActivity(), viewModel));
        menus.add(new SetSurfaceType(requireActivity(), viewModel));
        menus.add(new SetSourceTimeout(requireActivity(), viewModel));
        enhanceConstraintLayout.addEventListener(new EnhanceConstraintLayout.EventListener() {
            @Override
            public void onMotionEvent(MotionEvent ev) {
                hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            }

            @Override
            public void onKeyEvent(KeyEvent ev) {
                hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
            }

            @Override
            public void onVisibilityChanged(@NonNull View changedView, int visibility) {
                if (changedView != enhanceConstraintLayout) {
                    return;
                }
                enhanceConstraintLayout.removeCallbacks(hideMyself);
                if (visibility == View.VISIBLE && hideMyselfAt > 0) {
                    enhanceConstraintLayout.postDelayed(hideMyself, Math.max(hideMyselfAt-SystemClock.uptimeMillis(), 1));
                }
            }
        });
        initMenuList();
        initValueList();
    }

    protected void initValueList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        settingValueView.setLayoutManager(layoutManager);
        valueSelection = new SelectionWithFocus<>(settingValueView);
        RecyclerAdapter<SettingValue> valueViewAdapter = new RecyclerAdapter<>(
                requireActivity(),
                menus.get(0).getValues(),
                new ValueListViewHolderFactory(requireActivity(), R.layout.layout_list_item)
        );
        valueSelection.setAdapter(valueViewAdapter);
        settingValueView.setAdapter(valueViewAdapter);
        valueSelection.addSelectedListener(this::onValueSelected);
    }

    protected void initMenuList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setMeasurementCacheEnabled(false);
        settingMenuView.setLayoutManager(layoutManager);
        DefaultSelection<SettingMenu> defaultSelection = new DefaultSelection<>(settingMenuView);
        RecyclerAdapter<SettingMenu> menuViewAdapter = new RecyclerAdapter<>(
                requireActivity(),
                menus,
                new MenuListViewHolderFactory(requireActivity(), R.layout.layout_list_item)
        );
        defaultSelection.setAdapter(menuViewAdapter, 0);
        settingMenuView.setAdapter(menuViewAdapter);
        defaultSelection.addSelectedListener(this::onMenuSelected);
    }

    private void onValueSelected(int position, SettingValue value) {
        value.onSelected();
        if (value.isButton()) {
            Log.i("Setting", "clear selection...");
            enhanceConstraintLayout.postDelayed(valueSelection::clearSelection, 800);
        }
    }

    private void onMenuSelected(int position, SettingMenu menu) {
        Log.i("Setting", String.format(Locale.ENGLISH, "menu %s selected", menu.content()));
        int selectedPosition = menu.getSelectedPosition();
        RecyclerAdapter<SettingValue> valueAdapter = new RecyclerAdapter<>(
                requireActivity(),
                menu.getValues(),
                new ValueListViewHolderFactory(requireActivity(), R.layout.layout_list_item)
        );
        valueSelection.setAdapter(valueAdapter, selectedPosition);
        settingValueView.swapAdapter(valueAdapter, true);
    }

    public boolean isViewVisible() {
        return enhanceConstraintLayout.getVisibility() == View.VISIBLE;
    }

    public void toggleVisibility(boolean needFocus) {
        if (enhanceConstraintLayout.getVisibility() == View.GONE) {
            show(needFocus);
        } else {
            hide();
        }
    }

    public void toggleVisibility() {
        if (enhanceConstraintLayout.getVisibility() == View.GONE) {
            show();
        } else {
            hide();
        }
    }

    public void show(boolean needFocus) {
        show();
        if (needFocus && !enhanceConstraintLayout.hasFocus()) {
            settingMenuView.requestFocus();
        }
    }

    public void show() {
        hideMyselfAt = SystemClock.uptimeMillis() + AutoHideAfterMillis;
        enhanceConstraintLayout.setAlpha(0f);
        enhanceConstraintLayout.setVisibility(View.VISIBLE);
        enhanceConstraintLayout.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        enhanceConstraintLayout.setAlpha(1f);
                    }
                });
    }

    public void hide() {
        enhanceConstraintLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    /**
                     * {@inheritDoc}
                     *
                     * @param animation
                     */
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        enhanceConstraintLayout.setVisibility(View.GONE);
                    }
                });
    }
}