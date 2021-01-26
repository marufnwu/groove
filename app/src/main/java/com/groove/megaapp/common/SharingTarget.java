package com.groove.megaapp.common;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

public enum SharingTarget {

    FACEBOOK("com.facebook.katana"),
    INSTAGRAM("com.instagram.android"),
    TWITTER("com.twitter.android"),
    WHATSAPP("com.whatsapp");

    @NonNull
    public final String pkg;

    SharingTarget(@NotNull String pkg) {
        this.pkg = pkg;
    }
}
