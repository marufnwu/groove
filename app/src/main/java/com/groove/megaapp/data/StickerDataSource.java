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
import com.groove.megaapp.data.models.Sticker;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StickerDataSource extends PageKeyedDataSource<Integer, Sticker> {

    private static final String TAG = "StickerDataSource";

    @Nullable private final Iterable<Integer> mSections;
    @Nullable private final String mQ;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public StickerDataSource(@Nullable Iterable<Integer> sections, @Nullable String q) {
        mSections = sections;
        mQ = q;
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, Sticker> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.stickersIndex(mQ, mSections, 1)
                .enqueue(new Callback<Wrappers.Paginated<Sticker>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Sticker>> call,
                            @Nullable Response<Wrappers.Paginated<Sticker>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Sticker> stickers = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(stickers.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Sticker>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching stickers has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Sticker> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, Sticker> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.stickersIndex(mQ, mSections, params.key)
                .enqueue(new Callback<Wrappers.Paginated<Sticker>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Sticker>> call,
                            @Nullable Response<Wrappers.Paginated<Sticker>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Sticker> stickers = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(stickers.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Sticker>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching stickers has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Sticker> {

        @Nullable public String q;
        @Nullable public Iterable<Integer> sections;

        public MutableLiveData<StickerDataSource> source = new MutableLiveData<>();

        public Factory(@Nullable Iterable<Integer> sections, @Nullable String q) {
            this.sections = sections;
            this.q = q;
        }

        @NonNull
        @Override
        public DataSource<Integer, Sticker> create() {
            StickerDataSource source = new StickerDataSource(sections, q);
            this.source.postValue(source);
            return source;
        }
    }
}
