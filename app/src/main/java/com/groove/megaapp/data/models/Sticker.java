package com.groove.megaapp.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Sticker implements Parcelable {

    public int id;
    public String image;
    public Date createdAt;
    public Date updatedAt;

    public Sticker() {
    }

    protected Sticker(Parcel in) {
        id = in.readInt();
        image = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(image);
    }

    public static final Creator<Sticker> CREATOR = new Creator<Sticker>() {

        @Override
        public Sticker createFromParcel(Parcel in) {
            return new Sticker(in);
        }

        @Override
        public Sticker[] newArray(int size) {
            return new Sticker[size];
        }
    };
}
