package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.ads.BannerAdProvider;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.NotificationDataSource;
import com.groove.megaapp.data.models.Advertisement;
import com.groove.megaapp.data.models.Notification;
import com.groove.megaapp.utils.AdsUtil;

public class NotificationsFragment extends Fragment {

    private BannerAdProvider mAd;
    private NotificationFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Advertisement ad = AdsUtil.findByLocationAndType("notifications", "banner");
        if (ad != null) {
            mAd = new BannerAdProvider(ad);
        }
        mModel = new ViewModelProvider(this).get(NotificationFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.header_back).setVisibility(View.INVISIBLE);
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.notifications_label);
        ImageButton messages = view.findViewById(R.id.header_more);
        messages.setImageResource(R.drawable.ic_baseline_message_24);
        messages.setOnClickListener(c -> ((MainActivity)requireActivity()).showThreads());
        NotificationAdapter adapter = new NotificationAdapter();
        RecyclerView notifications = view.findViewById(R.id.notifications);
        notifications.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        notifications.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        mModel.notifications.observe(getViewLifecycleOwner(), adapter::submitList);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            NotificationDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }
            List<?> list = mModel.notifications.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        if (mAd != null) {
            View ad = mAd.create(requireContext());
            if (ad != null) {
                LinearLayout banner = view.findViewById(R.id.banner);
                banner.removeAllViews();
                banner.addView(ad);
            }
        }
    }

    public static NotificationsFragment newInstance() {
        return new NotificationsFragment();
    }

    private void showPlayerSlider(int clip) {
        ((MainActivity)requireActivity()).showPlayerSlider(clip, null);
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }
    
    private class NotificationAdapter extends PagedListAdapter<Notification, NotificationViewHolder> {

        protected NotificationAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            Notification notification = getItem(position);
            //noinspection ConstantConditions
            if (notification.user != null) {
                if (TextUtils.isEmpty(notification.user.photo)) {
                    holder.photo.setActualImageResource(R.drawable.photo_placeholder);
                } else {
                    holder.photo.setImageURI(notification.user.photo);
                }
                holder.photo.setOnClickListener(v -> showProfile(notification.user.id));
            } else {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
                holder.photo.setOnClickListener(null);
            }
            String username = notification.user != null ? notification.user.username : getString(R.string.deleted);
            if (TextUtils.equals(notification.type, "commented_on_your_clip")) {
                holder.content.setText(getString(R.string.notification_commented_on_your_clip, username));
            } else if (TextUtils.equals(notification.type, "liked_your_clip")) {
                holder.content.setText(getString(R.string.notification_liked_your_clip, username));
            } else if (TextUtils.equals(notification.type, "mentioned_you_in_comment")) {
                holder.content.setText(getString(R.string.notification_mentioned_you_in_comment, username));
            } else if (TextUtils.equals(notification.type, "posted_new_clip")) {
                holder.content.setText(getString(R.string.notification_posted_new_clip, username));
            } else if (TextUtils.equals(notification.type, "started_following_you")) {
                holder.content.setText(getString(R.string.notification_started_following_you, username));
            } else if (TextUtils.equals(notification.type, "tagged_you_in_clip")) {
                holder.content.setText(getString(R.string.notification_tagged_you_in_clip, username));
            } else {
                holder.content.setText(getString(R.string.notification_else));
            }
            holder.when.setText(
                    DateUtils.getRelativeTimeSpanString(
                            requireContext(), notification.createdAt.getTime(), true));
            holder.thumbnailContainer.setVisibility(
                    notification.clip != null ? View.VISIBLE : View.GONE);
            if (notification.clip != null) {
                holder.thumbnail.setImageURI(notification.clip.screenshot);
                holder.thumbnail.setOnClickListener(v -> showPlayerSlider(notification.clip.id));
            }
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
    }
    
    public static class NotificationFragmentViewModel extends ViewModel {

        public NotificationFragmentViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new NotificationDataSource.Factory();
            state = Transformations.switchMap(factory.source, input -> input.state);
            notifications = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Notification>> notifications;
        public final NotificationDataSource.Factory factory;
        public final LiveData<LoadingState> state;
    }

    private static class NotificationViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView content;
        public SimpleDraweeView thumbnail;
        public View thumbnailContainer;
        public TextView when;

        public NotificationViewHolder(@NonNull View root) {
            super(root);
            photo = root.findViewById(R.id.photo);
            content = root.findViewById(R.id.content);
            thumbnail = root.findViewById(R.id.thumbnail);
            thumbnailContainer = root.findViewById(R.id.thumbnail_container);
            when = root.findViewById(R.id.when);
        }
    }
}
