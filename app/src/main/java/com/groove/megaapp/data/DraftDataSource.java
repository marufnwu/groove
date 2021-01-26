package com.groove.megaapp.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.dbs.ClientDatabase;
import com.groove.megaapp.data.entities.Draft;

public class DraftDataSource extends ItemKeyedDataSource<Integer, Draft> {

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Draft> callback) {
        state.postValue(LoadingState.LOADING);
        ClientDatabase db = MainApplication.getContainer().get(ClientDatabase.class);
        List<Draft> items = db.drafts().findAll(params.requestedLoadSize);
        callback.onResult(items);
        state.postValue(LoadingState.LOADED);

    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Draft> callback) {
        state.postValue(LoadingState.LOADING);
        ClientDatabase db = MainApplication.getContainer().get(ClientDatabase.class);
        List<Draft> items = db.drafts().findAll(params.key, params.requestedLoadSize);
        callback.onResult(items);
        state.postValue(LoadingState.LOADED);
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Draft> callback) {
    }

    @NonNull
    @Override
    public Integer getKey(@NonNull Draft item) {
        return item.id;
    }

    public static class Factory extends DataSource.Factory<Integer, Draft> {

        public MutableLiveData<DraftDataSource> source = new MutableLiveData<>();

        @NotNull
        @Override
        public DataSource<Integer, Draft> create() {
            DraftDataSource source = new DraftDataSource();
            this.source.postValue(source);
            return source;
        }
    }
}
