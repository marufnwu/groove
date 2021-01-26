package com.groove.megaapp.autocomplete;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import com.groove.megaapp.R;

public class UserViewHolder extends RecyclerView.ViewHolder {

    public SimpleDraweeView photo;
    public TextView name;
    public TextView username;

    public UserViewHolder(@NonNull View root) {
        super(root);
        photo = root.findViewById(R.id.photo);
        name = root.findViewById(R.id.name);
        username = root.findViewById(R.id.username);
    }
}
