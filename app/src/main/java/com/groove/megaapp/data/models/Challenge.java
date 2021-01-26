package com.groove.megaapp.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.Date;

public class Challenge implements Parcelable {

    public String id;
    public String hashtag;
    public String image;
    @Nullable
    public String description;
    public Date createdAt;
    public Date updatedAt;

    public static final Creator<Challenge> CREATOR = new Creator<Challenge>() {
        @Override
        public Challenge createFromParcel(Parcel in) {
            return new Challenge(in);
        }

        @Override
        public Challenge[] newArray(int size) {
            return new Challenge[size];
        }
    };

    public Challenge() {
    }

    protected Challenge(Parcel in) {
        id = in.readString();
        hashtag = in.readString();
        image = in.readString();
        description = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(hashtag);
        dest.writeString(image);
        dest.writeString(description);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
