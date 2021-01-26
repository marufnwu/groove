package com.groove.megaapp.autocomplete;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.groove.megaapp.R;
import com.groove.megaapp.data.models.User;

class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {

    private final Context mContext;
    private List<User> mItems;
    private final OnClickListener mListener;

    protected UserAdapter(@NonNull Context context, @NonNull OnClickListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        final User user = mItems.get(position);
        if (TextUtils.isEmpty(user.photo)) {
            holder.photo.setActualImageResource(R.drawable.photo_placeholder);
        } else {
            holder.photo.setImageURI(user.photo);
        }

        holder.name.setText(user.name);
        holder.username.setText("@" + user.username);
        holder.itemView.setOnClickListener(v -> mListener.onUserClick(user));
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(mContext)
                .inflate(R.layout.item_user_slim, parent, false);
        return new UserViewHolder(root);
    }

    public void submitData(List<User> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    interface OnClickListener {

        void onUserClick(User user);
    }
}
