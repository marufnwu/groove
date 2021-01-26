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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import com.vanniktech.emoji.EmojiPopup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
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
import com.groove.megaapp.data.MessageDataSource;
import com.groove.megaapp.data.api.REST;
import com.groove.megaapp.data.models.Message;
import com.groove.megaapp.data.models.Sticker;
import com.groove.megaapp.data.models.Thread;
import com.groove.megaapp.data.models.Wrappers;
import com.groove.megaapp.events.MessageEvent;
import com.groove.megaapp.utils.AutocompleteUtil;
import com.groove.megaapp.utils.SocialSpanUtil;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagesFragment extends Fragment implements SocialSpanUtil.OnSocialLinkClickListener {

    private static final String ARG_THREAD = "thread";
    private static final String ARG_TITLE = "title";
    private static final String TAG = "MessageFragment";

    private int mThread;
    private String mTitle;

    private EmojiPopup mEmoji;
    private MessageFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SharedConstants.REQUEST_CODE_PICK_STICKER && resultCode == Activity.RESULT_OK && data != null) {
            Sticker sticker = data.getParcelableExtra(StickerPickerActivity.EXTRA_STICKER);
            try {
                JSONObject json = new JSONObject();
                json.put("sticker", sticker.id);
                submitMessage(json.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Could not encode sticker message.", e);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThread = requireArguments().getInt(ARG_THREAD, 0);
        mTitle = requireArguments().getString(ARG_TITLE);
        MessageFragmentViewModel.Factory factory =
                new MessageFragmentViewModel.Factory(mThread);
        mModel1 = new ViewModelProvider(this, factory).get(MessageFragmentViewModel.class);
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
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (event.getThread() == mThread) {
            MessageDataSource source = mModel1.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        }
        mModel2.areThreadsInvalid = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        LoadingState state = mModel1.state1.getValue();
        if (state != LoadingState.LOADED && state != LoadingState.LOADING) {
            loadThread();
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
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        mEmoji.dismiss();
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(mTitle);
        ImageView block = view.findViewById(R.id.header_more);
        block.setImageDrawable(
                ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_baseline_remove_circle_24));
        block.setOnClickListener(v -> confirmBlockUnblock());
        RecyclerView messages = view.findViewById(R.id.messages);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true);
        lm.setStackFromEnd(true);
        messages.setLayoutManager(lm);
        MessageAdapter adapter = new MessageAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int last = lm.findLastCompletelyVisibleItemPosition();
                if (last == -1 || positionStart >= adapter.getItemCount() - 1 && last == positionStart - 1) {
                    messages.scrollToPosition(positionStart);
                }
            }
        });
        messages.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        OverScrollDecoratorHelper.setUpOverScroll(
                messages, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        View content = view.findViewById(R.id.content);
        View error = view.findViewById(R.id.error);
        View loading1 = view.findViewById(R.id.loading1);
        mModel1.state1.observe(getViewLifecycleOwner(), state -> {
            content.setVisibility(state == LoadingState.LOADED ? View.VISIBLE : View.GONE);
            error.setVisibility(state == LoadingState.ERROR ? View.VISIBLE : View.GONE);
            loading1.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        TextView blocked = view.findViewById(R.id.blocked);
        mModel1.thread.observe(getViewLifecycleOwner(), thread -> {
            if (thread != null) {
                if (thread.user.blocked && thread.user.blocking) {
                    blocked.setText(R.string.message_blocking_both);
                    blocked.setVisibility(View.VISIBLE);
                } else if (thread.user.blocked) {
                    blocked.setText(R.string.message_blocked);
                    blocked.setVisibility(View.VISIBLE);
                } else if (thread.user.blocking) {
                    blocked.setText(R.string.message_blocking);
                    blocked.setVisibility(View.VISIBLE);
                } else {
                    blocked.setVisibility(View.GONE);
                }
            } else {
                blocked.setVisibility(View.GONE);
            }
        });
        View empty = view.findViewById(R.id.empty);
        View loading2 = view.findViewById(R.id.loading2);
        mModel1.messages.observe(getViewLifecycleOwner(), adapter::submitList);
        mModel1.state2.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel1.messages.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading2.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
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
            Thread thread = mModel1.thread.getValue();
            if (thread == null || thread.user.blocked || thread.user.blocking) {
                return;
            }

            Intent intent = new Intent(requireContext(), StickerPickerActivity.class);
            startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_STICKER);
        });
        sticker.setVisibility(getResources().getBoolean(R.bool.stickers_enabled) ? View.VISIBLE : View.GONE);
        composer.findViewById(R.id.submit).setOnClickListener(v -> {
            Thread thread = mModel1.thread.getValue();
            if (thread == null || thread.user.blocked || thread.user.blocking) {
                return;
            }

            Editable message = input.getText();
            if (TextUtils.isEmpty(message)) {
                return;
            }

            submitMessage(message);
            input.setText(null);
        });
        SocialSpanUtil.apply(input, "", null);
        if (getResources().getBoolean(R.bool.autocomplete_enabled)) {
            AutocompleteUtil.setupForHashtags(requireContext(), input);
            AutocompleteUtil.setupForUsers(requireContext(), input);
        }
    }

    private void blockUnblock() {
        Thread thread = mModel1.thread.getValue();
        REST rest = MainApplication.getContainer().get(REST.class);
        Call<ResponseBody> call;
        //noinspection ConstantConditions
        if (thread.user.blocked) {
            call = rest.blockedUnblock(thread.user.id);
        } else {
            call = rest.blockedBlock(thread.user.id);
        }

        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Response<ResponseBody> response
            ) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Blocking/unblocking user returned " + code + '.');
                if (response != null && response.isSuccessful()) {
                    loadThread();
                }
            }

            @Override
            public void onFailure(
                    @Nullable Call<ResponseBody> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed to block/unblock user.", t);
            }
        });
    }

    private void confirmBlockUnblock() {
        Thread thread = mModel1.thread.getValue();
        if (thread == null) {
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(thread.user.blocked
                        ? R.string.confirmation_unblock_user
                        : R.string.confirmation_block_user)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                    dialog.dismiss();
                    blockUnblock();
                })
                .show();
    }

    private void confirmDelete(Message message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirmation_delete_message)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                    dialog.dismiss();
                    deleteMessage(message);
                })
                .show();
    }

    private void deleteMessage(Message message) {
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.messagesDestroy(mThread, message.id)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Deleting message returned " + code + '.');
                        if (response != null && response.isSuccessful()) {
                            MessageDataSource source = mModel1.factory.source.getValue();
                            if (source != null) {
                                source.invalidate();
                            }
                            mModel2.areThreadsInvalid = true;
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to send message in thread.", t);
                    }
                });
    }

    private void loadThread() {
        mModel1.state1.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.threadsShow(mThread)
                .enqueue(new Callback<Wrappers.Single<Thread>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Thread>> call,
                            @Nullable Response<Wrappers.Single<Thread>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Loading thread information returned " + code + '.');
                        if (response != null && response.isSuccessful()) {
                            mModel1.thread.postValue(response.body().data);
                            mModel1.state1.postValue(LoadingState.LOADED);
                        } else {
                            mModel1.state1.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Thread>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to load thread information.", t);
                        mModel1.state1.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static MessagesFragment newInstance(int thread, String title) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_THREAD, thread);
        arguments.putString(ARG_TITLE, title);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private void submitMessage(CharSequence text) {
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.messagesCreate(mThread, text.toString())
                .enqueue(new Callback<Wrappers.Single<Message>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Message>> call,
                            @Nullable Response<Wrappers.Single<Message>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Submitting message returned " + code + '.');
                        if (response != null) {
                            if (response != null && response.isSuccessful()) {
                                MessageDataSource source = mModel1.factory.source.getValue();
                                if (source != null) {
                                    source.invalidate();
                                }

                                mModel2.areThreadsInvalid = true;
                            } else if (response.code() == 403) {
                                loadThread();
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Message>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to send message in thread.", t);
                    }
                });
    }

    private class MessageAdapter extends PagedListAdapter<Message, MessageViewHolder> {

        private static final int TYPE_INBOX = 100;
        private static final int TYPE_OUTBOX = 101;

        protected MessageAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public int getItemViewType(int position) {
            Message message = getItem(position);
            return message.user.me ? TYPE_OUTBOX : TYPE_INBOX;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(type == TYPE_INBOX ? R.layout.item_message_in : R.layout.item_message_out, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = getItem(position);
            if (message.user.me) {
                holder.delete.setOnClickListener(v -> confirmDelete(message));
            } else {
                if (TextUtils.isEmpty(message.user.photo)) {
                    holder.photo.setActualImageResource(R.drawable.photo_placeholder);
                } else {
                    holder.photo.setImageURI(message.user.photo);
                }
                holder.photo.setOnClickListener(v -> showProfile(message.user.id));
                holder.username.setText('@' + message.user.username);
                holder.username.setOnClickListener(v -> showProfile(message.user.id));
                holder.verified.setVisibility(message.user.verified ? View.VISIBLE : View.GONE);
            }

            if (message.sticker != null) {
                holder.text.setVisibility(View.GONE);
                holder.sticker.setVisibility(View.VISIBLE);
                holder.sticker.setImageURI(message.sticker.image);
            } else {
                holder.text.setVisibility(View.VISIBLE);
                holder.sticker.setVisibility(View.GONE);
                SocialSpanUtil.apply(holder.text, message.body, MessagesFragment.this);
            }
            holder.when.setText(
                    DateUtils.getRelativeTimeSpanString(
                            requireContext(), message.createdAt.getTime(), true));
        }
    }

    public static class MessageFragmentViewModel extends ViewModel {

        public MessageFragmentViewModel(int thread) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new MessageDataSource.Factory(thread);
            state2 = Transformations.switchMap(factory.source, input -> input.state);
            messages = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Message>> messages;
        public final MessageDataSource.Factory factory;
        public final MutableLiveData<LoadingState> state1 = new MutableLiveData<>(LoadingState.IDLE);
        public final LiveData<LoadingState> state2;
        public final MutableLiveData<Thread> thread = new MutableLiveData<>();

        private static class Factory implements ViewModelProvider.Factory {

            private final int mThread;

            public Factory(int thread) {
                mThread = thread;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new MessagesFragment.MessageFragmentViewModel(mThread);
            }
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView username;
        public ImageView verified;
        public TextView text;
        public SimpleDraweeView sticker;
        public TextView when;
        public View delete;

        public MessageViewHolder(@NonNull View root) {
            super(root);
            username = root.findViewById(R.id.username);
            verified = root.findViewById(R.id.verified);
            photo = root.findViewById(R.id.photo);
            text = root.findViewById(R.id.text);
            sticker = root.findViewById(R.id.sticker);
            when = root.findViewById(R.id.when);
            delete = root.findViewById(R.id.delete);
        }
    }
}
