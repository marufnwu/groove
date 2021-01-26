package com.groove.megaapp.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.util.ArrayList;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.ClipDataSource;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Thread;
import com.groove.megaapp.data.models.User;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.utils.PackageUtil;
import com.groove.megaapp.utils.SizeUtil;
import com.groove.megaapp.utils.SocialSpanUtil;
import com.groove.megaapp.utils.TextFormatUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment implements SocialSpanUtil.OnSocialLinkClickListener {

    private static final String ARG_USER = "user";
    private static final String TAG = "ProfileFragment";

    private ProfileFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;
    private Integer mUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = requireArguments().getInt(ARG_USER, 0);
        if (mUser <= 0) {
            mUser = null;
        }

        mModel1 = new ViewModelProvider(this).get(ProfileFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity()).get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        ViewPager2 pager = getView().findViewById(R.id.pager);
        LoadingState state = mModel1.state.getValue();
        User user = mModel1.user.getValue();
        if ((mModel2.isProfileInvalid || user == null) && state != LoadingState.LOADING) {
            loadUser();
        } else if (user != null && pager.getAdapter() == null) {
            showClipsGrid(user);
        }
    }

    @Override
    public void onSocialHashtagClick(String hashtag) {
        Log.v(TAG, "User clicked hashtag: " + hashtag);
        ArrayList<String> hashtags = new ArrayList<>();
        hashtags.add(hashtag.substring(1));
        Bundle params = new Bundle();
        params.putStringArrayList(ClipDataSource.PARAM_HASHTAGS, hashtags);
        ((MainActivity) requireActivity()).showClips(hashtag, params);
    }

    @Override
    public void onSocialMentionClick(String username) {
        Log.v(TAG, "User clicked username: " + username);
        ((MainActivity) requireActivity()).showProfilePage(username.substring(1));
    }

    @Override
    public void onSocialUrlClick(String url) {
        Log.v(TAG, "User clicked URL: " + url);
        ((MainActivity) requireActivity()).showUrlBrowser(url, null, false);
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        back.setVisibility(mUser == null ? View.GONE : View.VISIBLE);
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.profile_label);
        ImageButton extra = view.findViewById(R.id.header_extra);
        extra.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_flag_24));
        extra.setOnClickListener(v -> reportUser());
        View more = view.findViewById(R.id.header_more);
        more.setOnClickListener(v -> {
            int menu = R.menu.guest_profile_menu;
            User user = mModel1.user.getValue();
            if (user != null && mModel2.isLoggedIn()) {
                menu = user.verified ? R.menu.auth_profile_menu_verified : R.menu.auth_profile_menu;
            }

            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.edit:
                        showEditor();
                        break;
                    case R.id.share:
                        shareProfile();
                        break;
                    case R.id.verification:
                        showRequestVerification();
                        break;
                    case R.id.settings:
                        ((MainActivity)requireActivity()).showAbout();
                        break;
                }

                return true;
            });
            popup.show();
        });
        SimpleDraweeView photo = view.findViewById(R.id.photo);
        photo.setOnClickListener(v -> {
            User user = mModel1.user.getValue();
            if (user != null && !TextUtils.isEmpty(user.photo)) {
                ((MainActivity)requireActivity())
                        .showPhotoViewer("@" + user.username, Uri.parse(user.photo));
            }
        });
        View actions = view.findViewById(R.id.actions);
        actions.setVisibility(mUser == null ? View.GONE : View.VISIBLE);
        MaterialButton follow = view.findViewById(R.id.follow);
        follow.setOnClickListener(v -> {
            if (mModel2.isLoggedIn()) {
                followUnfollowUser();
            } else {
                Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
            }
        });
        View chat = view.findViewById(R.id.chat);
        chat.setOnClickListener(v -> {
            if (mModel2.isLoggedIn()) {
                startChat();
            } else {
                Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
            }
        });
        view.findViewById(R.id.followers_count)
                .setOnClickListener(v -> showFollowerFollowing(false));
        view.findViewById(R.id.followed_count)
                .setOnClickListener(v -> showFollowerFollowing(true));
        View loading = view.findViewById(R.id.loading);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            if (state == LoadingState.ERROR) {
                Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        View qr = view.findViewById(R.id.qr);
        qr.setOnClickListener(v -> {
            User user = mModel1.user.getValue();
            if (user != null) {
                ((MainActivity)requireActivity()).showQrSheet(user);
            }
        });
        mModel1.user.observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                return;
            }

            extra.setVisibility(user.me ? View.GONE : View.VISIBLE);
            if (TextUtils.isEmpty(user.photo)) {
                photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                photo.setImageURI(user.photo);
            }

            TextView name = view.findViewById(R.id.name);
            name.setText(user.name);
            TextView username = view.findViewById(R.id.username);
            username.setText('@' + user.username);
            view.findViewById(R.id.verified)
                    .setVisibility(user.verified ? View.VISIBLE : View.GONE);
            TextView location = view.findViewById(R.id.location);
            if (getResources().getBoolean(R.bool.locations_enabled) && !TextUtils.isEmpty(user.location)) {
                location.setText(user.location);
                location.setVisibility(View.VISIBLE);
            } else {
                location.setVisibility(View.GONE);
            }

            TextView bio = view.findViewById(R.id.bio);
            SocialSpanUtil.apply(bio, user.bio, this);
            bio.setVisibility(TextUtils.isEmpty(user.bio) ? View.GONE : View.VISIBLE);
            if (getResources().getBoolean(R.bool.profile_clips_count_enabled)) {
                TextView clips = view.findViewById(R.id.clips);
                clips.setText(TextFormatUtil.toShortNumber(user.clipsCount));
                view.findViewById(R.id.clips_count).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.clips_count).setVisibility(View.GONE);
            }

            TextView views = view.findViewById(R.id.views);
            views.setText(TextFormatUtil.toShortNumber(user.viewsCount));
            TextView likes = view.findViewById(R.id.likes);
            likes.setText(TextFormatUtil.toShortNumber(user.likesCount));
            TextView followers = view.findViewById(R.id.followers);
            followers.setText(TextFormatUtil.toShortNumber(user.followersCount));
            TextView followed = view.findViewById(R.id.followed);
            followed.setText(TextFormatUtil.toShortNumber(user.followedCount));
            actions.setVisibility(user.me ? View.GONE : View.VISIBLE);
            follow.setIconResource(user.followed ? R.drawable.ic_unfollow : R.drawable.ic_follow);
            follow.setText(user.followed ? R.string.unfollow_label : R.string.follow_label);
            LinearLayout links = view.findViewById(R.id.links);
            if (user.links == null || user.links.isEmpty()) {
                links.setVisibility(View.GONE);
            } else {
                links.removeAllViews();
                for (User.UserLink link : user.links) {
                    Log.v(TAG, "Link " + link.type + ": " + link.url);
                    ImageView icon = new ImageView(requireContext());
                    if (TextUtils.equals(link.type, "facebook")) {
                        icon.setImageResource(R.drawable.ic_facebook);
                        icon.setContentDescription(getString(R.string.facebook_label));
                    } else if (TextUtils.equals(link.type, "instagram")) {
                        icon.setImageResource(R.drawable.ic_instagram);
                        icon.setContentDescription(getString(R.string.instagram_label));
                    } else if (TextUtils.equals(link.type, "linkedin")) {
                        icon.setImageResource(R.drawable.ic_linkedin);
                        icon.setContentDescription(getString(R.string.linkedin_label));
                    } else if (TextUtils.equals(link.type, "snapchat")) {
                        icon.setImageResource(R.drawable.ic_snapchat);
                        icon.setContentDescription(getString(R.string.snapchat_label));
                    } else if (TextUtils.equals(link.type, "tiktok")) {
                        icon.setImageResource(R.drawable.ic_tiktok);
                        icon.setContentDescription(getString(R.string.tiktok_label));
                    } else if (TextUtils.equals(link.type, "twitter")) {
                        icon.setImageResource(R.drawable.ic_twitter);
                        icon.setContentDescription(getString(R.string.twitter_label));
                    } else if (TextUtils.equals(link.type, "youtube")) {
                        icon.setImageResource(R.drawable.ic_youtube);
                        icon.setContentDescription(getString(R.string.youtube_label));
                    }

                    icon.setOnClickListener(v -> {
                        if (link.url.startsWith("http")) {
                            onSocialUrlClick(link.url);
                        } else {
                            openRelatedApp(link);
                        }
                    });
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            SizeUtil.toPx(getResources(), 25),
                            SizeUtil.toPx(getResources(), 25)
                    );
                    params.setMarginStart(SizeUtil.toPx(getResources(), 5));
                    params.setMarginEnd(SizeUtil.toPx(getResources(), 5));
                    links.addView(icon, params);
                }

                links.setVisibility(View.VISIBLE);
            }

            qr.setVisibility(user.me ? View.VISIBLE : View.GONE);
            showClipsGrid(user);
        });
    }

    private void followUnfollowUser() {
        User user = mModel1.user.getValue();
        if (user == null) {
            return;
        }

        REST rest = MainApplication.getContainer().get(REST.class);
        Call<ResponseBody> call;
        if (user.followed) {
            call = rest.followersUnfollow(user.id);
        } else {
            call = rest.followersFollow(user.id);
        }

        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Response<ResponseBody> response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Updating follow/unfollow returned " + code + '.');
            }

            @Override
            public void onFailure(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Throwable t) {
                Log.e(TAG, "Failed to update follow/unfollow user.", t);
            }
        });
        if (user.followed) {
            user.followersCount--;
        } else {
            user.followersCount++;
        }

        user.followed = !user.followed;
        mModel1.user.postValue(user);
    }

    private void loadUser() {
        mModel1.state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        Call<Wrappers.Single<User>> call;
        if (mUser == null) {
            call = rest.profileShow();
        } else {
            call = rest.usersShow(mUser);
        }

        call.enqueue(new Callback<Wrappers.Single<User>>() {

            @Override
            public void onResponse(
                    @Nullable Call<Wrappers.Single<User>> call,
                    @Nullable Response<Wrappers.Single<User>> response
            ) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Fetching user profile returned " + code + '.');
                if (code == 200) {
                    mModel1.user.postValue(response.body().data);
                    mModel2.isProfileInvalid = false;
                    mModel1.state.postValue(LoadingState.LOADED);
                } else {
                    mModel1.state.postValue(LoadingState.ERROR);
                }
            }

            @Override
            public void onFailure(
                    @Nullable Call<Wrappers.Single<User>> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed when trying to retrieve profile.", t);
                mModel1.state.postValue(LoadingState.ERROR);
            }
        });
    }

    public static ProfileFragment newInstance(@Nullable Integer user) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle arguments = new Bundle();
        if (user != null) {
            arguments.putInt(ARG_USER, user);
        }

        fragment.setArguments(arguments);
        return fragment;
    }

    private void openRelatedApp(User.UserLink link) {
        if (TextUtils.equals(link.type, "facebook")) {
            Intent intent =
                    new Intent(Intent.ACTION_VIEW,
                            Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/" + link.url));
            intent.setPackage("com.facebook.katana");
            if (PackageUtil.isIntentNotResolvable(requireContext(), intent)) {
                intent =
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://facebook.com/" + link.url));
            }

            startActivity(intent);
        } else if (TextUtils.equals(link.type, "instagram")) {
            Intent intent =
                    new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://instagram.com/_u/" + link.url));
            intent.setPackage("com.instagram.android");
            if (PackageUtil.isIntentNotResolvable(requireContext(), intent)) {
                intent =
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://instagram.com/" + link.url));
            }

            startActivity(intent);
        } else if (TextUtils.equals(link.type, "linkedin")) {
            Intent intent =
                    new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.linkedin.com/in/" + link.url));
            intent.setPackage("com.linkedin.android");
            if (PackageUtil.isIntentNotResolvable(requireContext(), intent)) {
                intent =
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.linkedin.com/in/" + link.url));
            }

            startActivity(intent);
        } else if (TextUtils.equals(link.type, "snapchat")) {
            Uri uri = Uri.parse("https://snapchat.com/add/" + link.url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.snapchat.android");
            if (PackageUtil.isIntentNotResolvable(requireContext(), intent)) {
                intent = new Intent(Intent.ACTION_VIEW, uri);
            }

            startActivity(intent);
        } else if (TextUtils.equals(link.type, "tiktok")) {
            Intent intent =
                    new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://tiktok.com/@" + link.url));
            intent.setPackage("com.zhiliaoapp.musically");
            if (PackageUtil.isIntentNotResolvable(requireContext(), intent)) {
                intent =
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://tiktok.com/@" + link.url));
            }

            startActivity(intent);
        } else if (TextUtils.equals(link.type, "twitter")) {
            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=" + link.url));
            intent.setPackage("com.twitter.android");
            if (PackageUtil.isIntentNotResolvable(requireContext(), intent)) {
                intent =
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://twitter.com/" + link.url));
            }

            startActivity(intent);
        } else if (TextUtils.equals(link.type, "youtube")) {
            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW, Uri.parse("https://youtube.com/channel/" + link.url));
            intent.setPackage("com.google.android.youtube");
            if (PackageUtil.isIntentNotResolvable(requireContext(), intent)) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/channel/" + link.url));
            }

            startActivity(intent);
        }
    }

    private void reportUser() {
        User user = mModel1.user.getValue();
        if (user == null) {
            return;
        }

        if (mModel2.isLoggedIn()) {
            ((MainActivity)requireActivity()).reportSubject("user", user.id);
        } else {
            ((MainActivity) requireActivity()).showLoginSheet();
        }
    }

    private void shareProfile() {
        User user = mModel1.user.getValue();
        if (user == null) {
            return;
        }

        ((MainActivity) requireActivity()).showSharingOptions(user);
    }

    private void showEditor() {
        User user = mModel1.user.getValue();
        if (user != null) {
            ((MainActivity)requireActivity()).showEditProfile();
        }
    }

    private void showFollowerFollowing(boolean following) {
        User user = mModel1.user.getValue();
        if (user != null) {
            ((MainActivity)requireActivity()).showFollowerFollowing(user.id, following);
        }
    }

    private void showClipsGrid(User user) {
        TabLayout tabs = getView().findViewById(R.id.tabs);
        ViewPager2 pager = getView().findViewById(R.id.pager);
        pager.setAdapter(new ProfilePagerAdapter(user, this));
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_video_library_24));
                    break;
                case 1:
                    tab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_favorite_24));
                    break;
                case 2:
                    tab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_book_24));
                    break;
                case 3:
                    tab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_warning_24));
                    break;
            }
        }).attach();
    }

    private void showRequestVerification() {
        User user = mModel1.user.getValue();
        if (user != null) {
            ((MainActivity)requireActivity()).showRequestVerification();
        }
    }

    private void startChat() {
        User user = mModel1.user.getValue();
        if (user == null) {
            return;
        } else if (user.blocked) {
            Toast.makeText(requireContext(), R.string.message_blocked, Toast.LENGTH_SHORT).show();
            return;
        } else if (user.blocking) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.confirmation_unblock_user)
                    .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                    .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                        dialog.dismiss();
                        unblockUser();
                    })
                    .show();
        }

        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.threadsCreate(user.id)
                .enqueue(new Callback<Wrappers.Single<Thread>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Thread>> call,
                            @Nullable Response<Wrappers.Single<Thread>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Fetching chat thread returned " + code + '.');
                        if (response != null && response.isSuccessful()) {
                            Thread thread = response.body().data;
                            ((MainActivity)requireActivity())
                                    .showMessages("@" + user.username, thread.id);
                        }
                        progress.dismiss();
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Thread>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to start chat.", t);
                        progress.dismiss();
                    }
                });
    }

    private void unblockUser() {
        User user = mModel1.user.getValue();
        REST rest = MainApplication.getContainer().get(REST.class);
        Call<ResponseBody> call = rest.blockedUnblock(user.id);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Response<ResponseBody> response
            ) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Unblocking user returned " + code + '.');
                if (response != null && response.isSuccessful()) {
                    user.blocking = false;
                    mModel1.user.postValue(user);
                    startChat();
                }
            }

            @Override
            public void onFailure(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed to unblock user.", t);
            }
        });
    }

    public static class ProfileFragmentViewModel extends ViewModel {

        public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);
        public final MutableLiveData<User> user = new MutableLiveData<>();
    }

    private static class ProfilePagerAdapter extends FragmentStateAdapter {

        private final User mUser;

        public ProfilePagerAdapter(User user, @NonNull Fragment fragment) {
            super(fragment);
            mUser = user;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle params = new Bundle();
            switch (position) {
                case 3:
                    return DraftGridFragment.newInstance();
                case 2:
                    params.putBoolean(ClipDataSource.PARAM_SAVED, true);
                    break;
                case 1:
                    params.putBoolean(ClipDataSource.PARAM_LIKED, true);
                    break;
                default:
                    if (mUser.me) {
                        params.putBoolean(ClipDataSource.PARAM_MINE, true);
                    } else {
                        params.putInt(ClipDataSource.PARAM_USER, mUser.id);
                    }
            }

            return ClipGridFragment.newInstance(params, null, false);
        }

        @Override
        public int getItemCount() {
            return mUser.me ? 4 : 1;
        }
    }
}
