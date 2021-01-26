package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import androidx.paging.AsyncPagedListDiffer;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.danikula.videocache.HttpProxyCacheServer;
import com.jakewharton.rxbinding4.viewpager2.PageScrollEvent;
import com.jakewharton.rxbinding4.viewpager2.RxViewPager2;
import com.pixplicity.easyprefs.library.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.ads.InterstitialAdProvider;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.common.VisibilityAware;
import com.groove.megaapp.data.ClipDataSource;
import com.groove.megaapp.data.ClipPageDataSource;
import com.groove.megaapp.data.models.Advertisement;
import com.groove.megaapp.data.models.Clip;
import com.groove.megaapp.events.PlaybackEndedEvent;
import com.groove.megaapp.events.ResetPlayerSliderEvent;
import com.groove.megaapp.utils.AdsUtil;
import com.groove.megaapp.utils.CacheUtil;

public class PlayerSliderFragment extends Fragment {

    private static final String ARG_FIRST = "first";
    private static final String ARG_PARAMS = "params";
    private static final String TAG = "PlayerSliderFragment";

    private InterstitialAdProvider mAd1;
    private Advertisement mAd2;
    private Disposable mDisposable;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private PlayerSliderFragmentViewModel mModel;
    private ViewPager2 mPager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Player slider fragment created.");
        Bundle params = requireArguments().getBundle(ARG_PARAMS);
        if (params == null) {
            params = new Bundle();
        }
        int first = requireArguments().getInt(ARG_FIRST, -1);
        Log.v(TAG, "First clip as per arguments: " + first);
        if (first > 0) {
            params.putInt(ClipDataSource.PARAM_FIRST, first);
        }
        Advertisement ad1 = AdsUtil.findByLocationAndType("player", "interstitial");
        if (ad1 != null) {
            mAd1 = new InterstitialAdProvider(ad1);
        }
        int interval = SharedConstants.DEFAULT_PAGE_SIZE;
        mAd2 = AdsUtil.findByLocationAndType("player", "native");
        if (mAd2 != null) {
            interval = mAd2.getInterval();
        }
        params.putInt(ClipDataSource.PARAM_COUNT, interval);
        long seen = Prefs.getLong(SharedConstants.PREF_CLIPS_SEEN_UNTIL, 0);
        params.putLong(ClipDataSource.PARAM_SEEN, seen);
        Prefs.putLong(SharedConstants.PREF_CLIPS_SEEN_UNTIL, System.currentTimeMillis());
        Set<String> languages = Prefs.getStringSet(SharedConstants.PREF_PREFERRED_LANGUAGES, null);
        if (languages != null && !languages.isEmpty()) {
            params.putStringArrayList(ClipDataSource.PARAM_LANGUAGES, new ArrayList<>(languages));
        }

        PlayerSliderFragmentViewModel.Factory factory =
                new PlayerSliderFragmentViewModel.Factory(params, mAd2 != null);
        mModel = new ViewModelProvider(this, factory).get(PlayerSliderFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.v(TAG, "Player slider view is being created.");
        return inflater.inflate(R.layout.fragment_player_slider, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Player slider fragment is being destroyed.");
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackEndedEvent(PlaybackEndedEvent event) {
        Log.v(TAG, "Playback for clip #" + event.getClip() + " has ended.");
        try {
            if (!mModel.scrolling) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            }
        } catch (Exception ignore) {
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResetPlayerSliderEvent(ResetPlayerSliderEvent event) {
        ClipPageDataSource source = mModel.factory.source.getValue();
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
        PlayerSliderAdapter adapter = new PlayerSliderAdapter(this);
        mPager = view.findViewById(R.id.pager);
        mPager.setAdapter(adapter);
        mModel.clips.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            if (mModel.current > 0) {
                mPager.setCurrentItem(mModel.current, false);
            }
        });
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            ClipPageDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            Log.v(TAG, "Loading state is " + state.name() + ".");
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        try {
            RecyclerView rv = (RecyclerView) mPager.getChildAt(0);
            //noinspection ConstantConditions
            rv.getLayoutManager().setItemPrefetchEnabled(false);
            rv.setItemViewCacheSize(0);
        } catch (Exception ignore) {
        }

        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.v(TAG, "Selected page is " + position + "; previous was " + mModel.current + ".");
                if (mModel.current != position) {
                    VisibilityAware hidden =
                            (VisibilityAware) adapter.getFragmentByPosition(mModel.current);
                    if (hidden != null) {
                        hidden.setVisibleOrNot(false);
                    }
                }

                VisibilityAware visible =
                        (VisibilityAware) adapter.getFragmentByPosition(position);
                if (visible != null) {
                    visible.setVisibleOrNot(true);
                }

                mModel.current = position;
                if (getResources().getBoolean(R.bool.prefetching_enabled)) {
                    Clip next = null;
                    try {
                        next = adapter.getItem(position + 1);
                    } catch (Exception ignore) {
                    }

                    if (next != null) {
                        Log.v(TAG, "Pre-fetching clip #" + next.id);
                        HttpProxyCacheServer proxy =
                                MainApplication.getContainer().get(HttpProxyCacheServer.class);
                        CacheUtil.prefetch(proxy, next.screenshot);
                        CacheUtil.prefetch(proxy, next.video);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mModel.scrolling = state == ViewPager2.SCROLL_STATE_DRAGGING;
            }
        });
        if (mAd1 != null) {
            Runnable show = mAd1.create(requireContext());
            if (show != null) {
                mDisposable = RxViewPager2.pageScrollEvents(mPager)
                        .throttleFirst(250, TimeUnit.MILLISECONDS)
                        .distinct(PageScrollEvent::getPosition)
                        .subscribe(event -> {
                            Log.v(TAG, "Scrolled page is " + event.getPosition() + ".");
                            if (mModel.viewed++ >= mAd1.getInterval()) {
                                mModel.viewed = 0;
                                mHandler.postDelayed(show, 250);
                            }
                        });
            }
        }
    }

    public static PlayerSliderFragment newInstance(@Nullable Bundle params) {
        PlayerSliderFragment fragment = new PlayerSliderFragment();
        Bundle arguments = new Bundle();
        arguments.putBundle(ARG_PARAMS, params);
        fragment.setArguments(arguments);
        return fragment;
    }

    private class PlayerSliderAdapter extends FragmentStateAdapter {

        private final AsyncPagedListDiffer<Clip> mDiffer;

        public PlayerSliderAdapter(@NonNull Fragment fragment) {
            super(fragment);
            mDiffer = new AsyncPagedListDiffer<>(
                    new AdapterListUpdateCallback(this),
                    new AsyncDifferConfig.Builder<>(new DiffUtilCallback<Clip>(x -> x.id)).build()
            );
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Clip clip = getItem(position);
            if (clip.ad) {
                return NativeAdFragment.newInstance(mAd2);
            }

            return PlayerFragment.newInstance(clip);
        }

        @Nullable
        public Fragment getFragmentByPosition(int position) {
            return getChildFragmentManager().findFragmentByTag("f" + getItemId(position));
        }

        public Clip getItem(int position) {
            return mDiffer.getItem(position);
        }

        @Override
        public int getItemCount() {
            return mDiffer.getItemCount();
        }

        public void submitList(PagedList<Clip> list) {
            mDiffer.submitList(list);
        }
    }

    public static class PlayerSliderFragmentViewModel extends ViewModel {

        public PlayerSliderFragmentViewModel(@NonNull Bundle params, boolean ads) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(params.getInt(ClipDataSource.PARAM_COUNT))
                    .build();
            factory = new ClipPageDataSource.Factory(params, ads);
            state = Transformations.switchMap(factory.source, input -> input.state);
            clips = new LivePagedListBuilder<>(factory, config).build();
        }

        public int current = 0;
        public final LiveData<PagedList<Clip>> clips;
        public final ClipPageDataSource.Factory factory;
        public final LiveData<LoadingState> state;
        public boolean scrolling = false;
        public int viewed = 0;

        private static class Factory implements ViewModelProvider.Factory {

            private final boolean mAds;
            @NonNull private final Bundle mParams;

            public Factory(@NonNull Bundle params, boolean ads) {
                mParams = params;
                mAds = ads;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new PlayerSliderFragmentViewModel(mParams, mAds);
            }
        }
    }
}
