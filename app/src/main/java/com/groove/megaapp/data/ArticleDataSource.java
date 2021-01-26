package com.groove.megaapp.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import java.util.ArrayList;
import java.util.List;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Article;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArticleDataSource extends PageKeyedDataSource<Integer, Article> {

    private static final String TAG = "ArticleDataSource";

    private final boolean mAds;
    private final int mCount;
    @Nullable private final Iterable<Integer> mSections;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public ArticleDataSource(@Nullable Iterable<Integer> sections, boolean ads, int count) {
        mSections = sections;
        mAds = ads;
        mCount = count;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, Article> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.articlesIndex(null, mSections, 1, mCount)
                .enqueue(new Callback<Wrappers.Paginated<Article>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Article>> call,
                            @Nullable Response<Wrappers.Paginated<Article>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Article> articles = response.body();
                            if (mAds && articles.data.size() == mCount) {
                                List<Article> copy = new ArrayList<>(articles.data);
                                copy.add(createDummy());
                                callback.onResult(copy,null, 2);
                            } else {
                                callback.onResult(articles.data,null, 2);
                            }

                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Article>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching articles has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Article> callback
    ) {
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, Article> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.articlesIndex(null, mSections, params.key, mCount)
                .enqueue(new Callback<Wrappers.Paginated<Article>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Article>> call,
                            @Nullable Response<Wrappers.Paginated<Article>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Article> articles = response.body();
                            if (mAds && articles.data.size() == mCount) {
                                List<Article> copy = new ArrayList<>(articles.data);
                                copy.add(createDummy());
                                callback.onResult(copy,params.key + 1);
                            } else {
                                callback.onResult(articles.data,params.key + 1);
                            }

                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Article>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching articles has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    private static Article createDummy() {
        Article dummy = new Article();
        dummy.id = -1;
        dummy.ad = true;
        return dummy;
    }

    public static class Factory extends DataSource.Factory<Integer, Article> {

        private final boolean mAds;
        private final int mCount;
        @Nullable public Iterable<Integer> sections;

        public MutableLiveData<ArticleDataSource> source = new MutableLiveData<>();

        public Factory(@Nullable Iterable<Integer> sections, boolean ads, int count) {
            this.sections = sections;
            mAds = ads;
            mCount = count;
        }

        @NonNull
        @Override
        public DataSource<Integer, Article> create() {
            ArticleDataSource source = new ArticleDataSource(sections, mAds, mCount);
            this.source.postValue(source);
            return source;
        }
    }
}
