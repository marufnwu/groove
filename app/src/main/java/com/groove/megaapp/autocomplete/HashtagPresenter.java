package com.groove.megaapp.autocomplete;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.otaliastudios.autocomplete.RecyclerViewPresenter;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Hashtag;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HashtagPresenter extends RecyclerViewPresenter<Hashtag> {

    private static final String TAG = "HashtagPresenter";

    private HashtagAdapter mAdapter;
    private Call<Wrappers.Paginated<Hashtag>> mCall;
    private final Context mContext;

    public HashtagPresenter(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected HashtagAdapter instantiateAdapter() {
        return mAdapter = new HashtagAdapter(mContext, this::dispatchClick);
    }

    @Override
    protected void onQuery(@Nullable CharSequence q) {
        Log.v(TAG, "Querying '" + q + "' for hashtags autocomplete.");
        if (mCall != null) {
            mCall.cancel();
        }

        REST rest = MainApplication.getContainer().get(REST.class);
        mCall = rest.hashtagsIndex(q != null ? q.toString() : null, 1);
        mCall.enqueue(new Callback<Wrappers.Paginated<Hashtag>>() {

            @Override
            public void onResponse(
                    @Nullable Call<Wrappers.Paginated<Hashtag>> call,
                    @Nullable Response<Wrappers.Paginated<Hashtag>> response
            ) {
                Log.v(TAG, "Server responded with " + response.code() + " status.");
                if (response.isSuccessful()) {
                    Wrappers.Paginated<Hashtag> hashtags = response.body();
                    mAdapter.submitData(hashtags.data);
                }
            }

            @Override
            public void onFailure(
                    @Nullable Call<Wrappers.Paginated<Hashtag>> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Fetching hashtags has failed.", t);
            }
        });
    }
}
