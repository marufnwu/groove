package com.groove.megaapp.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class Song implements Parcelable {

    public int id;
    public String title;
    @Nullable
    public String artist;
    @Nullable
    public String album;
    public String audio;
    @Nullable
    public String cover;
    public int duration;
    @Nullable
    public String details;
    public Date createdAt;
    public Date updatedAt;
    public List<SongSection> sections;
    public int clipsCount;

    public Song() {
    }

    protected Song(Parcel in) {
        id = in.readInt();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        audio = in.readString();
        cover = in.readString();
        duration = in.readInt();
        clipsCount = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(audio);
        dest.writeString(cover);
        dest.writeInt(duration);
        dest.writeInt(clipsCount);
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {

        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
