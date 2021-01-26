package com.groove.megaapp.ads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import com.groove.megaapp.data.models.Advertisement;
import com.groove.megaapp.utils.SizeUtil;

public class BannerAdProvider {

    private final Advertisement mAd;

    public BannerAdProvider(Advertisement ad) {
        mAd = ad;
    }

    @Nullable
    public View create(Context context) {
        switch (mAd.network) {
            case "admob": {
                AdView ad = new AdView(context);
                ad.setAdSize(AdSize.BANNER);
                ad.setAdUnitId(mAd.unit);
                ad.loadAd(new AdRequest.Builder().build());
                return ad;
            }
            case "custom": {
                AppCompatImageView ad = new AppCompatImageView(context);
                ad.setLayoutParams(
                        new ViewGroup.LayoutParams(
                                SizeUtil.toPx(context.getResources(), 320),
                                SizeUtil.toPx(context.getResources(), 50)));
                ad.setScaleType(ImageView.ScaleType.FIT_CENTER);
                //noinspection ConstantConditions
                if (mAd.image.endsWith(".gif")) {
                    Glide.with(context)
                            .asGif()
                            .load(mAd.image)
                            .into(ad);
                } else {
                    Glide.with(context)
                            .load(mAd.image)
                            .into(ad);
                }

                ad.setOnClickListener(v ->
                    context.startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(mAd.link))));
                return ad;
            }
            default:
                return null;
        }
    }
}
