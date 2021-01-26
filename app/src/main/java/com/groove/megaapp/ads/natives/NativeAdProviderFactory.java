package com.groove.megaapp.ads.natives;

import android.content.Context;

import androidx.annotation.Nullable;

import com.groove.megaapp.data.models.Advertisement;

final public class NativeAdProviderFactory {

    @Nullable
    public static NativeAdProvider create(Context context, Advertisement ad, int count) {
        switch (ad.network) {
            case "admob":
                return new AdMobNativeAdProvider(ad, context, count);
            default:
                break;
        }

        return null;
    }
}
