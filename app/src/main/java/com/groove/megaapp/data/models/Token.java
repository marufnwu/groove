package com.groove.megaapp.data.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Token implements Parcelable {

    public String token;
    public boolean existing;

    public Token() {
    }

    protected Token(Parcel in) {
        token = in.readString();
        existing = in.readByte() != 0;
    }

    public static final Creator<Token> CREATOR = new Creator<Token>() {
        @Override
        public Token createFromParcel(Parcel in) {
            return new Token(in);
        }

        @Override
        public Token[] newArray(int size) {
            return new Token[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(token);
        dest.writeByte((byte) (existing ? 1 : 0));
    }
}
