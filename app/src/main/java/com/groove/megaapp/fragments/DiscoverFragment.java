package com.groove.megaapp.fragments;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;
import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.ads.BannerAdProvider;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.ClipDataSource;
import com.groove.megaapp.data.ClipItemDataSource;
import com.groove.megaapp.data.ClipSectionDataSource;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Advertisement;
import com.groove.megaapp.data.models.Challenge;
import com.groove.megaapp.data.models.Clip;
import com.groove.megaapp.data.models.ClipSection;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.utils.AdsUtil;
import com.groove.megaapp.utils.TextFormatUtil;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverFragment extends Fragment {

    private static final String TAG = "DiscoverFragment";

    private BannerAdProvider mAd;
    private DiscoverFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Advertisement ad = AdsUtil.findByLocationAndType("discover", "banner");
        if (ad != null) {
            mAd = new BannerAdProvider(ad);
        }
        mModel = new ViewModelProvider(this).get(DiscoverFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        LoadingState state = mModel.state3.getValue();
        if (state != LoadingState.LOADED && state != LoadingState.LOADING) {
            loadChallenges();
        }
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView news = view.findViewById(R.id.header_back);
        news.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_travel_explore_24));
        news.setOnClickListener(v -> ((MainActivity)requireActivity()).showNews());
        if (!getResources().getBoolean(R.bool.news_enabled)) {
            news.setVisibility(View.INVISIBLE);
        }
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.discover_label);
        ImageButton search = view.findViewById(R.id.header_more);
        search.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_search_24));
        search.setOnClickListener(v -> ((MainActivity)requireActivity()).showSearch());
        RecyclerView sections = view.findViewById(R.id.sections);
        VerticalAdapter adapter = new VerticalAdapter();
        sections.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            ClipSectionDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        mModel.sections.observe(getViewLifecycleOwner(), adapter::submitList);
        View loading = view.findViewById(R.id.loading);
        mModel.state1.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        mModel.challenges.observe(getViewLifecycleOwner(), challenges -> {
            if (challenges != null && !challenges.isEmpty()) {
                showChallengesSlider(challenges);
            }
        });
        mModel.state3.observe(getViewLifecycleOwner(), state ->
                Log.v(TAG, "Loading challenges state is " + state + "."));
        if (mAd != null) {
            View ad = mAd.create(requireContext());
            if (ad != null) {
                LinearLayout banner = view.findViewById(R.id.banner);
                banner.removeAllViews();
                banner.addView(ad);
            }
        }
    }

    private void loadChallenges() {
        mModel.state3.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.challengesIndex()
                .enqueue(new Callback<Wrappers.Paginated<Challenge>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Challenge>> call,
                            @Nullable Response<Wrappers.Paginated<Challenge>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Loading challenges from server returned " + code + ".");
                        if (response != null && response.isSuccessful()) {
                            List<Challenge> challenges = response.body().data;
                            mModel.challenges.postValue(challenges);
                            mModel.state3.postValue(LoadingState.LOADED);
                        } else {
                            mModel.state3.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Challenge>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to load challenges from server.", t);
                        mModel.state3.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static DiscoverFragment newInstance() {
        return new DiscoverFragment();
    }

    private void showChallengesSlider(List<Challenge> challenges) {
        View container = getView().findViewById(R.id.challenges);
        container.setVisibility(View.VISIBLE);
        ViewPager2 pager = getView().findViewById(R.id.pager);
        pager.setAdapter(new ChallengePagerAdapter(this, challenges));
        pager.setOffscreenPageLimit(1);
        float npx = getResources()
                .getDimension(R.dimen.viewpager_adjacent_visibility);
        float cpx = getResources()
                .getDimension(R.dimen.viewpager_current_margin);
        pager.addItemDecoration(new RecyclerView.ItemDecoration() {

            private final int mMargin = Math.round(cpx);

            @Override
            public void getItemOffsets(
                    @NonNull Rect out,
                    @NonNull View view,
                    @NonNull RecyclerView parent,
                    @NonNull RecyclerView.State state) {
                out.left = mMargin;
                out.right = mMargin;
            }
        });
        ViewPager2.PageTransformer transformer = (page, position) -> {
            page.setTranslationX(-(npx + cpx) * position);
            page.setScaleY(1 - (0.25f * Math.abs(position)));
            page.setAlpha(0.25f + (1 - Math.abs(position)));
        };
        pager.setPageTransformer(transformer);
        WormDotsIndicator indicator = getView().findViewById(R.id.indicator);
        indicator.setViewPager2(pager);
    }

    private void showPlayerSlider(int clip, List<ClipSection> sections) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (ClipSection section : sections) {
            ids.add(section.id);
        }
        Bundle params = new Bundle();
        params.putIntegerArrayList(ClipDataSource.PARAM_SECTIONS, ids);
        ((MainActivity)requireActivity()).showPlayerSlider(clip, params);
    }

    private void showSection(String name, int id) {
        ArrayList<Integer> sections = new ArrayList<>();
        sections.add(id);
        Bundle params = new Bundle();
        params.putIntegerArrayList(ClipDataSource.PARAM_SECTIONS, sections);
        ((MainActivity) requireActivity()).showClips(name, params);
    }

    private static class ChallengePagerAdapter extends FragmentStateAdapter {

        private final List<Challenge> mChallenges;

        public ChallengePagerAdapter(@NonNull Fragment fragment, List<Challenge> challenges) {
            super(fragment);
            mChallenges = challenges;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ChallengeFragment.newInstance(mChallenges.get(position));
        }

        @Override
        public int getItemCount() {
            return mChallenges.size();
        }
    }

    private class HorizontalAdapter extends PagedListAdapter<Clip, ClipViewHolder> {

        protected HorizontalAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @NonNull
        @Override
        public ClipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_clip_discover, parent, false);
            return new ClipViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull ClipViewHolder holder, int position) {
            Clip clip = getItem(position);
            holder.likes.setText(TextFormatUtil.toShortNumber(clip.likesCount));
            //noinspection unchecked
            Glide.with(requireContext())
                    .asGif()
                    .load(clip.preview)
                    .thumbnail(new RequestBuilder[]{
                            Glide.with(requireContext()).load(clip.screenshot).centerCrop()
                    })
                    .apply(RequestOptions.placeholderOf(R.drawable.image_placeholder).centerCrop())
                    .into(holder.preview);
            holder.itemView.setOnClickListener(v -> showPlayerSlider(clip.id, clip.sections));
        }
    }

    private class VerticalAdapter extends PagedListAdapter<ClipSection, SectionViewHolder> {

        protected VerticalAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
            ClipSection section = getItem(position);
            holder.all.setOnClickListener(v -> showSection(section.name, section.id));
            holder.all.setText(getString(R.string.see_all_label, TextFormatUtil.toShortNumber(section.clipsCount)));
            holder.title.setOnClickListener(v -> showSection(section.name, section.id));
            holder.title.setText(section.name);
            holder.load(section.id);
        }

        @NonNull
        @Override
        public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_discover_section, parent, false);
            return new SectionViewHolder(root);
        }
    }

    public static class DiscoverFragmentViewModel extends ViewModel {

        public DiscoverFragmentViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new ClipSectionDataSource.Factory();
            state1 = Transformations.switchMap(factory.source, input -> input.state);
            sections = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<ClipSection>> sections;
        public final ClipSectionDataSource.Factory factory;
        public final LiveData<LoadingState> state1;
        public final MutableLiveData<LoadingState> state3 = new MutableLiveData<>(LoadingState.IDLE);
        public final MutableLiveData<List<Challenge>> challenges = new MutableLiveData<>();
    }

    private static class ClipViewHolder extends RecyclerView.ViewHolder {

        public ImageView preview;
        public TextView likes;

        public ClipViewHolder(@NonNull View root) {
            super(root);
            preview = root.findViewById(R.id.preview);
            likes = root.findViewById(R.id.likes);
        }
    }

    private class SectionViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView all;
        public ProgressBar loading;
        public RecyclerView clips;

        public LiveData<PagedList<Clip>> items;
        public LiveData<LoadingState> state;

        public SectionViewHolder(@NonNull View root) {
            super(root);
            title = root.findViewById(R.id.title);
            all = root.findViewById(R.id.all);
            clips = root.findViewById(R.id.clips);
            loading = root.findViewById(R.id.loading);
            LinearLayoutManager llm =
                    new LinearLayoutManager(
                            requireContext(), LinearLayoutManager.HORIZONTAL, false);
            clips.setLayoutManager(llm);
            OverScrollDecoratorHelper.setUpOverScroll(
                    clips, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
        }

        public void load(int section) {
            HorizontalAdapter adapter = new HorizontalAdapter();
            clips.setAdapter(new SlideInBottomAnimationAdapter(adapter));
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            ArrayList<Integer> sections = new ArrayList<>();
            sections.add(section);
            Bundle params = new Bundle();
            params.putIntegerArrayList(ClipDataSource.PARAM_SECTIONS, sections);
            ClipItemDataSource.Factory factory = new ClipItemDataSource.Factory(params);
            state = Transformations.switchMap(factory.source, input -> input.state);
            state.observe(getViewLifecycleOwner(), state ->
                    loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE));
            items = new LivePagedListBuilder<>(factory, config).build();
            items.observe(getViewLifecycleOwner(), adapter::submitList);
        }
    }
}
