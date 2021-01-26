package com.groove.megaapp.data;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Clip;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClipPageDataSource extends PageKeyedDataSource<Integer, Clip> implements ClipDataSource {

    private static final String TAG = "ClipPageDataSource";

    private final boolean mAds;
    private final Bundle mParams;
    private final int mSeed = new Random().nextInt(99999 - 1000) + 1000;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public ClipPageDataSource(@NonNull Bundle params, boolean ads) {
        mParams = params;
        mAds = ads;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, Clip> callback
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
        Integer first = mParams.getInt(PARAM_FIRST);
        Long seen = mParams.getLong(PARAM_SEEN);
        Integer count = mParams.getInt(PARAM_COUNT);
        rest.clipsIndex(mine, q, liked, saved, following, user, song, languages, sections, hashtags, mSeed, seen, first, null, null, null, count)
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
                            if (mAds && clips.data.size() == count) {
                                List<Clip> copy = new ArrayList<>(clips.data);
                                copy.add(createDummy());
                                callback.onResult(copy,null, 2);
                            } else {
                                callback.onResult(clips.data,null, 2);
                            }

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
            @NonNull LoadCallback<Integer, Clip> callback
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
        Long seen = mParams.getLong(PARAM_SEEN);
        Integer count = mParams.getInt(PARAM_COUNT);
        rest.clipsIndex(mine, q, liked, saved, following, user, song, languages, sections, hashtags, mSeed, seen, null, null, null, params.key, count)
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
                            if (mAds && clips.data.size() == count) {
                                List<Clip> copy = new ArrayList<>(clips.data);
                                copy.add(0, createDummy());
                                callback.onResult(copy,params.key - 1);
                            } else {
                                callback.onResult(clips.data,params.key - 1);
                            }

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
    @SuppressWarnings("ConstantConditions")
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, Clip> callback
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
        Long seen = mParams.getLong(PARAM_SEEN);
        Integer count = mParams.getInt(PARAM_COUNT);
        rest.clipsIndex(mine, q, liked, saved, following, user, song, languages, sections, hashtags, mSeed, seen, null, null, null, params.key, count)
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
                            if (mAds && clips.data.size() == count) {
                                List<Clip> copy = new ArrayList<>(clips.data);
                                copy.add(createDummy());
                                callback.onResult(copy,params.key + 1);
                            } else {
                                callback.onResult(clips.data,params.key + 1);
                            }

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

    private static Clip createDummy() {
        Clip dummy = new Clip();
        dummy.id = -1;
        dummy.ad = true;
        return dummy;
    }

    public static class Factory extends DataSource.Factory<Integer, Clip> {

        private final boolean mAds;
        @NonNull public Bundle params;

        public MutableLiveData<ClipPageDataSource> source = new MutableLiveData<>();

        public Factory(@NonNull Bundle params, boolean ads) {
            mAds = ads;
            this.params = params;
        }

        @NonNull
        @Override
        public DataSource<Integer, Clip> create() {
            ClipPageDataSource source = new ClipPageDataSource(params, mAds);
            this.source.postValue(source);
            return source;
        }
    }
}
