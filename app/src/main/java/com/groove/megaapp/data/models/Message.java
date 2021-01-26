package com.groove.megaapp.data.models;

import androidx.annotation.Nullable;

import java.util.Date;

public class Message {

    public int id;
    public String body;
    public Date createdAt;
    public Date updatedAt;
    public Thread thread;
    public User user;
    @Nullable
    public Sticker sticker;
}
