package com.groove.megaapp.ads.natives;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.models.Advertisement;

abstract public class NativeAdProvider {

    protected final Advertisement mAd;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.LOADING);

    public NativeAdProvider(Advertisement ad) {
        mAd = ad;
    }

    public final int getInterval() {
        return mAd.getInterval();
    }

    @Nullable
    public abstract View create(LayoutInflater inflater, @Nullable ViewGroup parent);
}
