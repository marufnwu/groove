package com.groove.megaapp.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.SuggestionDataSource;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.User;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SuggestionsActivity extends AppCompatActivity {

    private static final String TAG = "SuggestionsActivity";

    private SuggestionsActivityViewModel mModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);
        mModel = new ViewModelProvider(this).get(SuggestionsActivityViewModel.class);
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.suggestions_label);
        findViewById(R.id.header_back).setVisibility(View.GONE);
        ImageButton done = findViewById(R.id.header_more);
        done.setImageResource(R.drawable.ic_baseline_check_24);
        done.setOnClickListener(view -> finish());
        SuggestionAdapter adapter = new SuggestionAdapter();
        RecyclerView suggestions = findViewById(R.id.suggestions);
        suggestions.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        suggestions.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        OverScrollDecoratorHelper.setUpOverScroll(
                suggestions, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel.suggestions.observe(this, adapter::submitList);
        TextView empty = findViewById(R.id.empty);
        View loading = findViewById(R.id.loading);
        mModel.state.observe(this, state -> {
            List<?> list = mModel.suggestions.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    private void followUser(int user) {
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.followersFollow(user)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Following user returned " + code + '.');
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t) {
                        Log.e(TAG, "Failed to update follow user.", t);
                    }
                });
    }

    private class SuggestionAdapter extends PagedListAdapter<User, SuggestionViewHolder> {

        protected SuggestionAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
            final User suggestion = getItem(position);
            if (TextUtils.isEmpty(suggestion.photo)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(suggestion.photo);
            }
            holder.name.setText(suggestion.name);
            holder.username.setText("@" + suggestion.username);
            holder.verified.setVisibility(suggestion.verified ? View.VISIBLE : View.GONE);
            holder.follow.setOnClickListener(v -> {
                followUser(suggestion.id);
                suggestion.followed = true;
                v.setVisibility(View.GONE);
            });
            holder.follow.setVisibility(suggestion.followed ? View.GONE : View.VISIBLE);
        }

        @NonNull
        @Override
        public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(SuggestionsActivity.this)
                    .inflate(R.layout.item_user, parent, false);
            return new SuggestionViewHolder(root);
        }
    }

    public static class SuggestionsActivityViewModel extends ViewModel {

        public SuggestionsActivityViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            SuggestionDataSource.Factory factory = new SuggestionDataSource.Factory();
            state = Transformations.switchMap(factory.source, input -> input.state);
            suggestions = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<LoadingState> state;
        public final LiveData<PagedList<User>> suggestions;
    }

    private static class SuggestionViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView name;
        public TextView username;
        public ImageView verified;
        public Button follow;

        public SuggestionViewHolder(@NonNull View root) {
            super(root);
            photo = root.findViewById(R.id.photo);
            name = root.findViewById(R.id.name);
            username = root.findViewById(R.id.username);
            verified = root.findViewById(R.id.verified);
            follow = root.findViewById(R.id.follow);
        }
    }
}
