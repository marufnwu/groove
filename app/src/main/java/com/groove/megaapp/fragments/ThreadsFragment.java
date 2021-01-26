package com.groove.megaapp.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.view.SimpleDraweeView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.ThreadDataSource;
import com.groove.megaapp.data.models.Thread;
import com.groove.megaapp.events.MessageEvent;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class ThreadsFragment extends Fragment {

    private ThreadFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel1 = new ViewModelProvider(this).get(ThreadFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_threads, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        ThreadDataSource source = mModel1.factory.source.getValue();
        if (source != null) {
            source.invalidate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mModel2.areThreadsInvalid) {
            mModel2.areThreadsInvalid = false;
            ThreadDataSource source = mModel1.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.header_back).setVisibility(View.INVISIBLE);
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.conversations_label);
        view.findViewById(R.id.header_more).setVisibility(View.INVISIBLE);
        ThreadAdapter adapter = new ThreadAdapter();
        RecyclerView threads = view.findViewById(R.id.threads);
        threads.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        OverScrollDecoratorHelper.setUpOverScroll(
                threads, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            ThreadDataSource source = mModel1.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel1.threads.observe(getViewLifecycleOwner(), adapter::submitList);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }
            List<?> list = mModel1.threads.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    public static ThreadsFragment newInstance() {
        return new ThreadsFragment();
    }

    private void showMessenger(Thread thread) {
        ((MainActivity)requireActivity()).showMessages("@" + thread.user.username, thread.id);
    }

    private class ThreadAdapter extends PagedListAdapter<Thread, ThreadViewHolder> {

        protected ThreadAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @NonNull
        @Override
        public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_thread, parent, false);
            return new ThreadViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
            Thread thread = getItem(position);
            //noinspection ConstantConditions
            if (TextUtils.isEmpty(thread.user.photo)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(thread.user.photo);
            }
            holder.username.setText('@' + thread.user.username);
            holder.verified.setVisibility(thread.user.verified ? View.VISIBLE : View.GONE);
            if (thread.latest == null) {
                holder.message.setVisibility(View.GONE);
                holder.when.setVisibility(View.GONE);
            } else {
                holder.message.setText(thread.latest.body);
                holder.message.setVisibility(View.VISIBLE);
                holder.when.setText(
                        DateUtils.getRelativeTimeSpanString(
                                requireContext(), thread.latest.createdAt.getTime(), true));
                holder.when.setVisibility(View.VISIBLE);
            }
            holder.itemView.setOnClickListener(v -> showMessenger(thread));
        }
    }

    public static class ThreadFragmentViewModel extends ViewModel {

        public ThreadFragmentViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new ThreadDataSource.Factory();
            state = Transformations.switchMap(factory.source, input -> input.state);
            threads = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Thread>> threads;
        public final ThreadDataSource.Factory factory;
        public final LiveData<LoadingState> state;
    }

    public static class ThreadViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView username;
        public View verified;
        public TextView message;
        public TextView when;

        public ThreadViewHolder(@NonNull View root) {
            super(root);
            photo = root.findViewById(R.id.photo);
            username = root.findViewById(R.id.username);
            verified = root.findViewById(R.id.verified);
            message = root.findViewById(R.id.message);
            when = root.findViewById(R.id.when);
        }
    }
}
