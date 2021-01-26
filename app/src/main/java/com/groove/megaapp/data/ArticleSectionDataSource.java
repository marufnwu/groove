package com.groove.megaapp.data;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.ArticleSection;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("LongLogTag")
public class ArticleSectionDataSource extends PageKeyedDataSource<Integer, ArticleSection> {

    private static final String TAG = "ArticleSectionDataSource";

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, ArticleSection> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.articleSectionsIndex(null, 1)
                .enqueue(new Callback<Wrappers.Paginated<ArticleSection>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<ArticleSection>> call,
                            @Nullable Response<Wrappers.Paginated<ArticleSection>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<ArticleSection> sections = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(sections.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<ArticleSection>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching article sections has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, ArticleSection> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, ArticleSection> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.articleSectionsIndex(null, params.key)
                .enqueue(new Callback<Wrappers.Paginated<ArticleSection>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<ArticleSection>> call,
                            @Nullable Response<Wrappers.Paginated<ArticleSection>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<ArticleSection> sections = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(sections.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<ArticleSection>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching article sections has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, ArticleSection> {

        public MutableLiveData<ArticleSectionDataSource> source = new MutableLiveData<>();

        @NonNull
        @Override
        public DataSource<Integer, ArticleSection> create() {
            ArticleSectionDataSource source = new ArticleSectionDataSource();
            this.source.postValue(source);
            return source;
        }
    }
}
