package com.groove.megaapp.data.models;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class Comment {

    public int id;
    public String text;
    public Date createdAt;
    public Date updatedAt;
    public User user;
    public List<String> hashtags;
    public List<User> mentions;
    @Nullable
    public Sticker sticker;
}
