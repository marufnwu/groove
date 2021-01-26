package com.groove.megaapp.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class User implements Parcelable {

    public int id;
    public String name;
    @Nullable
    public String photo;
    public String username;
    @Nullable
    public String email;
    @Nullable
    public String phone;
    @Nullable
    public String bio;
    public boolean verified;
    @Nullable
    public String location;
    @Nullable
    public Double latitude;
    @Nullable
    public Double longitude;
    public Date createdAt;
    public Date updatedAt;
    public int followersCount;
    public int followedCount;
    public int clipsCount;
    public int likesCount;
    public int viewsCount;
    public boolean me;
    public boolean follower;
    public boolean followed;
    public boolean blocking;
    public boolean blocked;
    @Nullable
    public List<UserLink> links;

    public User() {
    }

    protected User(Parcel in) {
        id = in.readInt();
        name = in.readString();
        photo = in.readString();
        username = in.readString();
        email = in.readString();
        phone = in.readString();
        bio = in.readString();
        verified = in.readByte() != 0;
        followersCount = in.readInt();
        followedCount = in.readInt();
        clipsCount = in.readInt();
        likesCount = in.readInt();
        viewsCount = in.readInt();
        me = in.readByte() != 0;
        follower = in.readByte() != 0;
        followed = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(photo);
        dest.writeString(username);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeString(bio);
        dest.writeByte((byte) (verified ? 1 : 0));
        dest.writeInt(followersCount);
        dest.writeInt(followedCount);
        dest.writeInt(clipsCount);
        dest.writeInt(likesCount);
        dest.writeInt(viewsCount);
        dest.writeByte((byte) (me ? 1 : 0));
        dest.writeByte((byte) (follower ? 1 : 0));
        dest.writeByte((byte) (followed ? 1 : 0));
    }

    public static final Creator<User> CREATOR = new Creator<User>() {

        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public static class UserLink {

        public String type;
        public String url;
    }
}
