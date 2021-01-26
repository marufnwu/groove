package com.groove.megaapp.ads;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;

import com.groove.megaapp.activities.InterstitialAdActivity;
import com.groove.megaapp.data.models.Advertisement;

public class InterstitialAdProvider {

    private static final String TAG = "InterstitialAdProvider";

    private final Advertisement mAd;

    public InterstitialAdProvider(Advertisement ad) {
        mAd = ad;
    }

    @Nullable
    public Runnable create(Context context) {
        switch (mAd.network) {
            case "admob": {
                InterstitialAd ad = new InterstitialAd(context);
                ad.setAdUnitId(mAd.unit);
                ad.setAdListener(new AdListener() {

                    @Override
                    public void onAdClosed() {
                        ad.loadAd(new AdRequest.Builder().build());
                    }

                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.e(TAG, "Interstitial ad from AdMob failed to load.\n" + error.toString());
                    }

                    public void onAdLoaded() {
                        Log.v(TAG, "Interstitial ad from AdMob was loaded.");
                    }
                });
                ad.loadAd(new AdRequest.Builder().build());
                return () -> {
                    Log.v(TAG, "Showing interstitial ad; loaded: " + ad.isLoaded());
                    if (ad.isLoaded()) {
                        ad.show();
                    }
                };
            }
            case "custom":
                return () -> {
                    Intent intent = new Intent(context, InterstitialAdActivity.class);
                    intent.putExtra(InterstitialAdActivity.EXTRA_IMAGE, mAd.image);
                    intent.putExtra(InterstitialAdActivity.EXTRA_LINK, mAd.link);
                    context.startActivity(intent);
                };
            default:
                return null;
        }
    }

    public final int getInterval() {
        return mAd.getInterval();
    }
}
