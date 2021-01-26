package com.groove.megaapp.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.shchurov.particleview.ParticleView;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.activities.RecorderActivity;
import com.groove.megaapp.common.VisibilityAware;
import com.groove.megaapp.data.ClipDataSource;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Clip;
import com.groove.megaapp.data.models.User;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.events.PlaybackEndedEvent;
import com.groove.megaapp.events.ResetPlayerSliderEvent;
import com.groove.megaapp.particles.SpinnerParticleSystem;
import com.groove.megaapp.particles.SpinnerTextureAtlasFactory;
import com.groove.megaapp.utils.AnimationUtil;
import com.groove.megaapp.utils.SizeUtil;
import com.groove.megaapp.utils.SocialSpanUtil;
import com.groove.megaapp.utils.TempUtil;
import com.groove.megaapp.utils.TextFormatUtil;
import com.groove.megaapp.utils.VideoUtil;
import com.groove.megaapp.workers.FixFastStartWorker2;
import com.groove.megaapp.workers.SaveToGalleryWorker;
import com.groove.megaapp.workers.WatermarkWorker;
import okhttp3.ResponseBody;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerFragment extends Fragment implements AnalyticsListener, VisibilityAware, SocialSpanUtil.OnSocialLinkClickListener {

    private static final String ARG_CLIP = "clip";
    private static final String TAG = "PlayerFragment";

    private View mBufferingProgressBar;
    private Call<Wrappers.Single<Clip>> mCall;
    private Clip mClip;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private CheckBox mLikeCheckBox;
    private PlayerFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;
    private View mMusicDisc;
    private SpinnerParticleSystem mParticleSystem;
    private ParticleView mParticleView;
    private View mPlay;
    private SimpleExoPlayer mPlayer;
    private ProgressBar mProgressBar;
    private final Runnable mProgress = new Runnable() {

        @Override
        public void run() {
            long current = mPlayer.getCurrentPosition();
            float progress = mModel1.duration > 0 ? ((float) current / mModel1.duration) * 100 : 0f;
            mProgressBar.setProgress(Math.round(progress));
            mHandler.postDelayed(this, 250);
            mModel1.elapsed += 250;
            if (mModel1.elapsed >= getResources().getInteger(R.integer.clutter_free_playback_delay) && mModel1.visible) {
                toggleVisibility(false);
            }
        }
    };
    private boolean mStopped;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClip = requireArguments().getParcelable(ARG_CLIP);
        Log.v(TAG, "Clip " + mClip + " player is being created.");
        DefaultLoadControl control = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        10 * 1000,
                        20 * 1000,
                        1000,
                        1000)
                .createDefaultLoadControl();
        mPlayer = new SimpleExoPlayer.Builder(requireContext())
                .setLoadControl(control)
                .build();
        mPlayer.addAnalyticsListener(this);
        mModel1 = new ViewModelProvider(this).get(PlayerFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Clip " + mClip + " player is being destroyed.");
        stopPlayer();
        mPlayer.stop(true);
        mPlayer.release();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "Clip " + mClip + " player is being paused.");
        setVisibleOrNot(false);
    }

    @Override
    public void onPlayerStateChanged(@NotNull EventTime time, boolean play, int state) {
        if (mBufferingProgressBar != null) {
            mBufferingProgressBar.setVisibility(
                    state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
        }

        if (state == Player.STATE_READY) {
            mModel1.duration = mPlayer.getDuration();
            Log.v(TAG, "Player video duration is " + mModel1.duration + ".");
        }

        if (state == Player.STATE_ENDED && !mStopped) {
            EventBus.getDefault().post(new PlaybackEndedEvent(mClip.id));
            mStopped = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "Clip " + mClip + " player is being resumed.");
        setVisibleOrNot(true);
        mStopped = false;
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
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBufferingProgressBar = view.findViewById(R.id.buffering);
        mMusicDisc = view.findViewById(R.id.disc);
        mMusicDisc.setOnClickListener(v -> {
            if (mModel2.isLoggedIn()) {
                confirmUseAudio();
            } else {
                ((MainActivity) requireActivity()).showLoginSheet();
            }
        });
        PlayerView player = view.findViewById(R.id.player);
        player.setPlayer(mPlayer);
        mPlay = view.findViewById(R.id.play);
        mProgressBar = view.findViewById(R.id.progress);
        mProgressBar.setVisibility(
                getResources().getBoolean(R.bool.player_progress_enabled)
                        ? View.VISIBLE
                        : View.GONE);
        View overlay = view.findViewById(R.id.overlay);
        GestureDetector detector =
                new GestureDetector(requireContext(), new PlayerGestureListener());
        overlay.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
        View report = view.findViewById(R.id.report);
        report.setOnClickListener(v -> {
            if (mModel2.isLoggedIn()) {
                ((MainActivity)requireActivity()).reportSubject("clip", mClip.id);
            } else {
                ((MainActivity)requireActivity()).showLoginSheet();
            }
        });
        report.setVisibility(mClip.user.me ? View.GONE : View.VISIBLE);
        View edit = view.findViewById(R.id.edit);
        edit.setOnClickListener(v -> ((MainActivity) requireActivity()).showEditClip(mClip));
        edit.setVisibility(mClip.user.me ? View.VISIBLE : View.GONE);
        View delete = view.findViewById(R.id.delete);
        delete.setOnClickListener(v -> {
            if (mModel2.isLoggedIn()) {
                confirmDeletion();
            } else {
                ((MainActivity) requireActivity()).showLoginSheet();
            }
        });
        delete.setVisibility(mClip.user.me ? View.VISIBLE : View.GONE);
        TextView views = view.findViewById(R.id.views);
        views.setText(TextFormatUtil.toShortNumber(mClip.viewsCount));
        TextView likes = view.findViewById(R.id.likes);
        likes.setText(TextFormatUtil.toShortNumber(mClip.likesCount));
        mLikeCheckBox = view.findViewById(R.id.like);
        mLikeCheckBox.setChecked(mClip.liked);
        mLikeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                if (mModel2.isLoggedIn()) {
                    likeUnlike(checked);
                } else {
                    mLikeCheckBox.setOnCheckedChangeListener(null);
                    mLikeCheckBox.setChecked(false);
                    mLikeCheckBox.setOnCheckedChangeListener(this);
                    ((MainActivity) requireActivity()).showLoginSheet();
                }
            }
        });
        TextView comments = view.findViewById(R.id.comments);
        comments.setText(TextFormatUtil.toShortNumber(mClip.commentsCount));
        comments.setVisibility(mClip.comments ? View.VISIBLE : View.GONE);
        View comment = view.findViewById(R.id.comment);
        comment.setOnClickListener(v -> showComments());
        comment.setVisibility(mClip.comments ? View.VISIBLE : View.GONE);
        CheckBox save = view.findViewById(R.id.save);
        save.setChecked(mClip.saved);
        save.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                if (mModel2.isLoggedIn()) {
                    saveUnsave(checked);
                } else {
                    save.setOnCheckedChangeListener(null);
                    save.setChecked(false);
                    save.setOnCheckedChangeListener(this);
                    ((MainActivity) requireActivity()).showLoginSheet();
                }
            }
        });
        View share = view.findViewById(R.id.share);
        if (getResources().getBoolean(R.bool.sharing_enabled)) {
            share.setOnClickListener(v ->
                    ((MainActivity)requireActivity()).showSharingOptions(mClip));
            share.setVisibility(View.VISIBLE);
        } else {
            share.setVisibility(View.GONE);
        }

        View download = view.findViewById(R.id.download);
        if (getResources().getBoolean(R.bool.downloads_enabled)) {
            download.setOnClickListener(v -> {
                if (EasyPermissions.hasPermissions(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    submitForDownload();
                } else {
                    EasyPermissions.requestPermissions(
                            this,
                            getString(R.string.permission_rationale_download),
                            SharedConstants.REQUEST_CODE_PERMISSIONS_DOWNLOAD,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            });
            download.setVisibility(View.VISIBLE);
        } else {
            download.setVisibility(View.GONE);
        }

        SimpleDraweeView photo = view.findViewById(R.id.photo);
        photo.setOnClickListener(v -> showProfile());
        if (TextUtils.isEmpty(mClip.user.photo)) {
            photo.setActualImageResource(R.drawable.photo_placeholder);
        } else {
            photo.setImageURI(mClip.user.photo);
        }

        ImageView following = view.findViewById(R.id.following);
        following.setImageResource(
                mClip.user.followed ? R.drawable.ic_following : R.drawable.ic_follow2);
        following.setOnClickListener(v -> {
            if (mModel2.isLoggedIn() && !mClip.user.me && !mClip.user.followed) {
                followUser();
            } else {
                showProfile();
            }
        });
        TextView follow = view.findViewById(R.id.follow);
        follow.setOnClickListener(v -> {
            if (mModel2.isLoggedIn() && !mClip.user.me && !mClip.user.followed) {
                followUser();
            } else {
                showProfile();
            }
        });
        follow.setText(mClip.user.followed ? R.string.following_label : R.string.follow_label);
        view.findViewById(R.id.verified)
                .setVisibility(mClip.user.verified ? View.VISIBLE : View.GONE);
        TextView username = view.findViewById(R.id.username);
        username.setOnClickListener(v -> showProfile());
        username.setText('@' + mClip.user.username);
        TextView song = view.findViewById(R.id.song);
        song.setSelected(true);
        if (mClip.song != null) {
            song.setOnClickListener(v -> {
                Bundle params = new Bundle();
                params.putInt(ClipDataSource.PARAM_SONG, mClip.song.id);
                ((MainActivity) requireActivity()).showClips(mClip.song.title, params);
            });
            song.setText(mClip.song.title);
        } else {
            song.setOnClickListener(null);
            song.setText(R.string.original_audio);
        }

        TextView description = view.findViewById(R.id.description);
        description.setText(mClip.description);
        description.setVisibility(TextUtils.isEmpty(mClip.description) ? View.GONE : View.VISIBLE);
        SocialSpanUtil.apply(description, mClip.description, this);
        View tagsw = view.findViewById(R.id.tags_wrapper);
        ChipGroup tags = view.findViewById(R.id.tags);
        boolean chips = getResources().getBoolean(R.bool.tags_chips_enabled);
        if (!chips || (mClip.mentions.isEmpty() && mClip.hashtags.isEmpty())) {
            tagsw.setVisibility(View.GONE);
        } else {
            tagsw.setVisibility(View.VISIBLE);
            tags.removeAllViews();
            for (User user : mClip.mentions) {
                Chip chip = new Chip(requireContext());
                chip.setOnClickListener(v -> showProfile(user.id));
                chip.setText("@" + user.username);
                tags.addView(chip);
                if (TextUtils.isEmpty(user.photo)) {
                    Glide.with(requireContext())
                            .load(R.drawable.photo_placeholder)
                            .circleCrop()
                            .into(new ChipTarget(chip));
                } else {
                    Glide.with(requireContext())
                            .load(user.photo)
                            .placeholder(R.drawable.photo_placeholder)
                            .circleCrop()
                            .into(new ChipTarget(chip));
                }
            }

            for (String hashtag : mClip.hashtags) {
                Chip chip = new Chip(requireContext());
                chip.setOnClickListener(v -> onSocialHashtagClick("#" + hashtag));
                chip.setText("#" + hashtag);
                tags.addView(chip);
            }
        }

        TextView location = view.findViewById(R.id.location);
        if (getResources().getBoolean(R.bool.locations_enabled) && !TextUtils.isEmpty(mClip.location)) {
            location.setText(mClip.location);
            location.setVisibility(View.VISIBLE);
        } else {
            location.setVisibility(View.GONE);
        }

        HttpProxyCacheServer cache = MainApplication.getContainer().get(HttpProxyCacheServer.class);
        DefaultDataSourceFactory factory =
                new DefaultDataSourceFactory(requireContext(), getString(R.string.app_name));
        MediaSource source = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(Uri.parse(cache.getProxyUrl(mClip.video)));
        if (!getResources().getBoolean(R.bool.auto_scroll_enabled)) {
            source = new LoopingMediaSource(source);
        }

        mPlayer.setPlayWhenReady(false);
        mPlayer.prepare(source, false, false);
        mParticleSystem = new SpinnerParticleSystem(SizeUtil.toPx(getResources(), 35));
        mParticleView = view.findViewById(R.id.particles);
        mParticleView.setParticleSystem(mParticleSystem);
        mParticleView.setTextureAtlasFactory(new SpinnerTextureAtlasFactory(requireContext()));
        View spacer = view.findViewById(R.id.spacer);
        boolean immersive = getResources().getBoolean(R.bool.immersive_mode_enabled);
        spacer.setVisibility(immersive ? View.VISIBLE : View.GONE);
    }

    private void confirmDeletion() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirmation_delete_clip)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                    dialog.dismiss();
                    deleteClip();
                })
                .show();
    }

    private void confirmUseAudio() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirmation_use_audio)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                    dialog.dismiss();
                    submitForUseAudio();
                })
                .show();
    }

    private void deleteClip() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.clipsDelete(mClip.id)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Deleting clip returned " + code + '.');
                        progress.dismiss();
                        if (code == 200) {
                            EventBus.getDefault().post(new ResetPlayerSliderEvent());
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to delete selected clip.", t);
                        progress.dismiss();
                    }
                });
    }

    private void followUser() {
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.followersFollow(mClip.user.id)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Updating follow/unfollow returned " + code + '.');
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to update follow/unfollow user.", t);
                    }
                });
        mClip.user.followed = true;
        mClip.user.followersCount++;
        ImageView following = getView().findViewById(R.id.following);
        TextView follow = getView().findViewById(R.id.follow);
        following.setImageResource(R.drawable.ic_following);
        follow.setText(R.string.following_label);
    }

    private void likeUnlike(boolean like) {
        REST rest = MainApplication.getContainer().get(REST.class);
        Call<ResponseBody> call;
        if (like) {
            call = rest.likesLike(mClip.id);
        } else {
            call = rest.likesUnlike(mClip.id);
        }

        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Response<ResponseBody> response
            ) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Updating like/unlike returned " + code + '.');
            }

            @Override
            public void onFailure(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed to update like/unlike status.", t);
            }
        });
        if (like) {
            mClip.likesCount++;
        } else {
            mClip.likesCount--;
        }

        mClip.liked = like;
        TextView likes = getView().findViewById(R.id.likes);
        likes.setText(TextFormatUtil.toShortNumber(mClip.likesCount));
        if (like) {
            showLikeAnimation();
        }
    }

    private void recordView() {
        if (mCall != null) {
            mCall.cancel();
        }

        REST rest = MainApplication.getContainer().get(REST.class);
        mCall = rest.clipsShow(mClip.id);
        mCall.enqueue(new Callback<Wrappers.Single<Clip>>() {

            @Override
            public void onResponse(
                    @Nullable Call<Wrappers.Single<Clip>> call,
                    @Nullable Response<Wrappers.Single<Clip>> response
            ) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Recording clip view on server returned " + code + '.');
            }

            @Override
            public void onFailure(
                    @Nullable Call<Wrappers.Single<Clip>> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed when record clip view on server.", t);
            }
        });
    }

    public static PlayerFragment newInstance(Clip clip) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_CLIP, clip);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void saveUnsave(boolean save) {
        REST rest = MainApplication.getContainer().get(REST.class);
        Call<ResponseBody> call;
        if (save) {
            call = rest.savesSave(mClip.id);
        } else {
            call = rest.savesUnsave(mClip.id);
        }

        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Response<ResponseBody> response
            ) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Updating save/unsave returned " + code + '.');
            }

            @Override
            public void onFailure(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed to update save/unsave status.", t);
            }
        });
        mClip.saved = save;
    }

    @Override
    public void setVisibleOrNot(boolean visible) {
        if (visible && !mPlayer.getPlayWhenReady()) {
            startPlayer();
            if (!mModel1.viewed) {
                recordView();
                mModel1.viewed = true;
            }
        } else if (!visible) {
            stopPlayer();
            if (mCall != null) {
                mCall.cancel();
            }
        }
    }

    private void showComments() {
        ((MainActivity)requireActivity()).showCommentsPage(mClip.id);
    }

    private void showLikeAnimation() {
        View heart = getView().findViewById(R.id.heart);
        heart.setAlpha(1f);
        heart.setScaleX(1f);
        heart.setScaleY(1f);
        heart.animate()
                .alpha(0f)
                .scaleXBy(1.25f)
                .scaleYBy(1.25f)
                .withStartAction(() -> heart.setVisibility(View.VISIBLE))
                .withEndAction(() -> heart.setVisibility(View.GONE))
                .start();
        if (getResources().getBoolean(R.bool.haptic_feedback_enabled)) {
            Vibrator vibrator =
                    (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }

    private void showProfile() {
        showProfile(mClip.user.id);
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private void startDiscAnimation() {
        if (!isAdded()) {
            return;
        }

        mMusicDisc.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_360));
        mParticleSystem.show(mParticleView.getWidth() / 2, mParticleView.getHeight() / 2);
        mParticleView.startRendering();
    }

    private void startPlayer() {
        if (mPlayer.getPlaybackState() == Player.STATE_ENDED) {
            mPlayer.seekTo(mPlayer.getCurrentWindowIndex(), 0);
        }

        mPlayer.setPlayWhenReady(true);
        mPlay.setVisibility(View.GONE);
        mHandler.postDelayed(this::startDiscAnimation, 500);
        if (getResources().getBoolean(R.bool.player_progress_enabled)) {
            mHandler.postDelayed(mProgress, 250);
        }
    }

    private void stopDiscAnimation() {
        mMusicDisc.clearAnimation();
        mParticleSystem.dismiss();
        mParticleView.stopRendering();
    }

    private void stopPlayer() {
        mPlayer.setPlayWhenReady(false);
        mPlay.setVisibility(View.VISIBLE);
        stopDiscAnimation();
        mHandler.removeCallbacks(mProgress);
    }

    @AfterPermissionGranted(SharedConstants.REQUEST_CODE_PERMISSIONS_DOWNLOAD)
    private void submitForDownload() {
        WorkManager wm = WorkManager.getInstance(requireContext());
        File fixed = TempUtil.createNewFile(requireContext(), ".mp4");
        boolean async = getResources().getBoolean(R.bool.downloads_async_enabled);
        String name = "VDO_" + mClip.id + "_" + System.currentTimeMillis() + ".mp4";
        Data data1 = new Data.Builder()
                .putString(SaveToGalleryWorker.KEY_FILE, fixed.getAbsolutePath())
                .putString(SaveToGalleryWorker.KEY_NAME, name)
                .putBoolean(WatermarkWorker.KEY_NOTIFICATION, async)
                .build();
        OneTimeWorkRequest request1 = new OneTimeWorkRequest.Builder(SaveToGalleryWorker.class)
                .setInputData(data1)
                .build();
        if (getResources().getBoolean(R.bool.downloads_watermark_enabled)) {
            File original = TempUtil.createNewFile(requireContext(), ".mp4");
            File watermarked = TempUtil.createNewFile(requireContext(), ".mp4");
            Data data2 = new Data.Builder()
                    .putString(FixFastStartWorker2.KEY_INPUT, watermarked.getAbsolutePath())
                    .putString(FixFastStartWorker2.KEY_OUTPUT, fixed.getAbsolutePath())
                    .build();
            OneTimeWorkRequest request2 = new OneTimeWorkRequest.Builder(FixFastStartWorker2.class)
                    .setInputData(data2)
                    .build();
            wm.beginWith(VideoUtil.createDownloadRequest(mClip, original, async))
                    .then(VideoUtil.createWatermarkRequest(mClip, original, watermarked, async))
                    .then(request2)
                    .then(request1)
                    .enqueue();
        } else {
            wm.beginWith(VideoUtil.createDownloadRequest(mClip, fixed, async))
                    .then(request1)
                    .enqueue();
        }

        if (async) {
            Toast.makeText(requireContext(), R.string.message_downloading_async, Toast.LENGTH_SHORT).show();
        } else {
            stopPlayer();
            KProgressHUD progress = KProgressHUD.create(requireActivity())
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setLabel(getString(R.string.progress_title))
                    .setCancellable(false)
                    .show();
            wm.getWorkInfoByIdLiveData(request1.getId())
                    .observe(getViewLifecycleOwner(), info -> {
                        boolean ended = info.getState() == WorkInfo.State.CANCELLED
                                || info.getState() == WorkInfo.State.FAILED
                                || info.getState() == WorkInfo.State.SUCCEEDED;
                        if (ended) {
                            progress.dismiss();
                        }

                        if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            Toast.makeText(requireContext(), R.string.message_clip_downloaded, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void submitForUseAudio() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        File downloaded = TempUtil.createNewFile(requireContext(), ".mp4");
        OneTimeWorkRequest request = VideoUtil.createDownloadRequest(mClip, downloaded, false);
        WorkManager wm = WorkManager.getInstance(requireContext());
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(getViewLifecycleOwner(), info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        progress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        Intent intent = new Intent(requireContext(), RecorderActivity.class);
                        intent.putExtra(RecorderActivity.EXTRA_AUDIO, Uri.fromFile(downloaded));
                        startActivity(intent);
                    }
                });
    }

    private void toggleVisibility(boolean show) {
        if (!getResources().getBoolean(R.bool.clutter_free_playback_enabled)) {
            return;
        }

        View right = getView().findViewById(R.id.right);
        AnimationUtil.toggleVisibilityToRight(right, show);
        View bottom = getView().findViewById(R.id.bottom);
        bottom.setVisibility(show ? View.VISIBLE : View.GONE);
        View left = getView().findViewById(R.id.left);
        AnimationUtil.toggleVisibilityToLeft(left, show);
        mModel1.visible = show;
    }

    private class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.v(TAG, "Received double-tap event on player overlay.");
            if (mClip.liked) {
                showLikeAnimation();
            } else {
                mLikeCheckBox.setChecked(true);
            }

            return super.onDoubleTap(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.v(TAG, "Received single-tap event on player overlay.");
            boolean playing = mPlayer.getPlayWhenReady()
                    && mPlayer.getPlaybackState() != Player.STATE_IDLE;
            if (playing) {
                stopPlayer();
                toggleVisibility(true);
            } else {
                startPlayer();
                toggleVisibility(false);
            }

            return super.onSingleTapConfirmed(e);
        }
    }

    public static class PlayerFragmentViewModel extends ViewModel {

        public long duration;
        public long elapsed;
        public boolean viewed = false;
        public boolean visible = true;
    }

    private static class ChipTarget extends CustomViewTarget<Chip, Drawable> {

        private final Chip mChip;

        public ChipTarget(@NonNull Chip chip) {
            super(chip);
            mChip = chip;
        }

        @Override
        public void onLoadFailed(@Nullable Drawable drawable) {
            mChip.setChipIcon(drawable);
        }

        @Override
        public void onResourceReady(
                @NonNull Drawable resource,
                @Nullable Transition<? super Drawable> transition
        ) {
            mChip.setChipIcon(resource);
        }

        @Override
        protected void onResourceCleared(@Nullable Drawable placeholder) {
            mChip.setChipIcon(placeholder);
        }
    }
}
