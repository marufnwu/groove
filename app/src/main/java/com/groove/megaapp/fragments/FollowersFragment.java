package com.groove.megaapp.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.FollowerDataSource;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.User;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowersFragment extends Fragment {

    public static final String ARG_USER = "user";
    public static final String ARG_FOLLOWING = "following";
    private static final String TAG = "FollowersFragment";

    private boolean mFollowing;
    private FollowerFollowingFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int user = requireArguments().getInt(ARG_USER);
        mFollowing = requireArguments().getBoolean(ARG_FOLLOWING);
        FollowerFollowingFragmentViewModel.Factory factory =
                new FollowerFollowingFragmentViewModel.Factory(user, mFollowing);
        mModel1 = new ViewModelProvider(this, factory)
                .get(FollowerFollowingFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_followers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView title = view.findViewById(R.id.header_title);
        title.setText(mFollowing ? R.string.following_label : R.string.followers_label);
        view.findViewById(R.id.header_back)
                .setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        view.findViewById(R.id.header_more)
                .setVisibility(View.GONE);
        FollowerAdapter adapter = new FollowerAdapter();
        RecyclerView followers = view.findViewById(R.id.followers);
        followers.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        followers.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        OverScrollDecoratorHelper.setUpOverScroll(
                followers, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel1.followers.observe(getViewLifecycleOwner(), adapter::submitList);
        TextView empty = view.findViewById(R.id.empty);
        empty.setText(mFollowing ? R.string.empty_followings : R.string.empty_followers);
        View loading = view.findViewById(R.id.loading);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel1.followers.getValue();
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

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    public static FollowersFragment newInstance(int user, boolean following) {
        FollowersFragment fragment = new FollowersFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_USER, user);
        arguments.putBoolean(ARG_FOLLOWING, following);
        fragment.setArguments(arguments);
        return fragment;
    }

    private class FollowerAdapter extends PagedListAdapter<User, FollowerViewHolder> {

        protected FollowerAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull FollowerViewHolder holder, int position) {
            final User follower = getItem(position);
            if (TextUtils.isEmpty(follower.photo)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(follower.photo);
            }
            holder.name.setText(follower.name);
            holder.username.setText("@" + follower.username);
            holder.verified.setVisibility(follower.verified ? View.VISIBLE : View.GONE);
            holder.follow.setOnClickListener(v -> {
                followUser(follower.id);
                follower.followed = true;
                v.setVisibility(View.GONE);
            });
            boolean followable = mModel2.isLoggedIn() && !follower.me && !follower.followed;
            holder.follow.setVisibility(followable ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> showProfile(follower.id));
        }

        @NonNull
        @Override
        public FollowerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_user, parent, false);
            return new FollowerViewHolder(root);
        }
    }

    public static class FollowerFollowingFragmentViewModel extends ViewModel {

        public FollowerFollowingFragmentViewModel(int user, boolean following) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            FollowerDataSource.Factory factory = new FollowerDataSource.Factory(user, following);
            state = Transformations.switchMap(factory.source, input -> input.state);
            followers = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<User>> followers;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final int mUser;
            private final boolean mFollowing;

            public Factory(int user, boolean following) {
                mUser = user;
                mFollowing = following;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new FollowerFollowingFragmentViewModel(mUser, mFollowing);
            }
        }
    }

    private static class FollowerViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView name;
        public TextView username;
        public ImageView verified;
        public Button follow;

        public FollowerViewHolder(@NonNull View root) {
            super(root);
            photo = root.findViewById(R.id.photo);
            name = root.findViewById(R.id.name);
            username = root.findViewById(R.id.username);
            verified = root.findViewById(R.id.verified);
            follow = root.findViewById(R.id.follow);
        }
    }
}
