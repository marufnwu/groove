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
import com.groove.megaapp.data.models.Comment;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentDataSource extends PageKeyedDataSource<Integer, Comment> {

    private static final String TAG = "CommentDataSource";

    private final int mClip;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public CommentDataSource(int clip) {
        mClip = clip;
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, Comment> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.commentsIndex(mClip, 1)
                .enqueue(new Callback<Wrappers.Paginated<Comment>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Comment>> call,
                            @Nullable Response<Wrappers.Paginated<Comment>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Comment> comments = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(comments.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Comment>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching comments has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Comment> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, Comment> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.commentsIndex(mClip, params.key)
                .enqueue(new Callback<Wrappers.Paginated<Comment>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Comment>> call,
                            @Nullable Response<Wrappers.Paginated<Comment>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Comment> comments = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(comments.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Comment>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching comments has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Comment> {

        private final int mClip;

        public MutableLiveData<CommentDataSource> source = new MutableLiveData<>();

        public Factory(int clip) {
            mClip = clip;
        }

        @NonNull
        @Override
        public DataSource<Integer, Comment> create() {
            CommentDataSource source = new CommentDataSource(mClip);
            this.source.postValue(source);
            return source;
        }
    }
}
