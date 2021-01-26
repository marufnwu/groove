package com.groove.megaapp.data;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;

import java.util.Collections;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Clip;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClipItemDataSource extends ItemKeyedDataSource<Integer, Clip> implements ClipDataSource {

    private static final String TAG = "ClipItemDataSource";

    private final Bundle mParams;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public ClipItemDataSource(@NonNull Bundle params) {
        mParams = params;
    }

    @NonNull
    @Override
    public Integer getKey(@NonNull Clip item) {
        return item.id;
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Clip> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        Boolean mine = mParams.getBoolean(PARAM_MINE);
        String q = mParams.getString(PARAM_Q);
        Boolean liked = mParams.getBoolean(PARAM_LIKED);
        Boolean saved = mParams.getBoolean(PARAM_SAVED);
        Boolean following = mParams.getBoolean(PARAM_FOLLOWING);
        Integer user = mParams.getInt(PARAM_USER);
        Integer song = mParams.getInt(PARAM_SONG);
        Iterable<String> languages = mParams.getStringArrayList(PARAM_LANGUAGES);
        Iterable<Integer> sections = mParams.getIntegerArrayList(PARAM_SECTIONS);
        Iterable<String> hashtags = mParams.getStringArrayList(PARAM_HASHTAGS);
        Integer count = mParams.getInt(PARAM_COUNT);
        rest.clipsIndex(mine, q, liked, saved, following, user, song, languages, sections, hashtags, null, null, params.requestedInitialKey, null, null, null, count)
                .enqueue(new Callback<Wrappers.Paginated<Clip>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Clip>> call,
                            @Nullable Response<Wrappers.Paginated<Clip>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Clip> clips = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(clips.data);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Clip>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching clips has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Clip> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        Boolean mine = mParams.getBoolean(PARAM_MINE);
        String q = mParams.getString(PARAM_Q);
        Boolean liked = mParams.getBoolean(PARAM_LIKED);
        Boolean saved = mParams.getBoolean(PARAM_SAVED);
        Boolean following = mParams.getBoolean(PARAM_FOLLOWING);
        Integer user = mParams.getInt(PARAM_USER);
        Integer song = mParams.getInt(PARAM_SONG);
        Iterable<String> languages = mParams.getStringArrayList(PARAM_LANGUAGES);
        Iterable<Integer> sections = mParams.getIntegerArrayList(PARAM_SECTIONS);
        Iterable<String> hashtags = mParams.getStringArrayList(PARAM_HASHTAGS);
        Integer count = mParams.getInt(PARAM_COUNT);
        rest.clipsIndex(mine, q, liked, saved, following, user, song, languages, sections, hashtags, null, null, null, params.key, null, null, count)
                .enqueue(new Callback<Wrappers.Paginated<Clip>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Clip>> call,
                            @Nullable Response<Wrappers.Paginated<Clip>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Clip> clips = response.body();
                            //noinspection ConstantConditions
                            Collections.reverse(clips.data);
                            callback.onResult(clips.data);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Clip>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching clips has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Clip> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        Boolean mine = mParams.getBoolean(PARAM_MINE);
        String q = mParams.getString(PARAM_Q);
        Boolean liked = mParams.getBoolean(PARAM_LIKED);
        Boolean saved = mParams.getBoolean(PARAM_SAVED);
        Boolean following = mParams.getBoolean(PARAM_FOLLOWING);
        Integer user = mParams.getInt(PARAM_USER);
        Integer song = mParams.getInt(PARAM_SONG);
        Iterable<String> languages = mParams.getStringArrayList(PARAM_LANGUAGES);
        Iterable<Integer> sections = mParams.getIntegerArrayList(PARAM_SECTIONS);
        Iterable<String> hashtags = mParams.getStringArrayList(PARAM_HASHTAGS);
        Integer count = mParams.getInt(PARAM_COUNT);
        rest.clipsIndex(mine, q, liked, saved, following, user, song, languages, sections, hashtags, null, null, null, null, params.key, null, count)
                .enqueue(new Callback<Wrappers.Paginated<Clip>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Clip>> call,
                            @Nullable Response<Wrappers.Paginated<Clip>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Clip> clips = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(clips.data);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Clip>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching clips has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Clip> {

        @NonNull public Bundle params;

        public MutableLiveData<ClipItemDataSource> source = new MutableLiveData<>();

        public Factory(@NonNull Bundle params) {
            this.params = params;
        }

        @NonNull
        @Override
        public DataSource<Integer, Clip> create() {
            ClipItemDataSource source = new ClipItemDataSource(params);
            this.source.postValue(source);
            return source;
        }
    }
}
