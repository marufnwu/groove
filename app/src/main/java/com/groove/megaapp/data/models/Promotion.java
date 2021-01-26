package com.groove.megaapp.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Promotion implements Parcelable {

    public String id;
    public String title;
    public String description;
    public String image;
    public Date createdAt;
    public Date updatedAt;

    public static final Creator<Promotion> CREATOR = new Creator<Promotion>() {

        @Override
        public Promotion createFromParcel(Parcel in) {
            return new Promotion(in);
        }

        @Override
        public Promotion[] newArray(int size) {
            return new Promotion[size];
        }
    };

    public Promotion() {
    }

    protected Promotion(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        image = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(image);
    }
}
