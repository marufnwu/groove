package com.groove.megaapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import com.groove.megaapp.R;
import com.groove.megaapp.ads.natives.AdMobNativeAdProvider;
import com.groove.megaapp.ads.natives.NativeAdProvider;
import com.groove.megaapp.ads.natives.NativeAdProviderFactory;
import com.groove.megaapp.common.LoadingState;
import com.groove.megaapp.common.VisibilityAware;
import com.groove.megaapp.data.models.Advertisement;
import com.groove.megaapp.events.PlaybackEndedEvent;

public class NativeAdFragment extends Fragment implements VisibilityAware {

    private static final String ARG_ADVERTISEMENT = "advertisement";

    private NativeAdProvider mAd;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRunnable =
            () -> EventBus.getDefault().post(new PlaybackEndedEvent(-1));

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Advertisement ad = requireArguments().getParcelable(ARG_ADVERTISEMENT);
        if (ad != null) {
            mAd = NativeAdProviderFactory.create(requireContext(), ad, 1);
            if (mAd instanceof AdMobNativeAdProvider) {
                ((AdMobNativeAdProvider) mAd).setLayout(R.layout.view_native_admob_full);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_native_ad, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean enabled = getResources().getBoolean(R.bool.auto_scroll_enabled)
                && getResources().getBoolean(R.bool.auto_scroll_ad_enabled);
        if (enabled) {
            int delay = getResources().getInteger(R.integer.auto_scroll_ad_delay);
            mHandler.postDelayed(mRunnable, TimeUnit.SECONDS.toMillis(delay));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View loading = view.findViewById(R.id.loading);
        View error = view.findViewById(R.id.error);
        if (mAd != null) {
            mAd.state.observe(getViewLifecycleOwner(), state -> {
                error.setVisibility(state == LoadingState.ERROR ? View.VISIBLE : View.GONE);
                loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
                if (state == LoadingState.LOADED) {
                    View ad = mAd.create(LayoutInflater.from(requireContext()), null);
                    if (ad != null) {
                        LinearLayout container = view.findViewById(R.id.container);
                        container.removeAllViews();
                        container.addView(ad);
                    }
                }
                if (state == LoadingState.ERROR) {
                    mHandler.postDelayed(mRunnable, 1000);
                }
            });
        } else {
            error.setVisibility(View.VISIBLE);
            loading.setVisibility(View.GONE);
            mHandler.postDelayed(mRunnable, 1000);
        }
    }

    public static NativeAdFragment newInstance(Advertisement ad) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_ADVERTISEMENT, ad);
        NativeAdFragment fragment = new NativeAdFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void setVisibleOrNot(boolean visible) {
    }
}
