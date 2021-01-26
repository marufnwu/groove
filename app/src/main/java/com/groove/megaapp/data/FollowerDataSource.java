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
import com.groove.megaapp.data.models.User;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("LongLogTag")
public class FollowerDataSource extends PageKeyedDataSource<Integer, User> {

    private static final String TAG = "FollowerDataSource";

    private final int mUser;
    private final boolean mFollowing;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public FollowerDataSource(int user, boolean following) {
        mUser = user;
        mFollowing = following;
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, User> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.followersIndex(mUser, mFollowing, 1)
                .enqueue(new Callback<Wrappers.Paginated<User>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<User>> call,
                            @Nullable Response<Wrappers.Paginated<User>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<User> users = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(users.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<User>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching followers has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, User> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, User> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.followersIndex(mUser, mFollowing, params.key)
                .enqueue(new Callback<Wrappers.Paginated<User>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<User>> call,
                            @Nullable Response<Wrappers.Paginated<User>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<User> users = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(users.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<User>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching followers has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, User> {

        private final int mUser;
        private final boolean mFollowing;

        public MutableLiveData<FollowerDataSource> source = new MutableLiveData<>();

        public Factory(int user, boolean following) {
            mUser = user;
            mFollowing = following;
        }

        @NonNull
        @Override
        public DataSource<Integer, User> create() {
            FollowerDataSource source = new FollowerDataSource(mUser, mFollowing);
            this.source.postValue(source);
            return source;
        }
    }
}
