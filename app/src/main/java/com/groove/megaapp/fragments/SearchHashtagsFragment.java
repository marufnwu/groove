package com.groove.megaapp.fragments;

import android.os.Bundle;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.ClipDataSource;
import com.groove.megaapp.data.HashtagDataSource;
import com.groove.megaapp.data.models.Hashtag;
import com.groove.megaapp.utils.TextFormatUtil;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class SearchHashtagsFragment extends Fragment {

    private SearchHashtagsFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
        SearchHashtagsFragmentViewModel.Factory factory =
                new SearchHashtagsFragmentViewModel.Factory(mModel2.searchTerm.getValue());
        mModel1 = new ViewModelProvider(this, factory)
                .get(SearchHashtagsFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_hashtags, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        HashtagAdapter adapter = new HashtagAdapter();
        RecyclerView hashtags = view.findViewById(R.id.hashtags);
        hashtags.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        hashtags.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        OverScrollDecoratorHelper.setUpOverScroll(
                hashtags, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel1.hashtags.observe(getViewLifecycleOwner(), adapter::submitList);
        TextView empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel1.hashtags.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        mModel2.searchTerm.observe(getViewLifecycleOwner(), q -> {
            mModel1.factory.q = q;
            HashtagDataSource source = mModel1.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
    }

    public static SearchHashtagsFragment newInstance() {
        return new SearchHashtagsFragment();
    }

    private void showHashtag(String name) {
        ArrayList<String> hashtags = new ArrayList<>();
        hashtags.add(name);
        Bundle params = new Bundle();
        params.putStringArrayList(ClipDataSource.PARAM_HASHTAGS, hashtags);
        ((MainActivity) requireActivity()).showClips("#" + name, params);
    }

    public static class SearchHashtagsFragmentViewModel extends ViewModel {

        public SearchHashtagsFragmentViewModel(@Nullable String q) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new HashtagDataSource.Factory(q);
            state = Transformations.switchMap(factory.source, input -> input.state);
            hashtags = new LivePagedListBuilder<>(factory, config).build();
        }

        public final HashtagDataSource.Factory factory;
        public final LiveData<PagedList<Hashtag>> hashtags;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final String mQ;

            public Factory(String q) {
                mQ = q;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new SearchHashtagsFragmentViewModel(mQ);
            }
        }
    }

    private class HashtagAdapter extends PagedListAdapter<Hashtag, HashtagViewHolder> {

        protected HashtagAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position) {
            final Hashtag hashtag = getItem(position);
            holder.name.setText(hashtag.name);
            holder.clips.setText(
                    getString(R.string.count_clips, TextFormatUtil.toShortNumber(hashtag.clips)));
            holder.itemView.setOnClickListener(v -> showHashtag(hashtag.name));
        }

        @NonNull
        @Override
        public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_hashtag, parent, false);
            return new HashtagViewHolder(root);
        }
    }

    private static class HashtagViewHolder extends RecyclerView.ViewHolder {

        public TextView name;
        public TextView clips;

        public HashtagViewHolder(@NonNull View root) {
            super(root);
            name = root.findViewById(R.id.name);
            clips = root.findViewById(R.id.clips);
        }
    }
}
