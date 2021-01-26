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
import com.groove.megaapp.data.models.Message;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageDataSource extends PageKeyedDataSource<Integer, Message> {

    private static final String TAG = "MessageDataSource";

    private final int mClip;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public MessageDataSource(int clip) {
        mClip = clip;
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, Message> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.messagesIndex(mClip, 1)
                .enqueue(new Callback<Wrappers.Paginated<Message>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Message>> call,
                            @Nullable Response<Wrappers.Paginated<Message>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Message> messages = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(messages.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Message>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching messages has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Message> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, Message> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.messagesIndex(mClip, params.key)
                .enqueue(new Callback<Wrappers.Paginated<Message>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Message>> call,
                            @Nullable Response<Wrappers.Paginated<Message>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Message> messages = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(messages.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Message>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching messages has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Message> {

        private final int mClip;

        public MutableLiveData<MessageDataSource> source = new MutableLiveData<>();

        public Factory(int clip) {
            mClip = clip;
        }

        @NonNull
        @Override
        public DataSource<Integer, Message> create() {
            MessageDataSource source = new MessageDataSource(mClip);
            this.source.postValue(source);
            return source;
        }
    }
}
