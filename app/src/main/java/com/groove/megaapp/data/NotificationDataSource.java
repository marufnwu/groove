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
import com.groove.megaapp.data.models.Notification;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationDataSource extends PageKeyedDataSource<Integer, Notification> {

    private static final String TAG = "NotificationDataSource";

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Notification> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.notificationsIndex(1)
                .enqueue(new Callback<Wrappers.Paginated<Notification>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Notification>> call,
                            @Nullable Response<Wrappers.Paginated<Notification>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Notification> notifications = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(notifications.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Notification>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching notifications has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Notification> callback
    ) {
    }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Notification> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.notificationsIndex(params.key)
                .enqueue(new Callback<Wrappers.Paginated<Notification>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Notification>> call,
                            @Nullable Response<Wrappers.Paginated<Notification>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Notification> notifications = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(notifications.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Notification>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching notifications has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Notification> {

        public MutableLiveData<NotificationDataSource> source = new MutableLiveData<>();

        @NonNull
        @Override
        public DataSource<Integer, Notification> create() {
            NotificationDataSource source = new NotificationDataSource();
            this.source.postValue(source);
            return source;
        }
    }
}
