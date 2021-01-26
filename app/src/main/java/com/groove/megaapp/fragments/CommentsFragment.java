package com.groove.megaapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.vanniktech.emoji.EmojiPopup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.groove.megaapp.MainApplication;
import com.groove.megaapp.R;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.activities.MainActivity;
import com.groove.megaapp.activities.StickerPickerActivity;
import com.groove.megaapp.common.DiffUtilCallback;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.ClipDataSource;
import com.groove.megaapp.data.CommentDataSource;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Comment;
import com.groove.megaapp.data.models.Sticker;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.utils.AutocompleteUtil;
import com.groove.megaapp.utils.SocialSpanUtil;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentsFragment extends Fragment implements SocialSpanUtil.OnSocialLinkClickListener {

    public static final String ARG_CLIP = "clip";
    private static final String TAG = "CommentFragment";

    private int mClip;

    private EmojiPopup mEmoji;
    private CommentFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SharedConstants.REQUEST_CODE_PICK_STICKER && resultCode == Activity.RESULT_OK && data != null) {
            Sticker sticker = data.getParcelableExtra(StickerPickerActivity.EXTRA_STICKER);
            try {
                JSONObject json = new JSONObject();
                json.put("sticker", sticker.id);
                submitComment(json.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Could not encode sticker comment.", e);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClip = requireArguments().getInt(ARG_CLIP);
        CommentFragmentViewModel.Factory factory = new CommentFragmentViewModel.Factory(mClip);
        mModel1 = new ViewModelProvider(this, factory).get(CommentFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                if (mEmoji.isShowing()) {
                    mEmoji.dismiss();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments, container, false);
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
    public void onStop() {
        super.onStop();
        mEmoji.dismiss();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.header_back)
                .setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.comments_label);
        view.findViewById(R.id.header_more).setVisibility(View.GONE);
        RecyclerView comments = view.findViewById(R.id.comments);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true);
        lm.setStackFromEnd(true);
        comments.setLayoutManager(lm);
        CommentsAdapter adapter = new CommentsAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int last = lm.findLastCompletelyVisibleItemPosition();
                if (last == -1 || positionStart >= adapter.getItemCount() - 1 && last == positionStart - 1) {
                    comments.scrollToPosition(positionStart);
                }
            }
        });
        comments.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        OverScrollDecoratorHelper.setUpOverScroll(
                comments, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel1.comments.observe(getViewLifecycleOwner(), adapter::submitList);
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel1.comments.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        View composer = view.findViewById(R.id.composer);
        EditText input = composer.findViewById(R.id.input);
        MaterialButton emoji = composer.findViewById(R.id.emoji);
        mEmoji = EmojiPopup.Builder.fromRootView(view)
                .setOnEmojiPopupDismissListener(() -> emoji.setIcon(ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_outline_emoji_emotions_24)))
                .setOnEmojiPopupShownListener(() -> emoji.setIcon(ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_baseline_keyboard_24)))
                .build(input);
        emoji.setOnClickListener(v -> mEmoji.toggle());
        emoji.setVisibility(getResources().getBoolean(R.bool.emoji_keyboard_enabled) ? View.VISIBLE : View.GONE);
        View sticker = composer.findViewById(R.id.sticker);
        sticker.setOnClickListener(v -> {
            if (!mModel2.isLoggedIn()) {
                Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
                return ;
            }

            Intent intent = new Intent(requireContext(), StickerPickerActivity.class);
            startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_STICKER);
        });
        sticker.setVisibility(getResources().getBoolean(R.bool.stickers_enabled) ? View.VISIBLE : View.GONE);
        composer.findViewById(R.id.submit).setOnClickListener(v -> {
            if (!mModel2.isLoggedIn()) {
                Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
                return;
            }

            Editable message = input.getText();
            if (TextUtils.isEmpty(message)) {
                return;
            }

            submitComment(message);
            input.setText(null);
        });
        SocialSpanUtil.apply(input, "", null);
        if (getResources().getBoolean(R.bool.autocomplete_enabled)) {
            AutocompleteUtil.setupForHashtags(requireContext(), input);
            AutocompleteUtil.setupForUsers(requireContext(), input);
        }
    }

    private void confirmDeletion(int comment) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirmation_delete_comment)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                    dialog.dismiss();
                    deleteComment(comment);
                })
                .show();
    }

    private void deleteComment(int comment) {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.commentsDelete(mClip, comment)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Deleting comment returned " + code + '.');
                        if (code == 200) {
                            CommentDataSource source = mModel1.factory.source.getValue();
                            if (source != null) {
                                source.invalidate();
                            }
                        }
                        progress.dismiss();
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to delete selected comment.", t);
                        progress.dismiss();
                    }
                });
    }

    public static CommentsFragment newInstance(int clip) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_CLIP, clip);
        fragment.setArguments(arguments);
        return fragment;
    }

    @SuppressLint("SetTextI18n")
    private void prepareReply(Comment comment) {
        String text = "@" + comment.user.username + " ";
        //noinspection ConstantConditions
        View composer = getView().findViewById(R.id.composer);
        EditText input = composer.findViewById(R.id.input);
        input.setText(text);
        input.setSelection(text.length());
        input.requestFocus();
        InputMethodManager imm =
                ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        //noinspection ConstantConditions
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    private void reportComment(int comment) {
        ((MainActivity)requireActivity()).reportSubject("comment", comment);
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private void submitComment(CharSequence text) {
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.commentsCreate(mClip, text.toString())
                .enqueue(new Callback<Wrappers.Single<Comment>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Comment>> call,
                            @Nullable Response<Wrappers.Single<Comment>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Submitting comment returned " + code + '.');
                        if (response != null && response.isSuccessful()) {
                            CommentDataSource source = mModel1.factory.source.getValue();
                            if (source != null) {
                                source.invalidate();
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Comment>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to submit comment on clip.", t);
                    }
                });
    }

    private class CommentsAdapter extends PagedListAdapter<Comment, CommentViewHolder> {

        public CommentsAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(root);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment comment = getItem(position);
            //noinspection ConstantConditions
            if (TextUtils.isEmpty(comment.user.photo)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(comment.user.photo);
            }
            holder.username.setText('@' + comment.user.username);
            holder.verified.setVisibility(comment.user.verified ? View.VISIBLE : View.GONE);
            if (comment.sticker != null) {
                holder.text.setVisibility(View.GONE);
                holder.sticker.setVisibility(View.VISIBLE);
                holder.sticker.setImageURI(comment.sticker.image);
            } else {
                holder.text.setVisibility(View.VISIBLE);
                holder.sticker.setVisibility(View.GONE);
                SocialSpanUtil.apply(holder.text, comment.text, CommentsFragment.this);
            }
            holder.when.setText(
                    DateUtils.getRelativeTimeSpanString(
                            requireContext(), comment.createdAt.getTime(), true));
            holder.photo.setOnClickListener(v -> showProfile(comment.user.id));
            holder.username.setOnClickListener(v -> showProfile(comment.user.id));
            holder.reply.setOnClickListener(v -> {
                if (mModel2.isLoggedIn()) {
                    prepareReply(comment);
                } else {
                    Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
                }
            });
            holder.reply.setVisibility(comment.user.me ? View.GONE : View.VISIBLE);
            holder.report.setOnClickListener(v -> {
                if (mModel2.isLoggedIn()) {
                    reportComment(comment.id);
                } else {
                    Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
                }
            });
            holder.report.setVisibility(comment.user.me ? View.GONE : View.VISIBLE);
            holder.delete.setVisibility(comment.user.me ? View.GONE : View.VISIBLE);
            holder.delete.setOnClickListener(v -> {
                if (mModel2.isLoggedIn()) {
                    confirmDeletion(comment.id);
                } else {
                    Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
                }
            });
            holder.delete.setVisibility(comment.user.me ? View.VISIBLE : View.GONE);
        }
    }

    public static class CommentFragmentViewModel extends ViewModel {

        public CommentFragmentViewModel(int clip) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new CommentDataSource.Factory(clip);
            state = Transformations.switchMap(factory.source, input -> input.state);
            comments = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Comment>> comments;
        public final CommentDataSource.Factory factory;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final int mClip;

            public Factory(int clip) {
                mClip = clip;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new CommentFragmentViewModel(mClip);
            }
        }
    }

    private static class CommentViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView username;
        public ImageView verified;
        public TextView text;
        public SimpleDraweeView sticker;
        public TextView when;
        public View reply;
        public View report;
        public View delete;

        public CommentViewHolder(@NonNull View root) {
            super(root);
            username = root.findViewById(R.id.username);
            verified = root.findViewById(R.id.verified);
            photo = root.findViewById(R.id.photo);
            text = root.findViewById(R.id.text);
            sticker = root.findViewById(R.id.sticker);
            when = root.findViewById(R.id.when);
            reply = root.findViewById(R.id.reply);
            report = root.findViewById(R.id.report);
            delete = root.findViewById(R.id.delete);
        }
    }
}
