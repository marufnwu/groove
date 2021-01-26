package com.groove.megaapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.view.SimpleDraweeView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.UploadActivity;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.DraftDataSource;
import com.groove.megaapp.data.entities.Draft;
import com.groove.megaapp.events.ResetDraftsEvent;

public class DraftGridFragment extends Fragment {

    private DraftGridFragmentViewModel mModel;

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SharedConstants.REQUEST_CODE_UPLOAD_CLIP && resultCode == Activity.RESULT_OK) {
            DraftDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(this).get(DraftGridFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clip_grid, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResetDraftsEvent(ResetDraftsEvent event) {
        DraftDataSource source = mModel.factory.source.getValue();
        if (source != null) {
            source.invalidate();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.header).setVisibility(View.GONE);
        RecyclerView clips = view.findViewById(R.id.clips);
        DraftGridAdapter adapter = new DraftGridAdapter();
        clips.setAdapter(new SlideInBottomAnimationAdapter(adapter));
        GridLayoutManager glm = new GridLayoutManager(requireContext(), 3);
        clips.setLayoutManager(glm);
        mModel.drafts.observe(getViewLifecycleOwner(), adapter::submitList);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            DraftDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }

            List<?> list = mModel.drafts.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    public static DraftGridFragment newInstance() {
        return new DraftGridFragment();
    }

    private void openForUpload(Draft draft) {
        Intent intent = new Intent(requireContext(), UploadActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(UploadActivity.EXTRA_DRAFT, draft);
        startActivityForResult(intent, SharedConstants.REQUEST_CODE_UPLOAD_CLIP);
    }

    private class DraftGridAdapter extends PagedListAdapter<Draft, DraftGridViewHolder> {

        protected DraftGridAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @NonNull
        @Override
        public DraftGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_clip_draft, parent, false);
            return new DraftGridViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull DraftGridViewHolder holder, int position) {
            Draft draft = getItem(position);
            holder.preview.setImageURI(Uri.fromFile(new File(draft.screenshot)));
            holder.itemView.setOnClickListener(v -> openForUpload(draft));
        }
    }

    public static class DraftGridFragmentViewModel extends ViewModel {

        public DraftGridFragmentViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new DraftDataSource.Factory();
            state = Transformations.switchMap(factory.source, input -> input.state);
            drafts = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Draft>> drafts;
        public final DraftDataSource.Factory factory;
        public final LiveData<LoadingState> state;
    }

    private static class DraftGridViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView preview;

        public DraftGridViewHolder(@NonNull View root) {
            super(root);
            preview = root.findViewById(R.id.preview);
        }
    }
}
