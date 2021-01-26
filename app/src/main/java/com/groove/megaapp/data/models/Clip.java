package com.groove.megaapp.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public class Clip implements Parcelable {

    public int id;
    public String video;
    public String screenshot;
    public String preview;
    @Nullable
    public String description;
    public String language;
    @JsonProperty("private")
    public boolean _private;
    public boolean comments;
    public int duration;
    @Nullable
    public String location;
    @Nullable
    public Double latitude;
    @Nullable
    public Double longitude;
    public Date createdAt;
    public Date updatedAt;
    public User user;
    @Nullable public Song song;
    public List<ClipSection> sections;
    public int viewsCount;
    public int likesCount;
    public int commentsCount;
    public boolean liked;
    public boolean saved;
    public List<String> hashtags;
    public List<User> mentions;

    @JsonIgnore
    public boolean ad;

    public Clip() {
    }

    protected Clip(Parcel in) {
        id = in.readInt();
        video = in.readString();
        screenshot = in.readString();
        preview = in.readString();
        description = in.readString();
        language = in.readString();
        _private = in.readByte() != 0;
        comments = in.readByte() != 0;
        duration = in.readInt();
        user = in.readParcelable(User.class.getClassLoader());
        song = in.readParcelable(Song.class.getClassLoader());
        sections = in.createTypedArrayList(ClipSection.CREATOR);
        viewsCount = in.readInt();
        likesCount = in.readInt();
        commentsCount = in.readInt();
        liked = in.readByte() != 0;
        saved = in.readByte() != 0;
        hashtags = in.createStringArrayList();
        mentions = in.createTypedArrayList(User.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(video);
        dest.writeString(screenshot);
        dest.writeString(preview);
        dest.writeString(description);
        dest.writeString(language);
        dest.writeByte((byte) (_private ? 1 : 0));
        dest.writeByte((byte) (comments ? 1 : 0));
        dest.writeInt(duration);
        dest.writeParcelable(user, flags);
        dest.writeParcelable(song, flags);
        dest.writeTypedList(sections);
        dest.writeInt(viewsCount);
        dest.writeInt(likesCount);
        dest.writeInt(commentsCount);
        dest.writeByte((byte) (liked ? 1 : 0));
        dest.writeByte((byte) (saved ? 1 : 0));
        dest.writeStringList(hashtags);
        dest.writeTypedList(mentions);
    }

    public static final Creator<Clip> CREATOR = new Creator<Clip>() {

        @Override
        public Clip createFromParcel(Parcel in) {
            return new Clip(in);
        }

        @Override
        public Clip[] newArray(int size) {
            return new Clip[size];
        }
    };
}
