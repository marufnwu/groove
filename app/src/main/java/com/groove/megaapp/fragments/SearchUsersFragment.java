package com.groove.megaapp.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.UserDataSource;
import com.groove.megaapp.data.models.User;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class SearchUsersFragment extends Fragment {

    private SearchUsersFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
        SearchUsersFragmentViewModel.Factory factory =
                new SearchUsersFragmentViewModel.Factory(mModel2.searchTerm.getValue());
        mModel1 = new ViewModelProvider(this, factory)
                .get(SearchUsersFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UserAdapter adapter = new UserAdapter();
        RecyclerView users = view.findViewById(R.id.users);
        users.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        users.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        OverScrollDecoratorHelper.setUpOverScroll(
                users, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel1.users.observe(getViewLifecycleOwner(), adapter::submitList);
        TextView empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel1.users.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        mModel2.searchTerm.observe(getViewLifecycleOwner(), q -> {
            mModel1.factory.q = q;
            UserDataSource source = mModel1.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
    }

    public static SearchUsersFragment newInstance() {
        return new SearchUsersFragment();
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    public static class SearchUsersFragmentViewModel extends ViewModel {

        public SearchUsersFragmentViewModel(@Nullable String q) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new UserDataSource.Factory(q);
            state = Transformations.switchMap(factory.source, input -> input.state);
            users = new LivePagedListBuilder<>(factory, config).build();
        }

        public final UserDataSource.Factory factory;
        public final LiveData<PagedList<User>> users;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final String mQ;

            public Factory(String q) {
                mQ = q;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new SearchUsersFragmentViewModel(mQ);
            }
        }
    }

    private class UserAdapter extends PagedListAdapter<User, UserViewHolder> {

        protected UserAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            final User user = getItem(position);
            if (TextUtils.isEmpty(user.photo)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(user.photo);
            }

            holder.name.setText(user.name);
            holder.username.setText("@" + user.username);
            holder.verified.setVisibility(user.verified ? View.VISIBLE : View.GONE);
            holder.follow.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> showProfile(user.id));
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(root);
        }
    }

    private static class UserViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView name;
        public TextView username;
        public ImageView verified;
        public View follow;

        public UserViewHolder(@NonNull View root) {
            super(root);
            photo = root.findViewById(R.id.photo);
            name = root.findViewById(R.id.name);
            username = root.findViewById(R.id.username);
            verified = root.findViewById(R.id.verified);
            follow = root.findViewById(R.id.follow);
        }
    }
}
