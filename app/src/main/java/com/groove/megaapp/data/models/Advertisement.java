package com.groove.megaapp.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.Date;

import com.groove.megaapp.SharedConstants;

public class Advertisement implements Parcelable {

    public String id;
    public String location;
    public String network;
    public String type;
    @Nullable
    public String unit;
    @Nullable
    public String image;
    @Nullable
    public String link;
    @Nullable
    public Integer interval;
    public Date createdAt;
    public Date updatedAt;

    public static final Creator<Advertisement> CREATOR = new Creator<Advertisement>() {

        @Override
        public Advertisement createFromParcel(Parcel in) {
            return new Advertisement(in);
        }

        @Override
        public Advertisement[] newArray(int size) {
            return new Advertisement[size];
        }
    };

    public Advertisement() {
    }

    protected Advertisement(Parcel in) {
        id = in.readString();
        location = in.readString();
        network = in.readString();
        type = in.readString();
        unit = in.readString();
        image = in.readString();
        link = in.readString();
        if (in.readByte() == 0) {
            interval = null;
        } else {
            interval = in.readInt();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public final int getInterval() {
        return interval != null ? interval : SharedConstants.DEFAULT_PAGE_SIZE;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(location);
        dest.writeString(network);
        dest.writeString(type);
        dest.writeString(unit);
        dest.writeString(image);
        dest.writeString(link);
        if (interval == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(interval);
        }
    }
}
