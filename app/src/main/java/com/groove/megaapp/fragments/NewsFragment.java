package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.ads.BannerAdProvider;
import com.groove.megaapp.ads.InterstitialAdProvider;
import com.groove.megaapp.ads.natives.NativeAdProvider;
import com.groove.megaapp.ads.natives.NativeAdProviderFactory;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.ArticleDataSource;
import com.groove.megaapp.data.ArticleSectionDataSource;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;
import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.groove.megaapp.data.models.Advertisement;
import com.groove.megaapp.data.models.Article;
import com.groove.megaapp.data.models.ArticleSection;
import com.groove.megaapp.utils.AdsUtil;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class NewsFragment extends Fragment {

    private BannerAdProvider mAd1;
    private InterstitialAdProvider mAd2;
    private NativeAdProvider mAd3;
    private NewsFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Advertisement ad1 = AdsUtil.findByLocationAndType("news", "banner");
        if (ad1 != null) {
            mAd1 = new BannerAdProvider(ad1);
        }
        Advertisement ad2 = AdsUtil.findByLocationAndType("news", "interstitial");
        if (ad2 != null) {
            mAd2 = new InterstitialAdProvider(ad2);
        }
        int interval = SharedConstants.DEFAULT_PAGE_SIZE;
        Advertisement ad3 = AdsUtil.findByLocationAndType("news", "native");
        if (ad3 != null) {
            mAd3 = NativeAdProviderFactory.create(requireContext(), ad3, 5);
            if (mAd3 != null) {
                interval = mAd3.getInterval();
            }
        }
        NewsFragmentViewModel.Factory factory =
                new NewsFragmentViewModel.Factory(mAd3 != null, interval);
        mModel = new ViewModelProvider(this, factory).get(NewsFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.news_label);
        view.findViewById(R.id.header_more).setVisibility(View.INVISIBLE);
        RecyclerView articles = view.findViewById(R.id.articles);
        ArticleAdapter adapter1 = new ArticleAdapter();
        articles.setAdapter(new SlideInLeftAnimationAdapter(adapter1));
        mModel.articles.observe(getViewLifecycleOwner(), adapter1::submitList);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            ArticleDataSource source = mModel.factory1.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel.state1.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }

            List<?> list = mModel.articles.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        RecyclerView sections = view.findViewById(R.id.sections);
        LinearLayoutManager llm =
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        sections.setLayoutManager(llm);
        ArticleSectionAdapter adapter2 = new ArticleSectionAdapter();
        sections.setAdapter(new SlideInBottomAnimationAdapter(adapter2));
        OverScrollDecoratorHelper.setUpOverScroll(
                sections, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
        mModel.sections.observe(getViewLifecycleOwner(), adapter2::submitList);
        mModel.selection.observe(getViewLifecycleOwner(), integers -> {
            mModel.factory1.sections = integers;
            ArticleDataSource source = mModel.factory1.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        if (mAd1 != null) {
            View ad = mAd1.create(requireContext());
            if (ad != null) {
                LinearLayout banner = view.findViewById(R.id.banner);
                banner.removeAllViews();
                banner.addView(ad);
            }
        }
    }

    public static NewsFragment newInstance() {
        return new NewsFragment();
    }

    private void showUrlBrowser(Article article) {
        boolean ad = false;
        if (mAd2 != null) {
            if (mModel.viewed >= mAd2.getInterval()) {
                mModel.viewed = 0;
                ad = true;
            }

            mModel.viewed++;
        }
        if (ad) {
            Runnable show = mAd2.create(requireContext());
            if (show != null) {
                show.run();
            }
        } else {
            ((MainActivity)requireActivity())
                    .showUrlBrowser(article.link, article.title, true);
        }
    }

    private static class AdViewHolder extends RecyclerView.ViewHolder {

        public LinearLayout container;
        public View error;
        public View loading;

        public AdViewHolder(@NonNull View root) {
            super(root);
            container = root.findViewById(R.id.container);
            error = root.findViewById(R.id.error);
            loading = root.findViewById(R.id.loading);
        }
    }

    private class ArticleAdapter extends PagedListAdapter<Article, RecyclerView.ViewHolder> {

        private static final int TYPE_ARTICLE = 100;
        private static final int TYPE_AD = 200;

        public ArticleAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public int getItemViewType(int position) {
            Article article = getItem(position);
            //noinspection ConstantConditions
            return article.ad ? TYPE_AD : TYPE_ARTICLE;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Article article = getItem(position);
            //noinspection ConstantConditions
            if (article.ad) {
                AdViewHolder avh = (AdViewHolder)holder;
                View ad = mAd3.create(getLayoutInflater(), avh.container);
                if (ad != null) {
                    avh.container.removeAllViews();
                    avh.container.addView(ad);
                }

                avh.error.setVisibility(ad == null ? View.VISIBLE : View.GONE);
                avh.loading.setVisibility(View.GONE);
            } else {
                ArticleViewHolder avh = (ArticleViewHolder)holder;
                Glide.with(requireContext())
                        .load(article.image)
                        .placeholder(R.drawable.image_placeholder)
                        .into(avh.image);
                avh.title.setText(article.title);
                avh.snippet.setText(article.snippet);
                if (TextUtils.isEmpty(article.source)) {
                    avh.publisher.setVisibility(View.GONE);
                    avh.publisherContainer.setVisibility(View.GONE);
                } else {
                    avh.publisher.setText(article.source);
                    avh.publisher.setVisibility(View.VISIBLE);
                    avh.publisherContainer.setVisibility(View.VISIBLE);
                }

                avh.when.setText(
                        DateUtils.getRelativeTimeSpanString(
                                requireContext(), article.publishedAt.getTime(), true));
                avh.image.setOnClickListener(v -> showUrlBrowser(article));
                avh.title.setOnClickListener(v -> showUrlBrowser(article));
                avh.snippet.setOnClickListener(v -> showUrlBrowser(article));
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            if (viewType == TYPE_AD) {
                View view = inflater.inflate(R.layout.item_native_ad, parent, false);
                return new AdViewHolder(view);
            }

            View view = inflater.inflate(R.layout.item_article, parent, false);
            return new ArticleViewHolder(view);
        }
    }

    private class ArticleSectionAdapter extends PagedListAdapter<ArticleSection, ArticleSectionViewHolder> {

        public ArticleSectionAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public void onBindViewHolder(@NonNull ArticleSectionViewHolder holder, int position) {
            ArticleSection section = getItem(position);
            //noinspection ConstantConditions
            holder.chip.setText(section.name);
            List<Integer> now = mModel.selection.getValue();
            holder.chip.setChecked(now != null && now.contains(section.id));
            holder.chip.setOnCheckedChangeListener((v, checked) -> {
                List<Integer> then = mModel.selection.getValue();
                if (checked && !then.contains(section.id)) {
                    then.add(section.id);
                } else if (!checked && then.contains(section.id)) {
                    then.remove((Integer) section.id);
                }

                mModel.selection.postValue(then);
            });
        }

        @NonNull
        @Override
        public ArticleSectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_article_section, parent, false);
            return new ArticleSectionViewHolder(view);
        }
    }

    private static class ArticleSectionViewHolder extends RecyclerView.ViewHolder {

        public Chip chip;

        public ArticleSectionViewHolder(@NonNull View root) {
            super(root);
            chip = root.findViewById(R.id.chip);
            chip.setCheckable(true);
        }
    }

    private static class ArticleViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title;
        TextView snippet;
        TextView publisher;
        View publisherContainer;
        TextView when;

        public ArticleViewHolder(@NonNull View root) {
            super(root);
            image = root.findViewById(R.id.image);
            title = root.findViewById(R.id.title);
            snippet = root.findViewById(R.id.snippet);
            publisher = root.findViewById(R.id.publisher);
            publisherContainer = root.findViewById(R.id.publisher_container);
            when = root.findViewById(R.id.when);
        }
    }

    public static class NewsFragmentViewModel extends ViewModel {

        public NewsFragmentViewModel(boolean ads, int count) {
            PagedList.Config config1 = new PagedList.Config.Builder()
                    .setPageSize(count)
                    .build();
            factory1 = new ArticleDataSource.Factory(null, ads, count);
            state1 = Transformations.switchMap(factory1.source, input -> input.state);
            articles = new LivePagedListBuilder<>(factory1, config1).build();
            PagedList.Config config2 = new PagedList.Config.Builder()
                    .setPageSize(100)
                    .build();
            factory2 = new ArticleSectionDataSource.Factory();
            state2 = Transformations.switchMap(factory2.source, input -> input.state);
            sections = new LivePagedListBuilder<>(factory2, config2).build();
        }

        public int ad = 0;
        public final LiveData<PagedList<Article>> articles;
        public final ArticleDataSource.Factory factory1;
        public final ArticleSectionDataSource.Factory factory2;
        public final LiveData<PagedList<ArticleSection>> sections;
        public final MutableLiveData<List<Integer>> selection =
                new MutableLiveData<>(new ArrayList<>());
        public final LiveData<LoadingState> state1;
        public final LiveData<LoadingState> state2;
        public int viewed = 0;

        private static class Factory implements ViewModelProvider.Factory {

            private final boolean mAds;
            private final int mCount;

            public Factory(boolean ads, int count) {
                mAds = ads;
                mCount = count;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new NewsFragmentViewModel(mAds, mCount);
            }
        }
    }
}
