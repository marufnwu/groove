package com.groove.megaapp.ads.natives;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;

import java.util.ArrayList;
import java.util.List;

import com.groove.megaapp.R;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.data.models.Advertisement;

public class AdMobNativeAdProvider extends NativeAdProvider {

    private static final String TAG = "AdMobNativeAdProvider";

    private final List<UnifiedNativeAd> mAds = new ArrayList<>();
    private int mLayout = R.layout.view_native_admob_item;
    private int mPosition = 0;

    public AdMobNativeAdProvider(Advertisement ad, Context context, int count) {
        super(ad);
        AdLoader loader = new AdLoader.Builder(context, ad.unit)
                .forUnifiedNativeAd(mAds::add)
                .withAdListener(new AdListener() {

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.e(TAG, "Native ad from AdMob failed to load.\n" + error.toString());
                        state.postValue(LoadingState.ERROR);
                    }

                    @Override
                    public void onAdLoaded() {
                        Log.v(TAG, "Native ad from AdMob was loaded.");
                        state.postValue(LoadingState.LOADED);
                    }
                })
                .build();
        loader.loadAds(new AdRequest.Builder().build(), count);
    }

    @Nullable
    @Override
    public View create(LayoutInflater inflater, @Nullable ViewGroup parent) {
        if (mAds.isEmpty()) {
            return null;
        }

        View root = inflater.inflate(mLayout, parent, false);
        UnifiedNativeAdView adv = root.findViewById(R.id.ad);
        TextView advertiser = root.findViewById(R.id.advertiser);
        TextView body = root.findViewById(R.id.body);
        Button cta = root.findViewById(R.id.cta);
        TextView headline = root.findViewById(R.id.headline);
        ImageView icon = root.findViewById(R.id.icon);
        MediaView media = root.findViewById(R.id.media);
        RatingBar stars = root.findViewById(R.id.stars);
        TextView store = root.findViewById(R.id.store);
        media.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewAdded(View parent, View child) {
                if (child instanceof ImageView) {
                    ImageView image = (ImageView)child;
                    image.setAdjustViewBounds(true);
                }
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
            }
        });
        adv.setAdvertiserView(advertiser);
        adv.setBodyView(body);
        adv.setCallToActionView(cta);
        adv.setHeadlineView(headline);
        adv.setIconView(icon);
        adv.setMediaView(media);
        adv.setStarRatingView(stars);
        adv.setStoreView(store);
        int i = mPosition + 1;
        if (i > mAds.size() - 1) {
            i = 0;
        }

        UnifiedNativeAd ad = mAds.get(mPosition = i);
        if (ad.getAdvertiser() != null) {
            advertiser.setText(ad.getAdvertiser());
            advertiser.setVisibility(View.VISIBLE);
        } else {
            advertiser.setVisibility(View.GONE);
        }

        body.setText(ad.getBody());
        cta.setText(ad.getCallToAction());
        headline.setText(ad.getHeadline());
        if (ad.getIcon() != null) {
            icon.setImageDrawable(ad.getIcon().getDrawable());
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
        }

        MediaContent mc = ad.getMediaContent();
        if (mc != null) {
            media.setMediaContent(mc);
            media.setVisibility(View.VISIBLE);
        } else {
            media.setVisibility(View.GONE);
        }

        if (ad.getStarRating() != null) {
            stars.setRating(ad.getStarRating().floatValue());
            stars.setVisibility(View.VISIBLE);
        } else {
            stars.setVisibility(View.GONE);
        }

        if (ad.getStore() != null) {
            store.setText(ad.getStore());
            store.setVisibility(View.VISIBLE);
        } else {
            store.setVisibility(View.GONE);
        }

        return root;
    }

    public void setLayout(@LayoutRes int layout) {
        mLayout = layout;
    }
}
