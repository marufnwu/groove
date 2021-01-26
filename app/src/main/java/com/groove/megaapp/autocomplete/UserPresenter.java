package com.groove.megaapp.autocomplete;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.otaliastudios.autocomplete.RecyclerViewPresenter;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.User;
import com.groove.megaapp.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserPresenter extends RecyclerViewPresenter<User> {

    private static final String TAG = "UserPresenter";

    private UserAdapter mAdapter;
    private Call<Wrappers.Paginated<User>> mCall;
    private final Context mContext;

    public UserPresenter(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected UserAdapter instantiateAdapter() {
        return mAdapter = new UserAdapter(mContext, this::dispatchClick);
    }

    @Override
    protected void onQuery(@Nullable CharSequence q) {
        Log.v(TAG, "Querying '" + q + "' for users autocomplete.");
        if (mCall != null) {
            mCall.cancel();
        }

        REST rest = MainApplication.getContainer().get(REST.class);
        mCall = rest.usersIndex(q != null ? q.toString() : null, 1);
        mCall.enqueue(new Callback<Wrappers.Paginated<User>>() {

            @Override
            public void onResponse(
                    @Nullable Call<Wrappers.Paginated<User>> call,
                    @Nullable Response<Wrappers.Paginated<User>> response
            ) {
                Log.v(TAG, "Server responded with " + response.code() + " status.");
                if (response.isSuccessful()) {
                    Wrappers.Paginated<User> users = response.body();
                    mAdapter.submitData(users.data);
                }
            }

            @Override
            public void onFailure(
                    @Nullable Call<Wrappers.Paginated<User>> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Fetching users has failed.", t);
            }
        });
    }
}
