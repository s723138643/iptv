package com.orion.iptv.ui.shares;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import com.orion.iptv.ui.video.VideoPlayerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.HttpUrl;

public class SharesContentFragment extends Fragment {

    private Share share;
    private FileNode currentNode;
    private RecyclerAdapter<FileNodeViewHolder, FileNode> adapter;
    private TextView pathHint;
    private ProgressBar loading;
    private TextView toast;
    private Long toastHideAt;


    private WebDavClient client;
    private Handler mHandler;

    private final Runnable toastHideHandler = new Runnable() {
        @Override
        public void run() {
            if (SystemClock.uptimeMillis() >= toastHideAt) {
                setViewVisible(toast, false);
            } else {
                mHandler.postAtTime(this, toastHideAt);
            }
        }
    };

    public SharesContentFragment() {
        super(R.layout.fragment_shares_content);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharesViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharesViewModel.class);
        mHandler = new Handler(requireContext().getMainLooper());
        share = viewModel.getSelectedShare().getValue();
        assert share != null;
        client = new WebDavClient(share);
        currentNode = share.getRoot();
        pathHint = view.findViewById(R.id.current_path);
        ImageButton homeButton = view.findViewById(R.id.shares_home);
        homeButton.setOnClickListener(buttonView -> {
            currentNode = share.getRoot();
            refresh();
        });

        loading = view.findViewById(R.id.loading);
        loading.setVisibility(View.GONE);
        toast = view.findViewById(R.id.toast);
        toast.setVisibility(View.GONE);

        RecyclerView nodes = view.findViewById(R.id.collections);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        nodes.setLayoutManager(layoutManager);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        nodes.addItemDecoration(itemDecoration);

        adapter = new RecyclerAdapter<>(
                requireContext(),
                new ArrayList<>(),
                new FileNodeViewHolderFactory(requireContext(), R.layout.layout_list_item)
        );
        adapter.setRepeatClickEnabled(true);
        adapter.setOnSelectedListener((position, node) -> {
            if (node.isRoot()) {
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack();
            } else if (node.isFile()) {
                play(node);
            } else if (node.getName().equals("..")) {
                currentNode = node.backup();
                refresh();
            } else {
                currentNode = node;
                refresh();
            }
        });
        nodes.setAdapter(adapter);
        nodes.addOnItemTouchListener(adapter.new OnItemTouchListener(requireContext(), nodes));
        refresh();
    }

    public void refresh() {
        setViewVisible(toast,false);
        setViewVisible(loading, true);
        pathHint.setText(currentNode.getAbsolutePath());
        client.list(
                currentNode,
                children -> {
                    Log.i("SharesContentFragment", String.format(Locale.ENGLISH, "current path: %s, is root: %b", currentNode.getPath(), currentNode.isRoot()));
                    if (children == null) {
                        children = new ArrayList<>();
                    }
                    FileNode backup = backupNode();
                    Log.i("SharesContentFragment", String.format(Locale.ENGLISH, "backup path: %s, is root: %b", backup.getPath(), backup.isRoot()));
                    if (children.size() > 0) {
                        children.set(0, backup);
                    } else {
                        children.add(backup);
                    }
                    java.util.List<FileNode> finalChildren = children;
                    mHandler.post(() -> {
                        setViewVisible(loading, false);
                        adapter.setData(finalChildren);
                    });
                },
                err -> {
                    Log.e("ShareContentFragment", err.toString());
                    mHandler.post(() -> {
                        setViewVisible(loading, false);
                        showToast(err.toString());
                        adapter.setData(List.of(backupNode()));
                    });
                }
        );
    }

    @NonNull
    private FileNode backupNode() {
        if (currentNode.isRoot()) {
            return new FileNode("..", currentNode.getPath(), false, currentNode.backup());
        }
        FileNode parent = currentNode.backup();
        return new FileNode("..", parent.getPath(), false, parent);
    }

    private Uri makeUri(FileNode node) {
        JSONObject config = share.getConfig();
        try {
            String server = config.getString("server");
            HttpUrl httpUrl = HttpUrl.parse(server);
            assert httpUrl != null;
            String url = httpUrl.newBuilder()
                    .addPathSegments(node.getPath())
                    .build()
                    .toString();
            return Uri.parse(url);
        } catch (JSONException err) {
            Log.e("SharesContentFragment", err.toString());
            return null;
        }
    }

    private void showToast(String message) {
        toast.setText(message);
        setViewVisible(toast, true);
        toastHideAt = SystemClock.uptimeMillis() + 5*1000;
        mHandler.postAtTime(toastHideHandler, toastHideAt);
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
        startActivity(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shares_content, container, false);
    }
}