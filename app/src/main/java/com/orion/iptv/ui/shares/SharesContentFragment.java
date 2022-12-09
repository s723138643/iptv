package com.orion.iptv.ui.shares;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orion.iptv.R;
import com.orion.iptv.network.WebDavClient;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.DefaultSelection;
import com.orion.iptv.recycleradapter.SelectionWithFocus;
import com.orion.iptv.ui.video.VideoPlayerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.HttpUrl;

public class SharesContentFragment extends Fragment {
    private static final String TAG = "SharesContentFragment";
    private Share share;
    private FileNode path;
    ImageButton homeButton;
    private RecyclerView nodes;
    DefaultSelection<FileNode> defaultSelection;
    private TextView pathHint;
    private ProgressBar loading;
    private TextView toast;

    private WebDavClient client;
    private Handler mHandler;

    private final HideToastTasker hideToastTasker = new HideToastTasker();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shares_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pathHint = view.findViewById(R.id.current_path);
        loading = view.findViewById(R.id.loading);
        loading.setVisibility(View.GONE);
        toast = view.findViewById(R.id.toast);
        toast.setVisibility(View.GONE);

        nodes = view.findViewById(R.id.collections);
        homeButton = view.findViewById(R.id.shares_home);

        SharesViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharesViewModel.class);
        path = (FileNode) requireArguments().getSerializable("path");
        mHandler = new Handler(requireContext().getMainLooper());

        share = viewModel.getSelectedShare();
        if (share == null) {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
            return;
        }
        client = new WebDavClient(share);
        homeButton.setOnClickListener(buttonView -> requireActivity()
                .getSupportFragmentManager()
                .popBackStack(SharesHomeFragment.TAG, 0));
        initView();
        refresh();
    }

    @Override
    public void onStop() {
        super.onStop();
        client.cancel();
        mHandler.removeCallbacksAndMessages(null);
    }

    private void initView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        nodes.setLayoutManager(layoutManager);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        nodes.addItemDecoration(itemDecoration);
        defaultSelection = new SelectionWithFocus<>(nodes);
        defaultSelection.setCanRepeatSelect(true);

        RecyclerAdapter<FileNode> adapter = new RecyclerAdapter<>(
                requireContext(),
                new ArrayList<>(),
                new FileNodeViewHolderFactory(requireContext(), R.layout.layout_list_item)
        );
        defaultSelection.setAdapter(adapter);
        defaultSelection.addSelectedListener((position, node) -> {
            if (node == FileNode.PARENT) {
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack();
            } else if (node == FileNode.CURRENT) {
                refresh();
            } else if (node.isFile()) {
                play(node);
            } else {
                Bundle args = new Bundle();
                args.putSerializable("path", node);
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.shares_container_view, SharesContentFragment.class, args)
                        .addToBackStack(null)
                        .commit();
            }
        });
        nodes.setAdapter(adapter);
    }

    public void refresh() {
        setViewVisible(toast,false);
        setViewVisible(loading, true);
        pathHint.setText(path.getAbsolutePath());
        client.list(
                path,
                new WebDavClient.Callback() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ShareContentFragment", e.toString());
                        mHandler.post(() -> {
                            setViewVisible(loading, false);
                            showToast(e.toString());
                            RecyclerAdapter<FileNode> adapter = new RecyclerAdapter<>(
                                    requireActivity(),
                                    List.of(FileNode.CURRENT, FileNode.PARENT),
                                    new FileNodeViewHolderFactory(requireContext(), R.layout.layout_list_item)
                            );
                            defaultSelection.setAdapter(adapter);
                            nodes.swapAdapter(adapter, true);
                        });
                    }

                    @Override
                    public void onResponse(@Nullable List<FileNode> children) {
                        Log.i(TAG, String.format(Locale.ENGLISH, "current path: %s", path.getPath()));
                        List<FileNode> items = new ArrayList<>(children == null ? 2 : children.size() + 2);
                        items.add(0, FileNode.PARENT);
                        items.add(0, FileNode.CURRENT);
                        if (children != null) {
                            items.addAll(children);
                        }
                        mHandler.post(() -> {
                            setViewVisible(loading, false);
                            RecyclerAdapter<FileNode> adapter = new RecyclerAdapter<>(
                                    requireActivity(),
                                    items,
                                    new FileNodeViewHolderFactory(requireContext(), R.layout.layout_list_item)
                            );
                            defaultSelection.setAdapter(adapter);
                            nodes.swapAdapter(adapter, true);
                        });
                    }
                }
        );
    }

    private Uri makeUri(FileNode node) {
        JSONObject config = share.getConfig();
        try {
            String server = config.getString("server");
            HttpUrl httpUrl = HttpUrl.parse(server);
            assert httpUrl != null;
            HttpUrl.Builder builder = httpUrl.newBuilder().addPathSegments(node.getPath());
            return Uri.parse(builder.build().toString());
        } catch (JSONException err) {
            Log.e("SharesContentFragment", err.toString());
            return null;
        }
    }

    private void showToast(String message) {
        toast.setText(message);
        // 先开启延时任务然后再显示toast，
        // 避免被已经开启的延时任务提前隐藏
        hideToastTasker.engage();
        setViewVisible(toast, true);
    }

    private void setViewVisible(View view, boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }

    public void play(FileNode node) {
        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
        intent.setData(makeUri(node));
        JSONObject config = share.getConfig();
        if (config.has("username")) {
            Bundle bundle = new Bundle();
            try {
                bundle.putString("username", config.getString("username"));
                bundle.putString("password", config.getString("password"));
                intent.putExtra("auth", bundle);
            } catch (JSONException ignored){
            }
        }
        startActivity(intent);
    }

    private class HideToastTasker implements Runnable {
        private boolean engaged;
        private Long toastHideAt;

        @Override
        public void run() {
            if (!engaged) {
                return;
            }
            if (SystemClock.uptimeMillis() >= toastHideAt) {
                setViewVisible(toast, false);
                engaged = false;
            } else {
                mHandler.postAtTime(this, toastHideAt);
            }
        }

        public void engage() {
            toastHideAt = SystemClock.uptimeMillis() + 5*1000;
            if (!engaged) {
                engaged = true;
                mHandler.postAtTime(this, toastHideAt);
            }
        }
    }
}