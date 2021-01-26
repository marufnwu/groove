package com.groove.megaapp.autocomplete;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.groove.megaapp.R;
import com.groove.megaapp.data.models.Hashtag;
import com.groove.megaapp.utils.TextFormatUtil;

public class HashtagAdapter extends RecyclerView.Adapter<HashtagViewHolder> {

    private final Context mContext;
    private List<Hashtag> mItems;
    private final OnClickListener mListener;

    protected HashtagAdapter(@NonNull Context context, @NonNull OnClickListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position) {
        final Hashtag hashtag = mItems.get(position);
        holder.name.setText("#" + hashtag.name);
        holder.clips.setText(
                mContext.getString(R.string.count_clips, TextFormatUtil.toShortNumber(hashtag.clips)));
        holder.itemView.setOnClickListener(v -> mListener.onHashtagClick(hashtag));
    }

    @NonNull
    @Override
    public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(mContext)
                .inflate(R.layout.item_hashtag_slim, parent, false);
        return new HashtagViewHolder(root);
    }

    public void submitData(List<Hashtag> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    interface OnClickListener {

        void onHashtagClick(Hashtag hashtag);
    }
}
