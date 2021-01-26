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
import com.groove.megaapp.data.models.User;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserDataSource extends PageKeyedDataSource<Integer, User> {

    private static final String TAG = "UserDataSource";

    private final String mQ;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public UserDataSource(@Nullable String q) {
        mQ = q;
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, User> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.usersIndex(mQ, 1)
                .enqueue(new Callback<Wrappers.Paginated<User>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<User>> call,
                            @Nullable Response<Wrappers.Paginated<User>> response
                    ) {
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<User> users = response.body();
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
                        Log.e(TAG, "Fetching users has failed.", t);
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
        rest.usersIndex(mQ, params.key)
                .enqueue(new Callback<Wrappers.Paginated<User>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<User>> call,
                            @Nullable Response<Wrappers.Paginated<User>> response
                    ) {
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<User> users = response.body();
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
                        Log.e(TAG, "Fetching users has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, User> {

        public String q;

        public MutableLiveData<UserDataSource> source = new MutableLiveData<>();

        public Factory(String q) {
            this.q = q;
        }

        @NonNull
        @Override
        public DataSource<Integer, User> create() {
            UserDataSource source = new UserDataSource(q);
            this.source.postValue(source);
            return source;
        }
    }
}
