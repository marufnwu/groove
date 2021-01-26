package com.groove.megaapp.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Hashtag;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HashtagDataSource extends PageKeyedDataSource<Integer, Hashtag> {

    private static final String TAG = "HashtagDataSource";

    private final String mQ;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public HashtagDataSource(@Nullable String q) {
        mQ = q;
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, Hashtag> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.hashtagsIndex(mQ, 1)
                .enqueue(new Callback<Wrappers.Paginated<Hashtag>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Hashtag>> call,
                            @Nullable Response<Wrappers.Paginated<Hashtag>> response
                    ) {
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Hashtag> hashtags = response.body();
                            callback.onResult(hashtags.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Hashtag>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching hashtags has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Hashtag> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, Hashtag> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.hashtagsIndex(mQ, params.key)
                .enqueue(new Callback<Wrappers.Paginated<Hashtag>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Hashtag>> call,
                            @Nullable Response<Wrappers.Paginated<Hashtag>> response
                    ) {
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Hashtag> hashtags = response.body();
                            callback.onResult(hashtags.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Hashtag>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching hashtags has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Hashtag> {

        public String q;

        public MutableLiveData<HashtagDataSource> source = new MutableLiveData<>();

        public Factory(String q) {
            this.q = q;
        }

        @NonNull
        @Override
        public DataSource<Integer, Hashtag> create() {
            HashtagDataSource source = new HashtagDataSource(q);
            this.source.postValue(source);
            return source;
        }
    }
}
